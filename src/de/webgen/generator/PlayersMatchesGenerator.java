/* Copyright (C) 2020 Christoph Theis */

package de.webgen.generator;

import de.webgen.database.IDatabase;
import de.webgen.database.match.Match;
import de.webgen.generator.report.Report;
import java.sql.SQLException;
import java.util.List;


public class PlayersMatchesGenerator extends Generator {

    @Override
    public String generate(List<List<Match>> matchList, IDatabase database) throws SQLException {
        if (matchList.isEmpty())
            return "";
        
        StringBuilder sb = new StringBuilder();
        
        int lastMtNr = -1;
        
        sb.append("<table class=\"matches table table-bordered\">").append(SEP);

        for (Match mt : matchList.get(0)) {
            // Skip match for 3rd place if there is no such match
            if (mt.gr.grNoThirdPlace && (mt.gr.grSize >> mt.mtRound) == 1 && mt.mtMatch == 2)
                continue;
            
            if (lastMtNr != mt.mtNr) {
                if (lastMtNr != -1) {
                    sb.append("</tbody>").append(SEP);
                }
                
                sb
                    .append("<tbody>").append(SEP)
                    .append("<tr class=\"header\">")
                    .append("<th colspan=\"15\">")
                    .append("<span class=\"headerline\">")
                    .append("<span class=\"event\">").append(mt.gr.cp.cpName).append("</span>")
                    .append("<span class=\"group\">").append(mt.gr.grDesc).append("</span>")
                    .append("<span class=\"round\" ").append(mt.roundToString()).append(">").append("</span>")
                    .append("</span>")
                ;
                
                sb.append("<span class=\"headerline\">");
                
                if (mt.mtDateTime != null && mt.mtDateTime.getDate() != 0) {
                    sb
                        .append("<span class=\"date\" data-i18n=\"report.format.long.date\" ")
                        .append("data-i18n-options=\'{")
                        .append("\"wday\" : \"").append(mt.mtDateTime.getDay() + 1).append("\", ")
                        .append("\"day\" : \"").append(String.format("%02d", mt.mtDateTime.getDate())).append("\", ")
                        .append("\"month\" : \"").append(mt.mtDateTime.getMonth() + 1).append("\"")
                        .append("}\'>").append("</span>")
                    ;
                } else {
                    sb.append("<span class=\"date\"></span>");
                }
                     
                if (mt.mtDateTime != null && (mt.mtDateTime.getHours() != 0 || mt.mtTable != 0)) {
                    sb
                        .append("<span class=\"time\" data-i18n=\"report.format.short.time\" ")
                        .append("data-i18n-options=\'{")
                        .append("\"hour\" : \"").append(String.format("%02d", mt.mtDateTime.getHours())).append("\", ")
                        .append("\"minute\" : \"").append(String.format("%02d", mt.mtDateTime.getMinutes())).append("\" ")
                        .append("}\'>").append("</span>")
                    ;
                } else {
                    sb.append("<span class=\"time\"></span>");
                }
                
                if (mt.mtTable != 0) {
                    sb
                        .append("<span class=\"mttable\" data-i18n=\"report.format.short.table\" ")
                        .append("data-i18n-options=\'{")
                        .append("\"table\" : \"").append(mt.mtTable).append("\" ")
                        .append("}\'>").append("</span>")
                    ;
                } else {
                    sb.append("<span class=\"mttable\"></span>");
                }
                
                sb.append("</span>");
                
                sb.append("</th>").append("</tr>").append(SEP);
                
                lastMtNr = mt.mtNr;
            }
            
            sb.append(generateMatchItem(mt, database, false));
        }
        
        if (lastMtNr != -1)
            sb.append("<tbody>").append(SEP);
        
        sb.append("</table>");
        
        return sb.toString();
    }

    @Override
    public String generate(Report report, IDatabase database) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
