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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JSpinner;
import javax.swing.JFormattedTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
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
        installPortSpinner(jSpinnerPort);
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
        jTextFieldCertificate.setText(webGenerator.getCertificateFile());
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
        
        SwingUtilities.invokeLater(this::startHttp);        
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
        
        int port = ((Number) jSpinnerPort.getValue()).intValue();
        
        try {
            InetSocketAddress address = new InetSocketAddress(ip, port);
            if (port == HTTPS_PORT) {
                // HTTPS with the certificate from the settings. Load it before binding the port,
                // so a missing or broken certificate doesn't leave a half started server
                javax.net.ssl.SSLContext ssl = ServerKeystore.createSslContext(
                        new File(webGenerator.getCertificateFile()),
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

        if (httpServer.getAddress().getAddress().isLoopbackAddress()) {
            httpServer.createContext("/settings", exchange -> {
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
            }).getFilters().add(accessLog);

            // Access log as readable HTML page; not logged itself, the auto-refresh would flood the log
            httpServer.createContext("/logs", exchange -> {
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
    }


    private void addLogEntry(LogEntry entry) {
        javax.swing.DefaultComboBoxModel model = (javax.swing.DefaultComboBoxModel) jComboBoxLogging.getModel();
        model.insertElementAt(entry, 0);

        while (model.getSize() > MAX_LOG_ENTRIES)
            model.removeElementAt(model.getSize() - 1);

        jComboBoxLogging.setSelectedIndex(0);
        applyLevelStyle(jComboBoxLogging, entry.record.getLevel());
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



    // Allowed ports: 0 (any), the named well-known ports below 1024, and 1024..65535
    private static final java.util.NavigableMap<Integer, String> NAMED_PORTS = new java.util.TreeMap<>(java.util.Map.of(
            0,   "any",
            80,  "http",
            443, "https"));
    private static final int FIRST_FREE_PORT = 1024;
    private static final int LAST_PORT       = 65535;

    private static boolean isAllowedPort(int port) {
        return NAMED_PORTS.containsKey(port) || (port >= FIRST_FREE_PORT && port <= LAST_PORT);
    }

    // Number model whose arrows skip the ports that are not allowed: any -> http -> https -> 1024 -> 1025 ...
    private static class PortSpinnerModel extends javax.swing.SpinnerNumberModel {
        PortSpinnerModel(int port) {
            super(port, 0, LAST_PORT, 1);
        }

        @Override
        public Object getNextValue() {
            int port = getNumber().intValue();
            Integer named = NAMED_PORTS.higherKey(port);
            if (port < FIRST_FREE_PORT)
                return named != null && named < FIRST_FREE_PORT ? named : FIRST_FREE_PORT;
            return port < LAST_PORT ? port + 1 : null;
        }

        @Override
        public Object getPreviousValue() {
            int port = getNumber().intValue();
            if (port > FIRST_FREE_PORT)
                return port - 1;
            return NAMED_PORTS.lowerKey(port);      // null at "any": no further down
        }
    }

    // Replaces the spinner's model and formatter: named ports are shown by name, other ports below 1024 are rejected
    private void installPortSpinner(JSpinner spinner) {
        int port = ((Number) spinner.getValue()).intValue();
        spinner.setModel(new PortSpinnerModel(isAllowedPort(port) ? port : 0));

        JFormattedTextField tf = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();

        NumberFormatter fmt = new NumberFormatter(new DecimalFormat("#0")) {
            @Override
            public String valueToString(Object value) throws ParseException {
                if (value instanceof Number && NAMED_PORTS.containsKey(((Number) value).intValue()))
                    return NAMED_PORTS.get(((Number) value).intValue());
                return super.valueToString(value);
            }

            @Override
            public Object stringToValue(String text) throws ParseException {
                String t = text == null ? "" : text.trim();
                if (t.isEmpty())
                    return 0;
                for (java.util.Map.Entry<Integer, String> e : NAMED_PORTS.entrySet())
                    if (e.getValue().equalsIgnoreCase(t))
                        return e.getKey();

                Object value = super.stringToValue(t);
                if (!isAllowedPort(((Number) value).intValue()))
                    throw new ParseException("Port not allowed: " + t, 0);
                return value;
            }
        };
        fmt.setValueClass(Integer.class);
        fmt.setMinimum(0);
        fmt.setMaximum(LAST_PORT);
        fmt.setAllowsInvalid(true);        // otherwise typing letters ("any", "http") is blocked
        fmt.setCommitsOnValidEdit(false);  // commit on Enter / focus loss, not per keystroke

        tf.setFormatterFactory(new DefaultFormatterFactory(fmt));
        tf.setColumns(5);
        tf.setToolTipText("any, http (80), https (443) or 1024 - 65535");
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
        jLabel10 = new javax.swing.JLabel();
        jComboBoxServer = new javax.swing.JComboBox<>();
        jLabel11 = new javax.swing.JLabel();
        jSpinnerPort = new javax.swing.JSpinner();
        jTextFieldPort = new javax.swing.JTextField();
        jLabelCertificate = new javax.swing.JLabel();
        jTextFieldCertificate = new javax.swing.JTextField();
        jButtonCertificate = new javax.swing.JButton();
        jButtonGenerateCertificate = new javax.swing.JButton();
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
        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jComboBoxLogging = new javax.swing.JComboBox();
        jButtonStart = new javax.swing.JButton();
        jProgressBar = new javax.swing.JProgressBar();
        jButtonView = new javax.swing.JButton();
        jButtonOpen = new javax.swing.JButton();
        jButtonClear = new javax.swing.JButton();

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
                    .addGroup(jPanel16Layout.createSequentialGroup()
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
                                                .addGap(0, 52, Short.MAX_VALUE))
                                            .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                                .addGroup(jPanel16Layout.createSequentialGroup()
                                    .addComponent(jLabel8)
                                    .addGap(30, 30, 30)))
                            .addGroup(jPanel16Layout.createSequentialGroup()
                                .addComponent(jLabel9)
                                .addGap(61, 61, 61)))
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextFieldDescription)
                            .addComponent(jTextFieldTournamentUrl)
                            .addComponent(jTextFieldNewsUrl)
                            .addGroup(jPanel16Layout.createSequentialGroup()
                                .addGap(129, 129, 129)
                                .addComponent(jRadioButtonFlagTypeNone)
                                .addGap(18, 18, 18)
                                .addComponent(jRadioButtonFlagTypeNation)
                                .addGap(18, 18, 18)
                                .addComponent(jRadioButtonFlagTypeRegion)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jTextFieldUserConfig, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jTextFieldTitle, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(jPanel16Layout.createSequentialGroup()
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel16)
                            .addComponent(jLabel7))
                        .addGap(65, 65, 65)
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel16Layout.createSequentialGroup()
                                .addComponent(jCheckBoxLivtickerEnabled)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jButtonLivetickerProperties))
                            .addComponent(jComboBoxInterval, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
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

        jSpinnerPort.setModel(new javax.swing.SpinnerNumberModel(0, 0, 65535, 1));
        jSpinnerPort.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerPortStateChanged(evt);
            }
        });

        jTextFieldPort.setEditable(false);
        jTextFieldPort.setColumns(5);

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

        javax.swing.GroupLayout jPanelSettingsLayout = new javax.swing.GroupLayout(jPanelSettings);
        jPanelSettings.setLayout(jPanelSettingsLayout);
        jPanelSettingsLayout.setHorizontalGroup(
            jPanelSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelSettingsLayout.createSequentialGroup()
                        .addGroup(jPanelSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanelSettingsLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(jLabelCertificate)
                                .addGap(37, 37, 37)
                                .addComponent(jTextFieldCertificate, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonCertificate)
                                .addGap(18, 18, 18)
                                .addComponent(jButtonGenerateCertificate, javax.swing.GroupLayout.DEFAULT_SIZE, 85, Short.MAX_VALUE)
                                .addGap(13, 13, 13)))
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelSettingsLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel10)
                        .addGap(49, 49, 49)
                        .addComponent(jComboBoxServer, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinnerPort, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jTextFieldPort)
                        .addGap(19, 19, 19))))
        );
        jPanelSettingsLayout.setVerticalGroup(
            jPanelSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel10)
                    .addGroup(jPanelSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jComboBoxServer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel11)
                        .addComponent(jSpinnerPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelCertificate)
                    .addComponent(jTextFieldCertificate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonCertificate)
                    .addComponent(jButtonGenerateCertificate))
                .addContainerGap(84, Short.MAX_VALUE))
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
                    .addComponent(jComboBoxEvent, 0, 426, Short.MAX_VALUE)
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
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE))
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
                .addContainerGap(18, Short.MAX_VALUE))
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

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jButtonStart)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 219, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonView)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonOpen))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jComboBoxLogging, javax.swing.GroupLayout.PREFERRED_SIZE, 380, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonClear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
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
                    .addComponent(jButtonClear))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
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

    private void jRadioButtonFlagTypeNoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonFlagTypeNoneActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButtonFlagTypeNoneActionPerformed

    private void jComboBoxServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxServerActionPerformed
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

    private void jSpinnerPortStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinnerPortStateChanged
        // No change if we value is any and port number is higher than n the free range
        if ( (((Number) jSpinnerPort.getValue()).intValue() == 0) &&
             (httpServer.getAddress().getPort() >= 49152) )
            return;
        
        startHttp();
    }//GEN-LAST:event_jSpinnerPortStateChanged

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
        File current = new File(jTextFieldCertificate.getText().strip());
        File dir = current.getParentFile() != null && current.getParentFile().isDirectory()
                ? current.getParentFile()
                : new File(webGenerator.getIndexXmlFile()).getParentFile();

        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser(dir);
        chooser.setDialogTitle("Certificate (keystore) for HTTPS");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Keystore (*.p12, *.pfx, *.jks)", "p12", "pfx", "jks"));
        if (current.isFile())
            chooser.setSelectedFile(current);

        if (chooser.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION)
            return;

        jTextFieldCertificate.setText(chooser.getSelectedFile().getAbsolutePath());
        commitCertificateFile();
    }//GEN-LAST:event_jButtonCertificateActionPerformed

    // Takes the path from the text field into the settings and saves index.xml if it changed
    private void commitCertificateFile() {
        String file = jTextFieldCertificate.getText().strip();
        markCertificateField();

        boolean changed = !file.equals(webGenerator.getCertificateFile());
        if (changed) {
            webGenerator.setCertificateFile(file);
            webGenerator.storeSettings();
        }

        // HTTPS uses the certificate: restart with a new one, or start if it failed before
        // (e.g. the file was just generated)
        if (((Number) jSpinnerPort.getValue()).intValue() == HTTPS_PORT && (changed || httpServer == null))
            startHttp();
    }

    // Red text if a path is entered but the file doesn't exist; "Generate" only if there is no file yet
    private void markCertificateField() {
        String file = jTextFieldCertificate.getText().strip();
        boolean missing = !file.isEmpty() && !new File(file).isFile();
        jTextFieldCertificate.setForeground(missing ? java.awt.Color.RED : javax.swing.UIManager.getColor("TextField.foreground"));
        jButtonGenerateCertificate.setEnabled(file.isEmpty() || !new File(file).exists());
    }

    // Suggestion if no file is set: <tournament directory>/<server>.p12, outside the directory the web server delivers
    private File defaultCertificateFile() {
        File serverDir = new File(webGenerator.getIndexXmlFile()).getAbsoluteFile().getParentFile();
        File dir = serverDir.getParentFile() != null ? serverDir.getParentFile() : serverDir;
        return new File(dir, webGenerator.getServer() + ".p12");
    }

    private void jButtonGenerateCertificateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGenerateCertificateActionPerformed
        String path = jTextFieldCertificate.getText().strip();
        if (path.isEmpty()) {
            path = defaultCertificateFile().getPath();
            jTextFieldCertificate.setText(path);
        }

        final File file = new File(path);
        if (file.exists()) {
            // Only new files; to replace a certificate delete or rename the old file first
            markCertificateField();
            return;
        }

        jButtonGenerateCertificate.setEnabled(false);
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));

        // keytool needs about a second, so not on the Swing thread
        new javax.swing.SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ServerKeystore.generate(file, ServerKeystore.password(webGenerator.getDatabaseName()));
                return null;
            }

            @Override
            protected void done() {
                setCursor(null);
                try {
                    get();
                    commitCertificateFile();
                    Logger.getLogger(ServerPanel.class.getName()).log(Level.INFO, "Certificate generated: {0}", file.getPath());
                } catch (Exception e) {
                    Throwable cause = e instanceof java.util.concurrent.ExecutionException && e.getCause() != null ? e.getCause() : e;
                    Logger.getLogger(ServerPanel.class.getName()).log(Level.SEVERE, "Cannot generate certificate", cause);
                    javax.swing.JOptionPane.showMessageDialog(ServerPanel.this,
                            "Cannot generate the certificate:\n" + cause.getMessage(),
                            "Certificate", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
                markCertificateField();
            }
        }.execute();
    }//GEN-LAST:event_jButtonGenerateCertificateActionPerformed

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
    private javax.swing.JComboBox jComboBoxInterval;
    private javax.swing.JComboBox jComboBoxLogging;
    private javax.swing.JComboBox<String> jComboBoxServer;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
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
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel16;
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
    private javax.swing.JSpinner jSpinnerPort;
    private javax.swing.JTabbedPane jTabbedPane;
    private javax.swing.JTable jTableGroups;
    private javax.swing.JTable jTableReports;
    private javax.swing.JTextField jTextFieldBundleName;
    private javax.swing.JTextField jTextFieldCertificate;
    private javax.swing.JTextField jTextFieldDescription;
    private javax.swing.JTextField jTextFieldDir;
    private javax.swing.JTextField jTextFieldHost;
    private javax.swing.JTextField jTextFieldNewsUrl;
    private javax.swing.JTextField jTextFieldPort;
    private javax.swing.JTextField jTextFieldTitle;
    private javax.swing.JTextField jTextFieldTournamentUrl;
    private javax.swing.JTextField jTextFieldUser;
    private javax.swing.JTextField jTextFieldUserConfig;
    // End of variables declaration//GEN-END:variables

}
