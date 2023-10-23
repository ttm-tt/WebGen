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
        
        // cpCategory might become an empty string, make it null again
        if ((cpCategory != null) && cpCategory.isBlank())
            cpCategory = null;
    }
    
    public void read(Element el) {
        cpCategory = getAttribute(el, ATTRIBUTE_CPATEGORY);
        cpName = getAttribute(el, ATTRIBUTE_CPNAME);
        grStage = getAttribute(el, ATTRIBUTE_GRSTAGE);
        grName = getAttribute(el, ATTRIBUTE_GRNAME);
        enabled = Boolean.parseBoolean(getAttribute(el, ATTRIBUTE_ENABLED));
        disabled = Boolean.parseBoolean(getAttribute(el, ATTRIBUTE_DISABLED));
        
        // cpCategory might become an empty string, make it null again
        if ((cpCategory != null) && cpCategory.isBlank())
            cpCategory = null;
        
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
        setAttribute(el, ATTRIBUTE_CPATEGORY, cpCategory);
        setAttribute(el, ATTRIBUTE_CPNAME, cpName);
        setAttribute(el, ATTRIBUTE_GRSTAGE, grStage);
        setAttribute(el, ATTRIBUTE_GRNAME, grName);
        setAttribute(el, ATTRIBUTE_ENABLED, Boolean.toString(enabled));
        setAttribute(el, ATTRIBUTE_DISABLED, Boolean.toString(disabled));
        setAttribute(el, ATTRIBUTE_TS, ts.toString());
        
        return el;
    }
    
    public boolean isActive() {
        return enabled || (published && !disabled);
    }
    
    public int compare(XmlGroup gr) {
        int ret = 0;

        // Sort null to top, so it comes just before empty string
        if (cpCategory == null && gr.cpCategory != null)
            return -1;
        if (cpCategory != null && gr.cpCategory == null)
            return +1;

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
    
    // Take care of null values
    private void setAttribute(Element el, String name, String value) {
        if (value != null && !value.isBlank())
            el.setAttribute(name, value);
        else if (el.hasAttribute(name))
            el.removeAttribute(name);
    }
    
    private String getAttribute(Element el, String name) {
        if (!el.hasAttribute(name))
            return null;

        return el.getAttribute(name);
    }
    
    private boolean getBoolean(Element el, String name) {
        String val = getAttribute(el, name);
        if (val == null)
            return false;
        return Boolean.parseBoolean(val);
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
