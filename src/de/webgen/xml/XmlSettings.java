/* Copyright (C) 2020 Christoph Theis */

package de.webgen.xml;

import de.webgen.WebGen.FlagType;
import org.w3c.dom.Element;


public class XmlSettings {
    private String getAttribute(Element el, String key, String def) {
        String ret = el.getAttribute(key);
        if (ret == null || ret.isEmpty())
            ret = def;
        
        return ret;
    }
    
    public void read(Element el) {
        title = el.getAttribute(ATTRIBUTE_TITLE);
        description = el.getAttribute(ATTRIBUTE_DESCRIPTION);
        url = el.getAttribute(ATTRIBUTE_TOURNAMENTURL);
        news = el.getAttribute(ATTRIBUTE_NEWSURL);
        ltSettings = el.getAttribute(ATTRIBUTE_LTSETTINGS);
        ltEnabled = Boolean.parseBoolean(el.getAttribute(ATTRIBUTE_LTENABLED));
        autoGroups = Boolean.parseBoolean(el.getAttribute(ATTRIBUTE_AUTOGROUPS));
        interval = Integer.decode(el.getAttribute(ATTRIBUTE_INTERVAL));
        flagType = FlagType.valueOf(getAttribute(el, ATTRIBUTE_FLAGTYPE, "None"));
        userConfig = el.getAttribute(ATTRIBUTE_USERCONFIG);
    }
    
    public Element write(Element el) {
        el.setAttribute(ATTRIBUTE_TITLE, title);
        el.setAttribute(ATTRIBUTE_DESCRIPTION, description);
        el.setAttribute(ATTRIBUTE_TOURNAMENTURL, url);
        el.setAttribute(ATTRIBUTE_NEWSURL, news);
        el.setAttribute(ATTRIBUTE_LTSETTINGS, ltSettings);
        el.setAttribute(ATTRIBUTE_LTENABLED, Boolean.toString(ltEnabled));
        el.setAttribute(ATTRIBUTE_AUTOGROUPS, Boolean.toString(autoGroups));
        el.setAttribute(ATTRIBUTE_INTERVAL, Integer.toString(interval));
        el.setAttribute(ATTRIBUTE_FLAGTYPE, flagType.toString());
        el.setAttribute(ATTRIBUTE_USERCONFIG, userConfig);
        
        return el;
    }
    
    public String title;
    public String description;
    public String url;
    public String news;
    public String userConfig;
    public String ltSettings;
    public boolean ltEnabled;
    public boolean autoGroups;
    public int     interval;
    public FlagType flagType = FlagType.None;
    
    private static final String ATTRIBUTE_TITLE = "TITLE";
    private static final String ATTRIBUTE_DESCRIPTION = "DESCRIPTION";
    private static final String ATTRIBUTE_TOURNAMENTURL = "TOURNAMENTURL";
    private static final String ATTRIBUTE_LTSETTINGS = "LTSETTINGS";
    private static final String ATTRIBUTE_LTENABLED = "LTENABLED";
    private static final String ATTRIBUTE_NEWSURL = "NEWSURL";
    private static final String ATTRIBUTE_INTERVAL = "INTERVAL";
    private static final String ATTRIBUTE_AUTOGROUPS = "AUTOGROUPS";
    private static final String ATTRIBUTE_FLAGTYPE = "FLAGTYPE";
    private static final String ATTRIBUTE_USERCONFIG = "USERCONFIG";
}
