/* Copyright (C) 2020 Christoph Theis */

package de.webgen.xml;

/**
 * An interface to the index.xml "database"
 */

import de.webgen.database.Group;
import de.webgen.generator.report.Report;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class XmlProperties {
    
    public XmlProperties() {
    }
    
    public void load(String file) {
        doc = XMLUtilities.readXmlFile(file);
        
        if (doc.getDocumentElement() == null)
            doc.appendChild(doc.createElement(XML_FILE_ROOT_NAME));

        for (Element e : XMLUtilities.getChildElementsWithTagName(doc.getDocumentElement(), ELEMENT_GROUP)) {
            XmlGroup xmlGroup = new XmlGroup();
            xmlGroup.read(e);
            addGroup(xmlGroup);
        }
        
        for (Element e : XMLUtilities.getChildElementsWithTagName(doc.getDocumentElement(), ELEMENT_REPORT)) {
            XmlReport xmlReport = new XmlReport();
            xmlReport.read(e);
            addReport(xmlReport);
        }
        
        for (Element e : XMLUtilities.getChildElementsWithTagName(doc.getDocumentElement(), ELEMENT_SETTINGS)) {
            settings = new XmlSettings();
            settings.read(e);
        }
        
        for (Element e : XMLUtilities.getChildElementsWithTagName(doc.getDocumentElement(), ELEMENT_DATE)) {
            XmlDate xmlDate = new XmlDate();
            xmlDate.read(e);
            addDate(xmlDate);
        }        
        
        for (Element e : XMLUtilities.getChildElementsWithTagName(doc.getDocumentElement(), ELEMENT_FTP)) {
            ftpSettings = new XmlFtpSettings();
            ftpSettings.read(e);
        }        
    }
    
    public void store(String file) {
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = docBuilder.newDocument();
            doc.appendChild(doc.createElement(XML_FILE_ROOT_NAME));
            
            Element el;
            
            el = doc.createElement(ELEMENT_SETTINGS);
            doc.getDocumentElement().appendChild(settings.write(el));

            el = doc.createElement(ELEMENT_FTP);
            doc.getDocumentElement().appendChild(ftpSettings.write(el));
            
            for (XmlGroup xmlGroup : getGroups()) {
                if (xmlGroup.included) {
                    el = doc.createElement(ELEMENT_GROUP);
                    doc.getDocumentElement().appendChild(xmlGroup.write(el));
                }
            }
            
            for (XmlReport xmlReport : getReports()) {
                if (xmlReport.included) {
                    el = doc.createElement(ELEMENT_REPORT);
                    doc.getDocumentElement().appendChild(xmlReport.write(el));
                }
            }
            
            for (XmlDate xmlDate : getDates()) {
                el = doc.createElement(ELEMENT_DATE);
                doc.getDocumentElement().appendChild(xmlDate.write(el));
            }
            
            XMLUtilities.writeXmlFile(doc, file);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(XmlProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public List<XmlGroup> getGroups() {
        List<XmlGroup> groupList = new java.util.ArrayList<>();
        for (Map<String, XmlGroup> map : groups.values()) {
            for (XmlGroup xmlGroup : map.values()) {
                if (xmlGroup.included)
                    groupList.add(xmlGroup);
            }
        }
        
        return groupList;
    }
    
    public void setGroups(Group[] groupList) {
        for (Group gr : groupList) {
            if (!hasGroup(gr))
                addGroup(gr);
        }
        
    }
    
    public boolean hasGroup(Group gr) {
        return groups.containsKey(gr.cp.cpName) && groups.get(gr.cp.cpName).containsKey(gr.grName);
    }
    
    
    public XmlGroup getGroup(Group gr) {
        if (!groups.containsKey(gr.cp.cpName))
            return null;
        if (!groups.get(gr.cp.cpName).containsKey(gr.grName))
            return null;
        
        return groups.get(gr.cp.cpName).get(gr.grName);
    }
    
    public List<XmlReport> getReports() {
        List<XmlReport> reportList = new java.util.ArrayList<>();
        for (XmlReport xmlReport : reports.values()) {
            if (xmlReport.included)
                reportList.add(xmlReport);
        }
        
        return reportList;
    }
    
    public void setReports(Report[] reportList) {
        for (Report rp : reportList) {
            if (!hasReport(rp))
                addReport(new XmlReport(rp));
        }    
    }
    
    public boolean hasReport(Report rp) {
        return reports.containsKey(rp.getType().name());
    }
    
    public XmlReport getReport(Report rp) {
        if (!reports.containsKey(rp.getType().name()))
            return null;
        
        return reports.get(rp.getType().name());
    }
    
    public List<XmlDate> getDates() {
        List<XmlDate> dateList = new java.util.ArrayList<>();
        dateList.addAll(dates.values());
        
        return dateList;
    }
    
    public boolean hasDate(String date) {
        return dates.containsKey(date);
    }
    
    public XmlDate getDate(String date) {
        if (!dates.containsKey(date))
            return null;
        
        return dates.get(date);
    }
    
    public XmlSettings getSettings() {
        return settings;
    }
    
    public XmlFtpSettings getFtpSettings() {
        return ftpSettings;
    }
    
    public void addGroup(Group gr) {
        addGroup(new XmlGroup(gr));
    }
    
    public void addGroup(XmlGroup gr) {
        if (!groups.containsKey(gr.cpName))
            groups.put(gr.cpName, new java.util.HashMap<String, XmlGroup>());
        
        if (!groups.get(gr.cpName).containsKey(gr.grName))
            groups.get(gr.cpName).put(gr.grName, gr);
    }
    
    public void removeGroup(XmlGroup xmlGroup) {
        if (groups.containsKey(xmlGroup.cpName)) {
            groups.get(xmlGroup.cpName).remove(xmlGroup.grName);
            if (groups.get(xmlGroup.cpName).isEmpty())
                groups.remove(xmlGroup.cpName);
        }
    }
    
    // Einen WB entfernen. 
    // Da der WB nicht mehr existiert kann er nur per Namen adressiert werden.
    public void removeAllGroups(String cpName) {
        groups.remove(cpName);
    }
    
    public void setGroupEnabled(Group gr, boolean enabled) {
        if (!hasGroup(gr))
            addGroup(gr);
        
        if (groups.containsKey(gr.cp.cpName)) {
            if (groups.get(gr.cp.cpName).containsKey(gr.grName)) {
                groups.get(gr.cp.cpName).get(gr.grName).enabled = enabled;
                if (enabled)
                    groups.get(gr.cp.cpName).get(gr.grName).included = true;
            }
        }
    }
    
    public boolean isGroupEnabled(Group gr) {
        if (groups.containsKey(gr.cp.cpName)) {
            return groups.get(gr.cp.cpName).containsKey(gr.grName) && groups.get(gr.cp.cpName).get(gr.grName).enabled;
        }
        
        return false;
    }
    
    public void setGroupDisabled(Group gr, boolean disabled) {
        if (!hasGroup(gr))
            addGroup(gr);
        
        if (groups.containsKey(gr.cp.cpName)) {
            if (groups.get(gr.cp.cpName).containsKey(gr.grName)) {
                groups.get(gr.cp.cpName).get(gr.grName).disabled = disabled;
            }
        }
    }
    
    public boolean isGroupDisabled(Group gr) {
        if (groups.containsKey(gr.cp.cpName)) {
            return groups.get(gr.cp.cpName).containsKey(gr.grName) && groups.get(gr.cp.cpName).get(gr.grName).disabled;
        }
        
        return false;
    }
    
    public void setGroupPublished(Group gr, boolean published) {
        if (!hasGroup(gr))
            addGroup(gr);
        
        setGroupPublished(gr.cp.cpName, gr.grName, published);
    }
    
    public void setGroupPublished(String cpName, String grName, boolean published) {
        if (groups.containsKey(cpName)) {
            if (groups.get(cpName).containsKey(grName)) {
                groups.get(cpName).get(grName).published = published;
                if (published)
                    groups.get(cpName).get(grName).included = true;
            }
        }        
    }
    
    public boolean isGroupPublished(Group gr) {
        if (groups.containsKey(gr.cp.cpName) && groups.get(gr.cp.cpName).containsKey(gr.grName)) {
            return groups.get(gr.cp.cpName).get(gr.grName).published;
        }
        
        return false;
    }
    
    public boolean isGroupActive(Group gr) {
        if (groups.containsKey(gr.cp.cpName)) {
            return groups.get(gr.cp.cpName).containsKey(gr.grName) && groups.get(gr.cp.cpName).get(gr.grName).isActive();
        }
        
        return false;
    }
    
    public void setPublishedGroups(Group[] groupList) {
        for (XmlGroup xmlGroup : getGroups()) {
            xmlGroup.published = false;
        }
        
        for (Group gr : groupList) {
            setGroupPublished(gr, true);
        }
    }
    
    public boolean isGroupIncluded(Group gr) {
        if (!hasGroup(gr))
            return false;
        
        return groups.get(gr.cp.cpName).get(gr.grName).included;
    }
    
    public void addReport(Report rep) {
        addReport(new XmlReport(rep));
    }
    
    public void addReport(XmlReport xmlReport) {
        reports.put(xmlReport.name, xmlReport);
    }
    
    public void removeReport(XmlReport xmlReport) {
        reports.remove(xmlReport.name);
    }
    
    
    public void setReportActive(Report rep, boolean active) {
        if (!hasReport(rep))
            addReport(rep);
        
        setReportActive(rep.getType().name(), active);
    }

    
    public void setReportActive(String name, boolean active) {
        if (reports.containsKey(name)) {
            reports.get(name).active = active;
            if (active)
                reports.get(name).included = true;
        }
    }

    
    public boolean isReportActive(Report rep) {
        if (!hasReport(rep))
            addReport(rep);
        
        return reports.containsKey(rep.getType().name()) && reports.get(rep.getType().name()).active;
    }
    
    
    public boolean isReportIncluded(Report rep) {
        if (!hasReport(rep))
            return false;
        
        return reports.get(rep.getType().name()).included;
    }
            
    
    public void addDate(XmlDate date) {
        dates.put(date.name, date);
    }
    
    public void removeDate(XmlDate date) {
        dates.remove(date.name);
    }
    
    private static final String XML_FILE_ROOT_NAME = "WEB_GEN_SETTINGS";

    private static final String ELEMENT_GROUP = "GROUP";
    private static final String ELEMENT_REPORT = "REPORT";
    private static final String ELEMENT_DATE = "DATE";
    private static final String ELEMENT_FTP = "FTP";
    private static final String ELEMENT_SETTINGS = "SETTINGS";

    private Document doc;
    
    private Map<String, Map<String, XmlGroup>>   groups = new java.util.HashMap<>();
    private Map<String, XmlReport>  reports = new java.util.HashMap<>();
    private Map<String, XmlDate>    dates = new java.util.HashMap<>();
    private XmlSettings settings = new XmlSettings();
    private XmlFtpSettings ftpSettings = new XmlFtpSettings();
}
