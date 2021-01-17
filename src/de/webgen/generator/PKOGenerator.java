/* Copyright (C) 2020 Christoph Theis */

package de.webgen.generator;


import de.webgen.database.IDatabase;
import de.webgen.database.Group;
import de.webgen.database.match.Match;
import static de.webgen.generator.Generator.SEP;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class PKOGenerator extends KOGenerator {

    public PKOGenerator() {
    }


    @Override
    public String generate(List<List<Match>> matchList, IDatabase database) throws SQLException {
        Group gr = matchList.get(0).get(0).gr;
        
        if (gr == null)
            return "";
        
        int groupSize = gr.grSize;

        List<List<Match>>[] koGames = new ArrayList[groupSize/2];
        for (int i = 0; i < koGames.length; i++) {
            koGames[i] = new ArrayList<>();
        }
        
        if (gr.grNofRounds == 0) {
            // Anzahl der Spiele in erster Runde je Raster
            int matchesInRound = groupSize / 2;

            for (int j = 0; j < matchList.size(); j++, matchesInRound /= 2) {
                List<Match> allGamesInRound = matchList.get(j);

                // Anzahl der Raster in einer Runde
                int rasterCount = (int)Math.pow(2, j);
                for (int k = 0; k < rasterCount; k++) {
                    ArrayList<Match> roundGames = new ArrayList<>();
                    int start = k * matchesInRound;
                    for (int i = start; i < matchesInRound + start; i++) {
                        roundGames.add(allGamesInRound.get(i));
                    }

                    koGames[k * matchesInRound].add(roundGames);
                }
            }
        } else {
            // Anzahl der Spiele in erster Runde je Raster
            int matchesInRound = (1 << gr.grNofRounds) / 2;
            
            for (int j = 0; j < gr.grNofRounds; j++, matchesInRound /= 2) {
                List<Match> allGamesInRound = matchList.get(j);
                
                // Anzahl der Raster in einer Runde                
                int rasterCount = (gr.grNofMatches > 0 ? gr.grNofMatches : gr.grSize / 2) / matchesInRound;
                for (int k = 0; k < rasterCount; k++) {
                    ArrayList<Match> roundGames = new ArrayList<>();
                    int start = k * matchesInRound;
                    for (int i = start; i < matchesInRound + start; i++) {
                        roundGames.add(allGamesInRound.get(i));
                    }
                    
                    koGames[k * matchesInRound].add(roundGames);
                }
            }
        }

        StringBuilder buffer = new StringBuilder();
        
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

        // Header
        buffer.append("<div id=\"ko-nav\" class=\"row col-12 pb-3\">").append(SEP);
        buffer.append("<ul class=\"nav nav-tabs\">").append(SEP);  
        if (gr.grNofRounds != 1 && matchList.size() > 1)
            buffer.append("<li class=\"nav-item\"><a class=\"nav-link\" href=\"#\" id=\"one-round\">").append("<span data-i18n=\"nav.ko.one-round\"></span>").append("</a>").append("</li>").append(SEP);
        buffer.append("<li class=\"nav-item\"><a class=\"nav-link\" href=\"#\" id=\"all-rounds\">").append("<span data-i18n=\"nav.ko.all-rounds\"></span>").append("</a>").append("</li>").append(SEP);
        
        if (gr.cp.isTeam())
            buffer.append("<li class=\"nav-item\"><a class=\"nav-link\" href=\"#\" id=\"teammatches\">").append("<span data-i18n=\"nav.ko.teammatches\"></span>").append("</a>").append("</li>").append(SEP);
        
        buffer.append("</ul>").append(SEP);
        buffer.append("</div>").append(SEP);
        
        if (matchList.isEmpty() || matchList.get(0).isEmpty()) {
            buffer.append("<div id=\"all-rounds-content\" class=\"ko-content\" style=\"display:none;\"></div>").append(SEP);
            
            return buffer.toString();
        }
        
        int nofRounds = koGames[0].size();
        StringBuilder content = null;

        // One round
        if (gr.grNofRounds != 1 && matchList.size() > 1) {
            content = new StringBuilder();
            
            int maxRound = koGames[0].size();
            
            for (int startRd = 0; startRd < maxRound; startRd++) {
                if (!matchList.get(startRd).isEmpty()) {
                    Match mt = matchList.get(startRd).get(0);

                    if (mt.gr.grNofRounds > 0 && mt.mtRound > mt.gr.grNofRounds)
                        break;
                }
                
                content.append("<div class=\"row item group ko round").append(startRd == 0 ? " active" : "").append("\">").append(SEP);
                
                content.append("<table class=\"ko draw\">").append(SEP);
                
                int startPos = 0;
                int endPos = gr.grWinner - 1;
                    
                for (int i = 0; i < koGames.length; i++) {
                    if (startRd < nofRounds - koGames[i].size())
                        continue;
                    
                    startPos = endPos + 1;
                    endPos += koGames[0].get(startRd).size() * 2;
                    
                    // Das zweite Raster ist das Spiel um Platz 3
                    if (i == 1 && gr.grNoThirdPlace)
                        continue;

                    if (i > 1 && gr.grOnlyThirdPlace)
                        break;
            
                    content.append("<tbody>");
                    
                    content
                        .append("<tr>")
                        .append("<th class=\"none\">")
                        .append("<span ")
                    ;
                    
                    if (gr.grNofRounds > 0 || gr.grNofMatches > 0 || gr.grWinner != 1)
                        content  
                                .append("data-i18n=\"pko.format.short.pos\" ")
                                .append("data-i18n-options=\'{\"from\" : \"").append(startPos).append("\", \"to\": \"").append(endPos).append("\"}\'")
                        ;
                    else if (startPos == 1 && endPos == 2)
                        content.append("<span data-i18n=\"ko.format.long.final\"></span>");
                    else if (startPos == 1 && endPos == 4)
                        content.append("<span data-i18n=\"ko.format.long.semifinal\"></span>");
                    else
                        content
                                .append("data-i18n=\"pko.format.short.pos\" ")
                                .append("data-i18n-options=\'{\"from\" : \"").append(startPos).append("\", \"to\": \"").append(endPos).append("\"}\'")
                        ;
                    
                    content
                        .append("></span>")
                        .append("</th>")
                        .append("</tr>")
                        .append(SEP);
                    
                
                    int round = startRd - (nofRounds - koGames[i].size());
                    
                    List<String> contentList = generateRaster(koGames[i], round, round + 1);
                    for (String tr : contentList) {
                        content.append("<tr>").append(tr).append("</tr>").append(SEP);
                    }
                    
                    content.append("</tbody>");

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
        
        // All rounds
        content = new StringBuilder();
        
        content.append("<div class=\"group ko\">").append(SEP);
        
        content.append("<table class=\"ko draw\">").append(SEP);

        for (int i = 0, actualPosition = 0; i < koGames.length; i++, actualPosition += 2) {
            // Das zweite Raster ist das Spiel um Platz 3
            if (i == 1 && gr.grNoThirdPlace)
                continue;
            
            if (i > 1 && gr.grOnlyThirdPlace)
                break;
            
            if (i > 0)
                content.append("  <tr><td class=\"none\"><br><br><br></td></tr>").append(SEP);

            // Ueberschriften
            int roundCount = koGames[i].size();
            
            content.append("<tr>");
            
            // Einrueckung der Ueberschriften
            if (nofRounds > roundCount)
                content.append("<th colspan=\"").append(nofRounds - roundCount).append("\"></th>");

            for (int j = 0; j < roundCount; j++) {
                int matchCount = ((ArrayList)(koGames[i].get(j))).size();
                int startPos = actualPosition + gr.grWinner;
                int endPos = startPos + (matchCount * 2) - 1;
                content
                    .append("<th>")
                    .append("<span ")
                ;
                
                if (gr.grNofRounds > 0 || gr.grNofMatches > 0 || gr.grWinner != 1)
                    content  
                            .append("data-i18n=\"pko.format.short.pos\" ")
                            .append("data-i18n-options=\'{\"from\" : \"").append(startPos).append("\", \"to\": \"").append(endPos).append("\"}\'")
                    ;
                else if (startPos == 1 && endPos == 2)
                    content.append("<span data-i18n=\"ko.format.long.final\"></span>");
                else if (startPos == 1 && endPos == 4)
                    content.append("<span data-i18n=\"ko.format.long.semifinal\"></span>");
                else
                    content
                            .append("data-i18n=\"pko.format.short.pos\" ")
                            .append("data-i18n-options=\'{\"from\" : \"").append(startPos).append("\", \"to\": \"").append(endPos).append("\"}\'")
                    ;
                
                content    
                    .append("></span>")
                    .append("</th>");
            }
            
            content.append("</tr>").append(SEP);

            content.append("<tr><td class=\"none\"><br></td></tr>").append(SEP);
            
            List<String> contentList = generateRaster(koGames[i]);
            
            for (int k = 0; k < contentList.size(); k++) {
                content.append("<tr>");
                for (int j = 0; j < nofRounds - roundCount; j++)
                    content.append("<td class=\"none\"></td>");
                content.append(contentList.get(k));
                content.append("</tr>").append(SEP);                
            }
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
        
        if (gr.cp.isTeam()) {
            content = new StringBuilder();
            
            int maxRound = koGames[0].size();
            
            for (int startRd = 0; startRd < maxRound; startRd++) {
                if (!matchList.get(startRd).isEmpty()) {
                    Match mt = matchList.get(startRd).get(0);

                    if (mt.gr.grNofRounds > 0 && mt.mtRound > mt.gr.grNofRounds)
                        break;
                }
                
                content.append("<div class=\"row item group ko round").append(startRd == 0 ? " active" : "").append("\">").append(SEP);
                
                int startPos = 0;
                int endPos = gr.grWinner - 1;
                
                for (int i = 0; i < koGames.length; i++) {
                    if (startRd < nofRounds - koGames[i].size())
                        continue;
                    
                    startPos = endPos + 1;
                    endPos += koGames[0].get(startRd).size() * 2;
                    
                    int round = startRd - (nofRounds - koGames[i].size());
                                        
                    content
                        .append("<h6>")
                        .append("<strong>")
                        .append("<span ")
                    ;
                    
                    if (gr.grNofRounds > 0 || gr.grNofMatches > 0 || gr.grWinner != 1)
                        content  
                                .append("data-i18n=\"pko.format.short.pos\" ")
                                .append("data-i18n-options=\'{\"from\" : \"").append(startPos).append("\", \"to\": \"").append(endPos).append("\"}\'")
                        ;
                    else if (startPos == 1 && endPos == 2)
                        content.append("<span data-i18n=\"ko.format.long.final\"></span>");
                    else if (startPos == 1 && endPos == 4)
                        content.append("<span data-i18n=\"ko.format.long.semifinal\"></span>");
                    else
                        content
                                .append("data-i18n=\"pko.format.short.pos\" ")
                                .append("data-i18n-options=\'{\"from\" : \"").append(startPos).append("\", \"to\": \"").append(endPos).append("\"}\'")
                        ;

                    content
                        .append("data-i18n=\"pko.format.short.pos\" ")
                        .append("data-i18n-options=\'{\"from\" : \"").append(startPos).append("\", \"to\": \"").append(endPos).append("\"}\'")
                    ;
                    
                    content
                        .append("></span>")
                        .append("</strong>")
                        .append("</h6>")
                        .append(SEP);                    
                
                    content.append("<table class=\"ko matches\">").append(SEP);
                    content.append(generateMatchList(koGames[i].get(round), database));            
                    content.append("</table>").append(SEP);
                }
                
                content.append("</div>").append(SEP);
            }
            
            buffer.append("<div id=\"teammatches-content\" class=\"ko-content col-12\" style=\"display:none;\">").append(SEP);
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
            buffer.append("</div>").append(SEP);
        }
        return buffer.toString();
    }
}
