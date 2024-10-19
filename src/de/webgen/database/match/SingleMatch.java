/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database.match;

import de.webgen.database.Database;
import de.webgen.database.Group;
import de.webgen.database.Player;
import de.webgen.database.Xx;
import java.sql.ResultSet;
import java.sql.SQLException;


public class SingleMatch extends Match {

    public  Player  plA;
    public  Player  plX;
    
    public int plAplID;
    public int plXplID;

    public static String getSelectString() {
        return "SELECT mt.grID, mt.mtID, mtNr, 0 AS mtMS, mtRound, mtMatch, 0 as mtReverse, mtTable, mtDateTime, mtTimestamp, " +
               "       1 AS mtMatches, mtBestOf, mt.mtResA, mt.mtResX, " +
               "       mtWalkOverA, mtWalkOverX, " +
               "       mtInjuredA, mtInjuredX, " +
               "       mtDisqualifiedA, mtDisqualifiedX, " +
               "       mtPrinted, mtChecked, " +
               "       stA, stX, tmAtmID, tmXtmID, " +
               "       plAplID, plAplNr, plApsFirst, plApsLast, plAnaName, plAnaDesc, plAnaRegion, " +
               "       plXplID, plXplNr, plXpsFirst, plXpsLast, plXnaName, plXnaDesc, plXnaRegion, " +
               "       xxA.stID, grQualA.grDesc, xxA.grPos, xxX.stID, grQualX.grDesc, xxX.grPos, " +                
               "       mtSet1.mtResA, mtSet1.mtResX, " +
               "       mtSet2.mtResA, mtSet2.mtResX, " +
               "       mtSet3.mtResA, mtSet3.mtResX, " +
               "       mtSet4.mtResA, mtSet4.mtResX, " +
               "       mtSet5.mtResA, mtSet5.mtResX, " +
               "       mtSet6.mtResA, mtSet6.mtResX, " +
               "       mtSet7.mtResA, mtSet7.mtResX, " +
               "       mtSet8.mtResA, mtSet8.mtResX, " +
               "       mtSet9.mtResA, mtSet9.mtResX  " +
               " FROM MtSingleList mt INNER JOIN GrList gr ON mt.grID = gr.grID " +
               "      INNER JOIN CpList cp ON gr.cpId = cp.cpID " +
               "      LEFT OUTER JOIN MtSet mtSet1 ON mt.mtID = mtSet1.mtID AND mtSet1.mtSet = 1 " +
               "      LEFT OUTER JOIN MtSet mtSet2 ON mt.mtID = mtSet2.mtID AND mtSet2.mtSet = 2 " +
               "      LEFT OUTER JOIN MtSet mtSet3 ON mt.mtID = mtSet3.mtID AND mtSet3.mtSet = 3 " +
               "      LEFT OUTER JOIN MtSet mtSet4 ON mt.mtID = mtSet4.mtID AND mtSet4.mtSet = 4 " +
               "      LEFT OUTER JOIN MtSet mtSet5 ON mt.mtID = mtSet5.mtID AND mtSet5.mtSet = 5 " +
               "      LEFT OUTER JOIN MtSet mtSet6 ON mt.mtID = mtSet6.mtID AND mtSet6.mtSet = 6 " +
               "      LEFT OUTER JOIN MtSet mtSet7 ON mt.mtID = mtSet7.mtID AND mtSet7.mtSet = 7 " +
               "      LEFT OUTER JOIN MtSet mtSet8 ON mt.mtID = mtSet8.mtID AND mtSet8.mtSet = 8 " +
               "      LEFT OUTER JOIN MtSet mtSet9 ON mt.mtID = mtSet9.mtID AND mtSet9.mtSet = 9 " +
               "      LEFT OUTER JOIN (XxList xxA LEFT OUTER JOIN GrList grQualA ON xxA.grID = grQualA.grID) ON mt.stA = xxA.stID " +
               "      LEFT OUTER JOIN (XxList xxX LEFT OUTER JOIN GrList grQualX ON xxX.grID = grQualX.grID) ON mt.stX = xxX.stID ";
    }
    
    public SingleMatch() {
    }

    public SingleMatch(Group gr, ResultSet rs) throws SQLException {
        this.gr = gr;
        
        read(rs);
    }
    
    @Override
    public void read(ResultSet rs) throws SQLException {
        plA = new Player();
        plX = new Player();
        
        xxA = new Xx();
        xxX = new Xx();
        
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
        mtInjuredA = rs.getBoolean(++idx);
        mtInjuredX = rs.getBoolean(++idx);
        mtDisqualifiedA = rs.getBoolean(++idx);
        mtDisqualifiedX = rs.getBoolean(++idx);
        
        mtPrinted = rs.getBoolean(++idx);
        mtChecked = rs.getBoolean(++idx);

        stA = rs.getInt(++idx);
        stX = rs.getInt(++idx);
        
        tmAtmID = rs.getInt(++idx);
        tmXtmID = rs.getInt(++idx);

        plAplID = plA.plID = rs.getInt(++idx);
        plA.plNr = rs.getInt(++idx);
        plA.psFirstName = Database.getString(rs, ++idx);
        plA.psLastName = Database.getString(rs, ++idx);
        plA.naName = Database.getString(rs, ++idx);
        plA.naDesc = Database.getString(rs, ++idx);
        plA.naRegion = Database.getString(rs, ++idx);

        plXplID = plX.plID = rs.getInt(++idx);
        plX.plNr = rs.getInt(++idx);
        plX.psFirstName = Database.getString(rs, ++idx);
        plX.psLastName = Database.getString(rs, ++idx);
        plX.naName = Database.getString(rs, ++idx);
        plX.naDesc = Database.getString(rs, ++idx);
        plX.naRegion = Database.getString(rs, ++idx);
        
        xxA.stID = rs.getInt(++idx);
        xxA.grDesc = Database.getString(rs, ++idx);
        xxA.grPos = rs.getInt(++idx);
        
        xxX.stID = rs.getInt(++idx);
        xxX.grDesc = Database.getString(rs, ++idx);
        xxX.grPos  = rs.getInt(++idx);

        result = new int[mtResA + mtResX][2];
        for (int i = 0; i < mtResA + mtResX; i++) {
            result[i][0] = rs.getInt(++idx);
            result[i][1] = rs.getInt(++idx);
        }            
    }

