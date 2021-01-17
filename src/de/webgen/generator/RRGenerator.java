/* Copyright (C) 2020 Christoph Theis */

package de.webgen.generator;

import de.webgen.WebGen;
import de.webgen.database.IDatabase;
import de.webgen.database.Group;
import de.webgen.database.match.Match;
import de.webgen.database.position.Groupposition;
import de.webgen.generator.report.Report;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class RRGenerator extends Generator {

    public RRGenerator() {
    }


    @Override
    public String generate(List<List<Match>> matchList, IDatabase database) throws SQLException {
        Group gr = matchList.get(0).get(0).gr;

        List<Groupposition> groupList = database.readGroupposition(gr);

        // Matchliste stuerzen
        Match groupMatches[][] = new Match[gr.grSize][gr.grSize];
        Map<Integer, Integer>  groupModus = new java.util.HashMap<>();

        for (int i = 0; i < groupList.size(); i++) {
            groupModus.put(groupList.get(i).stID, groupList.get(i).stNr);
        }

        for (int round = 0; round < matchList.size(); round++) {
            List<Match> roundList = matchList.get(round);

            for (int match = 0; match < roundList.size(); match++) {
                Match mt = roundList.get(match);
                groupMatches[groupModus.get(mt.stA) - 1][groupModus.get(mt.stX) - 1] = mt;
            }
        }

        StringBuilder content = new StringBuilder();
        
        content
            .append("<h4 class=\"row col-12 my-4\">")
            .append("<span data-i18n=\"rr.title\" ")
            .append("data-i18n-options=\'{")
            .append("\"cpname\" : \"").append(gr.cp.cpName).append("\"").append(", ")
            .append("\"cpdesc\" : \"").append(gr.cp.cpDesc.replaceAll("'", "&rsquo;")).append("\"").append(", ")
            .append("\"grname\" : \"").append(gr.grName).append("\"").append(", ")
            .append("\"grdesc\" : \"").append(gr.grDesc.replaceAll("'", "&rsquo;")).append("\"")
            .append("}\'></span>")
            .append("</h4>")
            .append(SEP)
        ;
        
        content.append("<div class=\"row col-12\">").append(SEP);
        content.append("<table class=\"rr draw table table-bordered\">").append(SEP);
        content.append("<thead>").append(SEP);

        content.append("<tr>").append(SEP);
        content.append("<th class=\"name\" scope=\"col\" colspan=\"2\">");
        content.append(gr.cp.cpName).append(" - ").append(gr.grDesc);
        content.append("</th>");

        for (int i = 0; i < gr.grSize; i++) {
            content.append("<th class=\"pos\" scope=\"col\">");
            content.append(i + 1);
            content.append("</th>");
        }

        content.append("<th class=\"rr points\" scope=\"col\">");
        content.append("<span data-i18n=\"rr.mt-pts\"></span>");
        content.append("</th>");
        
        content.append("<th class=\"games\" scope=\"col\">");
        content.append("<span data-i18n=\"rr.games\"></span>");
        content.append("</th>");
        
        content.append("<th class=\"standing\" scope=\"col\">");
        content.append("<span data-i18n=\"rr.stdng\"></span>");
        content.append("</th>");
                
        content.append("</tr>").append(SEP);
        content.append("</thead>").append(SEP);
        
        content.append("<tbody>").append(SEP);
        for (int i = 0; i < gr.grSize; i++) {
            content.append("  <tr>");
            content.append("<td class=\"pos\" scope=\"col\">");
            content.append(i + 1);
            content.append("</td>");
            content.append("<td class=\"name\" scope=\"col\">");

            if (groupList.get(i).stNr == i + 1)
                content.append(groupList.get(i).getEntry());
            else
                content.append("&nbsp;");

            content.append("</td>");

            for (int j = 0; j < gr.grSize; j++) {
                if (i == j)
                    content.append("    <td class=\"no-result\"></td>").append(SEP);
                else {
                    boolean swap = groupMatches[i][j] == null;
                    Match mt = swap ? groupMatches[j][i] : groupMatches[i][j];

                    content.append("<td class=\"result\" scope=\"col\">");

                    if (mt.mtWalkOverA && !swap || mt.mtWalkOverX && swap)
                        content.append("w/o");
                    else if (mt.mtInjuredA && !swap || mt.mtInjuredX && swap)
                        content.append("Inj.");
                    else if (mt.mtDisqualifiedA && !swap || mt.mtDisqualifiedX && swap)
                        content.append("Disqu.");
                    else if (mt.mtResA == 0 && mt.mtResX == 0)
                        content.append("<br>");
                    else if (mt.isFinished()) {
                        if (gr.cp.isTeam())
                            content.append("<a href=\"#\" tabindex=\"0\" role=\"button\" data-toggle=\"popover\" data-target=\"[data-webgen-teammatch=&quot;").append(mt.mtNr).append("&quot;]\">");
                        if (swap)
                            content.append(mt.mtResX).append("&nbsp;:&nbsp;").append(mt.mtResA);
                        else
                            content.append(mt.mtResA).append("&nbsp;:&nbsp;").append(mt.mtResX);
                        if (gr.cp.isTeam())
                            content.append("</a>");
                    } else {
                        content.append("<br>");
                    }

                    content.append("</td>").append(SEP);
                }
            }

            content.append("<td class=\"points\" scope=\"col\">");

            if (groupList.get(i).tmID == 0)
                content.append("<br>");
            else if (WebGen.isITTF())
                content.append(groupList.get(i).mtMatchPoints);
            else {
                content.append(groupList.get(i).mtPointsA);
                content.append("&nbsp;:&nbsp;");
                content.append(groupList.get(i).mtPointsX);
            }

            content.append("</td>");

            content.append("<td class=\"games\" scope=\"col\">");
            if (groupList.get(i).tmID == 0)
                content.append("<br>");
            else if (gr.cp.isTeam()) {
                content.append(groupList.get(i).mtMatchesA);
                content.append("&nbsp;:&nbsp;");
                content.append(groupList.get(i).mtMatchesX);
            } else {
                content.append(groupList.get(i).mtSetsA);
                content.append("&nbsp;:&nbsp;");
                content.append(groupList.get(i).mtSetsX);
            }

            content.append("</td>");

            content.append("<td class=\"standing\" scope=\"col\">");
            if (groupList.get(i).tmID == 0)
                content.append("<br>");
            else
                content.append(groupList.get(i).stPos + gr.grWinner - 1);
            content.append("</td>");
            content.append("</tr>").append(SEP);
        }
        content.append("</tbody>").append(SEP);
        content.append("</table>").append(SEP);
        content.append("</div>").append(SEP);

        //
        // Ausgabe der Ergebnisse im Detail
        //
        content.append("<p></p>").append(SEP);
        
        if (gr.grNotes != null) {
            StringBuilder notes = new StringBuilder();
            content.append("<div class=\"rr notes\">");

            for (String line : gr.grNotes.split("\n")) {
                if (notes.length() > 0)
                    notes.append("<br>");
                if (line.startsWith("*"))
                    notes.append("<strong>").append(line.substring(1).trim()).append("</strong>");
                else
                    notes.append(line);                       
            }
            content.append(notes);
            content.append("</div>");
            content.append("<p></p>");
        }
        
        content.append(generateMatchListTables(matchList, database));
        
        return content.toString();
    }
    
    protected String generateMatchListTables(List<List<Match>> matchList, IDatabase database) throws SQLException {
        if (matchList.isEmpty() || matchList.get(0).isEmpty())
            return "";
        
        Group gr = matchList.get(0).get(0).gr;
        
        if (gr == null)
            return "";
        
        StringBuilder content = new StringBuilder();
        
        for (int i = 0; i < matchList.size(); i++) {
            content.append("<div class=\"table-responsive\">").append(SEP);
            content
                .append("<h6>")
                .append("<strong>")
                .append("<span data-i18n=\"rr.format.long.round\" data-i18n-options=\'{\"round\" : \"").append(matchList.get(i).get(0).mtRound).append("\"}\'></span>")
                .append("</strong>")
                .append("</h6>")
                .append(SEP);
            
            content.append("<table class=\"rr matches\">").append(SEP);
            
            content.append(generateMatchList(matchList.get(i), database));
            
            content.append("</table>").append(SEP);
            content.append("</div>").append(SEP);
            content.append("<p></p>").append(SEP);            
        }
        
        return content.toString();
    }

    @Override
    public String generate(Report report, IDatabase database) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
