/* Copyright (C) 2020 Christoph Theis */

package de.webgen.xml;

import de.webgen.database.Group;
import java.sql.Timestamp;
import org.w3c.dom.Element;


public class XmlGroup {
    public String cpCategory;
    public String cpName;
    public int    grSortOrder;
    public String grStage;
    public String grName;
    // We need both because "!enabled" is not "disabled"
    public boolean enabled;   // expl. enabled
    public boolean disabled;  // expl. disabled
    public Timestamp ts = new Timestamp(0);

    // Nicht in XML
    public boolean published;
    public boolean included = false;
    
    public XmlGroup() {
        
    }
    
    public XmlGroup(Group gr) {
        cpCategory = gr.cp.cpCategory;
        cpName = gr.cp.cpName;
        grSortOrder = gr.grSortOrder;
        grStage = gr.grStage;
        grName = gr.grName;
        published = gr.grPublished;      
    }
    
    public void read(Element el) {
        cpCategory = el.getAttribute(ATTRIBUTE_CPATEGORY);
        cpName = el.getAttribute(ATTRIBUTE_CPNAME);
        grStage = el.getAttribute(ATTRIBUTE_GRSTAGE);
        grName = el.getAttribute(ATTRIBUTE_GRNAME);
        enabled = Boolean.parseBoolean(el.getAttribute(ATTRIBUTE_ENABLED));
        disabled = Boolean.parseBoolean(el.getAttribute(ATTRIBUTE_DISABLED));
        
        // backward compatibility
        if (el.hasAttribute(ATTRIBUTE_ACTIVE))
            enabled = Boolean.parseBoolean(ATTRIBUTE_ACTIVE);
        
        try {
            ts = Timestamp.valueOf(el.getAttribute(ATTRIBUTE_TS));
        } catch (Exception ex) {
            ts = new Timestamp(0);
        }
        
        included = true;
    }
    
    public Element write(Element el) {
        el.setAttribute(ATTRIBUTE_CPATEGORY, cpCategory);
        el.setAttribute(ATTRIBUTE_CPNAME, cpName);
        el.setAttribute(ATTRIBUTE_GRSTAGE, grStage);
        el.setAttribute(ATTRIBUTE_GRNAME, grName);
        el.setAttribute(ATTRIBUTE_ENABLED, Boolean.toString(enabled));
        el.setAttribute(ATTRIBUTE_DISABLED, Boolean.toString(disabled));
        el.setAttribute(ATTRIBUTE_TS, ts.toString());
        
        return el;
    }
    
    public boolean isActive() {
        return enabled || (published && !disabled);
    }
    
    public int compare(XmlGroup gr) {
        int ret = 0;

        if (cpCategory == null && gr.cpCategory != null)
            return +1;
        if (cpCategory != null && gr.cpCategory == null)
            return -1;

        if (cpCategory != null && gr.cpCategory != null)
            ret = cpCategory.compareTo(gr.cpCategory);
        if (ret == 0)
            ret = cpName.compareTo(gr.cpName);
        if (ret == 0)
            ret = grSortOrder - gr.grSortOrder;
        if (ret == 0)
            ret = grStage.compareTo(gr.grStage);
        if (ret == 0)
            ret = grName.compareTo(gr.grName);
        
        return ret;
    }
    
    private static final String ATTRIBUTE_GRNAME = "GRNAME";
    private static final String ATTRIBUTE_GRSORT = "GRSORTORDER";
    private static final String ATTRIBUTE_GRSTAGE ="GRSTAGE";
    private static final String ATTRIBUTE_CPNAME = "CPNAME";
    private static final String ATTRIBUTE_CPATEGORY = "CPCATEGORY";
    private static final String ATTRIBUTE_ACTIVE = "ACTIVE";
    private static final String ATTRIBUTE_ENABLED = "ENABLED";
    private static final String ATTRIBUTE_DISABLED = "DISABLED";
    private static final String ATTRIBUTE_TS = "TS";
}
