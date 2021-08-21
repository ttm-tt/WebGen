/* Copyright (C) 2020 Christoph Theis */

package de.webgen.generator;

import de.webgen.database.Association;
import de.webgen.database.Competition;
import de.webgen.database.IDatabase;
import de.webgen.database.Group;
import de.webgen.database.match.Match;
import de.webgen.generator.report.Report;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;

public abstract class Generator {

    public static final String   SEP = System.getProperty("line.separator");
    
    public static enum FilterTypes  {
        FilerEvent,
        FilterGroup,
        FilterAssocication,
        FilterName,
        FilterTable,
        FilterStartNumber,
        FilterExtID
    };

    public Generator() {
    }
    
    public abstract String generate(List<List<Match>> matchList, IDatabase database) throws SQLException;

    public abstract String generate(Report report, IDatabase database) throws SQLException;

    static String generateMatchList(List<Match> matchList, IDatabase database) throws SQLException {
        
        if (matchList.isEmpty())
            return "";
        
        Group gr = matchList.get(0).gr;
        
        if (gr == null)
            return "";
        
        boolean forceShowTime = gr.grModus == 1;
        for (Match mt : matchList) {
            forceShowTime &= (mt.tmAtmID == 0 && mt.tmXtmID == 0);
        }
        
        StringBuilder content = new StringBuilder();
        
        for (int j = 0; j < matchList.size(); j++) {
            Match mt = matchList.get(j);

            // Spiele mit Freilos ueberspringen
            // Ein Freilos hat st != 0 und entweder tmID == 0 und xx.grPos == 0 (oder kein xx)
            if (!forceShowTime) {
                if ( (mt.mtDateTime == null || mt.mtDateTime.getTime() == 0) ||
                     (mt.stA != 0 && mt.tmAtmID == 0 && (mt.xxA == null || mt.xxA.grPos == 0)) ||
                     (mt.stX != 0 && mt.tmXtmID == 0 && (mt.xxA == null || mt.xxA.grPos == 0)) )
                    continue;
            }

            content
                .append("<tbody data-webgen-teammatch=\"" + mt.mtNr + "\">").append(SEP)
                .append(generateMatchItem(mt, database, true))
                .append("</tbody>").append(SEP)
            ;
        }

        return content.toString();        
    }
    
