/* Copyright (C) 2020 Christoph Theis */

package de.webgen.generator.report;

import de.webgen.database.Competition;
import de.webgen.database.Database;
import de.webgen.database.DoublePlayer;
import de.webgen.generator.Generator;
import static de.webgen.generator.report.Report.SEP;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;


public class DoublesReport extends Report {
    
    public DoublesReport(ReportType type) {
        super(type);
    }
    
    @Override
    public java.sql.Timestamp getTimestamp(Database database) throws SQLException {
        return database.getEntryTimestamp(2);
    }

    @Override
    public String generate(Database database) throws SQLException {
        Competition[] events = database.readEvents();

        java.util.ArrayList<DoublePlayer> playersList = new java.util.ArrayList<>();

        for (int i = 0; i < events.length; i++) {
            if ( !events[i].isDouble() )
                continue;

            playersList.addAll( database.getDoubles(events[i]) );
        }
        
        return generate(database, playersList);
    }
    
    protected String generate(Database database, List<DoublePlayer> doubles) throws SQLException {                
        Competition[] events = database.readEvents(
                this.type == ReportType.Doubles ? Competition.CP_DOUBLE : Competition.CP_MIXED
        );

        java.util.ArrayList<DoublePlayer> playersList = new java.util.ArrayList<>();
        playersList.addAll(doubles);
        
/*        
        for (DoublePlayer pl : doubles) {
            DoublePlayer bd = new DoublePlayer();
            bd.cp = pl.cp;
            bd.pl = pl.bd;
            bd.bd = pl.pl;
            playersList.add(bd);
        }
*/

        DoublePlayer[] players = playersList.toArray(new DoublePlayer[0]);
        java.util.Arrays.sort(players, new java.util.Comparator<DoublePlayer>() {

            @Override
            public int compare(DoublePlayer o1, DoublePlayer o2) {
                return (o1.pl.plNr % 10000) - (o2.pl.plNr % 10000);
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
        ;
        if (this.type == ReportType.Doubles)
            sb.append("<span data-i18n=\"report.name.doubles\">").append("</span>");
        else
            sb.append("<span data-i18n=\"report.name.mixed\"").append("</span>");
        
        sb
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
        sb.append("<div class=\"row pb-3\">").append(SEP);
        sb.append("<div id=\"report-doubles-content\" class=\"report-content col-12 px-0\">").append(SEP);
        
        // Pagination
        sb.append("<div id=\"pagination\"></div>").append(SEP);

        sb.append("<table id=\"report-doubles\" class=\"report doubles table border\">").append(SEP);
        sb.append("<thead>").append(SEP).append("<tr>");
        sb.append("<th class=\"plnr sort up\">").append("<span data-i18n=\"report.plnr\" />").append("</th>");
        sb.append("<th class=\"name\">").append("<span data-i18n=\"report.plname\" />").append("</th>");
        sb.append("<th class=\"assoc\">").append("<span data-i18n=\"report.assoc\" />").append("</th>");
        sb.append("<th class=\"event\">").append("<span data-i18n=\"report.event\" />").append("</th>");        
        sb.append("</tr>").append(SEP);
        sb.append("</thead>").append(SEP);

        for (int j = 0; j < players.length; j++) {
            int plNr = players[j].pl.plNr % 10000;
            String target = "" + plNr + "_" + players[j].cp.cpName;
            String href = "pl_" + plNr + ".html";
            
            sb.append("<tbody>").append(SEP);
            sb.append("<tr class=\"report\" data-toggle=\"collapse\" data-target=\"[data-webgen-player=&quot;").append(target).append("&quot;]\">");
            sb.append("<td class=\"plnr\">")
                    .append("<div>").append(players[j].pl.plNr % 10000).append("</div>")
                    .append("<div>").append(players[j].bd.plNr % 10000).append("</div>")
                    .append("</td>");            
            sb.append("<td class=\"name\">")
                    .append("<div>").append(players[j].pl.psLastName).append(", ").append(players[j].pl.psFirstName).append("</div>")
                    .append("<div>").append(players[j].bd.psLastName).append(", ").append(players[j].bd.psFirstName).append("</div>")                    
                    .append("</td>");
            sb.append("<td class=\"assoc\">")
                    .append("<div>").append(players[j].pl.naDesc).append("</div>")
                    .append("<div>").append(players[j].bd.naDesc).append("</div>")
                    .append("</td>");
            sb.append("<td class=\"event\">").append(players[j].cp.cpName).append("</td>");
            sb.append("</tr>").append(SEP);
            sb.append("<tr class=\"collapse\" data-webgen-player=\"").append(target).append("\" data-href=\"").append(href).append("\">");
            sb.append("<td colspan=\"7\" class=\"p-0\"></td>");
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
