/* Copyright (C) 2020 Christoph Theis */

package de.webgen.gui;

import de.webgen.WebGen;
import de.webgen.database.Database;
import de.webgen.prefs.Properties;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JList;
import javax.swing.JOptionPane;
import org.ini4j.Ini;


public class MainFrame extends javax.swing.JFrame {

    private static final String versionNumber = "24.12.01";
    
    private static final ResourceBundle bundle = ResourceBundle.getBundle("de/webgen/gui/resources/WebGen"); // NOI18N
    private String tournament = null;

    private static void initPaths() {
        // tt32.ini
        File tt32 = Properties.findPath("tt32.ini");
        if (tt32 != null) {
            WebGen.iniFile = tt32;
            WebGen.baseDir = tt32.getParent();
        }

        // template
        File template = Properties.findPath("WebGen" + File.separator + "Template");
        if (template == null)
            template = Properties.findPath("Template");
        if (template != null)
            WebGen.templateDir = template.getPath();
    }

    private class ComboBoxLogRecord  {
        LogRecord record;
        long      threadId;

        public ComboBoxLogRecord(LogRecord record) {
            this.record = record;
            threadId = Thread.currentThread().getId();
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
            
            String server = "";
            
            if (jServerPane.getTabCount() > 0) {
                if (WebGen.threadIdMap.containsKey(threadId))
                    server = WebGen.threadIdMap.get(threadId).getServer();
                else
                    server = "All";
            } else {
                server = "All";
            }
            
            return server + ": [" + date + "] " + message;
        }
    }

    final private List<ComboBoxLogRecord> logList = new java.util.ArrayList<>();
    
