/* Copyright (C) 2020 Christoph Theis */

package de.webgen.generator.report;

import de.webgen.database.Competition;
import de.webgen.database.Database;
import de.webgen.database.DoublePlayer;
import java.sql.SQLException;


public class MixedReport extends DoublesReport {

    public MixedReport(ReportType type) {
        super(type);
    }

    @Override
    public java.sql.Timestamp getTimestamp(Database database) throws SQLException {
        return database.getEntryTimestamp(3);
    }

    @Override
    public String generate(Database database) throws SQLException {
        Competition[] events = database.readEvents();

        java.util.ArrayList<DoublePlayer> playersList = new java.util.ArrayList<>();

        for (int i = 0; i < events.length; i++) {
            if ( !events[i].isMixed() )
                continue;

            playersList.addAll( database.getDoubles(events[i]) );
        }

        return generate(database, playersList);
    }
}
