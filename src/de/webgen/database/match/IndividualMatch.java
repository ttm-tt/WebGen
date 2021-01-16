/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database.match;

import de.webgen.database.Database;
import de.webgen.database.Group;
import de.webgen.database.Player;
import java.sql.ResultSet;
import java.sql.SQLException;


public class IndividualMatch extends Match {
    public Player plA;
    public Player plB;
    public Player plX;
    public Player plY;

    public static String getSelectString() {
        return "SELECT mt.grID, mt.mtID, mtNr, mt.mtMS, mtRound, mtMatch, mtReverse, mtTable, mtDateTime, mtTimestamp, " +
               "       1 AS mtMatches, mtBestOf, mt.mtResA, mt.mtResX, " +
               "       mtWalkOverA, mtWalkOverX, " +
               "       mtInjuredA, mtInjuredX, " +
               "       mtDisqualifiedA, mtDisqualifiedX, " +
               "       mt.mtPrinted, mt.mtChecked, " +
               "       stA, stX, tmAtmID, tmXtmID, " +
               "       plAplID, plAplNr, plApsFirst, plApsLast, plAnaName, plAnaDesc, plAnaRegion, " +
               "       plBplID, plBplNr, plBpsFirst, plBpsLast, plBnaName, plBnaDesc, plBnaRegion, " +
               "       plXplID, plXplNr, plXpsFirst, plXpsLast, plXnaName, plXnaDesc, plXnaRegion, " +
               "       plYplID, plYplNr, plYpsFirst, plYpsLast, plYnaName, plYnaDesc, plYnaRegion, " +
               "       mtSet1.mtResA, mtSet1.mtResX, " +
               "       mtSet2.mtResA, mtSet2.mtResX, " +
               "       mtSet3.mtResA, mtSet3.mtResX, " +
               "       mtSet4.mtResA, mtSet4.mtResX, " +
               "       mtSet5.mtResA, mtSet5.mtResX, " +
               "       mtSet6.mtResA, mtSet6.mtResX, " +
               "       mtSet7.mtResA, mtSet7.mtResX, " +
               "       mtSet8.mtResA, mtSet8.mtResX, " +
               "       mtSet9.mtResA, mtSet9.mtResX  " +
               " FROM MtIndividualList mt " +
               "      INNER JOIN GrList gr ON mt.grID = gr.grID " +
               "      INNER JOIN CpList cp ON gr.cpId = cp.cpID " +
               "      LEFT OUTER JOIN MtSet mtSet1 ON mt.mtID = mtSet1.mtID AND mt.mtMS = mtSet1.mtMS AND mtSet1.mtSet = 1 " +
               "      LEFT OUTER JOIN MtSet mtSet2 ON mt.mtID = mtSet2.mtID AND mt.mtMS = mtSet2.mtMS AND mtSet2.mtSet = 2 " +
               "      LEFT OUTER JOIN MtSet mtSet3 ON mt.mtID = mtSet3.mtID AND mt.mtMS = mtSet3.mtMS AND mtSet3.mtSet = 3 " +
               "      LEFT OUTER JOIN MtSet mtSet4 ON mt.mtID = mtSet4.mtID AND mt.mtMS = mtSet4.mtMS AND mtSet4.mtSet = 4 " +
               "      LEFT OUTER JOIN MtSet mtSet5 ON mt.mtID = mtSet5.mtID AND mt.mtMS = mtSet5.mtMS AND mtSet5.mtSet = 5 " +
               "      LEFT OUTER JOIN MtSet mtSet6 ON mt.mtID = mtSet6.mtID AND mt.mtMS = mtSet6.mtMS AND mtSet6.mtSet = 6 " +
               "      LEFT OUTER JOIN MtSet mtSet7 ON mt.mtID = mtSet7.mtID AND mt.mtMS = mtSet7.mtMS AND mtSet7.mtSet = 7 " +
               "      LEFT OUTER JOIN MtSet mtSet8 ON mt.mtID = mtSet8.mtID AND mt.mtMS = mtSet8.mtMS AND mtSet8.mtSet = 8 " +
               "      LEFT OUTER JOIN MtSet mtSet9 ON mt.mtID = mtSet9.mtID AND mt.mtMS = mtSet9.mtMS AND mtSet9.mtSet = 9 ";
    }

    public IndividualMatch() {

    }