    static String generateMatchItem(Match mt, IDatabase database, boolean includeSchedule) throws SQLException {
        if (mt == null)
            return "";
        
        int colspan = 7; // Max games
        Group gr = mt.gr;
        
        String tr = "<tr class=\"match\">" + SEP;
        StringBuilder row = new StringBuilder();

        row
            .append("<td class=\"name\" colspan=\"7\">")
            .append("<div class=\"left").append(mt.getWinnerAX() == (mt.mtReverse ? Match.Side.X : Match.Side.A) ? " winner" : "").append("\">")
            .append(mt.mtReverse ? mt.xToString(true, true, true) : mt.aToString(true, true, true)).append("</div>")
            .append("<div class=\"right").append(mt.getWinnerAX() == (mt.mtReverse ? Match.Side.A : Match.Side.X) ? " winner" : "").append("\">")
            .append(mt.mtReverse ? mt.aToString(true, true, true) : mt.xToString(true, true, true))
            .append("</div>")
            .append("</td>")
        ;

        if (!mt.isFinished()) {
            row.append("<td class=\"schedule\" colspan=\"").append(colspan+1).append("\">");
            if (includeSchedule && mt.mtDateTime != null && mt.mtDateTime.getTime() > 0) {
                row
                    .append("<span data-i18n=\"rr.format.long.date\" ")
                    .append("data-i18n-options=\'{")
                    .append("\"wday\" : \"").append(mt.mtDateTime.getDay() + 1).append("\", ")
                    .append("\"day\" : \"").append(String.format("%02d", mt.mtDateTime.getDate())).append("\", ")
                    .append("\"month\" : \"").append(mt.mtDateTime.getMonth() + 1).append("\"")
                    .append("}\'></span>")
                ;

                if (mt.mtDateTime.getHours() != 0 || mt.mtTable != 0)
                    row
                        .append(" ")
                        .append("<span data-i18n=\"rr.format.long.time\" ")
                        .append("data-i18n-options=\'{")
                        .append("\"hour\" : \"").append(String.format("%02d", mt.mtDateTime.getHours())).append("\", ")
                        .append("\"minute\" : \"").append(String.format("%02d", mt.mtDateTime.getMinutes())).append("\" ")
                        .append("}\'></span>")
                    ;

                if (mt.mtTable != 0) {
                    row
                        .append(" ")
                        .append("<span data-i18n=\"rr.format.long.table\" ")
                        .append("data-i18n-options=\'{")
                        .append("\"table\" : \"").append(mt.mtTable).append("\" ")
                        .append("}\'></span>");
                }
            }

            row.append("</td>");    
            
            if (gr.cp.isTeam() && mt.mtMS == 0) {
                List<Match> indivMatches = database.readIndividualMatches(mt);
                if (indivMatches != null && indivMatches.size() > 0 && 
                        indivMatches.get(0).getPlA() != null && indivMatches.get(0).getPlA().plNr != 0) {
                    tr = "<tr class=\"match\" data-bs-toggle=\"collapse\" data-bs-target=\"[data-webgen-match=&quot;" + mt.mtNr + "&quot;]\">";

                    for (Match tmMt : indivMatches) {
                        row.append("</tr>").append(SEP);

                        row
                            .append("<tr class=\"individual collapse\" data-webgen-match=\"").append(mt.mtNr).append("\">")
                        ;

                        row
                            .append("<td class=\"name\" colspan=\"7\">")
                            .append("<div class=\"left\">")
                            .append(mt.mtReverse ? tmMt.xToString(true, true, false) : tmMt.aToString(true, true, false))
                            .append("</div>")
                            .append("<div class=\"right\">")
                            .append(mt.mtReverse ? tmMt.aToString(true, true, false) : tmMt.xToString(true, true, false))
                            .append("</div>")
                        ;

                        row
                            .append("<td class=\"result\">")
                            .append("<div class=\"left\">").append("</div>")
                            .append("<div class=\"right\">").append("</div>")
                            .append("</td>")
                        ;

                        row.append("<td class=\"game\" colspan=\"").append(colspan).append("\">").append("<br>").append("</td>");                    
                    }
                }
            }
        } else if (mt.mtWalkOverA || mt.mtWalkOverX) {
            row
                .append("<td class=\"result\">")
                .append("<div class=\"left\">")
                .append(mt.mtReverse ? mt.mtResX : mt.mtResA)
                .append("</div>")
                .append("<div class=\"right\">")
                .append(mt.mtReverse ? mt.mtResA : mt.mtResX)
                .append("</div>")
                .append("</td>")
                .append("<td class=\"game\" colspan=\"").append(colspan).append("\">").append("w/o").append("</td>")
            ;
        } else if (mt.mtInjuredA || mt.mtInjuredX) {
            row
                .append("<td class=\"result\">")
                .append("<div class=\"left\">")
                .append(mt.mtReverse ? mt.mtResX : mt.mtResA)
                .append("</div>")
                .append("<div class=\"right\">")
                .append(mt.mtReverse ? mt.mtResA : mt.mtResX)
                .append("</div>")
                .append("</td>")
                .append("<td class=\"game\" colspan=\"").append(colspan).append("\">").append("Inj.").append("</td>")
            ;
        } else if (mt.mtDisqualifiedA || mt.mtDisqualifiedX) {
            row
                .append("<td class=\"result\">")
                .append("<div class=\"left\">")
                .append(mt.mtReverse ? mt.mtResX : mt.mtResA)
                .append("</div>")
                .append("<div class=\"right\">")
                .append(mt.mtReverse ? mt.mtResA : mt.mtResX)
                .append("</div>")
                .append("</td>")
                .append("<td class=\"game\" colspan=\"").append(colspan).append("\">").append("Disqu.").append("</td>")
            ;
        } else if (gr.cp.isTeam() && mt.mtMS == 0) {
            tr = "<tr class=\"match\" data-bs-toggle=\"collapse\" data-bs-target=\"[data-webgen-match=&quot;" + mt.mtNr + "&quot;]\">";

            row
                .append("<td class=\"result\">")
                .append("<div class=\"left\">")
                .append(mt.mtReverse ? mt.mtResX : mt.mtResA)
                .append("</div>")
                .append("<div class=\"right\">")
                .append(mt.mtReverse ? mt.mtResA : mt.mtResX)
                .append("</div>")
                .append("</td>")
                .append("<td class=\"game\" colspan=\"").append(colspan).append("\">").append("<br>").append("</td>")
            ;
        
            List<Match> indivMatches = database.readIndividualMatches(mt);
            for (Match tmMt : indivMatches) {
                row.append("</tr>").append(SEP);

                row
                    .append("<tr class=\"individual collapse\" data-webgen-match=\"").append(mt.mtNr).append("\">")
                ;

                row
                    .append("<td class=\"name\" colspan=\"7\">")
                    .append("<div class=\"left").append(tmMt.getWinnerAX() == (mt.mtReverse ? Match.Side.X : Match.Side.A) ? " winner" : "").append("\">")
                    .append(mt.mtReverse ? tmMt.xToString(true, true, false) : tmMt.aToString(true, true, false))
                    .append("</div>")
                    .append("<div class=\"right").append(tmMt.getWinnerAX() == (mt.mtReverse ? Match.Side.A : Match.Side.X) ? " winner" : "").append("\">")
                    .append(mt.mtReverse ? tmMt.aToString(true, true, false) : tmMt.xToString(true, true, false))
                    .append("</div>")
                ;

                if (!tmMt.isFinished()) {
                    row
                        .append("<td class=\"result\">")
                        .append("<div class=\"left\">").append("</div>")
                        .append("<div class=\"right\">").append("</div>")
                        .append("</td>")
                    ;

                    row.append("<td class=\"game\" colspan=\"").append(colspan).append("\">").append("<br>").append("</td>");
                } else {
                    row
                        .append("<td class=\"result\">")
                        .append("<div class=\"left\">")
                        .append(mt.mtReverse ? tmMt.mtResX : tmMt.mtResA)
                        .append("</div>")
                        .append("<div class=\"right\">")
                        .append(mt.mtReverse ? tmMt.mtResA : tmMt.mtResX)
                        .append("</div>")
                        .append("</td>")
                    ;

                    for (int[] res : tmMt.result) {
                        if (mt.mtReverse) {
                            int tmp = res[0];
                            res[0] = res[1];
                            res[1] = tmp;
                        }
            
                        row.append("<td class=\"game\">");

                        if (res[0] > 0 || res[1] > 0)
                            row
                                .append("<div class=\"left").append(res[0] > res[1] ? " winner" : "").append("\">")
                                .append(res[0]).append("</div>")
                                .append("<div class=\"right").append(res[1] > res[0] ? " winner" : "").append("\">")
                                .append(res[1]).append("</div>")
                            ;
                        else
                            row.append("<td class=\"game empty\">").append("<br>").append("</td>");

                        row.append("</td>");
                    }

                    if (tmMt.result.length < colspan)
                        row.append("<td class=\"game\" colspan=\"").append(colspan - tmMt.result.length).append("\">").append("<br>").append("</td>");
                }
            }
        } else {                    
            row
                .append("<td class=\"result\">")
                .append("<div class=\"left\">")
                .append(mt.mtReverse ? mt.mtResX : mt.mtResA)
                .append("</div>")
                .append("<div class=\"right\">")
                .append(mt.mtReverse ? mt.mtResA : mt.mtResX)
                .append("</div>")
                .append("</td>")
            ;

            for (int[] res : mt.result) {
                if (mt.mtReverse) {
                    int tmp = res[0];
                    res[0] = res[1];
                    res[1] = tmp;
                }
            
                if (res[0] != 0 || res[1] != 0) {
                    row
                        .append("<td class=\"game\">")
                        .append("<div class=\"left").append(res[0] > res[1] ? " winner" : "").append("\">")
                        .append(res[0])
                        .append("</div>")
                        .append("<div class=\"right").append(res[1] > res[0] ? " winner" : "").append("\">")
                        .append(res[1])
                        .append("</div>")
                        .append("</td>")
                    ;
                }
                else {
                    row.append("<td class=\"game empty\">").append("<br>").append("</td>");
                }
            }

            if (mt.result.length < colspan) {
                row.append("<td class=\"game empty\" colspan=\"").append(colspan - mt.result.length).append("\">").append("<br>").append("</td>");
            }
        }

                
        row.insert(0, tr).append("</tr>").append(SEP);
        
        return row.toString();
    }
    