    /** Creates new form MainFrame */
    public MainFrame() throws IOException {
        initComponents();
        
        jMenuItemOpen.setEnabled(haveTournaments());
        jMenuItemServer.setEnabled(tournament != null && !tournament.isEmpty());
        
        javax.swing.JLabel label = new javax.swing.JLabel(bundle.getString("Open a tournament first"));
        label.setFont(label.getFont().deriveFont(36.f));
        label.setForeground(Color.GRAY);
        label.setHorizontalAlignment(javax.swing.JLabel.CENTER);
        jServerPane.add(bundle.getString("<new>"), label);

        Logger.getLogger("").addHandler(new Handler() {

            // synchronized, da merere gleichzeitig drauf zugreifen koennten
            @Override
            synchronized public void publish(LogRecord record) {
                if (record.getMessage() == null && record.getThrown() == null)
                    return;

                record.setResourceBundle(bundle);
                
                synchronized (logList) {
                    logList.add(0, new ComboBoxLogRecord(record));
                
                    while (logList.size() > 200)
                        logList.remove(200);
                }
                
                // Check ob es den aktuellen Tab betrifft, wenn nicht kann man hier aufhoeren
                long threadId = Thread.currentThread().getId();
                WebGen currentServer = WebGen.threadIdMap.get(threadId);
                if (currentServer != null && jServerPane.indexOfTab(currentServer.getServer()) != jServerPane.getSelectedIndex())
                    return;
                                        
                javax.swing.DefaultComboBoxModel model = (javax.swing.DefaultComboBoxModel) jComboBoxLogging.getModel();
                model.insertElementAt(new ComboBoxLogRecord(record), 0);

                while (model.getSize() > 100)
                    model.removeElementAt(model.getSize() - 1);

                jComboBoxLogging.setSelectedIndex(0);
                
                if (Level.SEVERE.intValue() <= record.getLevel().intValue()) {
                    jComboBoxLogging.setForeground(java.awt.Color.RED);
                    jComboBoxLogging.setFont(jComboBoxLogging.getFont().deriveFont(0));                            
                } else if (Level.FINE.intValue() >= record.getLevel().intValue()) {
                    jComboBoxLogging.setForeground(java.awt.Color.DARK_GRAY);
                    jComboBoxLogging.setFont(jComboBoxLogging.getFont().deriveFont(java.awt.Font.ITALIC));                                                
                } else {
                    jComboBoxLogging.setForeground(java.awt.Color.BLACK);
                    jComboBoxLogging.setFont(jComboBoxLogging.getFont().deriveFont(0));                                                
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }

        });


        Logger.getLogger("de.webgen").setLevel(Level.FINE);

        jComboBoxLogging.setRenderer(new javax.swing.DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value == null)
                    return c;

                LogRecord record = ((ComboBoxLogRecord) value).record;

                if (Level.SEVERE.intValue() <= record.getLevel().intValue()) {
                    c.setForeground(java.awt.Color.RED);
                    c.setFont(c.getFont().deriveFont(0));
                } else if (Level.FINE.intValue() >= record.getLevel().intValue()) {
                    c.setForeground(java.awt.Color.DARK_GRAY);
                    c.setFont(c.getFont().deriveFont(java.awt.Font.ITALIC));
                } else {
                    c.setForeground(java.awt.Color.BLACK);
                    c.setFont(c.getFont().deriveFont(0));
                }

                return c;
            }
        });

        Ini ini = new Ini(WebGen.iniFile);

        openTournament(ini.get("Open", "Last"));
    }

    // Oeffnet ein Turnier und initialisiert die Tabs
    private void openTournament(String tournament) throws IOException {
        this.tournament = tournament;
        
        jServerPane.removeAll();

        if (tournament != null && !tournament.isEmpty()) {
            String path = (new Ini(WebGen.iniFile)).get(tournament, "Path");

            File[] dirs = new File(WebGen.baseDir + File.separator + path).listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    if (!pathname.isDirectory())
                        return false;

                    return new File(pathname.getPath() + File.separator + "index.xml").exists();
                }
            });

            // listFiles liefert null, wenn es keine Files drunter gibt
            if (dirs == null)
                dirs = new File[0];

            for (File dir : dirs) {
                jServerPane.add(dir.getName(), new ServerPanel(dir.getName(), tournament, dir.getPath()));
            }
            
            if (jServerPane.getComponentCount() == 0) {
                javax.swing.JLabel label = new javax.swing.JLabel(bundle.getString("Add a server first"));
                label.setFont(label.getFont().deriveFont(36.f));
                label.setForeground(Color.GRAY);
                label.setHorizontalAlignment(javax.swing.JLabel.CENTER);
                jServerPane.add(bundle.getString("<new>"), label);
            }
                
            setTitle(MessageFormat.format(bundle.getString("WebGenerator - {0}"), tournament));
            
            jMenuItemServer.setEnabled(true);
            
            // Change last opened tournament to this one
            Ini ini = new Ini(WebGen.iniFile);
            ini.remove("Open", "Last");
            ini.add("Open", "Last", this.tournament);
            ini.store();            
        }

        pack();
    }


    //Legt einen neuen Server (Tab) an
    private void createServer(String server) {
        try {
            Ini ini = new Ini(WebGen.iniFile);
            File dir = new File(WebGen.baseDir + File.separator + ini.get(tournament, "Path") + File.separator + server);
            dir.mkdir();
            if (!dir.exists()) {
                return;
            }
            
            // Wenn die erste ServerPane das Dummy ist (JLabel "<new>"), diese entfernen
            if (jServerPane.getComponent(0) instanceof javax.swing.JLabel)
                jServerPane.remove(0);                
            
            jServerPane.add(server, new ServerPanel(server, tournament, dir.getPath()));
            jServerPane.setSelectedIndex(jServerPane.getTabCount() - 1);

            pack();
            repaint();
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
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

        jPanelAddTournament = new javax.swing.JPanel();
        serverLabel = new javax.swing.JLabel();
        serverTextField = new javax.swing.JTextField();
        databaseLabel = new javax.swing.JLabel();
        databaseTextField = new javax.swing.JTextField();
        authtypeCheckBox = new javax.swing.JCheckBox();
        userLabel = new javax.swing.JLabel();
        userTextField = new javax.swing.JTextField();
        pwdLabel = new javax.swing.JLabel();
        pwdTextField = new javax.swing.JPasswordField();
        testConnectionButton = new javax.swing.JButton();
        tournamentLabel = new javax.swing.JLabel();
        tournamentTextField = new javax.swing.JTextField();
        jServerPane = new javax.swing.JTabbedPane();
        jComboBoxLogging = new javax.swing.JComboBox();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenuTournment = new javax.swing.JMenu();
        jMenuItemAdd = new javax.swing.JMenuItem();
        jMenuItemOpen = new javax.swing.JMenuItem();
        jMenuItemServer = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItemDebug = new javax.swing.JCheckBoxMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenuHelp = new javax.swing.JMenu();
        jMenuItemCheckUpdate = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenuItemAbout = new javax.swing.JMenuItem();

        jPanelAddTournament.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                jPanelAddTournamentAncestorAdded(evt);
            }
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("de/webgen/gui/resources/WebGen"); // NOI18N
        serverLabel.setText(bundle.getString("MainFrame.serverLabel.text")); // NOI18N

        databaseLabel.setText(bundle.getString("MainFrame.databaseLabel.text")); // NOI18N

        authtypeCheckBox.setSelected(true);
        authtypeCheckBox.setText(bundle.getString("MainFrame.authtypeCheckBox.text")); // NOI18N
        authtypeCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        authtypeCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        authtypeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                authtypeCheckBoxActionPerformed(evt);
            }
        });

        userLabel.setText(bundle.getString("MainFrame.userLabel.text")); // NOI18N

        pwdLabel.setText(bundle.getString("MainFrame.pwdLabel.text")); // NOI18N

        testConnectionButton.setText(bundle.getString("MainFrame.testConnectionButton.text")); // NOI18N
        testConnectionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testConnectionButtonActionPerformed(evt);
            }
        });

        tournamentLabel.setText(bundle.getString("MainFrame.tournamentLabel.text")); // NOI18N

        javax.swing.GroupLayout jPanelAddTournamentLayout = new javax.swing.GroupLayout(jPanelAddTournament);
        jPanelAddTournament.setLayout(jPanelAddTournamentLayout);
        jPanelAddTournamentLayout.setHorizontalGroup(
            jPanelAddTournamentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelAddTournamentLayout.createSequentialGroup()
                .addGroup(jPanelAddTournamentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelAddTournamentLayout.createSequentialGroup()
                        .addGroup(jPanelAddTournamentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelAddTournamentLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(tournamentLabel))
                            .addGroup(jPanelAddTournamentLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(serverLabel))
                            .addGroup(jPanelAddTournamentLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(databaseLabel)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelAddTournamentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pwdLabel)
                            .addComponent(userLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelAddTournamentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(userTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE)
                            .addComponent(pwdTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE)))
                    .addGroup(jPanelAddTournamentLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(testConnectionButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanelAddTournamentLayout.createSequentialGroup()
                        .addGap(79, 79, 79)
                        .addComponent(databaseTextField))
                    .addGroup(jPanelAddTournamentLayout.createSequentialGroup()
                        .addGap(79, 79, 79)
                        .addComponent(serverTextField))
                    .addGroup(jPanelAddTournamentLayout.createSequentialGroup()
                        .addGap(79, 79, 79)
                        .addComponent(tournamentTextField))
                    .addGroup(jPanelAddTournamentLayout.createSequentialGroup()
                        .addGap(79, 79, 79)
                        .addComponent(authtypeCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelAddTournamentLayout.setVerticalGroup(
            jPanelAddTournamentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelAddTournamentLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelAddTournamentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tournamentLabel)
                    .addComponent(tournamentTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelAddTournamentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(serverLabel)
                    .addComponent(serverTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelAddTournamentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(databaseLabel)
                    .addComponent(databaseTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(16, 16, 16)
                .addComponent(authtypeCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelAddTournamentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(userLabel)
                    .addComponent(userTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelAddTournamentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pwdLabel)
                    .addComponent(pwdTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(23, 23, 23)
                .addComponent(testConnectionButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jServerPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jServerPaneStateChanged(evt);
            }
        });

        jMenuTournment.setText(bundle.getString("MainFrame.jMenuTournment.text")); // NOI18N

        jMenuItemAdd.setText(bundle.getString("MainFrame.jMenuItemAdd.text")); // NOI18N
        jMenuItemAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAddActionPerformed(evt);
            }
        });
        jMenuTournment.add(jMenuItemAdd);

        jMenuItemOpen.setText(bundle.getString("MainFrame.jMenuItemOpen.text")); // NOI18N
        jMenuItemOpen.setEnabled(false);
        jMenuItemOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenActionPerformed(evt);
            }
        });
        jMenuTournment.add(jMenuItemOpen);

        jMenuItemServer.setText(bundle.getString("MainFrame.jMenuItemServer.text")); // NOI18N
        jMenuItemServer.setEnabled(false);
        jMenuItemServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemServerActionPerformed(evt);
            }
        });
        jMenuTournment.add(jMenuItemServer);
        jMenuTournment.add(jSeparator1);

        jMenuItemDebug.setText(bundle.getString("Debug")); // NOI18N
        jMenuItemDebug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDebugActionPerformed(evt);
            }
        });
        jMenuTournment.add(jMenuItemDebug);
        jMenuTournment.add(jSeparator3);

        jMenuItemExit.setText(bundle.getString("Exit")); // NOI18N
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenuTournment.add(jMenuItemExit);

        jMenuBar1.add(jMenuTournment);

        jMenuHelp.setText(bundle.getString("MainFrame.jMenuHelp.text")); // NOI18N

        jMenuItemCheckUpdate.setText(bundle.getString("MainFrame.jMenuItemCheckUpdate.text")); // NOI18N
        jMenuItemCheckUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCheckUpdateActionPerformed(evt);
            }
        });
        jMenuHelp.add(jMenuItemCheckUpdate);
        jMenuHelp.add(jSeparator2);

        jMenuItemAbout.setText(bundle.getString("MainFrame.jMenuItemAbout.text")); // NOI18N
        jMenuItemAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAboutActionPerformed(evt);
            }
        });
        jMenuHelp.add(jMenuItemAbout);

        jMenuBar1.add(jMenuHelp);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jServerPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                    .addComponent(jComboBoxLogging, javax.swing.GroupLayout.Alignment.LEADING, 0, 450, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jServerPane, javax.swing.GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jComboBoxLogging, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItemAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAboutActionPerformed
        String aboutMessage =
                "<html>" +
                    "<b>WebGen</b><br>" +
                    "Version " + java.text.MessageFormat.format(bundle.getString("version"), versionNumber) + "<br>" +
                    bundle.getString("copyright") +
                "</html>";

        JOptionPane.showMessageDialog(this, aboutMessage, bundle.getString("About"), JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_jMenuItemAboutActionPerformed

    private void jMenuItemOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOpenActionPerformed
        try {
            Ini ini = new Ini(WebGen.iniFile);
            Object[] tournaments = ini.get("Tournaments").keySet().toArray();

            java.util.Arrays.sort(tournaments);
            
            String initial = tournament == null ? tournaments[0].toString() : tournament;

            Object ret = JOptionPane.showInputDialog(
                    this, bundle.getString("Select Tournament"),
                    bundle.getString("Select Tournament"), 
                    JOptionPane.PLAIN_MESSAGE, null, tournaments, initial);

            if (ret == null)
                return;

            if (ret.toString().equals(tournament))
                return;

            openTournament(ret.toString());

        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }

    }//GEN-LAST:event_jMenuItemOpenActionPerformed

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
        setVisible(false);
        System.exit(0);
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void jMenuItemServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemServerActionPerformed
        String server = JOptionPane.showInputDialog(this, "Enter name");

        if (server == null || server.isEmpty())
            return;

        createServer(server);
    }//GEN-LAST:event_jMenuItemServerActionPerformed

    private void jMenuItemCheckUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCheckUpdateActionPerformed
        java.io.InputStream is = null;
        try {
            java.net.URL url = new java.net.URL("http://downloads.ttm.co.at/webgen/current.txt");
            is = url.openStream();
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            String current = br.readLine();
            if (current.compareTo(versionNumber) > 0) {
                javax.swing.JPanel panel = new javax.swing.JPanel();
                panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
                panel.add(new javax.swing.JLabel(bundle.getString("A new version is available.")));
                panel.add(new javax.swing.JLabel(bundle.getString("Click OK to download and install it.")));
                
                if (javax.swing.JOptionPane.showConfirmDialog(this, panel, bundle.getString("Update Available"), javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.OK_OPTION)
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("http://downloads.ttm.co.at/webgen/install.exe"));
            } else {
                javax.swing.JOptionPane.showMessageDialog(this, bundle.getString("You are using the current version of WebGen."));
            }
        } catch (URISyntaxException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
            // Auch wenn eine FileNotFoundException ist liefert ex.getLocalisedMessage nur die URL und keinen besseren Fehler.
            String msg = bundle.getString("Could not determine current version.");
            javax.swing.JOptionPane.showMessageDialog(this, msg,  "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemCheckUpdateActionPerformed

    
    public static boolean isUpdateAvailable() {
        java.io.InputStream is = null;
        boolean update = false;
        try {
            java.net.URL url = new java.net.URL("http://downloads.ttm.co.at/webgen/current.txt");
            is = url.openStream();
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            String current = br.readLine();
            update = current.compareTo(versionNumber) > 0;
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return update;
    }
    
    
    private void jMenuItemDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDebugActionPerformed
        if (jMenuItemDebug.getState())
            Logger.getLogger(WebGen.class.getName()).setLevel(Level.ALL);
        else
            Logger.getLogger(WebGen.class.getName()).setLevel(Level.INFO);        
    }//GEN-LAST:event_jMenuItemDebugActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        java.util.prefs.Preferences prefs = Preferences.userRoot().node("/de/webgen");
        prefs.putInt("left", getBounds().x);
        prefs.putInt("top", getBounds().y);
        try {
            prefs.flush();
        } catch (BackingStoreException ex) {
            
        }
    }//GEN-LAST:event_formWindowClosing

    private void jServerPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jServerPaneStateChanged
        int idx = jServerPane.getSelectedIndex();
        if (idx == -1)
            return;
        
        String server = jServerPane.getTitleAt(idx);
        
        javax.swing.DefaultComboBoxModel model = (javax.swing.DefaultComboBoxModel) jComboBoxLogging.getModel();
        model.removeAllElements();
        
        synchronized (logList) {
            for (ComboBoxLogRecord record : logList) {
                if (!WebGen.threadIdMap.containsKey(record.threadId))
                    model.addElement(record);
                else if (WebGen.threadIdMap.get(record.threadId).getServer().equals(server))
                    model.addElement(record);
            }
        }
        
        while (model.getSize() > 100)
            model.removeElementAt(model.getSize() - 1);

        if (model.getSize() == 0)
            return;
        
        jComboBoxLogging.setSelectedIndex(0);

        ComboBoxLogRecord record = (ComboBoxLogRecord) model.getElementAt(0);
        if (record == null)
            return;
        
        if (Level.SEVERE.intValue() <= record.record.getLevel().intValue()) {
            jComboBoxLogging.setForeground(java.awt.Color.RED);
            jComboBoxLogging.setFont(jComboBoxLogging.getFont().deriveFont(0));                            
        } else if (Level.FINE.intValue() >= record.record.getLevel().intValue()) {
            jComboBoxLogging.setForeground(java.awt.Color.DARK_GRAY);
            jComboBoxLogging.setFont(jComboBoxLogging.getFont().deriveFont(java.awt.Font.ITALIC));                                                
        } else {
            jComboBoxLogging.setForeground(java.awt.Color.BLACK);
            jComboBoxLogging.setFont(jComboBoxLogging.getFont().deriveFont(0));                                                
        }
    }//GEN-LAST:event_jServerPaneStateChanged

    private void jMenuItemAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAddActionPerformed
        // Add a tournament to the local ini file. 

        int ret = JOptionPane.showConfirmDialog(
                        this, jPanelAddTournament, bundle.getString("Add Tournament"), 
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (ret != JOptionPane.OK_OPTION)
            return;

        String tournamentName = tournamentTextField.getText();
        String connectString = buildConnectString();
        
        try {
            Ini ini = new Ini(WebGen.iniFile);
            if (ini.get("Tournaments", tournamentName) != null) {
                JOptionPane.showMessageDialog(
                    this, bundle.getString("Tournament already exists"), 
                    bundle.getString("Add Tournament"), JOptionPane.ERROR_MESSAGE);
                
                return;
            }
            
            ini.add("Tournaments", tournamentName, connectString);
            ini.add(tournamentName, "Path", databaseTextField.getText());
            ini.store();
            
            (new File(WebGen.iniFile.getParent(), databaseTextField.getText())).mkdirs();
            
            jMenuItemOpen.setEnabled(true);
            
            openTournament(tournamentName);
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemAddActionPerformed

    private void authtypeCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_authtypeCheckBoxActionPerformed
        // Enable / Disable user and pwd according to authentication type
        if ( authtypeCheckBox.isSelected() ) {
            userTextField.setEnabled(false);
            pwdTextField.setEnabled(false);
        } else {
            userTextField.setEnabled(true);
            pwdTextField.setEnabled(true);
        }
    }//GEN-LAST:event_authtypeCheckBoxActionPerformed

    private String buildConnectString() {
        String server = serverTextField.getText();
        String dbName = databaseTextField.getText();
        boolean windowsAuth = authtypeCheckBox.isSelected();
        String user = userTextField.getText();
        String pwd = new String(pwdTextField.getPassword());
        
        StringBuilder sb = new StringBuilder();
        sb.append("DATABASE=").append(dbName).append(";");
        sb.append("DRIVER=SQL Server;");
        sb.append("SERVER=");
        if (server.equals("(local)"))
            sb.append("localhost");
        else
            sb.append(server);
        sb.append(";");
        
        if (windowsAuth)
            sb.append("Trusted_Connection=Yes;");
        else
            sb.append("Trusted_Connection=No;")
                    .append("UID=").append(user).append(";")
                    .append("PWD=").append(pwd).append(";");
        
        // Need to switch off ANSU_NULLS, Paddings, and Waarnings
        sb.append("AnsiNPW=No;");
        
        String connectionString = sb.toString();

        return connectionString;
    }
    
    private void testConnectionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testConnectionButtonActionPerformed
        String connectString = buildConnectString();
        Database db = new Database(connectString);
        
        if (db.testConnection()) {
            JOptionPane.showMessageDialog(
                this, "Database connection test successfull", "Connection Successful", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(
                this, "Database connection test failed", "Connection Failure", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_testConnectionButtonActionPerformed

    private void jPanelAddTournamentAncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_jPanelAddTournamentAncestorAdded
        // Start with focus on tournamnet input, not button
        tournamentTextField.requestFocus();
    }//GEN-LAST:event_jPanelAddTournamentAncestorAdded

    private boolean haveTournaments() {
        try {
            Ini ini = new Ini(WebGen.iniFile);
            return 
                ini.containsKey("Tournaments") &&
                !ini.getAll("Tournaments").isEmpty()
            ;
        } catch (IOException ex) {
            return false;
        }    
    }
    
    
    @Override
    public List<java.awt.Image> getIconImages() {
        // Icon from http://www.comfi.com/telecom-icons/
        java.util.ArrayList<java.awt.Image> imageList = new java.util.ArrayList<>();
        imageList.add(java.awt.Toolkit.getDefaultToolkit().getImage(getClass().getResource("/de/webgen/gui/resources/WebGen.png")));
        return imageList;
    }
    
    /**
    * @param args the command line arguments
    */
    @SuppressWarnings("UseSpecificCatch")
    public static void main(String args[]) {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable t) {

        }

        initPaths();

        if (WebGen.iniFile == null) {
            JOptionPane.showMessageDialog(null, MessageFormat.format(bundle.getString("File {0} not found"), "tt32.ini"), bundle.getString("Error"), JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        if (WebGen.templateDir == null) {
            JOptionPane.showMessageDialog(null, MessageFormat.format(bundle.getString("Directory {0} not found"), "template"), bundle.getString("Error"), JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        try {
            System.setProperty(org.ini4j.Config.KEY_PREFIX + org.ini4j.Config.PROP_ESCAPE, Boolean.FALSE.toString());
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(null, "Could not initialze org.ini4j: " + t.getLocalizedMessage(), bundle.getString("Error"), JOptionPane.ERROR_MESSAGE);            
        }
        
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    javax.swing.JFrame frame = new MainFrame();
                    
                    java.util.prefs.Preferences prefs = Preferences.userRoot().node("/de/webgen");
                    int x = prefs.getInt("left", 0);
                    int y = prefs.getInt("top", 0);
                    frame.setLocation(x, y);
                    
                    frame.setVisible(true);
                    
                    // Check for updates
                    if (isUpdateAvailable()) {
                        javax.swing.JPanel panel = new javax.swing.JPanel();
                        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
                        panel.add(new javax.swing.JLabel(bundle.getString("A new version is available.")));
                        panel.add(new javax.swing.JLabel(bundle.getString("Click OK to download and install it.")));

                        int ret = javax.swing.JOptionPane.showConfirmDialog(
                                frame, panel, bundle.getString("Update Available"), 
                                javax.swing.JOptionPane.OK_CANCEL_OPTION);
                        if (ret == javax.swing.JOptionPane.OK_OPTION)
                            java.awt.Desktop.getDesktop().browse(new java.net.URI("http://downloads.ttm.co.at/webgen/install.exe"));
                    }
                } catch (Exception ex) {
                    
                }
            }
        });
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox authtypeCheckBox;
    private javax.swing.JLabel databaseLabel;
    private javax.swing.JTextField databaseTextField;
    private javax.swing.JComboBox jComboBoxLogging;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu jMenuHelp;
    private javax.swing.JMenuItem jMenuItemAbout;
    private javax.swing.JMenuItem jMenuItemAdd;
    private javax.swing.JMenuItem jMenuItemCheckUpdate;
    private javax.swing.JCheckBoxMenuItem jMenuItemDebug;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemOpen;
    private javax.swing.JMenuItem jMenuItemServer;
    private javax.swing.JMenu jMenuTournment;
    private javax.swing.JPanel jPanelAddTournament;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JTabbedPane jServerPane;
    private javax.swing.JLabel pwdLabel;
    private javax.swing.JPasswordField pwdTextField;
    private javax.swing.JLabel serverLabel;
    private javax.swing.JTextField serverTextField;
    private javax.swing.JButton testConnectionButton;
    private javax.swing.JLabel tournamentLabel;
    private javax.swing.JTextField tournamentTextField;
    private javax.swing.JLabel userLabel;
    private javax.swing.JTextField userTextField;
    // End of variables declaration//GEN-END:variables
}
