/* Copyright (C) 2020 Christoph Theis */

package de.webgen.generator.report;

import de.webgen.database.Database;
import de.webgen.database.Player;
import de.webgen.generator.Generator;
import static de.webgen.generator.report.Report.SEP;
import java.sql.SQLException;
import java.util.EnumSet;


public class PlayersReport extends Report {

    public PlayersReport(ReportType type) {
        super(type);
    }

    @Override
    public java.sql.Timestamp getTimestamp(Database database) throws SQLException {
        return database.getPlayerTimestamp();
    }

    @Override
    public String generate(Database database) throws SQLException {
        Player[] players = database.getPlayers().toArray(new Player[0]);
        java.util.Arrays.sort(players, new java.util.Comparator<Player>() {

            @Override
            public int compare(Player o1, Player o2) {
                return (o1.plNr % 10000) - (o2.plNr % 10000);
            }
        });

        StringBuilder sb = new StringBuilder();
        
        // Opening
        sb
            .append("<div class=\"col-12\" id=\"report\">").append(SEP)
        ;
        
        // Title
        sb
            .append("<div class=\"row\">")
            .append("<h4>")
            .append("<span data-i18n=\"report.name.players\">").append("</span>")
            .append("</h4>")
            .append("</div>")
            .append(SEP)
        ;
        
        // Filter
        EnumSet<Generator.FilterTypes> what = EnumSet.of(
            Generator.FilterTypes.FilterAssocication,
            Generator.FilterTypes.FilterName,
            Generator.FilterTypes.FilterStartNumber,
            Generator.FilterTypes.FilterExtID
        );
        
        sb
            .append(Generator.writeFilter(what, database.readEvents(), database.readAssociations(), null))
        ;

        // Content
        sb.append("<div class=\"row pb-3\">").append(SEP);
        sb.append("<div id=\"report-players-content\" class=\"report-content col-12 px-0\">").append(SEP);
        
        // Pagination
        sb.append("<div id=\"pagination\"></div>").append(SEP);

        sb.append("<table id=\"report-players\" class=\"report players table border\">").append(SEP);
        sb.append("<thead>").append(SEP).append("<tr>");
        sb.append("<th class=\"plnr sort up\">").append("<span data-i18n=\"report.plnr\" />").append("</th>");
        sb.append("<th class=\"name\">").append("<span data-i18n=\"report.plname\" />").append("</th>");
        sb.append("<th class=\"assoc\">").append("<span data-i18n=\"report.assoc\" />").append("</th>");
        sb.append("<th class=\"extid\">").append("</th>");
        sb.append("</tr>").append(SEP);
        sb.append("</thead>").append(SEP);

        for (int i = 0; i < players.length; i++) {
            int plNr = players[i].plNr % 10000;
            String target = "" + plNr;
            String href = "pl_" + plNr + ".html";
            
            sb.append("<tbody>").append(SEP);
            sb.append("<tr class=\"report\" data-toggle=\"collapse\" data-target=\"[data-webgen-player=&quot;").append(target).append("&quot;]\">");
            sb.append("<td class=\"plnr\">").append("<div>").append(players[i].plNr).append("</div>").append("</div></td>");
            sb.append("<td class=\"name\">").append("<div>").append(players[i].psLastName).append(", ").append(players[i].psFirstName).append("</div>").append("</td>");
            sb.append("<td class=\"assoc\">").append("<div>").append(players[i].naDesc).append("</div>").append("</td>");
            sb.append("<td class=\"extid\">").append("<div>").append(players[i].plExtID).append("</div>").append("</td>");
            sb.append("</tr>").append(SEP);
            sb.append("<tr class=\"collapse\" data-webgen-player=\"").append(target).append("\" data-href=\"").append(href).append("\">");
            sb.append("<td colspan=\"3\" class=\"p-0\"></td>");
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
