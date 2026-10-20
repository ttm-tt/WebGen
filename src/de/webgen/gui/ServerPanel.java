/* Copyright (C) 2020 Christoph Theis */

package de.webgen.gui;

import de.webgen.WebGen;
import de.webgen.WebGen.FlagType;
import de.webgen.WebGen.RefreshInterval;
import de.webgen.database.Competition;
import de.webgen.database.Group;
import de.webgen.generator.report.Report;
import de.webgen.gui.model.GroupsTableModel;
import de.webgen.gui.model.ReportsTableModel;
import java.awt.Font;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;
import com.joseprio.httpserver.DigestAuthenticator;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.Filter;
import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import javax.xml.XMLConstants;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;


public class ServerPanel extends javax.swing.JPanel {

    final static Charset UTF8 = Charset.forName("UTF-8");
    
    WebGen webGenerator;
    
    HttpServer httpServer;

    // Logging: each panel shows the messages of its own WebGen and the general ones
    private static final java.util.ResourceBundle logBundle = java.util.ResourceBundle.getBundle("de/webgen/gui/resources/WebGen"); // NOI18N
    private static final int MAX_LOG_ENTRIES = 100;

    // All received entries, newest first; jComboBoxLogging only shows those passing jComboBoxFilterLog
    private final java.util.LinkedList<LogEntry> logEntries = new java.util.LinkedList<>();
    private Handler logHandler;

    private static class LogEntry {
        final LogRecord record;
        final WebGen    server;    // null: general message, not from a WebGen thread

        LogEntry(LogRecord record, WebGen server) {
            this.record = record;
            this.server = server;
        }

        @Override
        public String toString() {
            String date = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(record.getMillis()));
            String message;

            if (record.getMessage() != null)
                message = (new java.util.logging.SimpleFormatter()).formatMessage(record);
            else if (record.getThrown() != null)
                message = record.getThrown().getClass().getName() + ": " + record.getThrown().getLocalizedMessage();
            else
                message = record.toString();