    @Override
    public boolean isFinished() {
        if (stA == 0 || stX == 0)
            return false;
        
        if (stA != 0 && plA.plNr == 0 || stX != 0 && plX.plNr == 0)
            return mtDateTime == null || mtDateTime.getTime() == 0;

        if (!mtChecked)
            return false;
        
        if (mtWalkOverA || mtWalkOverX ||
            mtInjuredA || mtInjuredX ||
            mtDisqualifiedA || mtDisqualifiedX)
            return true;

        return mtResA > mtBestOf / 2 || mtResX > mtBestOf / 2;
    }

    @Override
    public Side getWinnerAX() {
        if (gr == null)
            return Side.None;
        else if (!isFinished())
            return Side.None;
        else if (stA != 0 && plA != null && plA.plNr == 0)
            return Side.X;
        else if (stX != 0 && plX != null && plX.plNr == 0)
            return Side.A;
        else if (mtWalkOverA || mtInjuredA || mtDisqualifiedA)
            return Side.X;
        else if (mtWalkOverX || mtInjuredX || mtDisqualifiedX)
            return Side.A;
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
                return "<span class=\"result\"><span data-i18n=\"result.format.short.wo\"></span>";
            else
                return "<span class=\"result\">" + mtResA + "&nbsp;:&nbsp;" + mtResX + "</span><span class=\"games\"><span data-i18n=\"result.format.short.wo\"></span>";
        }
        
        if (mtInjuredA || mtInjuredX) {
            if (forWinner)
                return "<span class=\"result\"><span data-i18n=\"result.format.short.injured\"></span>";
            else
                return "<span class=\"result\">" + mtResA + "&nbsp;:&nbsp;" + mtResX + "</span><span class=\"games\"><span data-i18n=\"result.format.short.injured\"></span>";
        }

        if (mtDisqualifiedA || mtDisqualifiedX) {
            if (forWinner)
                return "<span class=\"result\"><span data-i18n=\"result.format.short.disqualifiedWinner\"></span>";
            else
                return "<span class=\"result\">" + mtResA + "&nbsp;:&nbsp;" + mtResX + "</span><span class=\"games\"><span data-i18n=\"result.format.short.disqualifiedLoser\"></span>";
        }

        if (mtResA < mtBestOf / 2 && mtResX < mtBestOf / 2)
            return "<br>";

        StringBuilder buf = new StringBuilder();

        if (!forWinner || mtResA >= mtResX)
            buf.append("<span class=\"result\">").append(mtResA).append("&nbsp;:&nbsp;").append(mtResX).append("</span>");
        else if (mtResX > mtResA)
            buf.append("<span class=\"result\">").append(mtResX).append("&nbsp;:&nbsp;").append(mtResA).append("</span>");

        buf.append("<span class=\"games\">");

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

        buf.append("</span>");

        String resultString = buf.toString();

        if (resultString.length() == 0) {
            return "<br>";
        }
        return resultString;
    }

    @Override
    public String aToString(boolean withAssoc, boolean withNr, boolean withFlag) {
        if (plA != null && plA.plNr != 0)
            return plA.toString(withAssoc, withNr, withFlag);
        else if (xxA != null && xxA.grDesc != null)
            return xxA.toString();
        else
            return "<br>";
    }
    
    @Override
    public String aNrToString() {
        if (plA == null || plA.plNr == 0)
            return "<br>";
        
        return "" + plA.plNr;
    }
    
    @Override
    public String aAssocToString() {
        if (plA == null || plA.plNr == 0)
            return "<br>";
        
        return plA.naName;
    }

    @Override
    public String xToString(boolean withAssoc, boolean withNr, boolean withFlag) {
        if (plX != null && plX.plNr != 0)
            return plX.toString(withAssoc, withNr, withFlag);
        else if (xxX != null && xxX.grDesc != null)
            return xxX.toString();
        else
            return "<br>";
    }
    
    @Override
    public String xNrToString() {
        if (plX == null || plX.plNr == 0)
            return "<br>";
        
        return "" + plX.plNr;
    }
    
    @Override
    public String xAssocToString() {
        if (plX == null || plX.plNr == 0)
            return "<br>";
        
        return plX.naName;
    }

    @Override
    public Player getPlA() {
        return plA;
    }
    
    @Override
    public Player getPlB() {
        return null;
    }
    
    @Override
    public Player getPlX() {
        return plX;
    }
    
    @Override
    public Player getPlY() {
        return null;
    }
}
