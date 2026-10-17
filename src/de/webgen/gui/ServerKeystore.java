/* Copyright (C) 2020 Christoph Theis */

package de.webgen.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * Keystore (PKCS#12) with the private key and self-signed certificate for the HTTPS server.
 *
 * The keystore password is the name of the tournament database: the file is protected by the
 * file system, not by the password. PKCS#12 and keytool simply require one of at least 6 characters.
 */
class ServerKeystore {

    private static final int MIN_PASSWORD_LENGTH = 6;      // keytool's minimum

    /** Keystore password: the database name, filled up with '*' to at least 6 characters. */
    static String password(String databaseName) {
        StringBuilder sb = new StringBuilder(databaseName == null ? "" : databaseName.strip());
        while (sb.length() < MIN_PASSWORD_LENGTH)
            sb.append('*');
        return sb.toString();
    }

    static final String ALIAS = "webgen";

    /** File types that hold keys; never delivered by the web server. */
    static boolean isKeystoreFile(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(".p12") || n.endsWith(".pfx") || n.endsWith(".jks");
    }

    /**
     * TLS context for the HTTPS server with the key and certificate from the keystore.
     */
    static SSLContext createSslContext(File keystore, String password) throws IOException, GeneralSecurityException {
        if (keystore == null || keystore.getPath().isBlank())
            throw new IOException("No certificate configured");
        if (!keystore.isFile())
            throw new IOException("Certificate not found: " + keystore);

        char[] pwd = password.toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(keystore)) {
            ks.load(in, pwd);
        } catch (IOException e) {
            // PKCS#12 reports a wrong password as IOException
            throw new IOException("Cannot open certificate " + keystore + " (other database name?): " + e.getMessage(), e);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, pwd);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);
        return ctx;
    }

    /**
     * Creates a new keystore with a 2048 bit RSA key and a self-signed certificate,
     * valid for 10 years for localhost, this computer's name and its IP addresses.
     * Uses keytool from the running Java installation. The file must not exist.
     */
    static void generate(File keystore, String password) throws IOException, InterruptedException {
        if (keystore.exists())
            throw new IOException("File already exists: " + keystore);

        String exe = System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
        File keytool = new File(System.getProperty("java.home"), "bin" + File.separator + exe);
        if (!keytool.isFile())
            throw new IOException("keytool not found in " + keytool.getParent());

        File dir = keystore.getAbsoluteFile().getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs())
            throw new IOException("Cannot create directory " + dir);

        String hostName = localHostName();

        ProcessBuilder pb = new ProcessBuilder(
                keytool.getPath(), "-genkeypair",
                "-alias", ALIAS,
                "-keyalg", "RSA", "-keysize", "2048",
                "-validity", "3650",
                "-storetype", "PKCS12",
                "-keystore", keystore.getAbsolutePath(),
                "-dname", "CN=" + (hostName != null ? hostName : "localhost") + ", O=WebGen",
                "-ext", "SAN=" + subjectAlternativeNames(hostName),
                "-storepass:env", "WEBGEN_KEYSTORE_PASSWORD",
                "-noprompt");
        pb.environment().put("WEBGEN_KEYSTORE_PASSWORD", password);
        pb.redirectErrorStream(true);

        Process p = pb.start();
        // keytool writes in the console encoding
        String output = new String(p.getInputStream().readAllBytes(), consoleCharset());
        int rc = p.waitFor();

        if (rc != 0 || !keystore.isFile())
            throw new IOException("keytool failed (" + rc + "): " + output.strip());
    }

    // Names and addresses the certificate is valid for, so the browser can match the address in the URL
    static String subjectAlternativeNames(String hostName) {
        Set<String> san = new LinkedHashSet<>();
        san.add("dns:localhost");
        if (hostName != null)
            san.add("dns:" + hostName);
        san.add("ip:127.0.0.1");
        for (String ip : localAddresses())
            san.add("ip:" + ip);
        return String.join(",", san);
    }

    // Computer name, if it is a valid DNS name (keytool rejects e.g. underscores)
    private static String localHostName() {
        try {
            String name = InetAddress.getLocalHost().getHostName();
            if (name != null && name.matches("[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?"))
                return name;
        } catch (IOException e) {
            // no name, IP addresses only
        }
        return null;
    }

    // The same addresses the server panel offers: up, not loopback, not link-local
    private static List<String> localAddresses() {
        try {
            return NetworkInterface.networkInterfaces()
                .filter(ni -> {
                    try { return ni.isUp() && !ni.isLoopback(); }
                    catch (SocketException e) { return false; }
                })
                .flatMap(NetworkInterface::inetAddresses)
                .filter(addr -> !addr.isLoopbackAddress()
                             && !addr.isAnyLocalAddress()
                             && !addr.isLinkLocalAddress())
                .map(InetAddress::getHostAddress)
                .map(a -> a.contains("%") ? a.substring(0, a.indexOf('%')) : a)
                .collect(Collectors.toList());
        } catch (SocketException e) {
            return new ArrayList<>();
        }
    }

    private static Charset consoleCharset() {
        String enc = System.getProperty("stdout.encoding", System.getProperty("native.encoding"));
        try {
            return enc != null ? Charset.forName(enc) : Charset.defaultCharset();
        } catch (Exception e) {
            return Charset.defaultCharset();
        }
    }
}