    public IndividualMatch(Group gr, ResultSet rs) throws SQLException {
        this.gr = gr;

        read(rs);
    }

    public void read(ResultSet rs) throws SQLException {    
        plA = new Player();
        plB = new Player();
        plX = new Player();
        plY = new Player();
        
        int idx = 0;
        grID = rs.getInt(++idx);
        mtID = rs.getInt(++idx);
        mtNr = rs.getInt(++idx);
        mtMS = rs.getInt(++idx);
        mtRound = rs.getInt(++idx);
        mtMatch = rs.getInt(++idx);
        mtReverse = rs.getInt(++idx) != 0;
        mtTable = rs.getInt(++idx);
        mtDateTime = rs.getTimestamp(++idx);
        mtTimestamp = rs.getTimestamp(++idx);
        mtMatches = rs.getInt(++idx);
        mtBestOf = rs.getInt(++idx);
        mtResA = rs.getInt(++idx);
        mtResX = rs.getInt(++idx);

        mtWalkOverA = rs.getBoolean(++idx);
        mtWalkOverX = rs.getBoolean(++idx);
        mtInjuredA  = rs.getBoolean(++idx);
        mtInjuredX  = rs.getBoolean(++idx);
        mtDisqualifiedA = rs.getBoolean(++idx);
        mtDisqualifiedX = rs.getBoolean(++idx);
        
        mtPrinted = rs.getBoolean(++idx);
        mtChecked = rs.getBoolean(++idx);

        stA = rs.getInt(++idx);
        stX = rs.getInt(++idx);

        tmAtmID = rs.getInt(++idx);
        tmXtmID = rs.getInt(++idx);

        plA.plID = rs.getInt(++idx);
        plA.plNr = rs.getInt(++idx);
        plA.psFirstName = Database.getString(rs, ++idx);
        plA.psLastName = Database.getString(rs, ++idx);
        plA.naName = Database.getString(rs, ++idx);
        plA.naDesc = Database.getString(rs, ++idx);
        plA.naRegion = Database.getString(rs, ++idx);

        plB.plID = rs.getInt(++idx);
        plB.plNr = rs.getInt(++idx);
        plB.psFirstName = Database.getString(rs, ++idx);
        plB.psLastName = Database.getString(rs, ++idx);
        plB.naName = Database.getString(rs, ++idx);
        plB.naDesc = Database.getString(rs, ++idx);
        plB.naRegion = Database.getString(rs, ++idx);

        plX.plID = rs.getInt(++idx);
        plX.plNr = rs.getInt(++idx);
        plX.psFirstName = Database.getString(rs, ++idx);
        plX.psLastName = Database.getString(rs, ++idx);
        plX.naName = Database.getString(rs, ++idx);
        plX.naDesc = Database.getString(rs, ++idx);
        plX.naRegion = Database.getString(rs, ++idx);

        plY.plID = rs.getInt(++idx);
        plY.plNr = rs.getInt(++idx);
        plY.psFirstName = Database.getString(rs, ++idx);
        plY.psLastName = Database.getString(rs, ++idx);
        plY.naName = Database.getString(rs, ++idx);
        plY.naDesc = Database.getString(rs, ++idx);
        plY.naRegion = Database.getString(rs, ++idx);

        result = new int[mtResA + mtResX][2];
        for (int i = 0; i < mtResA + mtResX; i++) {
            result[i][0] = rs.getInt(++idx);
            result[i][1] = rs.getInt(++idx);
        }
    }

    @Override
    public boolean isFinished() {
        if (!mtChecked)
            return false;
        
        return mtResA > mtBestOf / 2 || mtResX > mtBestOf / 2;
    }
    
    @Override
    public Side getWinnerAX() {
        if (gr == null)
            return Side.None;
        else if (!isFinished())
            return Side.None;
        else if (mtResA > mtBestOf / 2)
            return Side.A;
       else if (mtResX > mtBestOf / 2)
                return Side.X;
        else
           return Side.None;
    }

