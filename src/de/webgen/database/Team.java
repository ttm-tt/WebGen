/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database;


public class Team {
    public int cpID;
    public Competition cp;
    public int    tmID;
    public String tmName;
    public String tmDesc;
    public String naName;
    public String naDesc;
    public String naRegion;
    
    public Team() {
        
    }
    
    
    public Team(Competition cp) {
        this.cp = cp;
        this.cpID = cp.cpID;
    }

    @Override
    public String toString() {
        return toString(true, true);
    }
    
    
    public String toString(boolean withAssoc, boolean withFlag) {
        if (tmName == null || tmName.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"team\">");
        
        if (withFlag) {
            sb
                .append("<span class=\"flag\">")
                .append("<img src=\"\" data-webgen-nation=\"").append(naName).append("\" data-webgen-region=\"").append(naRegion).append("\"></img>")
                .append("</span>")
            ;
        }
                        
        sb.append("<span class=\"teamname name\">").append(tmDesc.replaceAll(" ", "&nbsp;")).append("</span>");
        
        if (withAssoc)
            sb.append("<span class=\"assoc\">").append(naName).append("</span>");
        
        sb.append("</div>");
        
        return sb.toString();
    }
}
