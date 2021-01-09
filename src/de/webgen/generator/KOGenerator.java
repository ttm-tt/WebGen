/* Copyright (C) 2020 Christoph Theis */

package de.webgen.generator;

import de.webgen.database.Database;
import de.webgen.database.Group;
import de.webgen.database.match.Match;
import de.webgen.generator.report.Report;

import java.sql.SQLException;
import java.util.List;


public class KOGenerator extends Generator {

    public KOGenerator() {
    }


    @Override
    @SuppressWarnings("DeadBranch")
    public String generate(List<List<Match>> matchList, Database database) throws SQLException {
        StringBuilder buffer = new StringBuilder();
        
        Group gr = null;
        
        if (!matchList.isEmpty() && !matchList.get(0).isEmpty())
            gr = matchList.get(0).get(0).gr;
        
        if (gr == null)
            return "";
                        
        buffer
            .append("<h4 class=\"row col-12 my-4\">")
            .append("<span data-i18n=\"ko.title\" ")
            .append("data-i18n-options=\'{")
            .append("\"cpname\" : \"").append(gr.cp.cpName).append("\"").append(", ")
            .append("\"cpdesc\" : \"").append(gr.cp.cpDesc.replaceAll("'", "&rsquo;")).append("\"").append(", ")
            .append("\"grname\" : \"").append(gr.grName).append("\"").append(", ")
            .append("\"grdesc\" : \"").append(gr.grDesc.replaceAll("'", "&rsquo;")).append("\"")
            .append("}\'></span>")
            .append("</h4>")
            .append(SEP)
        ;
        
        buffer.append("<div id=\"ko-nav\" class=\"row col-12 pb-3\">").append(SEP);
        buffer.append("<ul class=\"nav nav-tabs\">").append(SEP);  
        if (gr.grNofRounds != 1 && matchList.size() > 1)
            buffer.append("<li class=\"nav-item\"><a class=\"nav-link\" href=\"#\" id=\"one-round\">").append("<span data-i18n=\"nav.ko.one-round\"></span>").append("</a>").append("</li>").append(SEP);
        buffer.append("<li class=\"nav-item\"><a class=\"nav-link\" href=\"#\" id=\"all-rounds\">").append("<span data-i18n=\"nav.ko.all-rounds\"></span>").append("</a>").append("</li>").append(SEP);
        
        if ( gr.grModus == Group.MOD_SKO && gr.grSize > 32 && 
             (gr.grNofRounds == 0 || gr.grNofRounds > 5) && (gr.grNofMatches == 0 || gr.grNofMatches > 16) ) {
            buffer.append("<li class=\"nav-item\"><a class=\"nav-link\" href=\"#\" id=\"last-32\" >").append("<span data-i18n=\"nav.ko.last-32\"></span>").append("</a>").append("</li>").append(SEP);
        }
             
        
        if (gr.cp.isTeam())
            buffer.append("<li class=\"nav-item\"><a class=\"nav-link\" href=\"#\" id=\"teammatches\">").append("<span data-i18n=\"nav.ko.teammatches\"></span>").append("</a>").append("</li>").append(SEP);
        
        buffer.append("</ul>").append(SEP);
        buffer.append("</div>").append(SEP);
        
        if (matchList.isEmpty() || matchList.get(0).isEmpty()) {
            buffer.append("<div id=\"all-rounds-content\" class=\"ko-content\" style=\"display:none;\"></div>").append(SEP);
            
            return buffer.toString();
        }
        
        List<String> contentList;
        StringBuilder content = new StringBuilder();
        
        int maxRound = gr.grNofRounds;
        if (maxRound == 0)
            maxRound = matchList.size();

        // One Round
        if (gr.grNofRounds != 1 && matchList.size() > 1) {
            for (int startRd = 0; startRd < maxRound; startRd++) {
                if (!matchList.get(startRd).isEmpty()) {
                    Match mt = matchList.get(startRd).get(0);

                    if (mt.gr.grNofRounds > 0 && mt.mtRound > mt.gr.grNofRounds)
                        break;
                }
                
                content.append("<div class=\"row item group ko round").append(startRd == 0 ? " active" : "").append("\">").append(SEP);

                contentList = generateRaster(matchList, startRd, startRd + 1);

                content.append("<table class=\"ko draw\">").append(SEP);

                content.append("  <tr>");

                content.append("<th>");
                
                if (!matchList.get(startRd).isEmpty()) {
                    Match mt = matchList.get(startRd).get(0);

                    if (mt.mtRound <= mt.gr.grQualRounds)
                        content.append("<span data-i18n=\"ko.format.long.qualification\" />");
                    else if (mt.gr.grNofRounds > 0 || mt.gr.grNofMatches > 0 || mt.gr.grWinner != 1)
                        content.append("<span data-i18n=\"ko.format.long.round\" data-i18n-options=\'{\"round\":\"").append(mt.mtRound - gr.grQualRounds).append("\"}\' />");
                    else {
                        switch (maxRound - mt.mtRound + 1) {
                            case 1 : // Final
                                content.append("<span data-i18n=\"ko.format.long.final\" />");
                                break;
                                
                            case 2 : // Semifinal
                                content.append("<span data-i18n=\"ko.format.long.semifinal\" />");
                                break;
                                
                            default :
                                content.append("<span data-i18n=\"ko.format.long.round\" data-i18n-options=\'{\"round\":\"").append(mt.mtRound - gr.grQualRounds).append("\"}\' />");
                                break;
                        }
                    }
                }
                
                content.append("</th>");

                // Und dann noch der Sieger
                if (false && matchList.size() > 0 && matchList.get(0).size() > 0 && matchList.get(0).get(0).gr.grNofRounds == 0)
                    content.append("<th>").append("<span data-i18n=\"ko.winner\"></span>").append("</th>");
                else
                    content.append("<th>").append("</th>");

                content.append("</tr>").append(SEP);

                for (int i = 0; i < contentList.size(); i++) {
                    content.append("  <tr>");
                    content.append(contentList.get(i));
                    content.append("</tr>").append(SEP);
                }

                content.append("</table>").append(SEP);     

                content.append("</div>").append(SEP);
            }

            buffer.append("<div id=\"one-round-content\" class=\"ko-content col-12\" style=\"display:none;\">").append(SEP);
            buffer.append("<div id=\"ko-nav-rounds\" class=\"row\">").append(SEP);
            buffer.append("<ul class=\"pagination col-6 justify-content-start\">");
            buffer.append("<li class=\"page-item ko-first mx-1\">");
            buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-first\"><span data-i18n=\"ko.first\"></span></a>");
            buffer.append("</li>");
            buffer.append("<li class=\"page-item ko-prev mx-1\">");
            buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-prev\"><span data-i18n=\"ko.prev\"></span></a>");
            buffer.append("</li>");
            buffer.append("</ul>").append(SEP);
            buffer.append("<ul class=\"pagination col-6 justify-content-end\">");
            buffer.append("<li class=\"page-item ko-next mx-1\">");
            buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-next\"><span data-i18n=\"ko.next\"></span></a>");
            buffer.append("</li>").append(SEP);
            buffer.append("<li class=\"page-item ko-last mx-1\">");
            buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-last\"><span data-i18n=\"ko.last\"></span></a>");
            buffer.append("</li>").append(SEP);
            buffer.append("</ul>").append(SEP);
            buffer.append("</div>").append(SEP);
            buffer.append(content);
            buffer.append("<div id=\"ko-nav-rounds\" class=\"row\">").append(SEP);
            buffer.append("<ul class=\"pagination col-6 justify-content-start\">");
            buffer.append("<li class=\"page-item ko-first mx-1\">");
            buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-first\"><span data-i18n=\"ko.first\"></span></a>");
            buffer.append("</li>");
            buffer.append("<li class=\"page-item ko-prev mx-1\">");
            buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-prev\"><span data-i18n=\"ko.prev\"></span></a>");
            buffer.append("</li>");
            buffer.append("</ul>").append(SEP);
            buffer.append("<ul class=\"pagination col-6 justify-content-end\">");
            buffer.append("<li class=\"page-item ko-next mx-1\">");
            buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-next\"><span data-i18n=\"ko.next\"></span></a>");
            buffer.append("</li>").append(SEP);
            buffer.append("<li class=\"page-item ko-last mx-1\">");
            buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-last\"><span data-i18n=\"ko.last\"></span></a>");
            buffer.append("</li>").append(SEP);
            buffer.append("</ul>").append(SEP);
            buffer.append("</div>").append(SEP);
            
            if (gr.grNotes != null) {
                StringBuilder notes = new StringBuilder();
                buffer.append("<div class=\"row ko notes\">");
                
                for (String line : gr.grNotes.split("\n")) {
                    if (notes.length() > 0)
                        notes.append("<br>");
                    if (line.startsWith("*"))
                        notes.append("<strong>").append(line.substring(1).trim()).append("</strong>");
                    else
                        notes.append(line);                       
                }
                buffer.append(notes);
                buffer.append("</div>");
            }

            buffer.append("</div>").append(SEP);
        }
        
        // All Rounds
        content = new StringBuilder();
        contentList = generateRaster(matchList);

        content.append("<div class=\"group ko\">").append(SEP);

        content.append("<table class=\"ko draw\">").append(SEP);
        
        content.append("  <tr>");

        for (int i = 0; i < matchList.size(); i++) {
            content.append("<th>");

            if (!matchList.get(i).isEmpty()) {
                Match mt = matchList.get(i).get(0);

                if (mt.gr.grNofRounds > 0 && mt.mtRound > mt.gr.grNofRounds)
                    break;
                else if(mt.mtRound <= mt.gr.grQualRounds) 
                    content.append("<span data-i18n=\"ko.format.long.qualification\" />");
                else if (mt.gr.grNofRounds > 0 || mt.gr.grNofMatches > 0 || mt.gr.grWinner != 1)
                    content.append("<span data-i18n=\"ko.format.long.round\" data-i18n-options=\'{\"round\":\"").append(mt.mtRound - gr.grQualRounds).append("\"}\' />");
                else {
                    switch (maxRound - mt.mtRound + 1) {
                        case 1 : // Final
                            content.append("<span data-i18n=\"ko.format.long.final\" />");
                            break;

                        case 2 : // Semifinal
                            content.append("<span data-i18n=\"ko.format.long.semifinal\" />");
                            break;

                        default :
                            content.append("<span data-i18n=\"ko.format.long.round\" data-i18n-options=\'{\"round\":\"").append(mt.mtRound - gr.grQualRounds).append("\"}\' />");
                            break;
                    }
                }
            }
            
            content.append("</th>");
        }
        
        // Und dann noch der Sieger
        if (matchList.size() > 0 && matchList.get(0).size() > 0 && matchList.get(0).get(0).gr.grNofRounds == 0)
            content.append("<th>").append("<span data-i18n=\"ko.winner\" />").append("</th>");
        else
            content.append("<th>").append("</th>");
        
        content.append("</tr>").append(SEP);

        for (int i = 0; i < contentList.size(); i++) {
            content.append("  <tr>");
            content.append(contentList.get(i));
            content.append("</tr>").append(SEP);
        }

        content.append("</table>").append(SEP);
        
        content.append("</div>").append(SEP);

        buffer.append("<div id=\"all-rounds-content\" class=\"ko-content\" style=\"display:none;\">").append(SEP);
        buffer.append(content);
        
        if (gr.grNotes != null) {
            StringBuilder notes = new StringBuilder();
            buffer.append("<div class=\"row ko notes\">");

            for (String line : gr.grNotes.split("\n")) {
                if (notes.length() > 0)
                    notes.append("<br>");
                if (line.startsWith("*"))
                    notes.append("<strong>").append(line.substring(1).trim()).append("</strong>");
                else
                    notes.append(line);                       
            }
            buffer.append(notes);
            buffer.append("</div>");
        }
                
        buffer.append("</div>").append(SEP);
        
        // Last 32
        if ( gr.grModus == Group.MOD_SKO && gr.grSize > 32 && 
             (gr.grNofRounds == 0 || gr.grNofRounds > 5) && (gr.grNofMatches == 0 || gr.grNofMatches > 16) ) {
            content = new StringBuilder();
            contentList = generateRaster(matchList, matchList.size() - 5, matchList.size());
            
            content.append("<div class=\"group ko\">").append(SEP);

            content.append("<table class=\"ko draw\">").append(SEP);

            content.append("  <tr>");

            for (int i = matchList.size() - 5; i < matchList.size(); i++) {
                content.append("<th>");

                if (!matchList.get(i).isEmpty()) {
                    Match mt = matchList.get(i).get(0);

                    if (mt.gr.grNofRounds > 0 && mt.mtRound > mt.gr.grNofRounds)
                        break;
                    else if(mt.mtRound <= mt.gr.grQualRounds)
                        content.append("<span data-i18n=\"ko.format.long.qualification\" />");
                    else if (mt.gr.grNofRounds > 0 || mt.gr.grNofMatches > 0 || mt.gr.grWinner != 1)
                        content.append("<span data-i18n=\"ko.format.long.round\" data-i18n-options=\'{\"round\":\"").append(mt.mtRound - gr.grQualRounds).append("\"}\' />");
                    else {
                        switch (maxRound - mt.mtRound+ 1) {
                            case 1 : // Final
                                content.append("<span data-i18n=\"ko.format.long.final\" />");
                                break;

                            case 2 : // Semifinal
                                content.append("<span data-i18n=\"ko.format.long.semifinal\" />");
                                break;

                            default :
                                content.append("<span data-i18n=\"ko.format.long.round\" data-i18n-options=\'{\"round\":\"").append(mt.mtRound - gr.grQualRounds).append("\"}\' />");
                                break;
                        }
                    }
                }
                
                content.append("</th>");
            }

            // Und dann noch der Sieger
            if (matchList.size() > 0 && matchList.get(0).size() > 0 && matchList.get(0).get(0).gr.grNofRounds == 0)
                content.append("<th>").append("<span data-i18n=\"ko.winner\" />").append("</th>");
            else
                content.append("<th>").append("</th>");

            content.append("</tr>").append(SEP);

            for (int i = 0; i < contentList.size(); i++) {
                content.append("  <tr>");
                content.append(contentList.get(i));
                content.append("</tr>").append(SEP);
            }

            content.append("</table>").append(SEP);
            content.append("</div>").append(SEP);

            buffer.append("<div id=\"last-32-content\" class=\"ko-content\" style=\"display:none;\">").append(SEP);
            buffer.append(content);
            
            if (gr.grNotes != null) {
                StringBuilder notes = new StringBuilder();
                buffer.append("<div class=\"row ko notes\">");
                
                for (String line : gr.grNotes.split("\n")) {
                    if (notes.length() > 0)
                        notes.append("<br>");
                    if (line.startsWith("*"))
                        notes.append("<strong>").append(line.substring(1).trim()).append("</strong>");
                    else
                        notes.append(line);                       
                }
                buffer.append(notes);
                buffer.append("</div>");
            }
                    
            buffer.append("</div>").append(SEP);
        }
        
        // Team matches
        if (gr.cp.isTeam()) {
            content = new StringBuilder();
            
            for (int startRd = 0; startRd < maxRound; startRd++) {
                if (!matchList.get(startRd).isEmpty()) {
                    Match mt = matchList.get(startRd).get(0);

                    if (mt.gr.grNofRounds > 0 && mt.mtRound > mt.gr.grNofRounds)
                        break;
                }
                
                content.append("<div class=\"row item group ko round").append(startRd == 0 ? " active" : "").append("\">").append(SEP);
                
                content
                    .append("<h6>")
                    .append("<strong>")
                ;
                if (!matchList.get(startRd).isEmpty()) {
                    Match mt = matchList.get(startRd).get(0);

                    if (mt.mtRound <= mt.gr.grQualRounds)
                        content.append("<span data-i18n=\"ko.format.long.qualification\" />");
                    else
                        content.append("<span data-i18n=\"ko.format.long.round\" data-i18n-options=\'{\"round\":\"").append(mt.mtRound - gr.grQualRounds).append("\"}\' />");
                }
                content
                    .append("</strong>")
                    .append("</h6>")
                    .append(SEP);
                    
                
                content.append("<table class=\"ko matches\">").append(SEP);
                content.append(generateMatchList(matchList.get(startRd), database));            
                content.append("</table>").append(SEP);

                content.append("</div>").append(SEP);
            }

            buffer.append("<div id=\"teammatches-content\" class=\"ko-content col-12\" style=\"display:none;\">").append(SEP);
            
            if (gr.grNofRounds != 1) {
                buffer.append("<div id=\"ko-nav-rounds\" class=\"row\">").append(SEP);
                buffer.append("<ul class=\"pagination col-6 justify-content-start\">");
                buffer.append("<li class=\"page-item ko-first mx-1\">");
                buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-first\"><span data-i18n=\"ko.first\"></span></a>");
                buffer.append("</li>");
                buffer.append("<li class=\"page-item ko-prev mx-1\">");
                buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-prev\"><span data-i18n=\"ko.prev\"></span></a>");
                buffer.append("</li>");
                buffer.append("</ul>").append(SEP);
                buffer.append("<ul class=\"pagination col-6 justify-content-end\">");
                buffer.append("<li class=\"page-item ko-next mx-1\">");
                buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-next\"><span data-i18n=\"ko.next\"></span></a>");
                buffer.append("</li>").append(SEP);
                buffer.append("<li class=\"page-item ko-last mx-1\">");
                buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-last\"><span data-i18n=\"ko.last\"></span></a>");
                buffer.append("</li>").append(SEP);
                buffer.append("</ul>").append(SEP);
                buffer.append("</div>").append(SEP);
            }
            buffer.append(content);
            if (gr.grNofRounds != 1) {
                buffer.append("<div id=\"ko-nav-rounds\" class=\"row\">").append(SEP);
                buffer.append("<ul class=\"pagination col-6 justify-content-start\">");
                buffer.append("<li class=\"page-item ko-first mx-1\">");
                buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-first\"><span data-i18n=\"ko.first\"></span></a>");
                buffer.append("</li>");
                buffer.append("<li class=\"page-item ko-prev mx-1\">");
                buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-prev\"><span data-i18n=\"ko.prev\"></span></a>");
                buffer.append("</li>");
                buffer.append("</ul>").append(SEP);
                buffer.append("<ul class=\"pagination col-6 justify-content-end\">");
                buffer.append("<li class=\"page-item ko-next mx-1\">");
                buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-next\"><span data-i18n=\"ko.next\"></span></a>");
                buffer.append("</li>").append(SEP);
                buffer.append("<li class=\"page-item ko-last mx-1\">");
                buffer.append("<a class=\"page-link\" href=\"#\" id=\"ko-last\"><span data-i18n=\"ko.last\"></span></a>");
                buffer.append("</li>").append(SEP);
                buffer.append("</ul>").append(SEP);
                buffer.append("</div>").append(SEP);
            }
            buffer.append("</div>").append(SEP);
        }
        
        return buffer.toString();
    }


    /*
     * Ein Spiel hat
     *      4 Zeilen in der ersten Runde
     *      8 Zeilen in der zweiten Runde
     *     16 Zeilen in der dritten Runde
     *     32 Zeilen in der vierten Runde
     */

    protected List<String> generateRaster(List<List<Match>> matchList) {
        if (matchList.isEmpty() || matchList.get(0).isEmpty())
            return new java.util.ArrayList<>();
        
        Group gr = matchList.get(0).get(0).gr;
        int maxRound = gr.grNofRounds;
        if (maxRound == 0)
            maxRound = matchList.size();
        
        return generateRaster(matchList, 0, maxRound);
    }
    
    protected List<String> generateRaster(List<List<Match>> matchList, int startRd, int endRd) {
        if (startRd >= matchList.size())
            return new java.util.ArrayList<>();
        
        Match mt = matchList.get(startRd).get(0);

        int maxRound = matchList.size();
        int maxMatch = matchList.get(startRd).size();

        if (mt.gr.grNofRounds > 0)
            maxRound = Math.min(maxRound, mt.gr.grNofRounds );
        if (mt.gr.grNofMatches > 0)
            maxMatch = Math.min(maxMatch, mt.gr.grNofMatches / (1 << (mt.mtRound - 1)));

        maxRound = Math.min(maxRound, endRd);
        
        StringBuilder[] contentArray = new StringBuilder[maxMatch * 4];

        for (int i = 0; i < contentArray.length; i++)
            contentArray[i] = new StringBuilder();

        for (int round = 0; round < endRd -startRd; round++, maxMatch /= 2) {
            if (round + startRd >= matchList.size())
                break;
            
            List<Match> roundList = matchList.get(round + startRd);
            int matchRows = (int) Math.pow(2, round) * 4;

            int idx = 0;

            for (int match = 0; match < maxMatch; match++) {
                mt = roundList.get(match);
                Match prevMt = null;

                if (round > 0)
                    prevMt = matchList.get(round + startRd - 1).get(2 * match);

                for (int x = 0; x < matchRows / 4 - 1; x++)
                    contentArray[idx + x].append("<td class=\"none\"><br></td>");

                idx += matchRows / 4 - 1;

                contentArray[idx].append("<td class=\"bottom\">");
                
                if (round == 0)
                    contentArray[idx].append("<span class=\"badge\">").append(2 * match + 1).append("</span>");
                
                contentArray[idx]
                        .append("<span class=\"ko-entry\" ").append(mt.stA != 0 ? "data-webgen-stid=\"" + mt.stA + "\"" : "").append(">")
                        .append(mt.aToString(true, true, round == 0))
                        .append("</span>");
                
                contentArray[idx].append("</td>");

                idx += 1;

                contentArray[idx].append("<td class=\"right\" valign=\"top\">");
                if (round == 0)
                    contentArray[idx].append("<br>");
                else if (prevMt == null)
                    contentArray[idx].append("<br>");
                else if (prevMt.isFinished())
                    contentArray[idx].append(prevMt.getResultShortForm(true));
                else if (prevMt.mtDateTime != null && prevMt.mtDateTime.getTime() != 0) {
                    contentArray[idx]
                        .append("<span data-i18n=\"ko.format.short.date\" ")
                        .append("data-i18n-options=\'{")
                        .append("\"wday\" : \"").append(prevMt.mtDateTime.getDay() + 1).append("\", ")
                        .append("\"day\" : \"").append(String.format("%02d", prevMt.mtDateTime.getDate())).append("\", ")
                        .append("\"month\" : \"").append(prevMt.mtDateTime.getMonth() + 1).append("\"")
                        .append("}\' />")
                    ;
                    
                    if (prevMt.mtDateTime.getHours() != 0 || prevMt.mtTable != 0)
                        contentArray[idx]
                            .append(" ")
                            .append("<span data-i18n=\"ko.format.short.time\" ")
                            .append("data-i18n-options=\'{")
                            .append("\"hour\" : \"").append(String.format("%02d", prevMt.mtDateTime.getHours())).append("\", ")
                            .append("\"minute\" : \"").append(String.format("%02d", prevMt.mtDateTime.getMinutes())).append("\" ")
                            .append("}\' />")
                        ;
                    
                    if (prevMt.mtTable != 0) {
                        contentArray[idx]
                            .append(" ")
                            .append("<span data-i18n=\"ko.format.short.table\" ")
                            .append("data-i18n-options=\'{")
                            .append("\"table\" : \"").append(prevMt.mtTable).append("\" ")
                            .append("}\' />");
                    }
                }
                else
                    contentArray[idx].append("<br>");
                
                contentArray[idx].append("</td>");

                idx += 1;

                for (int x = 0; x < matchRows / 2 - 2; x++)
                    contentArray[idx + x].append("<td class=\"right\"><br></td>");

                idx += matchRows / 2 - 2;

                contentArray[idx].append("<td class=\"bottomright\">");
                
                if (round == 0)
                    contentArray[idx].append("<span class=\"badge\">").append(2 * match + 2).append("</span>");
                
                contentArray[idx]
                        .append("<span class=\"ko-entry\" ").append(mt.stX != 0 ? "data-webgen-stid=\"" + mt.stX + "\"" : "").append(">")
                        .append(mt.xToString(true, true, round == 0))
                        .append("</span>");
                
                contentArray[idx].append("</td>");

                idx += 1;

                if (round > 0)
                    prevMt = matchList.get(round + startRd - 1).get(2 * match + 1);

                contentArray[idx].append("<td class=\"none\" valign=\"top\">");
                if (round == 0)
                    contentArray[idx].append("<br>");
                else if (prevMt == null)
                    contentArray[idx].append("<br>");
                else if (prevMt.isFinished())
                    contentArray[idx].append(prevMt.getResultShortForm(true));
                else if (prevMt.mtDateTime != null && prevMt.mtDateTime.getTime() != 0) {
                    contentArray[idx]
                        .append("<span data-i18n=\"ko.format.short.date\" ")
                        .append("data-i18n-options=\'{")
                        .append("\"wday\" : \"").append(prevMt.mtDateTime.getDay() + 1).append("\", ")
                        .append("\"day\" : \"").append(String.format("%02d", prevMt.mtDateTime.getDate())).append("\", ")
                        .append("\"month\" : \"").append(prevMt.mtDateTime.getMonth() + 1).append("\"")
                        .append("}\' />")
                    ;
                    
                    if (prevMt.mtDateTime.getHours() != 0 || prevMt.mtTable != 0)
                        contentArray[idx]
                            .append(" ")
                            .append("<span data-i18n=\"ko.format.short.time\" ")
                            .append("data-i18n-options=\'{")
                            .append("\"hour\" : \"").append(String.format("%02d", prevMt.mtDateTime.getHours())).append("\", ")
                            .append("\"minute\" : \"").append(String.format("%02d", prevMt.mtDateTime.getMinutes())).append("\" ")
                            .append("}\' />")
                        ;
                    
                    if (prevMt.mtTable != 0) {
                        contentArray[idx]
                            .append(" ")
                            .append("<span data-i18n=\"ko.format.short.table\" ")
                            .append("data-i18n-options=\'{")
                            .append("\"table\" : \"").append(prevMt.mtTable).append("\" ")
                            .append("}\' />");
                    }
                }
                else
                    contentArray[idx].append("<br>");

                contentArray[idx].append("</td>");

                idx += 1;

                for (int x = 0; x < matchRows / 4 - 1; x++)
                    contentArray[idx + x].append("<td class=\"none\"><br></td>");

                idx += matchRows / 4 - 1;
            }
        }

        // letzte Runde -> Sieger
        if (maxRound > 0)
        {
            int matchRows = (int) Math.pow(2, maxRound - startRd) * 4;
            int idx = 0;

            maxMatch = matchList.get(maxRound - 1).size();

            if (mt.gr.grNofMatches > 0)
                maxMatch = Math.min(maxMatch, mt.gr.grNofMatches / (1 << (maxRound + mt.mtRound - 2)));

            for (int match = 0; match < maxMatch; match++) {
                Match prevMt = matchList.get(maxRound - 1).get(match);

                for (int x = 0; x < matchRows / 4 - 1; x++)
                    contentArray[idx + x].append("<td class=\"none\"><br></td>");

                idx += matchRows / 4 - 1;

                contentArray[idx].append("<td class=\"bottom\">");
                contentArray[idx].append("<span class=\"ko-entry\" ");

                switch (prevMt.getWinnerAX()) {
                    case A :
                        contentArray[idx].append(prevMt.stA != 0 ? "data-webgen-stid=\"" + prevMt.stA + "\"" : "");
                        break;
                        
                    case X :
                        contentArray[idx].append(prevMt.stX != 0 ? "data-webgen-stid=\"" + prevMt.stX + "\"" : "");
                        
                    default :
                        break;
                }
                
                contentArray[idx].append(">");
                contentArray[idx]
                        .append(prevMt.winnerToString(true))
                        .append("</span>");                
                contentArray[idx].append("</td>");

                idx += 1;

                contentArray[idx].append("<td class=\"none\" valign=\"top\">");

                if (prevMt.isFinished())
                    contentArray[idx].append(prevMt.getResultShortForm(true));
                else if (prevMt.mtDateTime != null && prevMt.mtDateTime.getTime() != 0) {
                    contentArray[idx]
                        .append("<span data-i18n=\"ko.format.short.date\" ")
                        .append("data-i18n-options=\'{")
                        .append("\"wday\" : \"").append(prevMt.mtDateTime.getDay() + 1).append("\", ")
                        .append("\"day\" : \"").append(String.format("%02d", prevMt.mtDateTime.getDate())).append("\", ")
                        .append("\"month\" : \"").append(prevMt.mtDateTime.getMonth() + 1).append("\"")
                        .append("}\' />")
                    ;
                    
                    if (prevMt.mtDateTime.getHours() != 0 || prevMt.mtTable != 0)
                        contentArray[idx]
                            .append(" ")
                            .append("<span data-i18n=\"ko.format.short.time\" ")
                            .append("data-i18n-options=\'{")
                            .append("\"hour\" : \"").append(String.format("%02d", prevMt.mtDateTime.getHours())).append("\", ")
                            .append("\"minute\" : \"").append(String.format("%02d", prevMt.mtDateTime.getMinutes())).append("\" ")
                            .append("}\' />")
                        ;
                    
                    if (prevMt.mtTable != 0) {
                        contentArray[idx]
                            .append(" ")
                            .append("<span data-i18n=\"ko.format.short.table\" ")
                            .append("data-i18n-options=\'{")
                            .append("\"table\" : \"").append(prevMt.mtTable).append("\" ")
                            .append("}\' />");
                    }
                } else {
                    contentArray[idx].append("<br>");
                }

                contentArray[idx].append("</td>");

                idx += 1;

                for (int x = 0; x < matchRows / 4 - 1; x++)
                    contentArray[idx + x].append("<td class=\"none\"><br></td>");

                idx += matchRows / 4 - 1;
            }
        }

        List<String> contentList = new java.util.ArrayList<>();
        for (int i = 0; i < contentArray.length; i++)
            contentList.add(contentArray[i].toString());
        
        return contentList;
    }

    @Override
    public String generate(Report report, Database database) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