    @Override
    public String getResultShortForm(boolean forWinner) {
        if (mtWalkOverA || mtWalkOverX) {
            if (forWinner)
                return "<strong><span data-i18n=\"result.format.short.wo\"></strong>";
            else
                return "<strong>" + mtResA + "&nbsp;:&nbsp;" + mtResX + "</strong>&nbsp;&nbsp;(<span data-i18n=\"result.format.short.wo\"></span>)";
        }
        
        if (mtInjuredA || mtInjuredX) {
            if (forWinner)
                return "<strong><span data-i18n=\"result.format.short.injured\"></strong>";
            else
                return "<strong>" + mtResA + "&nbsp;:&nbsp;" + mtResX + "</strong>&nbsp;&nbsp;(<span data-i18n=\"result.format.short.injured\"></span>)";
        }

        if (mtDisqualifiedA || mtDisqualifiedX) {
            if (forWinner)
                return "<strong><span data-i18n=\"result.format.short.disqualified\"></strong>";
            else
                return "<strong>" + mtResA + "&nbsp;:&nbsp;" + mtResX + "</strong>&nbsp;&nbsp;(<span data-i18n=\"result.format.short.disqualified\"></span>)";
        }

        if (mtResA < mtBestOf / 2 && mtResX < mtBestOf / 2)
            return "<br>";

        StringBuilder buf = new StringBuilder();

        if (!forWinner || mtResA >= mtResX)
            buf.append("<strong>").append(mtResA).append("&nbsp;:&nbsp;").append(mtResX).append("</strong>");
        else if (mtResX > mtResA)
            buf.append("<strong>").append(mtResX).append("&nbsp;:&nbsp;").append(mtResA).append("</strong>");

        buf.append("&nbsp;&nbsp;(");

        int resA, resX;
        for (int i = 0; i < result.length; i++) {
            resA = result[i][0];
            resX = result[i][1];

            if (resA != 0 || resX != 0) {
                if (i > 0)
                    buf.append("&nbsp;");

                if (!forWinner || mtResA > mtResX) {
                    if (resA > resX) {
                        buf.append(resX);
                    }
                    else if (resA == 0) {
                        buf.append("-0");
                    }
                    else {
                        buf.append(-resA);
                    }
                }
                else if (mtResX > mtResA) {
                    if (resA > resX) {
                        buf.append(-resX);
                    }
                    else if (resX == 0) {
                        buf.append("-0");
                    }
                    else {
                        buf.append(resA);
                    }
                }
            }
        }

        buf.append(")");

        String resultString = buf.toString();

        if (resultString.length() == 0) {
            return "<br>";
        }
        return resultString;
    }

    @Override
    public String aToString(boolean withAssoc, boolean withNr, boolean withFlag) {
        if (plA == null || plA.plNr == 0)
            return "<br>";

        if (plB.plNr == 0)
            return plA.toString(withAssoc, withNr, false);

        return plA.toString(withAssoc, withNr, false) + plB.toString(withAssoc, withNr, false);
    }

    @Override
    public String aNrToString() {
        if (plA == null || plA.plNr == 0)
            return "<br>";

        if (plB.plNr == 0)
            return "" + plA.plNr;

        return plA.plNr + "<br>" + plB.plNr;
    }

    @Override
    public String aAssocToString() {
        if (plA == null || plA.plNr == 0)
            return "<br>";

        if (plB.plNr == 0)
            return plA.naName;

        return plA.naName + "<br>" + plB.naName;
    }

    @Override
    public String xToString(boolean withAssoc, boolean withNr, boolean withFlag) {
        if (plX == null || plX.plNr == 0)
            return "<br>";

        if (plY.plNr == 0)
            return plX.toString(withAssoc, withNr, false);

        return plX.toString(withAssoc, withNr, false) + plY.toString(withAssoc, withNr, false);
    }

    @Override
    public String xNrToString() {
        if (plX == null || plX.plNr == 0)
            return "<br>";

        if (plY.plNr == 0)
            return "" + plX.plNr;

        return plX.plNr + "<br>" + plY.plNr;
    }

    @Override
    public String xAssocToString() {
        if (plX == null || plX.plNr == 0)
            return "<br>";

        if (plY.plNr == 0)
            return plX.naName;

        return plX.naName + "<br>" + plY.naName;
    }

    @Override
    public String winnerToString(boolean withAssoc) {
        if (mtResA > mtBestOf / 2)
            return aToString(true, true, false);
        else if (mtResX > mtBestOf / 2)
            return xToString(true, true, false);
        else
            return "<br>";
    }
    
    @Override
    public Player getPlA() {
        return plA;
    }
    
    @Override
    public Player getPlB() {
        return plB;
    }
    
    @Override
    public Player getPlX() {
        return plX;
    }
    
    @Override
    public Player getPlY() {
        return plY;
    }
}
