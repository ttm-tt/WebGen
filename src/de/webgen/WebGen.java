/* Copyright (C) 2020 Christoph Theis */

package de.webgen;

import com.enterprisedt.net.ftp.FTPException;
import de.webgen.database.Competition;
import de.webgen.database.Database;
import de.webgen.database.Group;
import de.webgen.database.Player;
import de.webgen.database.Team;
import de.webgen.database.match.Match;
import de.webgen.generator.report.Report;
import de.webgen.generator.report.Report.ReportType;
import de.webgen.generator.DateGenerator;
import de.webgen.generator.Generator;
import de.webgen.generator.Generator.FilterTypes;
import static de.webgen.generator.Generator.SEP;
import de.webgen.generator.KOGenerator;
import de.webgen.generator.PKOGenerator;
import de.webgen.generator.PlayersMatchesGenerator;
import de.webgen.generator.RRGenerator;
import de.webgen.generator.ReportGenerator;
import de.webgen.prefs.PasswordCrypto;
import de.webgen.xml.XmlDate;
import de.webgen.xml.XmlGroup;
import de.webgen.xml.XmlProperties;
import de.webgen.xml.XmlReport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.ini4j.Ini;


public class WebGen {
    // public, ich brauche sie an vielen Stellen
    public static String baseDir = null;
    public static File   iniFile = null;
    public static String templateDir = null;
    
    // Convenience: we need the Charset in FileWriter
    public class FileWriter extends java.io.OutputStreamWriter {
        public FileWriter(String file) throws IOException {
            super(new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8);
        }
        
        public FileWriter(String file, boolean append) throws IOException {
            super(new java.io.FileOutputStream(file, append), java.nio.charset.StandardCharsets.UTF_8);
        }
        
        public FileWriter(File file) throws IOException {
            super(new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8);
        }
        
