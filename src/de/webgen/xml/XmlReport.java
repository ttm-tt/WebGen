/* Copyright (C) 2020 Christoph Theis */

package de.webgen.xml;

import de.webgen.generator.report.Report;
import de.webgen.generator.report.Report.ReportType;
import java.sql.Timestamp;
import org.w3c.dom.Element;


public class XmlReport {
    public XmlReport() {
        
    }
    
    public XmlReport(Report rp) {
        name = rp.getType().name();
    }
    
    public void read(Element el) {
        name = el.getAttribute(ATTRIBUTE_NAME);
        active = Boolean.parseBoolean(el.getAttribute(ATTRIBUTE_ACTIVE));
        try {
            ts = Timestamp.valueOf(el.getAttribute(ATTRIBUTE_TS));
        } catch (Exception ex) {
            ts = new Timestamp(0);
        }
        
        included = true;
    }
    
    public Element write(Element el) {
        el.setAttribute(ATTRIBUTE_NAME, name);
        el.setAttribute(ATTRIBUTE_ACTIVE, Boolean.toString(active));
        el.setAttribute(ATTRIBUTE_TS, ts.toString());
        
        return el;
    }
    
    public int compare(XmlReport rep) {
        return ReportType.valueOf(name).compareTo(ReportType.valueOf(rep.name));
    }
    
    public String name;
    public boolean active;
    public Timestamp ts = new Timestamp(0);
    
    // Not in XML
    public boolean included = false;
    
    private static final String ATTRIBUTE_NAME = "NAME";
    private static final String ATTRIBUTE_ACTIVE = "ACTIVE";
    private static final String ATTRIBUTE_TS = "TS";    
}
