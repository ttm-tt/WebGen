/* Copyright (C) 2020 Christoph Theis */

package de.webgen.generator.report;

import de.webgen.database.Competition;
import de.webgen.database.IDatabase;
import de.webgen.database.Team;
import de.webgen.database.TeamPlayer;
import de.webgen.generator.Generator;
import java.sql.SQLException;
import java.util.EnumSet;


public class TeamsReport extends Report{

    public TeamsReport(ReportType type) {
        super(type);
    }

    @Override
    public java.sql.Timestamp getTimestamp(IDatabase database) throws SQLException {
            return database.getEntryTimestamp(4);
    }

    @Override
    public String generate(IDatabase database) throws SQLException {
        Competition[] events = database.readEvents(Competition.CP_TEAM);

        StringBuilder sb = new StringBuilder();

        java.util.ArrayList<TeamPlayer> playerList = new java.util.ArrayList<>();
        java.util.ArrayList<Team> teamList = new java.util.ArrayList<>();

        for (Competition cp : events) {
            playerList.addAll(database.getTeamPlayers(cp));
            teamList.addAll(database.getTeams(cp));
        }

        Team[] teams = teamList.toArray(new Team[0]);
        java.util.Arrays.sort(teams, new java.util.Comparator<Team>() {

            @Override
            public int compare(Team o1, Team o2) {
                int ret = o1.tmDesc.compareTo(o2.tmDesc);
                if (ret == 0)
                    ret = o1.cp.cpName.compareTo(o2.cp.cpName);
                
                return ret;
            }
            
        });
                
        TeamPlayer[] players = playerList.toArray(new TeamPlayer[0]);

        java.util.Arrays.sort(players, new java.util.Comparator<TeamPlayer>() {

            @Override
            public int compare(TeamPlayer o1, TeamPlayer o2) {
                return (o1.pl.plNr % 10000) - (o2.pl.plNr % 10000);
            }
        });
        
        // Opening
        sb
            .append("<div class=\"col-12\" id=\"report\">").append(SEP)
        ;
        
        // Title
        sb
            .append("<div class=\"row\">")
            .append("<h4>")
            .append("<span data-i18n=\"report.name.teams\">").append("</span>")
            .append("</h4>")
            .append("</div>")
            .append(SEP)
        ;
        
        // Filter
        EnumSet<Generator.FilterTypes> what = EnumSet.of(
            Generator.FilterTypes.FilerEvent,
            Generator.FilterTypes.FilterAssocication,
            Generator.FilterTypes.FilterName,
            Generator.FilterTypes.FilterStartNumber
        );
        
        sb
            .append(Generator.writeFilter(what, events, database.readAssociations(), null))
        ;

        // Content
        // Navigation
        sb.append("<div class=\"row\" id=\"report-nav\">").append(SEP);        
        sb.append("<ul class=\"col-12 nav nav-tabs\">").append(SEP);
        sb.append("<li class=\"nav-item\">").append("<a class=\"nav-link active\" href=\"#\" id=\"report-teams-teams\">").append("<span data-i18n=\"nav.report.teams\" />").append("</a>").append("</li>").append(SEP);
        sb.append("<li class=\"nav-item\">").append("<a class=\"nav-link\" href=\"#\" id=\"report-teams-players\">").append("<span data-i18n=\"nav.report.players\" />").append("</a>").append("</li>").append(SEP);
        
        sb.append("</ul>").append(SEP);
        sb.append("</div>").append(SEP);
        
        // Teams
        sb.append("<div id=\"report-teams-teams-content\" class=\"report-content row\">").append(SEP);
        
        // Pagination
        sb.append("<div id=\"pagination\" class=\"pt-3\"></div>").append(SEP);
        
        sb.append("<table id=\"report-teams-teams\" class=\"report teams table border\">").append(SEP);
        sb.append("<thead class=\"bg-light\">").append(SEP);
        sb.append("<tr>");
        sb.append("<th class=\"team sort up\">").append("<span data-i18n=\"report.team\" />").append("</th>");
        sb.append("<th class=\"assoc\">").append("<span data-i18n=\"report.assoc\" />").append("</th>");
        sb.append("<th class=\"event\">").append("<span data-i18n=\"report.event\" />").append("</th>");
        sb.append("</tr>").append(SEP);
        sb.append("</thead>").append(SEP);
        
        for (Team tm : teams) {
            String target = "tm_" + tm.tmName.replaceAll("/", "_")  + "_" + tm.cp.cpName;
            String href = target + ".html";
            
            sb.append("<tbody>").append(SEP);
            sb.append("<tr class=\"report\" data-toggle=\"collapse\" data-target=\"[data-webgen-team=&quot;").append(target).append("&quot;]\">");
            sb.append("<td class=\"team\">").append(tm.tmDesc).append("</td>");
            sb.append("<td class=\"assoc\">").append("<div>").append(tm.naDesc).append("</div>").append("</td>");
            sb.append("<td class=\"event\">").append(tm.cp.cpName).append("</td>");
            sb.append("</tr>").append(SEP);
            sb.append("<tr class=\"collapse\" data-webgen-team=\"").append(target).append("\" data-href=\"").append(href).append("\">");
            sb.append("<td colspan=\"3\" class=\"p-0\"></td>");
            sb.append("</tr>").append(SEP);
            sb.append("</tbody>").append(SEP);
        }
        sb.append("</table>").append(SEP);
        sb.append("</div>").append(SEP);
                
        // Players
        sb.append("<div id=\"report-teams-players-content\" class=\"report-content row\">").append(SEP);
        
        // Pagination
        sb.append("<div id=\"pagination\" class=\"pt-3\"></div>").append(SEP);

        sb.append("<table id=\"report-teams-players\" class=\"report teams table border\">").append(SEP);
        sb.append("<thead class=\"bg-light\">").append(SEP).append("<tr>");
        
        sb.append("<th class=\"plnr sort up\">").append("<span data-i18n=\"report.plnr\" />").append("</th>");
        sb.append("<th class=\"name\">").append("<span data-i18n=\"report.plname\" />").append("</th>");
        sb.append("<th class=\"assoc\">").append("<span data-i18n=\"report.assoc\" />").append("</th>");
        sb.append("<th class=\"team\">").append("<span data-i18n=\"report.team\" />").append("</th>");
        sb.append("<th class=\"event\">").append("<span data-i18n=\"report.event\" />").append("</th>");
        sb.append("</tr>").append(SEP);
        sb.append("</thead>").append(SEP);
        
        for (int j = 0; j < players.length; j++) {
            int plNr = players[j].pl.plNr % 10000;
            String target = "pl_" + plNr + "_" + players[j].tm.cp.cpName;
            String href = "pl_" + plNr + ".html";
            
            sb.append("<tbody>").append(SEP);
            sb.append("<tr class=\"report\" data-toggle=\"collapse\" data-target=\"[data-webgen-player=&quot;").append(target).append("&quot;]\">");
            sb.append("<td class=\"plnr\">").append("<div>").append(players[j].pl.plNr % 10000).append("</div>").append("</td>");
            sb.append("<td class=\"name\">").append("<div>").append(players[j].pl.psLastName).append(", ").append(players[j].pl.psFirstName).append("</div>").append("</td>");
            sb.append("<td class=\"assoc\">").append("<div>").append(players[j].pl.naDesc).append("</div>").append("</td>");
            sb.append("<td class=\"team\">").append(players[j].tm.toString()).append("</td>");
            sb.append("<td class=\"event\">").append(players[j].tm.cp.cpName).append("</td>");
            sb.append("</tr>").append(SEP);
            sb.append("<tr class=\"collapse\" data-webgen-player=\"").append(target).append("\" data-href=\"").append(href).append("\">");
            sb.append("<td colspan=\"5\" class=\"p-0\"></td>");
            sb.append("</tr>").append(SEP);
            sb.append("</tbody>").append(SEP);
        }

        sb.append("</table>").append(SEP);
        sb.append("</div>").append(SEP);
        
        sb.append("</div>").append(SEP);

        sb.append("</div>").append(SEP);

        return sb.toString();
    }
}