        public FileWriter(File file, boolean append) throws IOException {
            super(new java.io.FileOutputStream(file, append), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    
    public static class LivetickerProperties {
        public LivetickerProperties() {
        }
        
        private String[] venues = new String[] {"update"};
        private int      timeout = 15;

        public String getVenues() {
            return toString(venues);
        }

        public void setVenues(String venues) {
            this.venues = fromString(venues);
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }    
        
        
        private String toString(String[] array) {
            StringBuilder sb = new StringBuilder();
            for (String s : array) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(s);
            }

            return sb.toString();
        }


        private String[] fromString(String list) {
            String[] array = list.replaceAll(", ", ",").split(",");
            return array;
        }
        
    }
    
    
    public static class RefreshInterval {
        private final int interval;

        public RefreshInterval(int seconds) {
            interval = seconds;
        }

        @Override
        public boolean equals(Object ri) {
            return (ri instanceof RefreshInterval) && interval == ((RefreshInterval) ri).interval;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 79 * hash + this.interval;
            return hash;
        }

        @Override
        public String toString() {
            java.util.ResourceBundle bundle =
                    java.util.ResourceBundle.getBundle("de/webgen/gui/resources/WebGen"); // NOI18N

            if (interval >= 120 || interval < 60)
                return (interval / 60) + " " + bundle.getString("Minutes");
            else
                return (interval / 60) + " " + bundle.getString("Minute");
        }

        public int getInterval() {
            return interval;
        }
    }

    public static RefreshInterval[] getIntervals() {
        return new RefreshInterval[] {
            new RefreshInterval(1 * 60),
            new RefreshInterval(2 * 60),
            new RefreshInterval(5 * 60),
            new RefreshInterval(10 * 60),
            new RefreshInterval(20 * 60),
            new RefreshInterval(30 * 60),
            new RefreshInterval(45 * 60),
            new RefreshInterval(60 * 60)
        };
    }
    
    public static enum FlagType {
        None,
        Nation,
        Region       
    }

    public static boolean isITTF() {
        return tableType == 1;
    }
    
    
    // -------------------------------------------------------------------------
    // Set things up
    @SuppressWarnings("UseSpecificCatch")
    public WebGen(String server, String tournament, String path) {
        this.server = server;
        this.tournament = tournament;
        this.path = path;
        try {
            this.database = new Database(new Ini(WebGen.iniFile).get("Tournaments", tournament));

            tableType = database.getTableType();            

            // Wenn index.xml noch nicht existiert, initialisieren
            if ( !(new File(path + File.separator + "index.xml")).exists() )
                initializeServer();
            else {
                // Sicherstellen, dass alle Verzeichnisse existieren, auch wenn sie leer sind
                // Mit einem Update kam "flags" dazu
                File folder;
                if ( (folder = new File(path, "flags")).exists())
                    folder.mkdirs();
            }

            processIndexFile();
        } catch (Exception ex) {
            Logger.getLogger(WebGen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // Initialisiere neuen Server
    private void initializeServer() throws IOException, ParserConfigurationException {
        (new File(path)).mkdirs();
        (new File(path + File.separator + "css")).mkdirs();
        (new File(path + File.separator + "js")).mkdirs();
        (new File(path + File.separator + "fonts")).mkdirs();
        (new File(path + File.separator + "img")).mkdirs();        
        (new File(path + File.separator + "flags")).mkdirs();        
        
        // Files von template kopieren
        copyFiles(WebGen.templateDir + File.separator + "css", path + File.separator + "css", ".css");
        copyFiles(WebGen.templateDir + File.separator + "js", path + File.separator + "js", ".js");
        copyFiles(WebGen.templateDir + File.separator + "fonts", path + File.separator + "fonts", null);
        copyFiles(WebGen.templateDir + File.separator + "img", path + File.separator + "img", null);
        copyFiles(WebGen.templateDir + File.separator + "flags", path + File.separator + "flags", ".png");
        copyFiles(WebGen.templateDir, path + File.separator, ".html");
        
        copyFile(WebGen.templateDir + File.separator + "index.html", path + File.separator + "index.html");

        // index.xml erzeugen
        File indexFile = new File(path + File.separator + "index.xml");

        if (!indexFile.exists()) {
            indexFile.createNewFile();

            new XmlProperties().store(indexFile.getPath());
        }
    }

    private void copyFiles(final String sourceDir, final String destDir, final String ext) {
        // copy Files
        File[] files;

        files = (new File(sourceDir)).listFiles(new java.io.FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                if (name.startsWith("."))
                    return false;
                
                // Exclude backup files
                if (name.endsWith(".swp") || name.endsWith(".bak") || name.endsWith("~"))
                    return false;
                
                return ext == null || name.toLowerCase().endsWith(ext);
            }
        });

        if (files == null)
            return;
        
        for (File file : files) {
            copyFile(file.getPath(), destDir + File.separator + file.getName());
        }

    }

    private void copyFile(String source, String dest) {
        java.io.FileInputStream  fis = null;
        java.io.FileOutputStream fos = null;
        try {
            fis = new java.io.FileInputStream(new File(source));
            fos = new java.io.FileOutputStream(new File(dest));

            byte[] tmp = new byte[1024];
            int    len = 0;

            while ( (len = fis.read(tmp)) > 0 )
                fos.write(tmp, 0, len);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WebGen.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WebGen.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fis != null)
                    fis.close();
                if (fos != null)
                    fos.close();
            } catch (IOException ex) {
                Logger.getLogger(WebGen.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    
    // -------------------------------------------------------------------------
    // Access to index.xml
    private void processIndexFile() throws SQLException {
        xmlProperties = new XmlProperties();
        xmlProperties.load(path + File.separator + "index.xml");
        
        xmlProperties.setPublishedGroups(database.readPublishedGroups());
    }
    
    public Competition[] getEvents() {
        try {
            return database.readEvents();
        } catch (SQLException ex) {
            Logger.getLogger(WebGen.class.getName()).log(Level.SEVERE, null, ex);

            return new Competition[0];
        }
    }

    public Group[] getGroups(Competition cp) {
        try {
            return database.readGroups(cp);
        } catch (SQLException ex) {
            Logger.getLogger(WebGen.class.getName()).log(Level.SEVERE, null, ex);
            return new Group[0];
        }
    }
    
    public void setGroupEnabled(Group gr, boolean enabled) {
        xmlProperties.setGroupEnabled(gr, enabled);
    }
    
    public boolean isGroupEnabled(Group gr) {
        return xmlProperties.isGroupEnabled(gr);
    }
    
    public void setGroupDisabled(Group gr, boolean disabled) {
        xmlProperties.setGroupDisabled(gr, disabled);
    }
    
    public boolean isGroupDisabled(Group gr) {
        return xmlProperties.isGroupDisabled(gr);
    }
    
    public boolean isGroupPublished(Group gr) {
        return xmlProperties.isGroupPublished(gr);
    }
    
    public Report[] getReports() {
        Set<Integer> typeSet = new java.util.HashSet<>();
        
        Competition[] events = getEvents();
        for (Competition event : events) {
            typeSet.add(event.cpType);
        }
        
        List<Report> list = new java.util.ArrayList<>();
        for (int i = 0; i < Report.ReportType.values().length; i++) {
            switch (ReportType.values()[i])  {
                case Players :
                    list.add(Report.getReportByType(i));
                    break;
                    
                case Singles :
                    if (typeSet.contains(Competition.CP_SINGLE))
                        list.add(Report.getReportByType(i));
                    break;
                    
                case Doubles :
                    if (typeSet.contains(Competition.CP_DOUBLE))
                        list.add(Report.getReportByType(i));
                    break;
                    
                case Mixed :
                    if (typeSet.contains(Competition.CP_MIXED))
                        list.add(Report.getReportByType(i));
                    break;
                    
                case Teams :
                    if (typeSet.contains(Competition.CP_TEAM))
                        list.add(Report.getReportByType(i));
                    break;
                    
                case PartnerWanted :
                    if (typeSet.contains(Competition.CP_DOUBLE) || typeSet.contains(Competition.CP_MIXED) || typeSet.contains(Competition.CP_TEAM))
                        list.add(Report.getReportByType(i));
                    break;
            }
        }

        return list.toArray(new Report[0]);
    }
    
    public void setReportActive(Report rep, boolean active) {
        xmlProperties.setReportActive(rep, active);
    }
    
    public boolean isReportActive(Report rep) {
        return xmlProperties.isReportActive(rep);
    }
    
    public String getFtpHost() {
        return xmlProperties.getFtpSettings().host;
    }

    public void setFtpHost(String host) {
        xmlProperties.getFtpSettings().host = host;
    }

    public String getFtpDirectory() {
        return xmlProperties.getFtpSettings().dir;
    }

    public void setFtpDirectory(String dir) {
        xmlProperties.getFtpSettings().dir = dir;
    }

    public String getFtpUser() {
        return xmlProperties.getFtpSettings().user;
    }

    public void setFtpUser(String user) {
        xmlProperties.getFtpSettings().user = user;
    }

    public String getFtpPassword() {
        return decryptPassword(xmlProperties.getFtpSettings().pwd);
    }

    public void setFtpPassword(String password) {
        xmlProperties.getFtpSettings().pwd = encryptPassword(password);
    }

    public void setFtpPassive(boolean ftpPassive) {
        xmlProperties.getFtpSettings().passive = ftpPassive;
    }

    public boolean getFtpPassive() {
        return xmlProperties.getFtpSettings().passive;
    }

    public void setFtpDebug(boolean ftpDebug) {
        this.ftpDebug = ftpDebug;
    }
    
    public void setFtpSecure(boolean ftpSecure) {
        xmlProperties.getFtpSettings().sftp = ftpSecure;
    }
    
    public boolean getFtpSecure() {
        return xmlProperties.getFtpSettings().sftp;
    }

    public RefreshInterval getInterval() {
        int tmp = xmlProperties.getSettings().interval;
        if (tmp == 0)
            tmp = 5;
        
        return new RefreshInterval(60 * tmp);
    }

    public void setInterval(RefreshInterval interval) {
        xmlProperties.getSettings().interval = interval.getInterval() / 60;
    }

    public void setIncludeGroups(boolean includeGroups) {
        xmlProperties.getSettings().autoGroups = includeGroups;
    }

    public boolean getIncludeGroups() {
        return xmlProperties.getSettings().autoGroups;
    }
    
    public void setTitle(String title) {
        xmlProperties.getSettings().title = title;
    }
    
    public String getTitle() {
        return xmlProperties.getSettings().title;
    }
    
    public void setDescription(String description) {
        xmlProperties.getSettings().description = description;
    }
    
    public String getDescription() {
        return xmlProperties.getSettings().description;
    }
        
    public void setTournamentUrl(String url) {
        xmlProperties.getSettings().url = url;
    }
    
    public String getTournamentUrl() {
        return xmlProperties.getSettings().url;
    }
    
    public void setUserConfig(String config) {
        xmlProperties.getSettings().userConfig = config;
    }
    
    public String getUserConfig() {
        return xmlProperties.getSettings().userConfig;
    }
    
    public void setLivetickerEnabled(boolean e) {
        xmlProperties.getSettings().ltEnabled = e;
    }
    
    public boolean getLivetickerEnabled() {
        return xmlProperties.getSettings().ltEnabled;
    }
    
    public void setLivetickerSettings(LivetickerProperties props) {
        xmlProperties.getSettings().ltSettings = gson.toJson(props, LivetickerProperties.class);
    }
        
    
    public LivetickerProperties getLivetickerSettings() {
        LivetickerProperties props = gson.fromJson(xmlProperties.getSettings().ltSettings, LivetickerProperties.class);
        if (props == null) {
            props = new LivetickerProperties();
        }
        
        return props;
    }

    public void setNewsUrl(String url) {
        xmlProperties.getSettings().news = url;
    }
        
    public String getNewsUrl() {
        return xmlProperties.getSettings().news;
    }
        
    public void setFlagType(FlagType flagType) {
        xmlProperties.getSettings().flagType = flagType;
    }
    
    public FlagType getFlagType() {
        return xmlProperties.getSettings().flagType;
    }
    
    
    public String getTournament() {
        return tournament;
    }
    
    
    public String getServer() {
        return server;
    }
    
    
    // -------------------------------------------------------------------------
    // Run WebGen
    public String getHtmlIndexFile() {
        return this.path + File.separator + "index.html";
    }
    
    public static final Map<Long, WebGen> threadIdMap = new java.util.HashMap<>();
    
    /**
     * Generierung starten.
     * @return true, wenn erfolgreich
     */
    public boolean start() {
        if (timer != null)
            return true;

        threadIdMap.put(Thread.currentThread().getId(), this);
        
        Logger.getLogger(getClass().getName()).log(Level.INFO, "WebGen started");
        
        try {
            // Create config.js, passiert nur beim Start
            createConfigFile();
        } catch (IOException | SQLException ex) {
            Logger.getLogger(WebGen.class.getName()).log(Level.SEVERE, null, ex);
        }

        // newsLastModified zuruecksetzen, damit die immer bei einem Start gelesen werden
        newsLastModified = 0;

        timer = new Timer();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                WebGen.this.run();
            }
        }).start();

        return true;
    }

    public void stop() {
        if (timerTask != null)
            timerTask.cancel();
        
        timer = null;
        timerTask = null;
        
        threadIdMap.put(Thread.currentThread().getId(), this);
        
        Logger.getLogger(getClass().getName()).log(Level.INFO, "WebGen stopped");
    }

    public boolean isRunning() {
        return timer != null;
    }

    public long scheduledExectionTime() {
        return timerTask == null ? 0 : timerTask.scheduledExecutionTime();
    }

    private void run() {
        threadIdMap.put(Thread.currentThread().getId(), WebGen.this);
        
        try {
            createFiles();
            
            updateGroups();
            updateDates();
            updateReports();
            updateNews();
            
            upload();
        
            // Allfaellige Aenderugnen im XML-Doc sichern
            xmlProperties.store(path + File.separator + "index.xml");        
            
            database.closeConnection();            
        } catch (IOException ex) {
            Logger.getLogger(WebGen.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(WebGen.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Throwable t) {
            Logger.getLogger(WebGen.class.getName()).log(Level.SEVERE, null, t);
        } 
        
        // timer kann hier schon wieder null sein, enn die Generierung gestoppt wurde
        if (timer != null) {
            timer.schedule( (timerTask = new TimerTask() {
                @Override
                public void run() {
                    WebGen.this.run();
                }
            }), getInterval().getInterval() * 1000);
        }
    }
    
    
    // -------------------------------------------------------------------------
    // Prueft index.xml, ob Eintraege verschwunden sind
    // Prueft / erzeugt index.html, etc.
    private void createFiles() throws IOException, SQLException {
        // index.xml auf obsolete Eintraege pruefen
        updateIndexXml();        
    }
    
    private void updateIndexXml() throws IOException, SQLException {
        boolean updateEventsFile = new File(getEventsFileName()).exists() == false;
        boolean updateReportsFile = new File(getReportsFileName()).exists() == false;
        boolean updateDatesFile = new File(getDatesFileName()).exists() == false;
        
        // Liste mit Gruppen und Datum
        List<XmlGroup> groupList = xmlProperties.getGroups();
        
        // Liste der Gruppen, die spielen, sind automatisch published
        Set<Integer> includedGroups = new java.util.HashSet<>();
        if (getIncludeGroups()) {
            for (Group gr : database.readCurrentGroups()) {
                includedGroups.add(gr.grID);
                
                if (!xmlProperties.hasGroup(gr)) {
                    xmlProperties.addGroup(gr);
                    updateEventsFile = true;
                }
                
                if (!xmlProperties.isGroupPublished(gr)) {
                    xmlProperties.setGroupPublished(gr, true);
                    updateEventsFile = true;
                }
            }
        }

        // Abgleich DB gegen index
        Map<String, Set<String>> cpMap = new java.util.HashMap<>();
        Set<String> dateSet = new java.util.HashSet<>();
        
        File eventsFile = new File(getEventsFileName());
        long eventFileTS = eventsFile.exists() ? eventsFile.lastModified() : 0;
        
        for (Competition cp : database.readEvents()) {
            File cpFile = new File(path + File.separator + cp.cpName + ".pdf");
            updateEventsFile |= cpFile.exists() && cpFile.lastModified() > eventFileTS;
            
            Set<String> grSet = new java.util.HashSet<>();
            for (Group gr : database.readGroups(cp)) {
                grSet.add(gr.grName);
                
                if (!gr.grPublished && !xmlProperties.isGroupIncluded(gr))
                    continue;
                
                if (xmlProperties.isGroupPublished(gr) != gr.grPublished) {
                    // Update grPublished, but not if the group is included because they are playing
                    if (gr.grPublished || !includedGroups.contains(gr.grID)) {
                        xmlProperties.setGroupPublished(gr, gr.grPublished);
                        updateEventsFile = true;
                    }
                }
                
                XmlGroup xmlGroup = xmlProperties.getGroup(gr);
                if (xmlGroup != null) {
                    if (    // Category
                            (xmlGroup.cpCategory == null && gr.cp.cpCategory != null) ||
                            (xmlGroup.cpCategory != null && gr.cp.cpCategory == null) ||
                            (xmlGroup.cpCategory != null && !xmlGroup.cpCategory.equals(gr.cp.cpCategory)) ||
                            // Sort order
                            (xmlGroup.grSortOrder != gr.grSortOrder) ||
                            // Stage
                            (!xmlGroup.grStage.equals(gr.grStage)) 
                    ) {
                        xmlGroup.cpCategory = gr.cp.cpCategory;
                        xmlGroup.grSortOrder = gr.grSortOrder;
                        xmlGroup.grStage = gr.grStage;
                        updateEventsFile = true;
                    }
                }
                
                // Dates aktualisieren
                for (Timestamp ts : database.getMatchDates(gr)) {
                    String date = getTimestampDate(ts);
                    
                    if (!xmlProperties.hasDate(date)) {
                        // Datum bislang unbekannt, daher auch das dates-File aktualisieren
                        updateDatesFile = true;
                        xmlProperties.addDate(new XmlDate(date));
                        updateDatesFile = true;
                    }
                    
                    dateSet.add(date);
                }
            }
            
            cpMap.put(cp.cpName, grSet);
        }
        
        // Und umgekehrt: Aus index alle Gruppen rauswerfen, die nicht mehr relevant sind
        for (XmlGroup group : groupList) {
            String cpName = group.cpName;
            String grName = group.grName;
            
            if (!cpMap.containsKey(cpName)) {
                xmlProperties.removeAllGroups(cpName);
                updateEventsFile = true;
            }
            else if (!cpMap.get(cpName).contains(grName)) {
                updateEventsFile = true;
                xmlProperties.removeGroup(group);
            }
        }
        
        // Und alle Daten, die nicht mehr relevant sind
        for (XmlDate date : xmlProperties.getDates()) {
            if (!dateSet.contains(date.name)) {
                xmlProperties.removeDate(date);
                updateDatesFile = true;
            }
        }
        
        // Dto. fuer Reports
        Report[] reports = getReports();
        
        Set<String> reportSet = new java.util.HashSet<>();
        
        for (Report report : reports) {
            if (!xmlProperties.hasReport(report)) {
                xmlProperties.addReport(report);
                updateReportsFile = true;
            }
            
            reportSet.add(report.getType().name());
        }
        
        List<XmlReport> list = xmlProperties.getReports();

        for (XmlReport xmlReport : list) {
            if (!reportSet.contains(xmlReport.name)) {
                xmlProperties.removeReport(xmlReport);
                updateReportsFile = true;
            }
        }    
        
        if (updateEventsFile)
            createEventsFile();
        if (updateDatesFile)
            createDatesFile();
        if (updateReportsFile)
            createReportsFile();
    }

    // -------------------------------------------------------------------------
    private String getConfigFileName() {
        return path + File.separator + "js" + File.separator + "config.js";
    }
    
    private void createConfigFile() throws IOException, SQLException {
        java.io.File configFile = new File(getConfigFileName());
        
        String title = getTitle();
        String description = getDescription();
        String url = getTournamentUrl();
        WebGen.LivetickerProperties props = getLivetickerSettings();
        boolean livetickerEnabled = getLivetickerEnabled();
        String news = getNewsUrl();
        String userConfig = getUserConfig();
        FlagType flagType = getFlagType();
        
        StringBuilder config = new StringBuilder();
        config
            .append("var config = {").append("\n")
            .append("  title : '").append(title.replaceAll("'", "&rsquo;")).append("',").append("\n")
            .append("  description : '").append(description.replaceAll("'", "&rsquo;")).append("',").append("\n")
            .append("  url : ").append(url.isEmpty() ? "false" : "'" + url + "'").append(",").append("\n")
            .append("  config : '").append(userConfig).append("',").append("\n")
            .append("  flagtype: ").append(flagType.ordinal()).append(",").append("\n")
        ;
        
        if (livetickerEnabled) {
            config
                .append("  liveticker : {").append("\n")
                .append("    venues : ").append(gson.toJson(props.venues)).append(", ").append("\n")
                .append("    timeout : ").append(props.getTimeout()).append(", ").append("\n")
                .append("    noUpdate : ").append("false").append("\n")
                .append("  }").append(", ").append("\n");
        } else {
            config.append("  liveticker : false").append(", ").append("\n");
        }
        
        config
            .append("  news : ").append(news.isEmpty() ? "false" : "'news.xml'").append("\n")
            .append("};")
        ;
        
        try (FileWriter fw = new FileWriter(configFile)) {
            fw.write(config.toString());
        }        
    }
    
    private String getEventsFileName() {
        return path + File.separator + "events.html";
    }
    
    
    private void createEventsFile() throws IOException, SQLException {
        String SEP = de.webgen.generator.Generator.SEP;
        
        List<XmlGroup> xmlGroups = xmlProperties.getGroups();

        xmlGroups.sort(new java.util.Comparator<XmlGroup>() {
            @Override
            public int compare(XmlGroup o1, XmlGroup o2) {
                return o1.compare(o2);
            }
        });

        StringBuilder buf = new StringBuilder();
        FileWriter    grFile = null;
        
        buf.append("<div class=\"row\">").append(SEP);
        buf.append("<ul class=\"list-group col-12\">").append(SEP);
        
        // Marker, wenn ein neuer WB kommt
        Set<String> cpSet = new java.util.HashSet<>();
        List<Group> grList = new java.util.ArrayList<>();
        
        Competition lastCp = null;
        Competition cp = null;
        
        for (XmlGroup xmlGroup : xmlGroups) {  
            String cpName = xmlGroup.cpName;
            String grName = xmlGroup.grName;
            
            File pdf = null;
            
            if ( !cpSet.contains(cpName) ) {
                if (grFile != null) {
                    // Close old event
                    grFile.write(createGroupsFile(grList));
                    grFile.close();
                    grFile = null;
                }
                
                grList.clear();
                
                // Neuer Wettbewerb
                cp = database.readEvent(cpName);

                if (cp == null) {
                    // WB wurde geloescht
                    xmlProperties.removeAllGroups(cpName);
                    continue;
                }
                
                // Close last category
                if (lastCp != null && lastCp.cpCategory != null && !lastCp.cpCategory.equals(cp.cpCategory)) {
                    buf.append("</ul>").append(SEP);
                    buf.append("</li>").append(SEP);
                }
                
                // Open next category
                if (cp.cpCategory != null && !cp.cpCategory.equals(lastCp == null ? null : lastCp.cpCategory)) {
                    buf
                            .append("<li class=\"list-group-item list-group-action bg-light p-0\" data-bs-toggle=\"collapse\" ")
                            .append("data-bs-target=\"[data-webgen-category=&quot;").append(cp.cpCategory).append("&quot;]\">")
                    ;
                    buf
                            .append("<span class=\"btn list-group-item bg-light text-left w-100 border-0 cpcateory\">")
                            .append("<span>").append(cp.cpCategory).append("</span></span>")
                    ;
                    buf
                            .append("<ul class=\"collapse list-group col-12 pr-0\" data-webgen-category=\"").append(cp.cpCategory).append("\">")
                            .append(SEP)
                    ;
                }
                
                lastCp = cp;
                
                cpSet.add(cp.cpName);
                
                grFile = new FileWriter(new File(path, cp.getFileName() + ".html"));

                buf
                        .append("<li class=\"list-group-item list-group-action")
                        .append(cp.cpCategory != null ? " border-left-0 border-right-0" : " bg-light")
                        .append("\"/>")
                ;
                
                buf
                        .append("<span class=\"cpname pr-1\">")
                        .append("<a ")
                        .append(    "href=\"").append(cp.getFileName()).append(".html\">")            
                        .append("<span>").append(cp.cpDesc).append("</span>")
                        .append("</a>")
                        .append("</span>")
                ;
                
                // Link auf PDF  
                if ( (pdf = new File(path, cp.cpName + ".pdf")).exists()) {
                    buf
                        .append("<span class=\"pdf\">")
                        .append("<a class=\"pdf\" href=\"").append(pdf.getName()).append("\" target=\"_blank\">" + "(PDF)" + "</a>")
                        .append("</span>")
                    ;
                }
                 
                buf
                        .append("</li>")
                        .append(SEP)
                ;                
            }
            
            Group gr = database.readGroup(cp, grName);
            if (gr == null) {
                // Gruppe wurde geloescht
                xmlProperties.removeGroup(xmlGroup);
                continue;
            }
            
            grList.add(gr);
        }
        
        if (grFile != null) {
            grFile.write(createGroupsFile(grList));
            grFile.close();
            grFile = null;
        }
        
        grList.clear();
                
        // Und schliessen
        if (lastCp != null && lastCp.cpCategory != null) {
            buf.append("</ul>").append(SEP);
            buf.append("</li>").append(SEP);
        }
        buf.append("</ul>").append(Generator.SEP);
        buf.append("</div>").append(Generator.SEP);

        try (FileWriter cpFile = new FileWriter(new File(getEventsFileName()))) {
            cpFile.write(buf.toString());
        }
    }
    
    private String createGroupsFile(List<Group> grList) throws IOException {
        String SEP = Generator.SEP;
        
        if (grList == null || grList.isEmpty())
            return "";
        
        grList.sort(new java.util.Comparator<Group>() {
            @Override
            public int compare(Group o1, Group o2) {
                return o1.compareTo(o2);
            }
        });
        
        StringBuilder grBuf = new StringBuilder();
                
        grBuf
            .append("<div class=\"col-12\" id=\"group\">")
        ;

        // Filter
        grBuf.append(Generator.writeFilter(EnumSet.of(FilterTypes.FilterGroup), null, null, grList.toArray(new Group[0])));
        
        // Content
        grBuf.append("<div class=\"row\" id=\"group-content\"></div>");
                
        return grBuf.toString();
    }

    private String getReportsFileName() {
        return path + File.separator + "reports.html";
    }

    private void createReportsFile() {
        List<XmlReport> list = xmlProperties.getReports();

        list.sort(new java.util.Comparator<XmlReport>() {

            @Override
            public int compare(XmlReport o1, XmlReport o2) {
                return o1.compare(o2);
            }
        });

        StringBuilder buf = new StringBuilder();
        
        buf.append("<div class=\"row\">").append(SEP);
        buf.append("<div class=\"list-group col-12\">").append(SEP);

        for (XmlReport xmlReport : list) {
            Report.ReportType rt = Report.ReportType.valueOf(xmlReport.name);
            Report report = Report.getReportByType(rt);
            
            buf
                    .append("<a class=\"list-group-item list-group-item-action bg-light\" ")
                    .append(    "href=\"").append(report.getFileName()).append(".html\">")            
                    .append("<span data-i18n=\"report.name.").append(report.toString().toLowerCase()).append("\">").append("</span>")
                    .append("</a>")
                    .append(SEP)
            ;
        }
        
        buf.append("</div>").append(Generator.SEP);
        buf.append("</div>").append(Generator.SEP);

        try {
            try (FileWriter reportsFile = new FileWriter(new File(getReportsFileName()))) {
                reportsFile.write(buf.toString());
            }
        } catch (java.io.IOException ex) {
            Logger.getLogger(WebGen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateNews() throws IOException {
        if ( getNewsUrl().isEmpty() )
            return;

        StringBuilder content = new StringBuilder();
        
        try {
            java.net.URL url = new java.net.URL(getNewsUrl());
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setIfModifiedSince(newsLastModified);
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.connect();
            if (conn.getResponseCode() != 200)
                return;

            byte[] bytes = new byte[1024];
            int len;

            // Skip BOM (EF BB BF) so vorhanden
            len = conn.getInputStream().read(bytes, 0, 3);
            if (len != 3 || bytes[0] != (byte) 0xEF || bytes[1] != (byte) 0xBB || bytes[2] != (byte) 0xBF)
                content.append(new String(bytes, 0, len, "UTF-8"));

            while ( (len = conn.getInputStream().read(bytes)) > 0 )
                content.append(new String(bytes, 0, len, "UTF-8"));

            newsLastModified = conn.getLastModified();
        } catch (java.net.SocketTimeoutException e) {
            return;
        }
        
        try (FileWriter fw = new FileWriter(new File(path, "news.xml"))) {
            fw.write(content.toString());
        }        
    }

    @SuppressWarnings("empty-statement")
    private void updateGroups() throws IOException, SQLException {
        List<Group> groups = new java.util.ArrayList<>();
        
        for (Competition cp : database.readEvents()) {
            for (Group gr : database.readGroups(cp)) {
                if (xmlProperties.isGroupActive(gr))
                    groups.add(gr);
            }
        }
        
        for (Group gr : groups) {
            database.updateTimestamp(gr);
            
            XmlGroup xmlGroup = xmlProperties.getGroup(gr);
            if (xmlGroup == null)
                continue;
                        
            // Filedatum pruefen
            File file = new File(path + File.separator + gr.getFileName() + ".html");
            if (!file.exists())
                xmlGroup.ts = new Timestamp(0);
            else if (xmlGroup.ts.getTime() == 0)
                xmlGroup.ts = new Timestamp(file.lastModified());
            else if (file.lastModified() < xmlGroup.ts.getTime())
                xmlGroup.ts = new Timestamp(file.lastModified());
        }

        java.util.Collections.sort(groups, new java.util.Comparator<Group>() {
            @Override
            public int compare(Group o1, Group o2) {
                return o1.timestamp.compareTo(o2.timestamp);
            }
        });

        for (Group gr : groups) {
            XmlGroup xmlGroup = xmlProperties.getGroup(gr);

            if (gr.timestamp.compareTo(xmlGroup.ts) <= 0)
                continue;

            updateGroup(gr, xmlGroup.ts);

            xmlGroup.ts = gr.timestamp;
        }
    }


    /**
     * Generiert das / die File fuer eine Gruppe
     * @param group
     * @param ts
     * @throws IOException
     */
    private void updateGroup(Group group, Timestamp ts) throws IOException, SQLException {
        Logger.getLogger(WebGen.class.getName()).log(Level.INFO, "Generate group {0}", group.getFileName());

        List<List<Match>> matchList = database.readMatches(group);

        String page = null;

        switch (group.grModus) {
            case Group.MOD_RR :
                page = (new RRGenerator()).generate(matchList, database);
                break;

            case Group.MOD_SKO :
                page = (new KOGenerator()).generate(matchList, database);
                break;

            case Group.MOD_PLO :
                page = (new PKOGenerator()).generate(matchList, database);
                break;

            case Group.MOD_DKO :
                throw new UnsupportedOperationException("Not supported yet.");
        }

        try (FileWriter fw = new FileWriter(new File(path + File.separator + group.getFileName() + ".html"))) {
            fw.write(page);
        }
    }

    private void updateReports() throws SQLException {
        Report[] reports = getReports();
        for (Report report : reports) {  
            if (!xmlProperties.hasReport(report))
                continue;
            
            XmlReport xmlReport = xmlProperties.getReport(report);
            
            File file = new File(path + File.separator + report.getFileName() + ".html");
            
            if (!file.exists())
                xmlReport.ts = new Timestamp(0);
            else if (xmlReport.ts.getTime() == 0)
                xmlReport.ts = new Timestamp(file.lastModified());
            else if (file.lastModified() < xmlReport.ts.getTime())
                xmlReport.ts = new Timestamp(file.lastModified());
            
            Timestamp ts = report.getTimestamp(database);
            if (ts == null)
                ts = new Timestamp(0);
            
            if (file.exists() && ts.compareTo(xmlReport.ts) <= 0)
                continue;
            
            xmlReport.ts = ts;
            
            Logger.getLogger(WebGen.class.getName()).log(Level.INFO, "Generate report {0}", report.getFileName());

            try {
                String page = (new ReportGenerator()).generate(report, database);
                try (FileWriter fw = new FileWriter(file)) {
                    fw.write(page);
                }
            } catch (IOException ex) {
                Logger.getLogger(WebGen.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private String getDatesFileName() {
        return path + File.separator + "dates.html";
    }
    
    private void createDatesFile() {
        List<XmlDate> list = xmlProperties.getDates();

        list.sort(new java.util.Comparator<XmlDate>() {

            @Override
            public int compare(XmlDate o1, XmlDate o2) {
                return o1.name.compareTo(o2.name);
            }
        });
                        
        StringBuilder buf = new StringBuilder();
        
        buf.append("<div class=\"row\">").append(Generator.SEP);
        buf.append("<div class=\"list-group col-12\">").append(Generator.SEP);

        for (XmlDate xmlDate : list) {
            Timestamp ts = Timestamp.valueOf(xmlDate.name + " 00:00:00");
            
            buf
                .append("<a class=\"list-group-item list-group-item-action bg-light\" ")
                .append("href=\"").append(getTimestampDate(ts)).append(".html\"")
                .append(">");
            buf
                .append("<span ")
                .append("data-i18n=\"dates.date.format\" ")
                .append("data-i18n-options=\'{")
                .append("\"day\": \"").append(String.format("%02d", ts.getDate())).append("\", ")
                .append("\"wday\": \"").append(ts.getDay() + 1).append("\", ")
                .append("\"month\": \"").append(ts.getMonth() + 1) .append("\"")
                .append("}\'")
                .append(">")
            ;
            buf.append("</span>");
            buf.append("</a>").append(Generator.SEP);
        }
        
        buf.append("</div>").append(Generator.SEP); // list-group
        buf.append("</div>").append(Generator.SEP); // row

        try (FileWriter reportsFile = new FileWriter(new File(getDatesFileName()))) {
            reportsFile.write(buf.toString());
        } catch (java.io.IOException ex) {
            Logger.getLogger(WebGen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void updateDates() throws SQLException, IOException {
        Generator generator = new DateGenerator();
        
        // Zusammen mit den "Matches per Day" werden auch die Spiele je Spieler und Mannschaft generiert
        // Dazu wird aus den Spielen, die sich seit dem letzten Mal geaendert haben, eine Liste
        // von Startnummern und Mannschaft IDs erstellt und fuer diese wird im Anschluss
        // das File mit allen Spielen dieser Spieler bzw. Mannschaften erstellt.
        
        // Liste der Startnummern
        Set<Integer> plNrs = new java.util.HashSet<>();
        // Liste der Mannschaft IDs
        Set<Integer> tmIDs = new java.util.HashSet<>();
        
        // Da in den Spielen nur ein ID vorkommt, ich aber die Dateien nach
        // Mannschaftsname und Wettbewerb benenne brauche ich ein mapping ID => Team
        Map<Integer, Team> teams = new java.util.HashMap<>();
        
        for (Competition cp : database.readEvents()) {
            if (cp.cpType != 4)
                continue;
            
            for (Team tm : database.getTeams(cp))
                teams.put(tm.tmID, tm);
        }
            
        for (XmlDate xmlDate : xmlProperties.getDates()) {
            File file = new File(path + File.separator + xmlDate.name + ".html");

            if (!file.exists())
                xmlDate.ts = new Timestamp(0);
            else if (xmlDate.ts.getTime() == 0) 
                xmlDate.ts = new Timestamp(file.lastModified());
            else if (file.lastModified() < xmlDate.ts.getTime())
                xmlDate.ts = new Timestamp(file.lastModified());    
            
            // Grob auf Aenderungen testen
            Timestamp lastTS = xmlDate.ts;
            Timestamp dateTS = database.getMatchTimestamp(xmlDate.name);
            if (dateTS != null && dateTS.compareTo(lastTS) <= 0)
                continue;
            
            // List<List<Match>>, damit das Interface spaeter passt
            List<List<Match>> matchList = new java.util.ArrayList<>();
            matchList.add(new java.util.ArrayList<>());
            
            // Jetzt per Gruppe testen, ob eine der selektierten Gruppen betroffen ist
            boolean needUpdate = false;        // Flag, ob die Zeit ueberhaupt generiert werden muss
            
            for (Group gr : database.readGroups(xmlDate.name)) {
                if (!xmlProperties.isGroupIncluded(gr))
                    continue;
                
                Timestamp mtTS = database.getMatchTimestamp(gr, xmlDate.name);
                if (mtTS != null && mtTS.compareTo(xmlDate.ts) > 0)
                    needUpdate = true;
                
                matchList.get(0).addAll(database.readMatches(gr, xmlDate.name));
            }
            
            if (!needUpdate || matchList.get(0).isEmpty())
                continue;
            
            Logger.getLogger(WebGen.class.getName()).log(Level.INFO, "Generate matches for {0}", xmlDate.name);

            Match[] matches = matchList.get(0).toArray(new Match[0]);
            java.util.Arrays.sort(matches, new java.util.Comparator<Match>() {

                @Override
                public int compare(Match o1, Match o2) {
                    int ret = o1.mtDateTime.compareTo(o2.mtDateTime);
                    if (ret == 0)
                        ret = o1.mtTable - o2.mtTable;
                    
                    return ret;
                }
            });
            
            for (Match mt : matches) {
                // Spiele, die sich seit dem letzten Update nicht geaendert habem, ignorieren.
                // Ich brauche hier nur eine Liste von Spielern, die seither gespielt haben.
                // Von diesen Spielern hole ich mir anschliessend wieder alle Spiele
                if (mt.mtTimestamp != null && mt.mtTimestamp.compareTo(lastTS) < 0)
                    continue;
                
                // Liveticker setzt jedesmal mtTimestamp
                // Uns interessieren aber nur Spiele, die nicht gespielt sind oder fertig sind
                if (mt.mtPrinted && !mt.mtChecked)
                    continue;
                
                if (mt.getPlA() != null)
                    plNrs.add(mt.getPlA().plNr % 10000);
                
                if (mt.getPlB() != null)
                    plNrs.add(mt.getPlB().plNr % 10000);
                
                if (mt.getPlX() != null)
                    plNrs.add(mt.getPlX().plNr % 10000);
                
                if (mt.getPlY() != null)
                    plNrs.add(mt.getPlY().plNr % 10000);

                if (mt.gr.cp.cpType == 4) {
                    if (mt.tmAtmID != 0)
                        tmIDs.add(mt.tmAtmID);
                    if (mt.tmXtmID != 0)
                        tmIDs.add(mt.tmXtmID);
                        
                    List<Match> tmList = database.readIndividualMatches(mt);
                    for (Match m : tmList) {
                        if (m.getPlA() != null)
                            plNrs.add(m.getPlA().plNr % 10000);

                        if (m.getPlB() != null)
                            plNrs.add(m.getPlB().plNr % 10000);

                        if (m.getPlX() != null)
                            plNrs.add(m.getPlX().plNr % 10000);

                        if (m.getPlY() != null)
                            plNrs.add(m.getPlY().plNr % 10000);
                    }
                }
            }
            
            matchList.clear();
            matchList.add(Arrays.asList(matches));
            
            // Den juengsten Timestamp dieser Spiele suchen
            Timestamp mtTS = new Timestamp(0);
            for (Match mt : matches) {
                if (mt.mtTimestamp != null && mtTS.before(mt.mtTimestamp))
                    mtTS = mt.mtTimestamp;
            }
            
            // Generiere Seite fuer dieses Datum
            String page = generator.generate(matchList, database);
            
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(page);
            }
            
            xmlDate.ts = mtTS;
        }
        
        generator = new PlayersMatchesGenerator();
        
        Map<Integer, Group> groupMap = new java.util.HashMap<>();
        
        for (Competition cp : database.readEvents()) {
            for (Group gr : database.readGroups(cp)) {
                if (!xmlProperties.isGroupIncluded(gr))
                    continue;
                
                groupMap.put(gr.grID, gr);
            }
        }
        
        // Ausserdem brauche ich eine Liste aller Mannschaften und Spieler, 
        // fuer die es kein File gibt (z.B. weil sich Name oder Startnr. geaendert haben)
        for (Team tm : teams.values()) {
            String name = "tm_" + tm.tmName.replaceAll("/", "_") + "_" + tm.cp.cpName + ".html";
            if ( ! new File(path, name).exists() )
                tmIDs.add(tm.tmID);
        }
        
        for (Player pl : database.getPlayers()) {
            int plNr = pl.plNr % 10000;
            String name = "pl_" + plNr + ".html";
            if ( ! new File(path, name).exists() )
                plNrs.add(plNr);
        }
        
        Logger.getLogger(WebGen.class.getName()).log(Level.INFO, "Generate matches of {0} teams", tmIDs.size());

        for (int tmID : tmIDs) {
            List<List<Match>> matches = new java.util.ArrayList<>();
            matches.add(new java.util.ArrayList<>());
            
            Match[] matchList = database.readTeamMatches(tmID).toArray(new Match[0]);
            
            java.util.Arrays.sort(matchList, new java.util.Comparator<Match>() {

                @Override
                public int compare(Match o1, Match o2) {
                    if (o1.mtDateTime == null) {
                        return o2.mtDateTime == null ? 0 : +1;
                    }
                    
                    if (o2.mtDateTime == null)
                        return -1;
                    
                    // Sortierung: Datum und zeit, Tisch, Nr (Runde und Spiel, da mtNr aufsteigend ist), Mannschaftsspiel
                    int ret = o1.mtDateTime.compareTo(o2.mtDateTime);
                    
                    if (ret == 0)
                        ret = o1.mtTable - o2.mtTable;
                    
                    if (ret == 0)
                        ret = o1.mtNr - o2.mtNr;
                    
                    if (ret == 0)
                        ret = o1.mtMS - o2.mtMS;
                    
                    return ret;
                }
            });
            
            for (Match mt : matchList) {
                // Nur Spiele in Gruppen von index.xml
                if (!groupMap.containsKey(mt.grID)) 
                    continue;
                
                // Gruppe setzen
                mt.gr = groupMap.get(mt.grID);                
                
                // Keine Freilose
                if (mt.stA != 0 && mt.tmAtmID == 0 || mt.stX != 0 && mt.tmXtmID == 0)
                    continue;
                
                // In Mannschaft nur gespielte Spiele
                if (mt.mtMS > 0 && !mt.isFinished() && !mt.mtChecked)
                    continue;
                
                // Wenn es eine max. Anzahl von Runden oder Spielen gibt, ebenfalls nicht
                if (mt.gr.grModus != 1) {
                    if (mt.gr.grNofRounds > 0 && mt.mtRound > mt.gr.grNofRounds)
                        continue;

                    if (mt.gr.grNofMatches > 0 && mt.mtMatch > (mt.gr.grNofMatches / (1 << (mt.mtRound - 1))))
                        continue;
                }
                
                matches.get(0).add(mt);
            }
            
            String page = generator.generate(matches, database);
            
            try (FileWriter fw = new FileWriter(new File(path, "tm_" + teams.get(tmID).tmName.replaceAll("/", "_") + "_" + teams.get(tmID).cp.cpName + ".html"))) {
                fw.write(page);
            }
        }
                
        Logger.getLogger(WebGen.class.getName()).log(Level.INFO, "Generate matches of {0} players", plNrs.size());

        for (int plNr : plNrs) {
            if (plNr == 0)
                continue;
            
            List<List<Match>> matches = new java.util.ArrayList<>();
            matches.add(new java.util.ArrayList<>());
            
            Match[] matchList = database.readPlayersMatches(plNr).toArray(new Match[0]);
            
            java.util.Arrays.sort(matchList, new java.util.Comparator<Match>() {

                @Override
                public int compare(Match o1, Match o2) {
                    if (o1.mtDateTime == null) {
                        return o2.mtDateTime == null ? 0 : +1;
                    }
                    
                    if (o2.mtDateTime == null)
                        return -1;
                    
                    // Sort by Date and time (desc), table, match nr (is same as by round and match)
                    int ret = -o1.mtDateTime.compareTo(o2.mtDateTime);
                    
                    if (ret == 0)
                        ret = o1.mtTable - o2.mtTable;
                    
                    if (ret == 0)
                        ret = o1.mtNr - o2.mtNr;
                    
                    if (ret == 0)
                        ret = o1.mtMS - o2.mtMS;
                    
                    return ret;
                }
            });
            
            for (Match mt : matchList) {
                // Nur Spiele in Gruppen von index.xml
                if (!groupMap.containsKey(mt.grID)) 
                    continue;
                
                mt.gr = groupMap.get(mt.grID);
                
                // Keine Freilose
                if (mt.stA != 0 && mt.tmAtmID == 0 || mt.stX != 0 && mt.tmXtmID == 0)
                    continue;
                
                // In Mannschaft nur gespielte Spiele
                if (mt.mtMS > 0 && !mt.isFinished())
                    continue;
                
                // Wenn es eine max. Anzahl von Runden oder Spielen gibt, ebenfalls nicht
                if (mt.gr.grModus != 1) {
                    if (mt.gr.grNofRounds > 0 && mt.mtRound > mt.gr.grNofRounds)
                        continue;

                    if (mt.gr.grNofMatches > 0 && mt.mtMatch > (mt.gr.grNofMatches / (1 << (mt.mtRound - 1))))
                        continue;
                }
                
                matches.get(0).add(mt);
            }
            
            String page = generator.generate(matches, database);
            
            try (FileWriter fw = new FileWriter(new File(path, "pl_" + plNr + ".html"))) {
                fw.write(page);
            }
        }
    }
    
    
    private String getTimestampDate(Timestamp ts) {
        // ISO-8601
        return String.format("%tF", ts);
    }

    /**
     * Upload der geaenderten Files
     */
    private void upload() {
        String ftpHost = getFtpHost();
        String ftpUser = getFtpUser();
        String ftpPwd = getFtpPassword();
        String ftpDir = getFtpDirectory();

        if (ftpHost.isEmpty())
            return;

        java.util.ArrayList<File> fileList = new java.util.ArrayList<>();
        File[] folderContent;

        // Alle HTML-Seiten, CSS, JavaScript und Bilder
        folderContent = (new File(path)).listFiles(
                new java.io.FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        // Exclude index.xml
                        if (name.equals("index.xml"))
                            return false;
                        
                        // Hidden files (in Unix and done so by Unix tools)
                        if (name.startsWith("."))
                            return false;
                        
                        // Backup files created by editors
                        if (name.toLowerCase().endsWith(".swp") ||
                            name.toLowerCase().endsWith(".bak") )
                            return false;
                        
                        return (name.endsWith(".html") ||
                                name.endsWith(".css") ||
                                name.endsWith(".js") ||
                                name.endsWith(".xml") ||
                                name.toLowerCase().endsWith(".gif") ||
                                name.toLowerCase().endsWith(".jpg") ||
                                name.toLowerCase().endsWith(".png") ||
                                name.toLowerCase().endsWith(".svg") ||
                                name.toLowerCase().endsWith(".pdf"));
                    }
                });
        

        fileList.addAll(Arrays.asList(folderContent));
        
        folderContent = new File(path, "js").listFiles();
        
        fileList.addAll(Arrays.asList(folderContent));
        
        folderContent = new File(path, "css").listFiles();
        
        fileList.addAll(Arrays.asList(folderContent));
        
        folderContent = new File(path, "fonts").listFiles();
        
        fileList.addAll(Arrays.asList(folderContent));
        
        folderContent = new File(path, "img").listFiles();
        
        fileList.addAll(Arrays.asList(folderContent));
        
        folderContent = new File(path, "flags").listFiles();
        
        fileList.addAll(Arrays.asList(folderContent));
        
        if (fileList.isEmpty())
            return;

        File[] files = fileList.toArray(new File[0]);
        
        // Aufsteigend nach Aenderungszeit sortieren
        java.util.Arrays.sort(files, new java.util.Comparator<File>() {

            @Override
            public int compare(File o1, File o2) {
                if (o1.lastModified() < o2.lastModified())
                    return -1;
                else if (o2.lastModified() < o1.lastModified())
                    return +1;
                else
                    return 0;
            }            
        });

        java.sql.Timestamp ts;
        
        ts = xmlProperties.getFtpSettings().ts;

        if (files[files.length - 1].lastModified() < ts.getTime())
            return;
        
        // Schleife bis alle Files hochgeladen wurden
        int retries = 5;
        
        do {
            com.enterprisedt.net.ftp.FileTransferClientInterface ftp = null;
            
            try {
                ftp = (com.enterprisedt.net.ftp.FileTransferClientInterface) Class.forName("at.co.ttm.ftp.FtpClient").getConstructor(Boolean.TYPE).newInstance(getFtpSecure());
            } catch (ClassNotFoundException ex) {
                // We expect this when we can't use the commercial lib
                Logger.getLogger(WebGen.class.getName()).log(Level.FINE, null, ex);                
            } catch (Exception ex) {
                // Anyting unexpected like the Spanish Inquistion
                Logger.getLogger(WebGen.class.getName()).log(Level.WARNING, null, ex);                
            }
            
            if (ftp == null) {
                ftp = new com.enterprisedt.net.ftp.FileTransferClient() {
                    @Override
                    public synchronized void connect() throws FTPException, IOException {
                        // UTF-8 Filenamen
                        masterContext.setControlEncoding("UTF-8");

                        super.connect();
                    }                
                };
            }

            if (ftpDebug) {
                // Wenn Debugging eingeschalten ist, auch den Log-Level setzen.
                // Eigentljch geschieht das fuer de.webgen in MainFrame, aber aus
                // seltsamen Gruenden kommt es vor, dass er zurueckgesetzt wird.
                // Oder es wird ueberhaupt ein anderer Logger verwendet ...
                Logger.getLogger(WebGen.class.getName()).setLevel(Level.FINE);

                ftp.setEventListener(new com.enterprisedt.net.ftp.EventAdapter() {
                    @Override
                    public void commandSent(String connId, String cmd) {
                        Logger.getLogger(WebGen.class.getName()).log(Level.FINE, cmd);
                    }

                    @Override
                    public void replyReceived(String connId, String reply) {
                        Logger.getLogger(WebGen.class.getName()).log(Level.FINE, reply);
                    }
                });
            }

            try {
                try {
                    ftp.setRemoteHost(ftpHost);
                    ftp.setUserName(ftpUser);
                    ftp.setPassword(ftpPwd);

                    if (getFtpPassive())
                        ftp.getAdvancedFTPSettings().setConnectMode(com.enterprisedt.net.ftp.FTPConnectMode.PASV);

                    ftp.setTimeout(120 * 1000);
                    ftp.connect();                
                    if (!ftpDir.isEmpty()) {
                        try {                            
                            ftp.changeDirectory(ftpDir);
                        } catch (com.enterprisedt.net.ftp.FTPException e) {
                            ftp.createDirectory(ftpDir);
                            ftp.changeDirectory(ftpDir);
                        }
                    }

                } catch (FTPException | IOException t) {
                    Logger.getLogger(WebGen.class.getName()).log(Level.SEVERE, null, t);
                    return;
                }

                Logger.getLogger(WebGen.class.getName()).log(Level.INFO, "Connected to server {0}", ftpHost);

                // Delay, sonst geht auf einigen Servern das folgende Upload nicht.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore
                }

                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory())
                        continue;

                    if (files[i].lastModified() < ts.getTime())
                        continue;

                    try {
                        String prefix = "";
                        if (files[i].getParent() != null) {
                            if (files[i].getParent().endsWith(File.separator + "js"))
                                prefix = "js";
                            else if (files[i].getParent().endsWith(File.separator + "css"))
                                prefix = "css";
                            else if (files[i].getParent().endsWith(File.separator + "fonts"))
                                prefix = "fonts";
                            else if (files[i].getParent().endsWith(File.separator + "img"))
                                prefix = "img";
                            else if (files[i].getParent().endsWith(File.separator + "flags"))
                                prefix = "flags";
                        }

                        Logger.getLogger(WebGen.class.getName()).log(
                                Level.INFO, "Upload file {0} with {1} bytes",
                                new String[] {(prefix.isEmpty() ? "" : prefix + "/") + files[i].getName(), "" + files[i].length()});

                        if (!prefix.isEmpty()) {
                            try {
                                String initDir = ftp.getRemoteDirectory();
                                ftp.changeDirectory(prefix); 
                                ftp.changeDirectory(initDir);
                            } catch (FTPException | IOException t) {
                                ftp.createDirectory(prefix);
                            }
                            
                            prefix = prefix + "/";
                        }

                        // ftp.uploadFile(files[i].getName(), files[i].getPath());
                        ftp.uploadFile(files[i].getPath(), prefix + files[i].getName());
                        ts.setTime(files[i].lastModified());
                        
                        retries = (i < files.length - 1) ? retries - 1 : 0;            
                    } catch (FTPException | IOException t) {
                        Logger.getLogger(WebGen.class.getName()).log(Level.SEVERE, null, t);

                        // Zeit wird weiter unten hochgezaehlt
                        ts.setTime(ts.getTime() - 1);
                        break;
                    }
                }

                /*
                try {
                    ftp.logout();

                    Logger.getLogger(WebGen.class.getName()).log(Level.INFO, "Logged out from server {0}", ftpHost);

                } catch (java.io.IOException ex) {
                    // Ignore
                    Logger.getLogger(WebGen.class.getName()).log(Level.FINE, null, ex);
                }
                 */
            } finally {
                try {
                    ftp.disconnect();
                } catch (FTPException | IOException t) {
                    // Ignore
                    Logger.getLogger(WebGen.class.getName()).log(Level.FINE, null, t);
                }

                Logger.getLogger(WebGen.class.getName()).log(Level.INFO, "Disconnected from {0}", ftpHost);
            }

            // Zeit um 1 hochzaehlen. Wenn alle Files uebertragen wurden, 
            // sind diese Files aelter als der Zeitstempel (und 1 ms spaeter wird
            // kein File mehr angelegt). Wenn es zwischendrin schief ging, wurde
            // dort der Zeitstempel um 1 erniedrigt, mit 1 mehr setze ich beim
            // naechsten mal richtig auf.
            ts.setTime(ts.getTime() + 1);
            xmlProperties.getFtpSettings().ts = ts;
            
            if (retries > 0) {
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        } while (retries > 0);
    }


    private String encryptPassword(String pwd) {
        return PasswordCrypto.encryptPassword(pwd);
    }


    private String decryptPassword(String pwd) {
        return PasswordCrypto.decryptPassword(pwd);
    }


    private static int tableType = 1;

    private String server;
    private String tournament;
    private String path;

    private Database database;

    private boolean ftpDebug;

    private Timer timer;
    private TimerTask timerTask;

    private long newsLastModified;
    
    private com.google.gson.Gson gson = new com.google.gson.Gson();
    
    private XmlProperties xmlProperties;
}
