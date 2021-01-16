/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database.match;

import de.webgen.database.Database;
import de.webgen.database.Group;
import de.webgen.database.Player;
import de.webgen.database.Team;
import de.webgen.database.Xx;
import java.sql.ResultSet;
import java.sql.SQLException;


public class TeamMatch extends Match {

    public Team tmA;
    public Team tmX;
    
    // Eigentlich nur ein Marker, ob es eine Nominierung gibt.
    // Genaugenommen frage ich se nur von Team A ab.
    private int nmID;

    public static String getSelectString() {
        return "SELECT mt.grID, mt.mtID, mtNr, 0 AS mtMS, mtRound, mtMatch, mtReverse, mtTable, mtDateTime, mtTimestamp, " +
               "       mtMatches, mtBestOf, mt.mtResA, mt.mtResX, " +
               "       mtWalkOverA, mtWalkOverX, " +
               "       mtInjuredA, mtInjuredX, " +
               "       mtDisqualifiedA, mtDisqualifiedX, " +
               "       mtPrinted, mtChecked, " +
               "       stA, stX, " +
               "       tmAtmID, tmAtmName, tmAtmDesc, tmAnaName, tmAnaDesc, tmAnaRegion, " +
               "       tmXtmID, tmXtmName, tmXtmDesc, tmXnaName, tmXnaDesc, tmXnaRegion, " +
               "       xxA.stID, grQualA.grDesc, xxA.grPos, xxX.stID, grQualX.grDesc, xxX.grPos, " +
               "       nm.nmID " +
               " FROM MtTeamList mt INNER JOIN GrList gr ON mt.grID = gr.grID " +
               "      INNER JOIN CpList cp ON gr.cpId = cp.cpID " +
               "      LEFT OUTER JOIN (XxList xxA LEFT OUTER JOIN GrList grQualA ON xxA.grID = grQualA.grID) ON mt.stA = xxA.stID " +
               "      LEFT OUTER JOIN (XxList xxX LEFT OUTER JOIN GrList grQualX ON xxX.grID = grQualX.grID) ON mt.stX = xxX.stID " +
               "      LEFT OUTER JOIN NmRec nm ON mt.mtID = nm.mtID AND mt.tmAtmID = nm.tmID ";
    }

    public TeamMatch() {
        tmA = new Team();
        tmX = new Team();
        
        xxA = new Xx();
        xxX = new Xx();
        
    }

    public TeamMatch(Group gr, ResultSet rs) throws SQLException {
        this.gr = gr;

        tmA = new Team(gr.cp);
        tmX = new Team(gr.cp);
        
        xxA = new Xx();
        xxX = new Xx();
        
        read(rs);
    }
     
    @Override
    public void read(ResultSet rs) throws SQLException {
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

        tmAtmID = tmA.tmID = rs.getInt(++idx);
        tmA.tmName = Database.getString(rs, ++idx);
        tmA.tmDesc = Database.getString(rs, ++idx); 
        tmA.naName = Database.getString(rs, ++idx); 
        tmA.naDesc = Database.getString(rs, ++idx); 
        tmA.naRegion = Database.getString(rs, ++idx); 

        tmXtmID = tmX.tmID = rs.getInt(++idx);
        tmX.tmName = Database.getString(rs, ++idx); 
        tmX.tmDesc = Database.getString(rs, ++idx); 
        tmX.naName = Database.getString(rs, ++idx); 
        tmX.naDesc = Database.getString(rs, ++idx); 
        tmX.naRegion = Database.getString(rs, ++idx); 
        
        xxA.stID = rs.getInt(++idx);
        xxA.grDesc = Database.getString(rs, ++idx);
        xxA.grPos = rs.getInt(++idx);
        
        xxX.stID = rs.getInt(++idx);
        xxX.grDesc = Database.getString(rs, ++idx);
        xxX.grPos  = rs.getInt(++idx);

        nmID = rs.getInt(++idx);
    }

    @Override
    public boolean isFinished() {
        if (stA == 0 || stX == 0)
            return false;
        
        if (stA != 0 && tmA.tmName == null || stX != 0 && tmX.tmName == null)
            return mtDateTime == null || mtDateTime.getTime() == 0;

        if (!mtChecked)
            return false;

        if (mtWalkOverA || mtWalkOverX ||
            mtInjuredA || mtInjuredX ||
            mtDisqualifiedA || mtDisqualifiedX)
            return true;

        return mtResA > mtMatches / 2 || mtResX > mtMatches / 2;
    }
    
    @Override
    public Side getWinnerAX() {
        if (gr == null)
            return Side.None;
        else if (!isFinished())
            return Side.None;
        else if (stA != 0 && tmA != null && tmA.tmName == null)
            return Side.X;
        else if (stX != 0 && tmX != null && tmX.tmName == null)
            return Side.A;
        else if (mtWalkOverA || mtInjuredA || mtDisqualifiedA)
            return Side.X;
        else if (mtWalkOverX || mtInjuredX || mtDisqualifiedX)
            return Side.A;
        else if (mtResA > mtMatches / 2)
            return Side.A;
        else if (mtResX > mtMatches / 2)
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
                return "<span class=\"result\"><span data-i18n=\"result.format.short.disqualified\"></span>";
            else
                return "<span class=\"result\">" + mtResA + "&nbsp;:&nbsp;" + mtResX + "</span><span class=\"games\"><span data-i18n=\"result.format.short.disqualified\"></span>";
        }

        // Wenn es noch keine Nominierung (von Team A) gibt
        if (nmID == 0) 
            return "<br>";

        return "<a href=\"#\" tabindex=\"0\" role=\"button\" data-toggle=\"popover\" data-target=\"[data-webgen-teammatch=&quot;" + mtNr + "&quot;]\">" +
               "<span class=\"result\">" +
               (!forWinner || mtResA > mtResX ? mtResA : mtResX) + "&nbsp;:&nbsp;" + (!forWinner || mtResA > mtResX ? mtResX : mtResA) +
               "</span>" +
               "</a>";
    }

    @Override
    public String aToString(boolean withAssoc, boolean withNr, boolean withFlag) {
        if (tmA != null && tmA.tmName != null)
            return tmA.toString(withAssoc, withFlag);
        else if (xxA != null && xxA.grDesc != null)
            return xxA.toString();
        else
            return "<br>";
    }
        
    @Override
    public String aNrToString() {
        return "<br>";
    }
        
    @Override
    public String aAssocToString() {
        if (tmA == null || tmA.tmName == null)
            return "<br>";
        
        return tmA.naName;
    }

    @Override
    public String xToString(boolean withAssoc, boolean withNr, boolean withFlag) {
        if (tmX != null && tmX.tmName != null)
            return tmX.toString(withAssoc, withFlag);
        else if (xxX != null && xxX.grDesc != null)
            return xxX.toString();
        else
            return "<br>";
    }
    
    @Override
    public String xNrToString() {
        return "<br>";
    }
        
    @Override
    public String xAssocToString() {
        if (tmX == null || tmX.tmName == null)
            return "<br>";
        
        return tmX.naName;
    }

    @Override
    public Player getPlA() {
        return null;
    }
    
    @Override
    public Player getPlB() {
        return null;
    }
    
    @Override
    public Player getPlX() {
        return null;
    }
    
    @Override
    public Player getPlY() {
        return null;
    }
}