    public static String writeFilter(EnumSet<FilterTypes> what, Competition[] events, Association[] assocs, Group[] groups) {
        StringBuilder buffer = new StringBuilder();
        
        // Filter
        buffer
            .append("<div id=\"filter\" class=\"row filter\">").append(SEP)
            .append("<fieldset class=\"col-12 my-3 filter border\">").append(SEP)
            .append("<legend><a href=\"#filter-content\" data-bs-toggle=\"collapse\" class=\"btn btn-light\">")
            .append("<span class=\"oi oi-chevron-bottom pr-3\"></span>")
            .append("<span class=\"h5\" data-i18n=\"report.filter.filter\"></span>")
            .append("</a></legend>").append(SEP)
            .append("<div id=\"filter-content\" class=\"collapse show\">").append(SEP)
        ;
        
        // Filter Events
        if (what.contains(FilterTypes.FilerEvent)) {
            buffer
                .append("<div class=\"row form-group\">").append(SEP)
                .append("<label for=\"events\" class=\"col-md-2 offset-md-1 col-form-label\"><span data-i18n=\"dates.filter.events\"></span></label>").append(SEP)
                .append("<div class=\"col-md-8\"><select class=\"form-select\" id=\"events\">")
                .append("<option value=\"\" data-i18n=\"report.filter.all-events\">").append("</option>")
            ;

            for (Competition cp : events) {
                buffer.append("<option value=\"").append(cp.cpName).append("\">").append(cp.cpDesc).append("</option>");
            }

            buffer
                .append("</select></div>").append(SEP)
                .append("</div>").append(SEP)
            ;
        }
        
        // Filter Groups
        if (what.contains(FilterTypes.FilterGroup)) {
            buffer
                .append("<div class=\"row form-group\">").append(SEP)
                .append("<label for=\"events\" class=\"col-md-2 offset-md-1 col-form-label\"><span data-i18n=\"events.filter.groups\"></span></label>").append(SEP)
                .append("<div class=\"col-md-8\"><select class=\"form-select\" id=\"groups\">")
            ;

            for (Group gr : groups) {
                buffer
                    .append("<option data-webgen-href=\"")
                    .append(gr.getFileName())
                    .append("\">")
                    .append(gr.grDesc)
                    .append("</option>")
                ;
            }

            buffer
                .append("</select></div>").append(SEP)
                .append("</div>").append(SEP)
            ;            
        }
        
        // Filter Associations
        if (what.contains(FilterTypes.FilterAssocication)) {
            buffer
                .append("<div class=\"row form-group\">").append(SEP)
                .append("<label for=\"assocs\" class=\"col-md-2 offset-md-1 col-form-label\"><span data-i18n=\"dates.filter.assocs\"></span></label>").append(SEP)
                .append("<div class=\"col-md-8\"><select class=\"form-select\" id=\"assocs\">")
                .append("<option value=\"\" data-i18n=\"report.filter.all-assocs\">").append("</option>")
            ;

            for (Association na : assocs) {
                buffer.append("<option value=\"").append(na.naName).append("\">").append(na.naDesc).append("</option>");
            }

            buffer
                .append("</select></div>").append(SEP)
                .append("</div>").append(SEP)
            ;
        }
        
        // Filter for name
        if (what.contains(FilterTypes.FilterName)) {
            buffer
                .append("<div class=\"row form-group\">").append(SEP)
                .append("<label for=\"names\" class=\"col-md-2 offset-md-1 col-form-label\"><span data-i18n=\"report.filter.names\"></span></label>").append(SEP)
                .append("<div class=\"col-md-8\"><input class=\"form-control\" type=\"text\" id=\"names\">").append("</div>").append(SEP)
                .append("</div>").append(SEP)
            ;
        }
        
        // Filter for table
        if (what.contains(FilterTypes.FilterTable)) {
            buffer
                .append("<div class=\"row form-group\">").append(SEP)
                .append("<label for=\"tables\" class=\"col-md-2 offset-md-1 col-form-label\"><span data-i18n=\"report.filter.tables\"></span></label>").append(SEP)
                .append("<div class=\"col-md-8\"><input class=\"form-control\" type=\"text\" id=\"tables\">").append("</div>").append(SEP)
                .append("</div>").append(SEP)
            ;
        }
                
        // Filter for startnumber
        if (what.contains(FilterTypes.FilterStartNumber)) {
            buffer
                .append("<div class=\"row form-group\">").append(SEP)
                .append("<label for=\"startno\" class=\"col-md-2 offset-md-1 col-form-label\"><span data-i18n=\"report.filter.startno\"></span></label>").append(SEP)
                .append("<div class=\"col-md-8\"><input class=\"form-control\" type=\"text\" id=\"startno\">").append("</div>").append(SEP)
                .append("</div>").append(SEP)
            ;
        } 
        
        // Closing
        buffer
            .append("</div>").append(SEP)
            .append("</fieldset>").append(SEP)
            .append("</div>").append(SEP);
                
        return buffer.toString();
    }
}
