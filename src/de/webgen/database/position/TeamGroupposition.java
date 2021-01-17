/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database.position;

import de.webgen.database.Competition;
import de.webgen.database.Database;
import de.webgen.database.Team;
import java.sql.ResultSet;
import java.sql.SQLException;


public class TeamGroupposition extends Groupposition {
    public Team tm;

    public static String getSelectString() {
        return
            "SELECT st.tmID, tb.stID, st.stNr, tb.stPos, " +
            "       tb.mtMatchPoints, tb.mtMatchCount, " +
            "       tb.mtPointsA, tb.mtPointsX, tb.mtMatchesA, tb.mtMatchesX, " +
            "       tb.mtSetsA, tb.mtSetsX, tb.mtBallsA, tb.mtBallsX, " +
            "       st.tmName, st.tmDesc, st.naName, st.naDesc, st.naRegion, " +
            "       grQual.grDesc, xx.grPos " +
            "  FROM TbSortFunc(?) tb INNER JOIN StTeamList st ON tb.stID = st.stID " +
            "       LEFT OUTER JOIN (XxList xx LEFT OUTER JOIN GrList grQual ON xx.grID = grQual.grID) ON st.stID = xx.stID "
            ;
    }

    public TeamGroupposition() {

    }


    public TeamGroupposition(Competition cp, ResultSet rs) throws SQLException {
        tm = new Team(cp);
        
        int idx = 0;
        tmID = rs.getInt(++idx);
        tm.tmID = tmID;
        
        stID = rs.getInt(++idx);
        stNr = rs.getInt(++idx);
        stPos = rs.getInt(++idx);

        mtMatchPoints = rs.getInt(++idx);
        mtMatchCount = rs.getInt(++idx);
        mtPointsA = rs.getInt(++idx);
        mtPointsX = rs.getInt(++idx);
        mtMatchesA = rs.getInt(++idx);
        mtMatchesX = rs.getInt(++idx);
        mtSetsA = rs.getInt(++idx);
        mtSetsX = rs.getInt(++idx);
        mtBallsA = rs.getInt(++idx);
        mtBallsX = rs.getInt(++idx);

        tm.tmName = Database.getString(rs, ++idx);
        tm.tmDesc = Database.getString(rs, ++idx);
        tm.naName = Database.getString(rs, ++idx);
        tm.naDesc = Database.getString(rs, ++idx);
        tm.naRegion = Database.getString(rs, ++idx);
        
        xx.grDesc = Database.getString(rs, ++idx);
        xx.grPos = rs.getInt(++idx);        
    }

    @Override
    public String getEntry() {
        if (tm.tmName != null && !tm.tmName.isEmpty())
            return tm.toString();
        else if (xx.grDesc != null && !xx.grDesc.isEmpty())
            return xx.toString();
        else
            return "";
    }
}
