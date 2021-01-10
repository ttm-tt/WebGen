/* Copyright (C) 2020 Christoph Theis */

package de.webgen.generator;

import de.webgen.database.IDatabase;
import de.webgen.database.match.Match;
import de.webgen.generator.report.Report;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.List;


public class DateGenerator extends Generator {

    public DateGenerator() {

    }
    
    @Override
    public String generate(List<List<Match>> matchList, IDatabase database) throws SQLException {
        
        if (matchList.isEmpty()) {
            return "";
        }
        
        Timestamp date = matchList.get(0).get(0).mtDateTime;
        
        StringBuilder buffer = new StringBuilder();
        
        Timestamp lastTime = null;
        java.text.DateFormat dfID = new java.text.SimpleDateFormat("HHmm");
        java.text.DateFormat dfContent = new java.text.SimpleDateFormat("HH:mm");
        
        // Opening
        buffer
            .append("<div class=\"col-12\" id=\"times\">").append(SEP)
        ;
        
        // Header
        buffer
            .append("<div class=\"row\">")
            .append("<h4 class=\"col-12\">")
            .append("<span data-i18n=\"dates.title\" ")
            .append(    "data-i18n-options=\'{")
            .append(    "\"day\": \"").append(String.format("%02d", date.getDate())).append("\", ")
            .append(    "\"wday\": \"").append(date.getDay() + 1).append("\", ")
            .append(    "\"month\": \"").append(date.getMonth() + 1) .append("\"")
            .append(    "}\'")
            .append(" />")
            .append("</h4>")
            .append("</div>")
            .append(SEP)
        ;
                    
        // Filter
        EnumSet<FilterTypes> what = EnumSet.of(
            FilterTypes.FilerEvent,
            FilterTypes.FilterAssocication,
            FilterTypes.FilterName,
            FilterTypes.FilterTable
        );
        
        buffer
            .append(writeFilter(what, database.readEvents(date), database.readAssociations(), null))
        ;
        
        // Matches
        for (Match mt : matchList.get(0)) {  
            // Spiele mit Freilos uebergehen
            if (mt.stA != 0 && mt.tmAtmID == 0 && mt.xxA.stID == 0 || mt.stX != 0 && mt.tmXtmID == 0 && mt.xxX.stID == 0)
                continue;
            
            if ( !mt.mtDateTime.equals(lastTime) ) {
                if (lastTime != null) {
                    buffer.append("</table>").append(SEP); // panel-body
                    buffer.append("</div>").append(SEP); // panel-collapse
                    buffer.append("</div>").append(SEP); // panel-default
                }        
                
                lastTime = mt.mtDateTime;
                            
                buffer
                    .append("<div class=\"row pb-3 timeround\">").append(SEP)
                    .append("<div class=\"col-12 px-0\">").append(SEP)
                    .append("<h4>")
                    .append("<a class=\"btn btn-light col-12 text-left\" data-toggle=\"collapse\" href=\"#collapse-").append(dfID.format(mt.mtDateTime)).append("\">").append(SEP)
                    .append("<span class=\"oi oi-chevron-left\"></span>").append(SEP)
                ;
                
                if (mt.mtDateTime.getHours() > 0 || mt.mtDateTime.getMinutes() > 0)
                    buffer.append("<span>").append(dfContent.format(mt.mtDateTime)).append("</span>");
                else
                    buffer.append("<span data-i18n=\"dates.time.no-time\">").append("</span>");
                
                buffer.append(SEP);
                
                buffer
                    .append("</a>").append(SEP)
                    .append("</h4>").append(SEP)
                    .append("</div>").append(SEP)
                ;
                
                buffer
                    .append("<div class=\"collapse col-12 px-0\" id=\"collapse-").append(dfID.format(mt.mtDateTime)).append("\">").append(SEP)   
                    .append("<table class=\"matches table table-bordered\">").append(SEP)
                ;                                        
            }
            
            buffer.append("<tbody>").append(SEP);
            
            buffer
                .append("<tr class=\"header\">")
                .append("<th colspan=\"15\">")
                .append("<span class=\"event\">").append(mt.gr.cp.cpName).append("</span>")
                .append("<span class=\"group\">").append(mt.gr.grDesc).append("</span>")
                .append("<span class=\"round\" ").append(mt.roundToString()).append(">").append("</span>")
            ;
            
            if (mt.mtTable == 0)
                buffer.append("<span class=\"mttable\"><span></span></span>");
            else
                buffer
                    .append("<span class=\"mttable\" data-i18n=\"report.format.short.table\" data-i18n-options=\'{\"table\" : ")
                    .append("\"").append(mt.mtTable).append("\"")
                    .append("}\'></span>")
                ;
            
            buffer
                .append("</th>")
                .append("</tr>")
                .append(SEP)
            ;
            
            buffer.append(generateMatchItem(mt, database, false));
            
            buffer.append("</tbody>").append(SEP);
        }
        
        buffer.append("</table>").append(SEP); // panel-body
        buffer.append("</div>").append(SEP); // panel-collapse
        buffer.append("</div>").append(SEP); // panel-default
        
        buffer.append("</div>").append(SEP); // panel-group
        
        return buffer.toString();
    }

    @Override
    public String generate(Report report, IDatabase database) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
