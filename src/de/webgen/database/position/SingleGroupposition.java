/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database.position;

import de.webgen.database.Database;
import de.webgen.database.Player;
import java.sql.ResultSet;
import java.sql.SQLException;


public class SingleGroupposition extends Groupposition {
    public Player pl;


    public static String getSelectString() {
        return
            "SELECT st.tmID, tb.stID, st.stNr, tb.stPos, " +
            "       tb.mtMatchPoints, tb.mtMatchCount, " +
            "       tb.mtPointsA, tb.mtPointsX, tb.mtMatchesA, tb.mtMatchesX, " +
            "       tb.mtSetsA, tb.mtSetsX, tb.mtBallsA, tb.mtBallsX, " +
            "       st.plNr, st.psLast, st.psFirst, st.naName, st.naDesc, st.naRegion, " +
            "       grQual.grDesc, xx.grPos " +
            "  FROM TbSortFunc(?) tb INNER JOIN StSingleList st ON tb.stID = st.stID " +
            "       LEFT OUTER JOIN (XxList xx LEFT OUTER JOIN GrList grQual ON xx.grID = grQual.grID) ON st.stID = xx.stID "
            ;
    }

    public SingleGroupposition() {

    }

    public SingleGroupposition(ResultSet rs) throws SQLException {
        byte[] b;
        
        pl = new Player();
        
        int idx = 0;
        tmID = rs.getInt(++idx);
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

        pl.plNr = rs.getInt(++idx);
        pl.psLastName = Database.getString(rs, ++idx);
        pl.psFirstName = Database.getString(rs, ++idx);
        pl.naName = Database.getString(rs, ++idx);
        pl.naDesc = Database.getString(rs, ++idx);
        pl.naRegion = Database.getString(rs, ++idx);
        
        xx.grDesc = Database.getString(rs, ++idx);
        xx.grPos = rs.getInt(++idx);        
    }

    @Override
    public String getEntry() {
        if (pl.plNr != 0)
            return pl.toString();
        else if (xx.grDesc != null && !xx.grDesc.isEmpty())
            return xx.toString();
        else
            return "";
    }
}



