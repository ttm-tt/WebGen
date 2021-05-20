/* Copyright (C) 2020 Christoph Theis */

package de.webgen.generator.report;

import de.webgen.database.Competition;
import de.webgen.database.IDatabase;
import de.webgen.database.SinglePlayer;
import de.webgen.generator.Generator;
import static de.webgen.generator.report.Report.SEP;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.EnumSet;


public class PartnerWantedReport extends Report {
    public PartnerWantedReport(ReportType type) {
        super(type);
    }

    @Override
    public java.sql.Timestamp getTimestamp(IDatabase database) throws SQLException {
        java.sql.Timestamp tsDouble = database.getEntryTimestamp(2);
        java.sql.Timestamp tsMixed  = database.getEntryTimestamp(3);
        
        if (tsMixed == null)
            return tsDouble;
        else if (tsDouble == null)
            return tsMixed;
        else
            return tsDouble.after(tsMixed) ? tsMixed : tsDouble;
    }

    @Override
    public String generate(IDatabase database) throws SQLException {

        Competition[] events = database.readEvents();

        StringBuilder sb = new StringBuilder();
        
        // Opening
        sb
            .append("<div class=\"col-12\" id=\"report\">").append(SEP)
        ;
        
        // Title
        sb
            .append("<div class=\"row\">")
            .append("<h4>")
            .append("<span data-i18n=\"report.name.partnerwanted\">").append("</span>")
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
            .append(Generator.writeFilter(what, 
                    Arrays.stream(events).filter(cp -> !cp.isSingle()).toArray(Competition[]::new), 
                    database.readAssociations(), null))
        ;
        
        // Content
        sb.append("<div class=\"row pb-3\">").append(SEP);
        sb.append("<div id=\"report-partnerwanted-content\" class=\"report-content col-12 px-0\">").append(SEP);
        
        // Pagination
        sb.append("<div id=\"pagination\"></div>").append(SEP);

        java.util.ArrayList<SinglePlayer> playerList = new java.util.ArrayList<>();

        for (int i = 0; i < events.length; i++) {
            if ( !events[i].isDouble() && !events[i].isMixed() && !events[i].isTeam() )
                continue;

            playerList.addAll(database.getPartnerWanted(events[i]));
        }

        SinglePlayer[] players = playerList.toArray(new SinglePlayer[0]);

        java.util.Arrays.sort(players, new java.util.Comparator<SinglePlayer>() {

            @Override
            public int compare(SinglePlayer o1, SinglePlayer o2) {
                return (o1.pl.plNr % 10000) - (o2.pl.plNr % 10000);
            }
        });

        sb.append("<table id=\"report-partner-wanted\" class=\"report partnerwanted table border\">").append(SEP);
        sb.append("<thead").append(SEP).append("><tr>");
        sb.append("<th class=\"plnr sort up\">").append("<span data-i18n=\"report.plnr\" />").append("</th>");
        sb.append("<th class=\"name\">").append("<span data-i18n=\"report.plname\" />").append("</th>");
        sb.append("<th class=\"sex\">").append("<span data-i18n=\"report.pssex\" />").append("</th>");
        sb.append("<th class=\"assoc\">").append("<span data-i18n=\"report.assoc\" />").append("</th>");
        sb.append("<th class=\"event\">").append("<span data-i18n=\"report.event\" />").append("</th>");
        sb.append("</tr>").append(SEP);
        sb.append("</thead>").append(SEP);

        sb.append("<tbody>").append(SEP);
        
        String[] sex = {"", "M", "F"};

        for (int j = 0; j < players.length; j++) {
            sb.append("<tbody>").append(SEP);
            sb.append("<tr class=\"report\">");
            sb.append("<td class=\"plnr\">").append("<div>").append(players[j].pl.plNr % 10000).append("</div>").append("</td>");
            sb.append("<td class=\"name\">").append("<div>").append(players[j].pl.psLastName).append(", ").append(players[j].pl.psFirstName).append("</div>").append("</td>");
            sb.append("<td class=\"sex\">").append(sex[players[j].pl.psSex]).append("</td>");
            sb.append("<td class=\"assoc\">").append("<div>").append(players[j].pl.naDesc).append("</div>").append("</td>");
            sb.append("<td class=\"event\">").append(players[j].cp.cpName).append("</td>");
            sb.append("</tr>").append(SEP);
            sb.append("</tbody>").append(SEP);
        }

        sb.append("</tbody>").append(SEP);
        sb.append("</table>").append(SEP);
        
        sb.append("</div>").append(SEP);
        
        sb.append("</div>").append(SEP);

        sb.append("</div>").append(SEP);

        return sb.toString();
    }
}
