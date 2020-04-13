/* Copyright (C) 2020 Christoph Theis */

package de.webgen.xml;

import java.sql.Timestamp;
import org.w3c.dom.Element;


public class XmlDate {
    public XmlDate() {
        
    }
    
    public XmlDate(String name) {
        this.name = name;
        this.ts = new Timestamp(0);
    }
    
    public void read(Element el) {
        name = el.getAttribute(ATTRIBUTE_NAME);
        try {
            ts = Timestamp.valueOf(el.getAttribute(ATTRIBUTE_TS));
        } catch (Exception ex) {
            ts = new Timestamp(0);
        }
    }
    
    public Element write(Element el) {
        el.setAttribute(ATTRIBUTE_NAME, name);
        el.setAttribute(ATTRIBUTE_TS, ts.toString());
        
        return el;        
    }
    public String name;
    public Timestamp ts;
    
    private static final String ATTRIBUTE_NAME = "NAME";
    private static final String ATTRIBUTE_TS = "TS";    
}
