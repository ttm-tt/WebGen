/* Copyright (C) 2020 Christoph Theis */

package de.webgen.xml;

import java.sql.Timestamp;
import org.w3c.dom.Element;


public class XmlFtpSettings {
    public void read(Element el) {
        host = el.getAttribute(ATTRIBUTE_FTPHOST);
        dir = el.getAttribute(ATTRIBUTE_FTPDIR);
        user = el.getAttribute(ATTRIBUTE_FTPUSER);
        pwd = el.getAttribute(ATTRIBUTE_FTPPWD);
        passive = Boolean.parseBoolean(el.getAttribute(ATTRIBUTE_FTPPASV));
        sftp = el.getAttribute(ATTRIBUTE_FTPPROT).equals("SFTP");
        
        try {
            ts = Timestamp.valueOf(el.getAttribute(ATTRIBUTE_TS));
        } catch (Exception ex) {
            ts = new Timestamp(0);
        }
    }
    
    public Element write(Element el) {
        el.setAttribute(ATTRIBUTE_FTPHOST, host);
        el.setAttribute(ATTRIBUTE_FTPDIR, dir);
        el.setAttribute(ATTRIBUTE_FTPUSER, user);
        el.setAttribute(ATTRIBUTE_FTPPWD, pwd);
        el.setAttribute(ATTRIBUTE_FTPPASV, Boolean.toString(passive));
        el.setAttribute(ATTRIBUTE_FTPPROT, sftp ? "SFTP" : "PLAIN");
        el.setAttribute(ATTRIBUTE_TS, ts.toString());
        
        return el;
    }
    
    public String host;
    public String dir;
    public String user;
    public String pwd;
    public boolean passive;
    public boolean sftp;
    public Timestamp ts = new Timestamp(0);
    
    private static final String ATTRIBUTE_FTPHOST = "HOST";
    private static final String ATTRIBUTE_FTPDIR = "DIR";
    private static final String ATTRIBUTE_FTPUSER = "USER";
    private static final String ATTRIBUTE_FTPPWD = "PASSWORD";
    private static final String ATTRIBUTE_FTPPASV = "PASV";
    private static final String ATTRIBUTE_FTPPROT = "PROT";
    private static final String ATTRIBUTE_TS = "TS";
}