            return (server == null ? "All" : server.getServer()) + ": [" + date + "] " + message;
        }
    }

    /** Creates new form ServerPanel */
    public ServerPanel(String server, String tournament, String path) throws SocketException {
        this.webGenerator = new WebGen(server, tournament, path);
        
        initComponents();
        installPortCombo();
        installLogging();
        
        jTableGroups.setModel(new GroupsTableModel());
        
        jTableReports.setModel(new ReportsTableModel());
        
        jTextFieldHost.setText(webGenerator.getFtpHost());
        jTextFieldDir.setText(webGenerator.getFtpDirectory());
        jTextFieldUser.setText(webGenerator.getFtpUser());
        jPasswordFieldPwd.setText(webGenerator.getFtpPassword());
        jCheckBoxPassiveMode.setSelected(webGenerator.getFtpPassive());
        jCheckBoxBundle.setSelected(webGenerator.getFtpBundle());
        jTextFieldBundleName.setText(webGenerator.getFtpBundleName());

        try {
            Class.forName("at.co.ttm.ftp.FtpClient");
            jCheckBoxSftp.setSelected(webGenerator.getFtpSecure());
        } catch (Exception ex) {
            jCheckBoxSftp.setVisible(false);
        }

        jComboBoxInterval.setSelectedItem(webGenerator.getInterval());
        
        jTextFieldTitle.setText(webGenerator.getTitle());
        jTextFieldDescription.setText(webGenerator.getDescription());
        jTextFieldTournamentUrl.setText(webGenerator.getTournamentUrl());
        jTextFieldUserConfig.setText(webGenerator.getUserConfig());
        jCheckBoxLivtickerEnabled.setSelected(webGenerator.getLivetickerEnabled());
        jTextFieldNewsUrl.setText(webGenerator.getNewsUrl());
        // Not configured and "no certificate" both show an empty field
        String certificate = webGenerator.getCertificateFile();
        if (!certificate.isBlank()) {
            String relative = relativeCertificatePath(certificate);
            if (!relative.equals(certificate)) {
                // e.g. F:\...\Server1\private.p12 -> private.p12
                webGenerator.setCertificateFile(relative);
                webGenerator.storeSettings();
            }
            certificate = relative;
        }
        jTextFieldCertificate.setText(certificate);
        markCertificateField();
        jTextFieldCertificate.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { markCertificateField(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { markCertificateField(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { markCertificateField(); }
        });
        
        jRadioButtonFlagTypeNone.setActionCommand("None");
        jRadioButtonFlagTypeNation.setActionCommand("Nation");
        jRadioButtonFlagTypeRegion.setActionCommand("Region");
        
        buttonGroupFlags.setSelected(jRadioButtonFlagTypeNone.getModel(), webGenerator.getFlagType() == FlagType.None);
        buttonGroupFlags.setSelected(jRadioButtonFlagTypeNation.getModel(), webGenerator.getFlagType() == FlagType.Nation);
        buttonGroupFlags.setSelected(jRadioButtonFlagTypeRegion.getModel(), webGenerator.getFlagType() == FlagType.Region);
        
        jComboBoxEventActionPerformed(null);
        jComboBoxIntervalActionPerformed(null);
        
        ((ReportsTableModel) jTableReports.getModel()).setReports(webGenerator.getReports());

        (new java.util.Timer()).scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                // Update ProgressBar
                long when = ServerPanel.this.webGenerator.scheduledExectionTime();
                long now = System.currentTimeMillis();
                if (now < when) {
                    jProgressBar.setValue((int) ((when - now) / 1000));
                    jProgressBar.setString("" + jProgressBar.getValue() + " s");
                } else {
                    jProgressBar.setValue(0);
                    jProgressBar.setString("");
                }
                
                // Update Groups
                if (jTabbedPane.getSelectedComponent() == jPanelGroups) {
                    jTableGroups.invalidate();
                }
            }
        }, 1000, 1000);
        
        List<String> list = NetworkInterface.networkInterfaces()
            .filter(ni -> {
                try { return ni.isUp() && !ni.isLoopback(); }
                catch (SocketException e) { return false; }
            })
            .flatMap(NetworkInterface::inetAddresses)
            .filter(addr -> !addr.isLoopbackAddress()
                         && !addr.isAnyLocalAddress()
                         && !addr.isLinkLocalAddress())
            .map(InetAddress::getHostAddress)
            .collect(Collectors.toList());
        
        for (String addr : list)
            jComboBoxServer.addItem(addr);
        
        loadHttpServerSetting();

        // Password for /settings and /logs: not shown on start; an entered one stays visible while WebGen runs
        jTextFieldPassword.setToolTipText("Password for /settings and /logs from other computers (any user name). "
                + "Empty: keep the stored password");
        jTextFieldPassword.addActionListener(e -> commitHttpPassword());
        jTextFieldPassword.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                commitHttpPassword();
            }
        });

        SwingUtilities.invokeLater(this::startHttp);        
    }
    
    
    // HTTP Digest authentication for /settings and /logs: any user name, the password from the panel.
    // Digest never sends the password itself, so it is also usable over plain HTTP.
    // The password is read on every request, so a change applies without a restart.
    // Without a password the pages don't exist for other hosts (404)
    private class AdminAuthenticator extends DigestAuthenticator {
        AdminAuthenticator(String realm) {
            super(realm);
        }

        @Override
        public Result authenticate(HttpExchange exchange) {
            if (webGenerator.getHttpPassword().isEmpty())
                return new Failure(404);
            return super.authenticate(exchange);
        }

        // HA1 = MD5(user:realm:password) for whatever user name the browser sends,
        // so only the password counts
        @Override
        public String gethAuthToken(String username) {
            return calculateAuthToken(username == null ? "" : username, webGenerator.getHttpPassword());
        }
    }

    // Stores an entered password (encrypted) in index.xml; an empty field keeps the stored one
    private void commitHttpPassword() {
        String password = jTextFieldPassword.getText();
        if (password.isEmpty() || password.equals(webGenerator.getHttpPassword()))
            return;

        webGenerator.setHttpPassword(password);
        webGenerator.storeSettings();
        Logger.getLogger(ServerPanel.class.getName()).log(Level.INFO, "Password for /settings and /logs changed");
    }

    // The well-known HTTPS port uses TLS, all other ports plain HTTP
    private static final int HTTPS_PORT = 443;

    // URL of a page on the running server, for the browser on this computer
    private String serverUrl(String path) {
        InetAddress addr = httpServer.getAddress().getAddress();
        // Bound to all interfaces or loopback: localhost, which is also in the certificate
        String host = addr.isAnyLocalAddress() || addr.isLoopbackAddress() ? "localhost" : addr.getHostAddress();
        if (host.contains(":"))
            host = "[" + host + "]";       // IPv6

        boolean https = httpServer instanceof HttpsServer;
        int port = httpServer.getAddress().getPort();
        boolean defaultPort = https ? port == 443 : port == 80;

        return (https ? "https://" : "http://") + host + (defaultPort ? "" : ":" + port) + path;
    }

    void startHttp() {
        if (httpServer != null)
            stopHttp();
        
        String ip = jComboBoxServer.getSelectedItem().toString();
        if (ip.equals("localhost"))
            ip = "127.0.0.1";
        else if (ip.equals("any"))
            ip = "0.0.0.0";
        
        int port = currentPort;
        
        try {
            InetSocketAddress address = new InetSocketAddress(ip, port);
            if (port == HTTPS_PORT) {
                // HTTPS with the certificate from the settings. Load it before binding the port,
                // so a missing or broken certificate doesn't leave a half started server
                javax.net.ssl.SSLContext ssl = ServerKeystore.createSslContext(
                        resolveCertificateFile(webGenerator.getCertificateFile()),
                        ServerKeystore.password(webGenerator.getDatabaseName()));
                HttpsServer https = HttpsServer.create(address, 0);
                https.setHttpsConfigurator(new HttpsConfigurator(ssl));
                httpServer = https;
            } else {
                httpServer = HttpServer.create(address, 0);
            }
        } catch (IOException | java.security.GeneralSecurityException ex) {
               Logger.getLogger(ServerPanel.class.getName()).log(Level.SEVERE,
                       "Cannot start " + (port == HTTPS_PORT ? "HTTPS" : "HTTP") + " server: " + ex.getMessage(), ex);
               httpServer = null;
               jTextFieldPort.setText("");
               
               return;            
        }

        // Every request is written to <server dir>/access.log
        Filter accessLog = createAccessLogFilter();

        // index.xml and the access logs are only hidden if the server is reachable from other hosts
        final boolean hideRestrictedFiles = !httpServer.getAddress().getAddress().isLoopbackAddress();

        // /settings and /logs: on localhost freely, from other hosts only with the password
        {
            HttpContext settingsContext = httpServer.createContext("/settings", exchange -> {
                try (exchange) {
                    File xml = new File(webGenerator.getIndexXmlFile());
                    File xsl = new File(WebGen.templateDir, "webgensettings.xsl");

                    TransformerFactory tf = TransformerFactory.newInstance();
                    tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                    Templates templates = tf.newTemplates(new StreamSource(xsl));

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    // Read the file on every request, so you always see the current state
                    templates.newTransformer().transform(new StreamSource(xml), new StreamResult(out));
                    byte[] html = out.toByteArray();
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, html.length);
                    exchange.getResponseBody().write(html);
                } catch (Exception e) {
                    byte[] msg = ("Error: " + e.getMessage()).getBytes("UTF-8");
                    exchange.sendResponseHeaders(500, msg.length);
                    exchange.getResponseBody().write(msg);
                }
            });
            settingsContext.getFilters().add(accessLog);

            // Access log as readable HTML page; not logged itself, the auto-refresh would flood the log
            HttpContext logsContext = httpServer.createContext("/logs", exchange -> {
                try (exchange) {
                    byte[] html = AccessLogPage.render(getAccessLogFile(), webGenerator.getServer(),
                            exchange.getRequestURI().getRawQuery());
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.getResponseHeaders().set("Cache-Control", "no-store");
                    exchange.sendResponseHeaders(200, html.length);
                    exchange.getResponseBody().write(html);
                } catch (Exception e) {
                    byte[] msg = ("Error: " + e.getMessage()).getBytes(UTF8);
                    exchange.sendResponseHeaders(500, msg.length);
                    exchange.getResponseBody().write(msg);
                }
            });

            if (hideRestrictedFiles) {
                AdminAuthenticator auth = new AdminAuthenticator("WebGen " + webGenerator.getServer());
                settingsContext.setAuthenticator(auth);
                logsContext.setAuthenticator(auth);
            }
        }

        httpServer.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange he) throws IOException {
                if (!he.getRequestMethod().equals("GET")) {
                    he.getResponseHeaders().add("Allow", "GET");
                    sendErrorResponse(he, 405, "Method not allowed");

                    return;
                }

                String uri = he.getRequestURI().getPath();

                // Don't escape
                if (uri.contains("/../")) {
                    sendErrorResponse(he, 404, "Not found");

                    return;
                }

                String path = new File(webGenerator.getIndexHtmlFile()).getParent();

                File file = new File(path, uri);

                // Only files inside the server directory: on Windows an encoded backslash ("..%5C")
                // would otherwise get past the "/../" check above
                File canonical = file.getCanonicalFile();
                if (!canonical.toPath().startsWith(new File(path).getCanonicalFile().toPath())) {
                    sendErrorResponse(he, 404, "Not found");

                    return;
                }

                // Keys are never delivered, wherever they are
                if (ServerKeystore.isKeystoreFile(canonical.getName())) {
                    sendErrorResponse(he, 404, "Not found");

                    return;
                }

                // index.xml (settings incl. FTP account) and the access logs are in the same directory,
                // but must not be downloadable from other hosts
                if (hideRestrictedFiles && isRestrictedFile(file)) {
                    sendErrorResponse(he, 404, "Not found");

                    return;
                }

                if (!file.exists()) {
                    sendErrorResponse(he, 404, "Not found");

                    return;
                } else if (file.isDirectory()) {

                    File index = new File(file, "index.html");
                    if (!index.exists()) {
                        sendErrorResponse(he, 404, "Not found");

                        return;
                    }

                    file = index;
                }

                String mime = null;

                // Sometimes probeContentType will return text/plain for html and js files
                if (file.getName().endsWith(".html"))
                    mime = "text/html";
                else if (file.getName().endsWith(".js"))
                    mime = "text/javascript";
                else
                    mime = Files.probeContentType(file.toPath());

                he.getResponseHeaders().add("Content-Type", mime == null ? "application/octet-stream" : mime);

                he.sendResponseHeaders(200, file.length());
                byte b[] = new byte[0x10000];
                int  count;
                try (FileInputStream fis = new FileInputStream(file)) {
                    while ( (count = fis.read(b)) >= 0 )
                        he.getResponseBody().write(b, 0, count);
                }
                he.getResponseBody().close();
            }

            private void sendErrorResponse(HttpExchange he, int code, String message) {
                try {
                    byte[] response = message.getBytes(UTF8);
                    he.sendResponseHeaders(code, response.length);
                    he.getResponseBody().write(response);                                
                    he.getResponseBody().close();
                } catch (IOException ex) {
                    Logger.getLogger(ServerPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).getFilters().add(accessLog);

        httpServer.start();
        jTextFieldPort.setText("" + httpServer.getAddress().getPort());
    }
    
    
    // ---- Access log in Apache "combined" format ----
    // 127.0.0.1 - - [17/Oct/2026:18:48:00 +0200] "GET /index.html HTTP/1.1" 200 2326 "-" "Mozilla/5.0 ..."

    private static final java.time.format.DateTimeFormatter APACHE_TIME =
            java.time.format.DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", java.util.Locale.ENGLISH);

    private final Object accessLogLock = new Object();

    // Files in the server directory which the web server must not deliver
    private boolean isRestrictedFile(File file) throws IOException {
        File canonical = file.getCanonicalFile();
        if (!canonical.getParentFile().equals(getAccessLogFile().getCanonicalFile().getParentFile()))
            return false;

        String name = canonical.getName().toLowerCase(java.util.Locale.ROOT);
        return name.equals("index.xml") || name.startsWith("access.log");
    }

    private File getAccessLogFile() {
        return new File(new File(webGenerator.getIndexXmlFile()).getParentFile(), "access.log");
    }

    // Counts the bytes of the response body (%b)
    private static class CountingOutputStream extends java.io.FilterOutputStream {
        long count = 0;

        CountingOutputStream(java.io.OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            count++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            count += len;
        }
    }

    private Filter createAccessLogFilter() {
        return new Filter() {
            @Override
            public String description() {
                return "Access log (Apache combined format)";
            }

            @Override
            public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
                java.time.ZonedDateTime time = java.time.ZonedDateTime.now();
                CountingOutputStream body = new CountingOutputStream(exchange.getResponseBody());
                exchange.setStreams(null, body);

                try {
                    chain.doFilter(exchange);
                } finally {
                    writeAccessLog(exchange, time, body.count);
                }
            }
        };
    }

    private void writeAccessLog(HttpExchange ex, java.time.ZonedDateTime time, long bytes) {
        String user    = ex.getPrincipal() == null ? "-" : ex.getPrincipal().getUsername();
        String referer = ex.getRequestHeaders().getFirst("Referer");
        String agent   = ex.getRequestHeaders().getFirst("User-Agent");
        int    status  = ex.getResponseCode();     // -1 if the handler failed before sending a response

        // Browsers ask for /favicon.ico on their own; we usually don't have one, so don't log the failures
        if (status == 404 && "/favicon.ico".equals(ex.getRequestURI().getPath()))
            return;

        String line = ex.getRemoteAddress().getAddress().getHostAddress()
                + " - " + user
                + " [" + APACHE_TIME.format(time) + "]"
                + " \"" + ex.getRequestMethod() + " " + escapeLog(ex.getRequestURI().toString()) + " " + ex.getProtocol() + "\""
                + " " + (status < 0 ? "-" : Integer.toString(status))
                + " " + (bytes > 0 ? Long.toString(bytes) : "-")
                + " \"" + (referer == null ? "-" : escapeLog(referer)) + "\""
                + " \"" + (agent == null ? "-" : escapeLog(agent)) + "\""
                + "\n";

        synchronized (accessLogLock) {
            try {
                Files.writeString(getAccessLogFile().toPath(), line, UTF8,
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException e) {
                Logger.getLogger(ServerPanel.class.getName()).log(Level.WARNING, "Cannot write access log", e);
            }
        }
    }

    // Like Apache: quotes and backslashes escaped, control characters as \xhh
    private static String escapeLog(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\')
                sb.append('\\').append(c);
            else if (c < 0x20 || c == 0x7f)
                sb.append(String.format("\\x%02x", (int) c));
            else
                sb.append(c);
        }
        return sb.toString();
    }


    void stopHttp() {
        if (httpServer == null)
            return;
        
        httpServer.stop(5);
        httpServer = null;
        
        jTextFieldPort.setText("");
    }
    
    
    private void installLogging() {
        jComboBoxLogging.setModel(new javax.swing.DefaultComboBoxModel<>());

        jComboBoxLogging.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                java.awt.Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof LogEntry)
                    applyLevelStyle(c, ((LogEntry) value).record.getLevel());
                return c;
            }
        });

        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getMessage() == null && record.getThrown() == null)
                    return;

                // Messages of other servers belong to their own tab
                WebGen server = WebGen.threadIdMap.get(Thread.currentThread().getId());
                if (server != null && server != webGenerator)
                    return;

                record.setResourceBundle(logBundle);
                LogEntry entry = new LogEntry(record, server);

                // publish() is called from any thread, Swing must be updated on the EDT
                SwingUtilities.invokeLater(() -> addLogEntry(entry));
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        };

        Logger.getLogger("").addHandler(logHandler);

        // Display filter: 1st entry everything, 2nd INFO and above, 3rd WARNING and above
        jComboBoxFilterLog.addActionListener(e -> showLogEntries());
    }

    // Lowest level shown with the current filter
    private Level logFilterLevel() {
        switch (jComboBoxFilterLog.getSelectedIndex()) {
            case 1:  return Level.INFO;
            case 2:  return Level.WARNING;
            default: return Level.ALL;
        }
    }

    // Fills the log combo box with the stored entries that pass the filter, newest first
    private void showLogEntries() {
        int min = logFilterLevel().intValue();
        javax.swing.DefaultComboBoxModel<Object> model = new javax.swing.DefaultComboBoxModel<>();
        for (LogEntry e : logEntries)
            if (e.record.getLevel().intValue() >= min)
                model.addElement(e);
        jComboBoxLogging.setModel(model);

        if (model.getSize() > 0) {
            jComboBoxLogging.setSelectedIndex(0);
            applyLevelStyle(jComboBoxLogging, ((LogEntry) model.getElementAt(0)).record.getLevel());
        } else {
            applyLevelStyle(jComboBoxLogging, Level.INFO);
        }
    }


    // Stores every entry; only the display depends on the filter
    private void addLogEntry(LogEntry entry) {
        logEntries.addFirst(entry);
        while (logEntries.size() > MAX_LOG_ENTRIES)
            logEntries.removeLast();

        javax.swing.DefaultComboBoxModel model = (javax.swing.DefaultComboBoxModel) jComboBoxLogging.getModel();
        if (entry.record.getLevel().intValue() >= logFilterLevel().intValue()) {
            model.insertElementAt(entry, 0);
            jComboBoxLogging.setSelectedIndex(0);
            applyLevelStyle(jComboBoxLogging, entry.record.getLevel());
        }

        // Entries dropped from the store disappear from the display as well
        while (model.getSize() > 0 && !logEntries.contains(model.getElementAt(model.getSize() - 1)))
            model.removeElementAt(model.getSize() - 1);
    }


    private static void applyLevelStyle(java.awt.Component c, Level level) {
        if (Level.SEVERE.intValue() <= level.intValue()) {
            c.setForeground(java.awt.Color.RED);
            c.setFont(c.getFont().deriveFont(Font.PLAIN));
        } else if (Level.FINE.intValue() >= level.intValue()) {
            c.setForeground(java.awt.Color.DARK_GRAY);
            c.setFont(c.getFont().deriveFont(Font.ITALIC));
        } else {
            c.setForeground(java.awt.Color.BLACK);
            c.setFont(c.getFont().deriveFont(Font.PLAIN));
        }
    }


    // Called when the panel is removed, e.g. when another tournament is opened
    @Override
    public void removeNotify() {
        if (logHandler != null) {
            Logger.getLogger("").removeHandler(logHandler);
            logHandler = null;
        }
        super.removeNotify();
    }



    // Ports offered in the port combo box, in this order; any other port number can be typed in
    private static final java.util.LinkedHashMap<Integer, String> NAMED_PORTS = new java.util.LinkedHashMap<>();
    static {
        NAMED_PORTS.put(0,    "any");
        NAMED_PORTS.put(80,   "http");
        NAMED_PORTS.put(443,  "https");
        NAMED_PORTS.put(8080, "http alt");
    }
    private static final int LAST_PORT = 65535;

    private int     currentPort = 0;            // last valid choice in jComboBoxPort, 0 = any
    private boolean loadingHttpSettings = false; // suppresses the action events while the settings are loaded

    // ---- Address of the web server in index.xml: HTTPSERVER="ip:port" ----
    //   "127.0.0.1:8080"  localhost        "0.0.0.0:443" or ":443"  all interfaces
    //   "[2001:db8::1]:80" IPv6            port 0                   any free port

    // Selects the stored address and port in the combo boxes
    private void loadHttpServerSetting() {
        String value = webGenerator.getHttpServer().strip();
        if (value.isEmpty())
            return;                                 // not set yet: localhost, any port

        String ip = value;
        String portText = "0";
        if (value.startsWith("[")) {                // [IPv6]:port
            int end = value.indexOf(']');
            ip = end > 0 ? value.substring(1, end) : value.substring(1);
            if (end > 0 && value.startsWith(":", end + 1))
                portText = value.substring(end + 2);
        } else if (value.indexOf(':') == value.lastIndexOf(':') && value.indexOf(':') >= 0) {
            ip = value.substring(0, value.indexOf(':'));     // IPv4 or empty
            portText = value.substring(value.indexOf(':') + 1);
        }                                           // else: only an address (IPv6 without brackets or IPv4)

        int port = parsePort(portText);
        if (port < 0) {
            Logger.getLogger(ServerPanel.class.getName()).log(Level.WARNING, "Invalid port in HTTPSERVER=\"{0}\", using any", value);
            port = 0;
        }

        String item;
        if (ip.isEmpty() || ip.equals("0.0.0.0") || ip.equals("::"))
            item = "any";
        else if (ip.equals("127.0.0.1") || ip.equals("::1") || ip.equalsIgnoreCase("localhost"))
            item = "localhost";
        else
            item = ip;

        loadingHttpSettings = true;
        try {
            boolean available = false;
            for (int i = 0; i < jComboBoxServer.getItemCount(); i++)
                available |= jComboBoxServer.getItemAt(i).equals(item);

            if (available)
                jComboBoxServer.setSelectedItem(item);
            else
                // e.g. a laptop in another network; the stored value stays until the user chooses another one
                Logger.getLogger(ServerPanel.class.getName()).log(Level.WARNING,
                        "Address {0} of the web server is not available on this computer, using localhost", ip);

            currentPort = port;
            showCurrentPort();
        } finally {
            loadingHttpSettings = false;
        }
    }

    // Stores the current address and port in index.xml, if they changed
    private void storeHttpServerSetting() {
        String ip = bindAddress();
        if (ip.contains(":"))
            ip = "[" + ip + "]";                    // IPv6
        String value = ip + ":" + currentPort;

        if (!value.equals(webGenerator.getHttpServer())) {
            webGenerator.setHttpServer(value);
            webGenerator.storeSettings();
        }
    }
    private boolean updatingPortList = false;   // suppresses the action events while the list is rebuilt

    // Name of a port for the combo box: "http" for 80, otherwise the number
    private static String portLabel(int port) {
        return NAMED_PORTS.getOrDefault(port, Integer.toString(port));
    }

    // Port from the text in the combo box: a name ("http", any case) or a number 0..65535; -1 if invalid
    private static int parsePort(String text) {
        String t = text == null ? "" : text.strip();
        for (java.util.Map.Entry<Integer, String> e : NAMED_PORTS.entrySet())
            if (e.getValue().equalsIgnoreCase(t))
                return e.getKey();
        try {
            int port = Integer.parseInt(t);
            return port >= 0 && port <= LAST_PORT ? port : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Address the server binds to, from the server combo box
    private String bindAddress() {
        String ip = jComboBoxServer.getSelectedItem().toString();
        if (ip.equals("localhost"))
            return "127.0.0.1";
        if (ip.equals("any"))
            return "0.0.0.0";
        return ip;
    }

    // Can the server bind to this port on the selected address? Our own running server doesn't count.
    // Tested by binding a socket the same way the server does, so it also covers
    // ports reserved by Windows or missing privileges on Linux (< 1024)
    private boolean isPortFree(int port) {
        if (port == 0)
            return true;
        if (httpServer != null && httpServer.getAddress().getPort() == port)
            return true;

        try (java.net.ServerSocket socket = new java.net.ServerSocket()) {
            socket.bind(new InetSocketAddress(bindAddress(), port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Port combo box: list rebuilt each time it opens, so ports taken by other programs are left out
    private void installPortCombo() {
        fillPortList();
        jComboBoxPort.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                fillPortList();
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
            }
        });

        StringBuilder tip = new StringBuilder();
        for (java.util.Map.Entry<Integer, String> e : NAMED_PORTS.entrySet())
            tip.append(e.getKey() == 0 ? e.getValue() : e.getValue() + " (" + e.getKey() + ")").append(", ");
        jComboBoxPort.setToolTipText(tip + "or type a port number (0 - " + LAST_PORT + ")");
    }

    // any, the named ports not used by another program, and an empty entry to type an own port number
    private void fillPortList() {
        updatingPortList = true;
        try {
            javax.swing.DefaultComboBoxModel<String> model = new javax.swing.DefaultComboBoxModel<>();
            // Configured without certificate: https is not offered (typing it asks to create one)
            boolean noHttps = webGenerator.isCertificateConfigured() && webGenerator.getCertificateFile().isBlank();
            for (java.util.Map.Entry<Integer, String> e : NAMED_PORTS.entrySet()) {
                if (e.getKey() == HTTPS_PORT && noHttps && currentPort != HTTPS_PORT)
                    continue;
                if (e.getKey() == currentPort || isPortFree(e.getKey()))
                    model.addElement(e.getValue());
            }
            model.addElement("");
            jComboBoxPort.setModel(model);
            jComboBoxPort.setSelectedItem(portLabel(currentPort));
        } finally {
            updatingPortList = false;
        }
    }

    // Shows the current port again, e.g. after an invalid input
    private void showCurrentPort() {
        updatingPortList = true;
        try {
            jComboBoxPort.setSelectedItem(portLabel(currentPort));
        } finally {
            updatingPortList = false;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupFlags = new javax.swing.ButtonGroup();
        jTabbedPane = new javax.swing.JTabbedPane();
        jPanelSettings = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jTextFieldTitle = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jTextFieldTournamentUrl = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jTextFieldNewsUrl = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        jTextFieldDescription = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jTextFieldUserConfig = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jRadioButtonFlagTypeNone = new javax.swing.JRadioButton();
        jRadioButtonFlagTypeNation = new javax.swing.JRadioButton();
        jRadioButtonFlagTypeRegion = new javax.swing.JRadioButton();
        jLabel16 = new javax.swing.JLabel();
        jCheckBoxLivtickerEnabled = new javax.swing.JCheckBox();
        jButtonLivetickerProperties = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jComboBoxInterval = new javax.swing.JComboBox();
        jPanel2 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jComboBoxServer = new javax.swing.JComboBox<>();
        jLabel11 = new javax.swing.JLabel();
        jComboBoxPort = new javax.swing.JComboBox<>();
        jTextFieldPort = new javax.swing.JTextField();
        jLabelCertificate = new javax.swing.JLabel();
        jTextFieldCertificate = new javax.swing.JTextField();
        jButtonCertificate = new javax.swing.JButton();
        jButtonGenerateCertificate = new javax.swing.JButton();
        jLabel17 = new javax.swing.JLabel();
        jTextFieldPassword = new javax.swing.JTextField();
        jPanelGroups = new javax.swing.JPanel();
        jPanel20 = new javax.swing.JPanel();
        jComboBoxEvent = new javax.swing.JComboBox();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTableGroups = new javax.swing.JTable();
        jTableGroups.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                // Reset values before changing them
                setBackground(null);
                setForeground(null);

                java.awt.Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Group gr = ((de.webgen.gui.model.GroupsTableModel) table.getModel()).getGroup(row);

                if (isSelected) {
                    // Nothing
                } else if (webGenerator.isGroupDisabled(gr)) {
                    comp.setBackground(java.awt.Color.RED);
                } else if (webGenerator.isGroupEnabled(gr)) {
                    comp.setBackground(java.awt.Color.GREEN);
                } else if (!webGenerator.isGroupPublished(gr)) {
                    comp.setForeground(java.awt.Color.GRAY);
                    comp.setFont(comp.getFont().deriveFont(Font.ITALIC));
                }

                return comp;
            }
        });
        jLabel3 = new javax.swing.JLabel();
        jButtonGroupAuto = new javax.swing.JButton();
        jButtonGroupEnable = new javax.swing.JButton();
        jButtonGroupDisable = new javax.swing.JButton();
        jCheckBoxIncludePlayedGroups = new javax.swing.JCheckBox();
        jPanelReports = new javax.swing.JPanel();
        jPanelLists = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTableReports = new javax.swing.JTable();
        jTableReports.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                // Reset colors
                setForeground(null);
                setBackground(null);

                java.awt.Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Report rep = ((de.webgen.gui.model.ReportsTableModel) table.getModel()).getReport(row);

                if (isSelected) {

                } else if (!webGenerator.isReportActive(rep)) {
                    comp.setForeground(java.awt.Color.GRAY);
                    comp.setFont(comp.getFont().deriveFont(Font.ITALIC));
                }

                return comp;
            }
        });
        jLabel6 = new javax.swing.JLabel();
        jButtonReportsEnable = new javax.swing.JButton();
        jButtonReportsDisable = new javax.swing.JButton();
        jPanelFtp = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldHost = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldDir = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldUser = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jPasswordFieldPwd = new javax.swing.JPasswordField();
        jCheckBoxPassiveMode = new javax.swing.JCheckBox();
        jCheckBoxSftp = new javax.swing.JCheckBox();
        jLabelBundleName = new javax.swing.JLabel();
        jTextFieldBundleName = new javax.swing.JTextField();
        jCheckBoxBundle = new javax.swing.JCheckBox();
        jCheckBoxFtpDebug = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        jComboBoxLogging = new javax.swing.JComboBox();
        jButtonStart = new javax.swing.JButton();
        jProgressBar = new javax.swing.JProgressBar();
        jButtonView = new javax.swing.JButton();
        jButtonOpen = new javax.swing.JButton();
        jButtonClear = new javax.swing.JButton();
        jComboBoxFilterLog = new javax.swing.JComboBox<>();

        setMinimumSize(new java.awt.Dimension(470, 500));

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("de/webgen/gui/resources/WebGen"); // NOI18N
        jTabbedPane.setName(bundle.getString("Reports")); // NOI18N

        jPanelSettings.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jLabel12.setText(bundle.getString("Title")); // NOI18N

        jLabel13.setText(bundle.getString("Tournament Url")); // NOI18N

        jLabel14.setText(bundle.getString("News Url")); // NOI18N

        jLabel15.setText(bundle.getString("Description")); // NOI18N

        jLabel8.setText("User Config");

        jLabel9.setText("Flags");

        buttonGroupFlags.add(jRadioButtonFlagTypeNone);
        jRadioButtonFlagTypeNone.setText("None");
        jRadioButtonFlagTypeNone.setBorder(null);
        jRadioButtonFlagTypeNone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonFlagTypeNoneActionPerformed(evt);
            }
        });

        buttonGroupFlags.add(jRadioButtonFlagTypeNation);
        jRadioButtonFlagTypeNation.setText("Nation");
        jRadioButtonFlagTypeNation.setBorder(null);

        buttonGroupFlags.add(jRadioButtonFlagTypeRegion);
        jRadioButtonFlagTypeRegion.setText("Region");
        jRadioButtonFlagTypeRegion.setBorder(null);

        jLabel16.setText(bundle.getString("Liveticker")); // NOI18N

        jCheckBoxLivtickerEnabled.setText(bundle.getString("Enabled")); // NOI18N
        jCheckBoxLivtickerEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxLivtickerEnabledActionPerformed(evt);
            }
        });

        jButtonLivetickerProperties.setText("...");
        jButtonLivetickerProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLivetickerPropertiesActionPerformed(evt);
            }
        });

        jLabel7.setText(bundle.getString("Interval")); // NOI18N

        jComboBoxInterval.setModel(new javax.swing.DefaultComboBoxModel(de.webgen.WebGen.getIntervals()));
        jComboBoxInterval.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxIntervalActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel16Layout.createSequentialGroup()
                                    .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGap(10, 10, 10))
                                .addGroup(jPanel16Layout.createSequentialGroup()
                                    .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel16Layout.createSequentialGroup()
                                            .addComponent(jLabel15)
                                            .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                            .addGroup(jPanel16Layout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addGap(30, 30, 30)))
                        .addGroup(jPanel16Layout.createSequentialGroup()
                            .addComponent(jLabel9)
                            .addGap(61, 61, 61)))
                    .addGroup(jPanel16Layout.createSequentialGroup()
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel16)
                            .addComponent(jLabel7))
                        .addGap(48, 48, 48)))
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel16Layout.createSequentialGroup()
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel16Layout.createSequentialGroup()
                                .addComponent(jCheckBoxLivtickerEnabled)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jButtonLivetickerProperties))
                            .addComponent(jComboBoxInterval, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jTextFieldDescription)
                    .addComponent(jTextFieldTournamentUrl)
                    .addComponent(jTextFieldNewsUrl)
                    .addGroup(jPanel16Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 133, Short.MAX_VALUE)
                        .addComponent(jRadioButtonFlagTypeNone)
                        .addGap(18, 18, 18)
                        .addComponent(jRadioButtonFlagTypeNation)
                        .addGap(18, 18, 18)
                        .addComponent(jRadioButtonFlagTypeRegion)
                        .addGap(0, 17, Short.MAX_VALUE))
                    .addComponent(jTextFieldUserConfig, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jTextFieldTitle, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldTitle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldDescription, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldTournamentUrl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldNewsUrl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jTextFieldUserConfig, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jRadioButtonFlagTypeNone)
                    .addComponent(jRadioButtonFlagTypeNation)
                    .addComponent(jRadioButtonFlagTypeRegion))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(jCheckBoxLivtickerEnabled)
                    .addComponent(jButtonLivetickerProperties))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBoxInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel10.setText("HTTP Server");

        jComboBoxServer.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "localhost", "any" }));
        jComboBoxServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxServerActionPerformed(evt);
            }
        });

        jLabel11.setText("Port");

        jComboBoxPort.setEditable(true);
        jComboBoxPort.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "any", "http", "https", "http alt", "" }));
        jComboBoxPort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxPortActionPerformed(evt);
            }
        });

        jTextFieldPort.setEditable(false);
        jTextFieldPort.setColumns(5);
        jTextFieldPort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldPortActionPerformed(evt);
            }
        });

        jLabelCertificate.setText("Certificate");

        jTextFieldCertificate.setToolTipText("Keystore (PKCS#12: .p12 / .pfx) with certificate and private key for HTTPS");
        jTextFieldCertificate.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextFieldCertificateFocusLost(evt);
            }
        });
        jTextFieldCertificate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldCertificateActionPerformed(evt);
            }
        });

        jButtonCertificate.setText("...");
        jButtonCertificate.setToolTipText("Choose the keystore file");
        jButtonCertificate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCertificateActionPerformed(evt);
            }
        });

        jButtonGenerateCertificate.setText("Generate");
        jButtonGenerateCertificate.setToolTipText("Create a new key and self-signed certificate in this file");
        jButtonGenerateCertificate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGenerateCertificateActionPerformed(evt);
            }
        });

        jLabel17.setText("Password");

        jTextFieldPassword.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldPasswordActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addGap(34, 34, 34)
                        .addComponent(jComboBoxServer, 0, 116, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxPort, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel17)
                            .addComponent(jLabelCertificate))
                        .addGap(45, 45, 45)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextFieldCertificate)
                            .addComponent(jTextFieldPassword, javax.swing.GroupLayout.DEFAULT_SIZE, 226, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonCertificate)))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonGenerateCertificate))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel10)
                        .addComponent(jComboBoxServer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel11)
                        .addComponent(jComboBoxPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelCertificate)
                    .addComponent(jTextFieldCertificate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonCertificate)
                    .addComponent(jButtonGenerateCertificate))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanelSettingsLayout = new javax.swing.GroupLayout(jPanelSettings);
        jPanelSettings.setLayout(jPanelSettingsLayout);
        jPanelSettingsLayout.setHorizontalGroup(
            jPanelSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelSettingsLayout.setVerticalGroup(
            jPanelSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane.addTab(bundle.getString("Settings"), jPanelSettings); // NOI18N

        jPanelGroups.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jComboBoxEvent.setModel(new javax.swing.DefaultComboBoxModel(webGenerator.getEvents()));
        jComboBoxEvent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxEventActionPerformed(evt);
            }
        });

        jTableGroups.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane3.setViewportView(jTableGroups);

        jLabel3.setText("With selected:");

        jButtonGroupAuto.setText("Auto");
        jButtonGroupAuto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGroupAutoActionPerformed(evt);
            }
        });

        jButtonGroupEnable.setText("Enable");
        jButtonGroupEnable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGroupEnableActionPerformed(evt);
            }
        });

        jButtonGroupDisable.setText("Disable");
        jButtonGroupDisable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGroupDisableActionPerformed(evt);
            }
        });

        jCheckBoxIncludePlayedGroups.setText(bundle.getString("Include Groups with Played Matches")); // NOI18N
        jCheckBoxIncludePlayedGroups.setSelected(webGenerator.getIncludeGroups());
        jCheckBoxIncludePlayedGroups.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxIncludePlayedGroupsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel20Layout = new javax.swing.GroupLayout(jPanel20);
        jPanel20.setLayout(jPanel20Layout);
        jPanel20Layout.setHorizontalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel20Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jCheckBoxIncludePlayedGroups)
                    .addComponent(jComboBoxEvent, 0, 434, Short.MAX_VALUE)
                    .addGroup(jPanel20Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonGroupAuto)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonGroupEnable)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonGroupDisable)))
                .addContainerGap())
        );
        jPanel20Layout.setVerticalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel20Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jComboBoxEvent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 263, Short.MAX_VALUE)
                .addGap(16, 16, 16)
                .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jButtonGroupAuto)
                    .addComponent(jButtonGroupEnable)
                    .addComponent(jButtonGroupDisable))
                .addGap(18, 18, 18)
                .addComponent(jCheckBoxIncludePlayedGroups)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanelGroupsLayout = new javax.swing.GroupLayout(jPanelGroups);
        jPanelGroups.setLayout(jPanelGroupsLayout);
        jPanelGroupsLayout.setHorizontalGroup(
            jPanelGroupsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelGroupsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(19, 19, 19))
        );
        jPanelGroupsLayout.setVerticalGroup(
            jPanelGroupsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelGroupsLayout.createSequentialGroup()
                .addComponent(jPanel20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane.addTab(bundle.getString("Groups"), jPanelGroups); // NOI18N

        jPanelReports.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jTableReports.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jTableReports.setTableHeader(null);
        jScrollPane6.setViewportView(jTableReports);

        jLabel6.setText("With selected:");

        jButtonReportsEnable.setText("Enable");
        jButtonReportsEnable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReportsEnableActionPerformed(evt);
            }
        });

        jButtonReportsDisable.setText("Disable");
        jButtonReportsDisable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReportsDisableActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelListsLayout = new javax.swing.GroupLayout(jPanelLists);
        jPanelLists.setLayout(jPanelListsLayout);
        jPanelListsLayout.setHorizontalGroup(
            jPanelListsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelListsLayout.createSequentialGroup()
                .addGroup(jPanelListsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelListsLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonReportsEnable)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonReportsDisable))
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 445, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelListsLayout.setVerticalGroup(
            jPanelListsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelListsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 149, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(jPanelListsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jButtonReportsEnable)
                    .addComponent(jButtonReportsDisable))
                .addGap(196, 196, 196))
        );

        javax.swing.GroupLayout jPanelReportsLayout = new javax.swing.GroupLayout(jPanelReports);
        jPanelReports.setLayout(jPanelReportsLayout);
        jPanelReportsLayout.setHorizontalGroup(
            jPanelReportsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelReportsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelLists, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(14, 14, 14))
        );
        jPanelReportsLayout.setVerticalGroup(
            jPanelReportsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelLists, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jTabbedPane.addTab(bundle.getString("Reports"), jPanelReports); // NOI18N

        jPanelFtp.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jLabel1.setText(bundle.getString("Server")); // NOI18N

        jLabel2.setText(bundle.getString("Directory")); // NOI18N

        jLabel4.setText(bundle.getString("User")); // NOI18N

        jLabel5.setText(bundle.getString("Password")); // NOI18N

        jCheckBoxPassiveMode.setText(bundle.getString("Passive mode")); // NOI18N
        jCheckBoxPassiveMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxPassiveModeActionPerformed(evt);
            }
        });

        jCheckBoxSftp.setText("Secure ftp");
        jCheckBoxSftp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxSftpActionPerformed(evt);
            }
        });

        jLabelBundleName.setText("Bundle name");

        jCheckBoxBundle.setText("Bundle upload");
        jCheckBoxBundle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxBundleActionPerformed(evt);
            }
        });

        jCheckBoxFtpDebug.setText(bundle.getString("Debug")); // NOI18N
        jCheckBoxFtpDebug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxFtpDebugActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabelBundleName)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBoxFtpDebug, javax.swing.GroupLayout.DEFAULT_SIZE, 305, Short.MAX_VALUE)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jTextFieldDir, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jTextFieldUser, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPasswordFieldPwd, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jTextFieldBundleName, javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBoxSftp)
                            .addComponent(jCheckBoxBundle)
                            .addComponent(jCheckBoxPassiveMode, javax.swing.GroupLayout.DEFAULT_SIZE, 307, Short.MAX_VALUE))
                        .addComponent(jTextFieldHost)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jCheckBoxFtpDebug, jCheckBoxPassiveMode});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextFieldHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jTextFieldUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jPasswordFieldPwd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelBundleName)
                    .addComponent(jTextFieldBundleName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                .addComponent(jCheckBoxFtpDebug)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxPassiveMode)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxSftp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxBundle)
                .addGap(10, 10, 10))
        );

        javax.swing.GroupLayout jPanelFtpLayout = new javax.swing.GroupLayout(jPanelFtp);
        jPanelFtp.setLayout(jPanelFtpLayout);
        jPanelFtpLayout.setHorizontalGroup(
            jPanelFtpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFtpLayout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(34, 34, 34))
        );
        jPanelFtpLayout.setVerticalGroup(
            jPanelFtpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFtpLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(122, Short.MAX_VALUE))
        );

        jTabbedPane.addTab(bundle.getString("Ftp"), jPanelFtp); // NOI18N

        jButtonStart.setText(bundle.getString("Start")); // NOI18N
        jButtonStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStartActionPerformed(evt);
            }
        });

        jButtonView.setText(bundle.getString("View")); // NOI18N
        jButtonView.setToolTipText(bundle.getString("View with external browser")); // NOI18N
        jButtonView.setEnabled(java.awt.Desktop.isDesktopSupported());
        jButtonView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonViewActionPerformed(evt);
            }
        });

        jButtonOpen.setText(bundle.getString("Open")); // NOI18N
        jButtonOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOpenActionPerformed(evt);
            }
        });

        jButtonClear.setText(bundle.getString("Clear")); // NOI18N
        jButtonClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearActionPerformed(evt);
            }
        });

        jComboBoxFilterLog.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All", "Info", "Error" }));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jButtonStart)
                        .addGap(11, 11, 11)
                        .addComponent(jProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 219, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jComboBoxLogging, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jComboBoxFilterLog, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonView, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButtonClear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonOpen, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonStart)
                    .addComponent(jProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButtonView)
                        .addComponent(jButtonOpen)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBoxLogging, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonClear)
                    .addComponent(jComboBoxFilterLog, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane)
                .addGap(24, 24, 24)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane.getAccessibleContext().setAccessibleName(bundle.getString("Settings")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxFtpDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxFtpDebugActionPerformed
        webGenerator.setFtpDebug(jCheckBoxFtpDebug.isSelected());
    }//GEN-LAST:event_jCheckBoxFtpDebugActionPerformed

    private void jCheckBoxPassiveModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxPassiveModeActionPerformed
        webGenerator.setFtpPassive(jCheckBoxPassiveMode.isSelected());
    }//GEN-LAST:event_jCheckBoxPassiveModeActionPerformed

    private void jCheckBoxIncludePlayedGroupsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxIncludePlayedGroupsActionPerformed
        webGenerator.setIncludeGroups(jCheckBoxIncludePlayedGroups.isSelected());
    }//GEN-LAST:event_jCheckBoxIncludePlayedGroupsActionPerformed

    private void jComboBoxEventActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxEventActionPerformed
        GroupsTableModel model = (GroupsTableModel) jTableGroups.getModel();

        Competition cp = (Competition) jComboBoxEvent.getSelectedItem();

        Group[] groups = webGenerator.getGroups(cp);

        Arrays.sort(groups, new java.util.Comparator<Group>() {
            @Override
            public int compare(Group o1, Group o2) {
                return o1.compareTo(o2);
            }
        });

        model.setGroups(groups);

        model.fireTableDataChanged();
    }//GEN-LAST:event_jComboBoxEventActionPerformed

    private void jButtonGroupAutoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGroupAutoActionPerformed
        for (int row : jTableGroups.getSelectedRows()) {
            Group gr = ((GroupsTableModel) jTableGroups.getModel()).getGroup(row);
            webGenerator.setGroupEnabled(gr, false);
            webGenerator.setGroupDisabled(gr, false);
        }
    }//GEN-LAST:event_jButtonGroupAutoActionPerformed

    private void jButtonGroupEnableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGroupEnableActionPerformed
        for (int row : jTableGroups.getSelectedRows()) {
            Group gr = ((GroupsTableModel) jTableGroups.getModel()).getGroup(row);
            webGenerator.setGroupEnabled(gr, true);
            webGenerator.setGroupDisabled(gr, false);
        }
    }//GEN-LAST:event_jButtonGroupEnableActionPerformed

    private void jButtonGroupDisableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGroupDisableActionPerformed
        for (int row : jTableGroups.getSelectedRows()) {
            Group gr = ((GroupsTableModel) jTableGroups.getModel()).getGroup(row);
            webGenerator.setGroupDisabled(gr, true);
            webGenerator.setGroupEnabled(gr, false);
        }
    }//GEN-LAST:event_jButtonGroupDisableActionPerformed

    private void jButtonReportsEnableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReportsEnableActionPerformed
        for (int row : jTableReports.getSelectedRows()) {
            Report rep = ((ReportsTableModel) jTableReports.getModel()).getReport(row);
            webGenerator.setReportActive(rep, true);
        }
    }//GEN-LAST:event_jButtonReportsEnableActionPerformed

    private void jButtonReportsDisableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReportsDisableActionPerformed
        for (int row : jTableReports.getSelectedRows()) {
            Report rep = ((ReportsTableModel) jTableReports.getModel()).getReport(row);
            webGenerator.setReportActive(rep, false);
        }
    }//GEN-LAST:event_jButtonReportsDisableActionPerformed

    private void jCheckBoxSftpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxSftpActionPerformed
        webGenerator.setFtpSecure(jCheckBoxSftp.isSelected());
    }//GEN-LAST:event_jCheckBoxSftpActionPerformed

    private void jCheckBoxBundleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxBundleActionPerformed
        webGenerator.setFtpBundle(jCheckBoxBundle.isSelected());
    }//GEN-LAST:event_jCheckBoxBundleActionPerformed

    private void jComboBoxServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxServerActionPerformed
        if (loadingHttpSettings)
            return;

        storeHttpServerSetting();

        String ip = jComboBoxServer.getSelectedItem().toString();
        if (ip.equals("localhost"))
            ip = "127.0.0.1";
        else if (ip.equals("any"))
            ip = "0.0.0.0";
        
        if (httpServer != null)
        {
            if (httpServer.getAddress().getAddress().getHostAddress().equals(ip))
                return;
        }
        
        startHttp();
    }//GEN-LAST:event_jComboBoxServerActionPerformed

    private void jComboBoxPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxPortActionPerformed
        if (updatingPortList)
            return;

        Object item = jComboBoxPort.getSelectedItem();
        String text = item == null ? "" : item.toString().strip();

        // The empty entry: the user is going to type a port number
        if (text.isEmpty())
            return;

        int port = parsePort(text);
        if (port < 0) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            showCurrentPort();
            return;
        }

        // While the certificate question is open (focus changes fire this event again)
        if (checkingCertificate)
            return;

        if (port != currentPort && !isPortFree(port)) {
            Logger.getLogger(ServerPanel.class.getName()).log(Level.WARNING,
                    "Port {0} is used by another program", Integer.toString(port));
            java.awt.Toolkit.getDefaultToolkit().beep();
            showCurrentPort();
            return;
        }

        // HTTPS needs a usable certificate; if there is none, ask to create one
        if (port == HTTPS_PORT && (port != currentPort || httpServer == null) && !checkCertificateForHttps()) {
            showCurrentPort();
            return;
        }

        boolean changed = port != currentPort;
        currentPort = port;
        showCurrentPort();          // e.g. "80" is shown as "http"
        storeHttpServerSetting();

        if (!changed && httpServer != null)
            return;

        // No change if the value is any and the server already runs on a port of the dynamic range
        if (port == 0 && httpServer != null && httpServer.getAddress().getPort() >= 49152)
            return;

        startHttp();
    }//GEN-LAST:event_jComboBoxPortActionPerformed

    private void jButtonOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOpenActionPerformed
        if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                File indexFile = new File(webGenerator.getIndexHtmlFile());
                Desktop.getDesktop().open(new File(indexFile.getParent()));
            } catch (IOException ex) {
                System.getLogger(ServerPanel.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }
    }//GEN-LAST:event_jButtonOpenActionPerformed

    private void jButtonViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonViewActionPerformed
        try {
            if (httpServer == null) {
                startHttp();
            }
            if (httpServer == null)
                return;     // could not start, the reason is in the log
            java.awt.Desktop.getDesktop().browse(new java.net.URI(serverUrl("/index.html")));
        } catch (IOException ex) {
            Logger.getLogger(ServerPanel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            System.getLogger(ServerPanel.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }//GEN-LAST:event_jButtonViewActionPerformed

    private void jButtonStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStartActionPerformed
        if (!webGenerator.isRunning()) {
            webGenerator.setFtpHost(jTextFieldHost.getText());
            webGenerator.setFtpDirectory(jTextFieldDir.getText());
            webGenerator.setFtpUser(jTextFieldUser.getText().strip());
            webGenerator.setFtpPassword(jPasswordFieldPwd.getText());
            webGenerator.setFtpBundleName(jTextFieldBundleName.getText());
            webGenerator.setTitle(jTextFieldTitle.getText());
            webGenerator.setDescription(jTextFieldDescription.getText());
            webGenerator.setTournamentUrl(jTextFieldTournamentUrl.getText());
            webGenerator.setNewsUrl(jTextFieldNewsUrl.getText());
            webGenerator.setUserConfig(jTextFieldUserConfig.getText());
            webGenerator.setFlagType(FlagType.valueOf(buttonGroupFlags.getSelection().getActionCommand()));
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("de/webgen/gui/resources/WebGen"); // NOI18N
                if (webGenerator.isRunning()) {
                    webGenerator.stop();
                    jButtonStart.setText(bundle.getString("Start"));
                    jProgressBar.setStringPainted(false);
                } else {
                    webGenerator.setInterval((RefreshInterval) jComboBoxInterval.getSelectedItem());
                    if (webGenerator.start()) {
                        jButtonStart.setText(bundle.getString("Stop"));
                        jProgressBar.setStringPainted(true);
                    }
                }
            }
        }).start();
    }//GEN-LAST:event_jButtonStartActionPerformed

    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearActionPerformed
        logEntries.clear();
        ((javax.swing.DefaultComboBoxModel) jComboBoxLogging.getModel()).removeAllElements();
        applyLevelStyle(jComboBoxLogging, Level.INFO);
    }//GEN-LAST:event_jButtonClearActionPerformed


    private void jTextFieldCertificateFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextFieldCertificateFocusLost
        commitCertificateFile();
    }//GEN-LAST:event_jTextFieldCertificateFocusLost

    private void jTextFieldCertificateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldCertificateActionPerformed
        commitCertificateFile();
    }//GEN-LAST:event_jTextFieldCertificateActionPerformed

    private void jButtonCertificateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCertificateActionPerformed
        // Start in the directory of the current file, otherwise in the server directory
        File current = resolveCertificateFile(jTextFieldCertificate.getText());
        File dir = current != null && current.getParentFile() != null && current.getParentFile().isDirectory()
                ? current.getParentFile()
                : serverDirectory();

        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser(dir);
        chooser.setDialogTitle("Certificate (keystore) for HTTPS");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Keystore (*.p12, *.pfx, *.jks)", "p12", "pfx", "jks"));
        if (current != null && current.isFile())
            chooser.setSelectedFile(current);

        if (chooser.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION)
            return;

        jTextFieldCertificate.setText(relativeCertificatePath(chooser.getSelectedFile().getAbsolutePath()));
        commitCertificateFile();
    }//GEN-LAST:event_jButtonCertificateActionPerformed

    // Takes the path from the text field into the settings and saves index.xml if it changed
    private void commitCertificateFile() {
        String file = jTextFieldCertificate.getText().strip();
        markCertificateField();

        // An empty field is stored as "" (no certificate), so it differs from "not configured"
        boolean changed = !webGenerator.isCertificateConfigured() || !file.equals(webGenerator.getCertificateFile());
        if (changed) {
            webGenerator.setCertificateFile(file);
            webGenerator.storeSettings();
        }

        // HTTPS uses the certificate: restart with a new one, or start if it failed before
        // (e.g. the file was just generated)
        if (currentPort == HTTPS_PORT && (changed || httpServer == null))
            startHttp();
    }

    // Red text if a path is entered but the file doesn't exist
    private void markCertificateField() {
        File file = resolveCertificateFile(jTextFieldCertificate.getText());
        boolean missing = file != null && !file.isFile();
        jTextFieldCertificate.setForeground(missing ? java.awt.Color.RED : javax.swing.UIManager.getColor("TextField.foreground"));
    }

    // Suggestion if no file is set: <tournament directory>/<server>.p12, outside the directory the web server delivers
    // Directory of index.xml / index.html, the root of the web server
    private File serverDirectory() {
        return new File(webGenerator.getIndexXmlFile()).getAbsoluteFile().getParentFile();
    }

    // Certificate path from the settings: absolute (also UNC, \\computer\share\...) as is,
    // relative to the server directory; null if empty
    private File resolveCertificateFile(String path) {
        String p = path == null ? "" : path.strip();
        if (p.isEmpty())
            return null;
        File file = new File(p);
        return file.isAbsolute() ? file : new File(serverDirectory(), p);
    }

    // Path to store: files inside the server directory relative to it ("private.p12"), others absolute
    private String relativeCertificatePath(String path) {
        File file = resolveCertificateFile(path);
        if (file == null)
            return "";
        java.nio.file.Path dir  = serverDirectory().toPath().normalize();
        java.nio.file.Path full = file.toPath().toAbsolutePath().normalize();
        if (full.startsWith(dir) && !full.equals(dir))
            return dir.relativize(full).toString();
        return path.strip();
    }

    // Creates the certificate in the background (keytool needs about a second) and runs onSuccess afterwards.
    // replace: an existing file is replaced, but only if the new one could be created
    private void generateCertificate(final File file, final boolean replace, final Runnable onSuccess) {
        jButtonGenerateCertificate.setEnabled(false);
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));

        new javax.swing.SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                String password = ServerKeystore.password(webGenerator.getDatabaseName());
                if (!replace || !file.exists()) {
                    ServerKeystore.generate(file, password);
                } else {
                    // Generate next to the old file and replace it only if that worked,
                    // so a failure doesn't leave the server without a certificate
                    File tmp = new File(file.getAbsoluteFile().getParentFile(), file.getName() + ".new.p12");
                    java.nio.file.Files.deleteIfExists(tmp.toPath());
                    ServerKeystore.generate(tmp, password);
                    java.nio.file.Files.move(tmp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                return null;
            }

            @Override
            protected void done() {
                setCursor(null);
                try {
                    get();
                    Logger.getLogger(ServerPanel.class.getName()).log(Level.INFO, "Certificate generated: {0}", file.getPath());
                    onSuccess.run();
                } catch (Exception e) {
                    Throwable cause = e instanceof java.util.concurrent.ExecutionException && e.getCause() != null ? e.getCause() : e;
                    Logger.getLogger(ServerPanel.class.getName()).log(Level.SEVERE, "Cannot generate certificate", cause);
                    javax.swing.JOptionPane.showMessageDialog(ServerPanel.this,
                            "Cannot generate the certificate:\n" + cause.getMessage(),
                            "Certificate", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
                jButtonGenerateCertificate.setEnabled(true);
                markCertificateField();
            }
        }.execute();
    }

    // ---- HTTPS needs a certificate ----

    private static final String DEFAULT_CERTIFICATE = "private.p12";   // in the server directory

    private boolean checkingCertificate = false;    // a dialog is open, ignore further port events

    // Why the certificate can't be used, or null if it can: missing, unreadable or not openable
    private String certificateProblem(String path) {
        File file = resolveCertificateFile(path);
        if (file == null)
            return "HTTPS needs a certificate, but none is configured.";
        if (!file.exists())
            return "The certificate file " + path + " does not exist.";
        if (!file.isFile() || !file.canRead())
            return "The certificate file " + path + " cannot be read.";
        try {
            ServerKeystore.createSslContext(file, ServerKeystore.password(webGenerator.getDatabaseName()));
            return null;
        } catch (Exception e) {
            return "The certificate file " + path + " cannot be used:\n" + e.getMessage();
        }
    }

    // Called before switching to https. true: the certificate is fine, go ahead.
    // false: stay on the current port; if the user agreed, a certificate is created and
    // the server switches to https afterwards
    private boolean checkCertificateForHttps() {
        String problem = certificateProblem(webGenerator.getCertificateFile());
        if (problem == null)
            return true;

        boolean none = webGenerator.getCertificateFile().isBlank();
        File target = resolveCertificateFile(DEFAULT_CERTIFICATE);
        boolean targetUsable = certificateProblem(DEFAULT_CERTIFICATE) == null;
        boolean replaceTarget = target.exists() && !targetUsable;

        String question = none
                ? "Shall a new certificate " + DEFAULT_CERTIFICATE + " be created?"
                : "Shall a new certificate " + DEFAULT_CERTIFICATE + " be configured?";
        if (replaceTarget)
            question += "\n(The existing file " + DEFAULT_CERTIFICATE + " will be replaced.)";

        int answer;
        checkingCertificate = true;
        try {
            answer = javax.swing.JOptionPane.showConfirmDialog(this, problem + "\n\n" + question,
                    "HTTPS certificate", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE);
        } finally {
            checkingCertificate = false;
        }

        if (answer != javax.swing.JOptionPane.YES_OPTION) {
            if (none) {
                // Deliberately no certificate: https is no longer offered in the port list
                useCertificate("");
            }
            return false;
        }

        // An existing, valid private.p12 is simply used; otherwise a new one is created
        if (targetUsable) {
            useCertificate(DEFAULT_CERTIFICATE);
            switchToHttps();
        } else {
            generateCertificate(target, replaceTarget, () -> {
                useCertificate(DEFAULT_CERTIFICATE);
                switchToHttps();
            });
        }
        return false;
    }

    // Puts the certificate path into the field and the settings ("" = none)
    private void useCertificate(String path) {
        jTextFieldCertificate.setText(path);
        if (!webGenerator.isCertificateConfigured() || !path.equals(webGenerator.getCertificateFile())) {
            webGenerator.setCertificateFile(path);
            webGenerator.storeSettings();
        }
        markCertificateField();
    }

    // Selects https and (re)starts the server with it
    private void switchToHttps() {
        currentPort = HTTPS_PORT;
        showCurrentPort();
        storeHttpServerSetting();
        startHttp();
    }

    private void jButtonGenerateCertificateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGenerateCertificateActionPerformed
        String path = jTextFieldCertificate.getText().strip();
        if (path.isEmpty()) {
            path = DEFAULT_CERTIFICATE;
            jTextFieldCertificate.setText(path);
        }

        final File file = resolveCertificateFile(path);
        final boolean replace = file.exists();
        if (replace) {
            int answer = javax.swing.JOptionPane.showConfirmDialog(this,
                    "Do you want to overwrite the file " + path + "?\n\n"
                    + "Browsers that accepted the old certificate will show the security warning again.",
                    "Certificate", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
            if (answer != javax.swing.JOptionPane.YES_OPTION)
                return;
        }

        generateCertificate(file, replace, () -> {
            // A running HTTPS server still uses the old certificate: restart it
            boolean restart = replace && currentPort == HTTPS_PORT && httpServer != null;
            commitCertificateFile();
            if (restart)
                startHttp();
        });
    }//GEN-LAST:event_jButtonGenerateCertificateActionPerformed

    private void jComboBoxIntervalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxIntervalActionPerformed
        RefreshInterval interval = (RefreshInterval) jComboBoxInterval.getModel().getSelectedItem();
        jProgressBar.setMaximum(interval.getInterval());
        webGenerator.setInterval(interval);
    }//GEN-LAST:event_jComboBoxIntervalActionPerformed

    private void jButtonLivetickerPropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLivetickerPropertiesActionPerformed
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            WebGen.LivetickerProperties props = webGenerator.getLivetickerSettings();
            if (props == null)
            props = new WebGen.LivetickerProperties();

            PropertyPanel panel = new PropertyPanel();
            panel.setObject(props, true, false);

            int ret = javax.swing.JOptionPane.showConfirmDialog(
                this, panel, "Configuration",
                javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.PLAIN_MESSAGE);

            if (ret == javax.swing.JOptionPane.OK_OPTION) {
                panel.updateObject(props);

                webGenerator.setLivetickerSettings(props);
            }
        } catch (Throwable t) {
            System.err.println(t.toString());
        }
    }//GEN-LAST:event_jButtonLivetickerPropertiesActionPerformed

    private void jCheckBoxLivtickerEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxLivtickerEnabledActionPerformed
        webGenerator.setLivetickerEnabled(jCheckBoxLivtickerEnabled.isSelected());
    }//GEN-LAST:event_jCheckBoxLivtickerEnabledActionPerformed

    private void jRadioButtonFlagTypeNoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonFlagTypeNoneActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButtonFlagTypeNoneActionPerformed

    private void jTextFieldPasswordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPasswordActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldPasswordActionPerformed

    private void jTextFieldPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPortActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldPortActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroupFlags;
    private javax.swing.JButton jButtonCertificate;
    private javax.swing.JButton jButtonClear;
    private javax.swing.JButton jButtonGenerateCertificate;
    private javax.swing.JButton jButtonGroupAuto;
    private javax.swing.JButton jButtonGroupDisable;
    private javax.swing.JButton jButtonGroupEnable;
    private javax.swing.JButton jButtonLivetickerProperties;
    private javax.swing.JButton jButtonOpen;
    private javax.swing.JButton jButtonReportsDisable;
    private javax.swing.JButton jButtonReportsEnable;
    private javax.swing.JButton jButtonStart;
    private javax.swing.JButton jButtonView;
    private javax.swing.JCheckBox jCheckBoxBundle;
    private javax.swing.JCheckBox jCheckBoxFtpDebug;
    private javax.swing.JCheckBox jCheckBoxIncludePlayedGroups;
    private javax.swing.JCheckBox jCheckBoxLivtickerEnabled;
    private javax.swing.JCheckBox jCheckBoxPassiveMode;
    private javax.swing.JCheckBox jCheckBoxSftp;
    private javax.swing.JComboBox jComboBoxEvent;
    private javax.swing.JComboBox<String> jComboBoxFilterLog;
    private javax.swing.JComboBox jComboBoxInterval;
    private javax.swing.JComboBox jComboBoxLogging;
    private javax.swing.JComboBox<String> jComboBoxPort;
    private javax.swing.JComboBox<String> jComboBoxServer;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelBundleName;
    private javax.swing.JLabel jLabelCertificate;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanelFtp;
    private javax.swing.JPanel jPanelGroups;
    private javax.swing.JPanel jPanelLists;
    private javax.swing.JPanel jPanelReports;
    private javax.swing.JPanel jPanelSettings;
    private javax.swing.JPasswordField jPasswordFieldPwd;
    private javax.swing.JProgressBar jProgressBar;
    private javax.swing.JRadioButton jRadioButtonFlagTypeNation;
    private javax.swing.JRadioButton jRadioButtonFlagTypeNone;
    private javax.swing.JRadioButton jRadioButtonFlagTypeRegion;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JTabbedPane jTabbedPane;
    private javax.swing.JTable jTableGroups;
    private javax.swing.JTable jTableReports;
    private javax.swing.JTextField jTextFieldBundleName;
    private javax.swing.JTextField jTextFieldCertificate;
    private javax.swing.JTextField jTextFieldDescription;
    private javax.swing.JTextField jTextFieldDir;
    private javax.swing.JTextField jTextFieldHost;
    private javax.swing.JTextField jTextFieldNewsUrl;
    private javax.swing.JTextField jTextFieldPassword;
    private javax.swing.JTextField jTextFieldPort;
    private javax.swing.JTextField jTextFieldTitle;
    private javax.swing.JTextField jTextFieldTournamentUrl;
    private javax.swing.JTextField jTextFieldUser;
    private javax.swing.JTextField jTextFieldUserConfig;
    // End of variables declaration//GEN-END:variables

}
