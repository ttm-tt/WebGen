/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database;


public class Player {

    public int    plNr;
    public String psFirstName;
    public String psLastName;
    public int    psSex;
    public String naName;
    public String naDesc;
    public String naRegion;
    public String plExtID;
    
    @Override
    public String toString() {
        return toString(true, true, true);
    }

    public String toString(boolean withAssoc, boolean withNr, boolean withFlag) {
        if (plNr == 0)
            return "";

        StringBuilder sb = new StringBuilder();
        
        sb.append("<div class=\"player\">");
        
        if (withFlag) {
            sb
                .append("<span class=\"flag\">")
                .append("<img src=\"\" data-webgen-nation=\"").append(naName).append("\" data-webgen-region=\"").append(naRegion).append("\"></img>")
                .append("</span>")
            ;
        }
                        
        if (withNr) {
            sb.append("<span class=\"plnr\">");
            sb.append(plNr % 10000);
            sb.append("</span>");
        }
        
        sb.append("<span class=\"name\">");
        sb.append(psLastName.replaceAll(" ", "&nbsp;"));
        sb.append(",&nbsp;");
        sb.append(psFirstName.replaceAll(" ", "&nbsp;"));
        sb.append("</span>");
        
        if (withAssoc) {
            sb.append("<span class=\"assoc\">");
            sb.append(naName);
            sb.append("</span>");
        }
        
        sb.append("</div>");

        return sb.toString();
    }
}
