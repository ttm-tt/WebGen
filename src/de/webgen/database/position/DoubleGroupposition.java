/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database.position;

import de.webgen.database.Database;
import de.webgen.database.Player;
import java.sql.ResultSet;
import java.sql.SQLException;


public class DoubleGroupposition extends Groupposition {
    public int plAplID;
    public int plBplID;
    
    public Player plA;
    public Player plB;

    public static String getSelectString() {
        return
            "SELECT st.tmID, tb.stID, st.stNr, tb.stPos, " +
            "       tb.mtMatchPoints, tb.mtMatchCount, " +
            "       tb.mtPointsA, tb.mtPointsX, tb.mtMatchesA, tb.mtMatchesX, " +
            "       tb.mtSetsA, tb.mtSetsX, tb.mtBallsA, tb.mtBallsX, " +
            "       st.plplID, st.plplNr, st.plpsLast, st.plpsFirst, st.plnaName, st.plnaDesc, st.plnaRegion, " +
            "       st.bdplID, st.bdplNr, st.bdpsLast, st.bdpsFirst, st.bdnaName, st.bdnaDesc, st.bdnaRegion, " +
            "       grQual.grDesc, xx.grPos " +
            "  FROM TbSortFunc(?) tb INNER JOIN StDoubleList st ON tb.stID = st.stID " +
            "       LEFT OUTER JOIN (XxList xx LEFT OUTER JOIN GrList grQual ON xx.grID = grQual.grID) ON st.stID = xx.stID "
            ;
    }

    public DoubleGroupposition() {

    }

    public DoubleGroupposition(ResultSet rs) throws SQLException {
        plA = new Player();
        plB = new Player();
        
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

        plAplID = plA.plID = rs.getInt(++idx);
        plA.plNr = rs.getInt(++idx);
        plA.psLastName = Database.getString(rs, ++idx);
        plA.psFirstName = Database.getString(rs, ++idx);
        plA.naName = Database.getString(rs, ++idx);
        plA.naDesc = Database.getString(rs, ++idx);
        plA.naRegion = Database.getString(rs, ++idx);

        plBplID = plB.plID = rs.getInt(++idx);
        plB.plNr = rs.getInt(++idx);
        plB.psLastName = Database.getString(rs, ++idx);
        plB.psFirstName = Database.getString(rs, ++idx);
        plB.naName = Database.getString(rs, ++idx);
        plB.naDesc = Database.getString(rs, ++idx);
        plB.naRegion = Database.getString(rs, ++idx);
        
        xx.grDesc = Database.getString(rs, ++idx);
        xx.grPos = rs.getInt(++idx);        
    }

    @Override
    public String getEntry() {
        if (plA.plNr != 0)
            return plA.toString() + "<br>" + plB.toString();
        else if (xx.grDesc != null && !xx.grDesc.isEmpty())
            return xx.toString();
        else
            return "<br>";
    }
}
