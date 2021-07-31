/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database;

import de.webgen.database.match.SingleMatch;
import de.webgen.database.match.Match;
import de.webgen.database.match.TeamMatch;
import de.webgen.database.match.DoubleMatch;
import de.webgen.database.match.IndividualMatch;
import de.webgen.database.position.DoubleGroupposition;
import de.webgen.database.position.Groupposition;
import de.webgen.database.position.SingleGroupposition;
import de.webgen.database.position.TeamGroupposition;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Database implements IDatabase {
    
    final static java.nio.charset.Charset charsetUTF = java.nio.charset.Charset.forName("UTF-16LE");
    final static java.nio.charset.Charset charsetISO = java.nio.charset.Charset.forName("ISO-8859-1");

    public static String getString(ResultSet rs, int idx) throws SQLException {
        return rs.getString(idx);
/*   
        // Old code: check if column is varchar or nvarchar and interpret the bytes accordingly
        // But jTDS reports nvarchar columns as varchar
        byte[] bytes = rs.getBytes(idx);
        if (bytes == null)
            return null;
        else if (bytes.length == 0)
            return "";
        else if (rs.getMetaData().getColumnType(idx) == java.sql.Types.NVARCHAR)
            return new String(bytes, charsetUTF);
        else
            return new String(bytes, charsetISO);
*/
    }

    public Database(String connectionString) {
        String[] parts = connectionString.split(";");
        java.util.HashMap<String, String> map = new java.util.HashMap<>();
        for (String part : parts) {
            String[] tmp = part.split("=");
            map.put(tmp[0], tmp.length > 1 ? tmp[1] : "");
        }
        
        String database[] = map.get("DATABASE").split("\\\\");
        
        StringBuilder buffer = new StringBuilder();
        buffer.append("jdbc:sqlserver://");
        if (!map.containsKey("SERVER"))
            buffer.append("localhost");
        else if (map.get("SERVER").equals("(local)"))
            buffer.append("localhost");
        else
            buffer.append(map.get("SERVER"));
        buffer.append(";");
        
        buffer.append("databaseName=").append(database[0]).append(";");
        if (database.length > 1)
            buffer.append("instanceName=").append(database[1]).append(";");
        
        if (!map.containsKey("Trusted_Connection") || !map.get("Trusted_Connection").equals("Yes"))
            buffer.append("user=").append(map.get("UID")).append(";").append("password=").append(map.get("PWD")).append(";");
        else
            buffer.append("integratedSecurity=true;");
        
        this.connectionString = buffer.toString();
    }


    @Override
    public boolean testConnection() {
        try (Connection conn = java.sql.DriverManager.getConnection(connectionString);) {
            return conn != null;
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.INFO, null, ex);
        }
        
        return false;
    }

    @Override
    public Association[] readAssociations() throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        String sql = Association.getSelectString() + " ORDER BY naDesc";
        
        
        try {
            stmt = prepareStatement(sql);
            
            List<Association> assocs = new java.util.ArrayList<>();

            res = stmt.executeQuery();
            while (res.next()) {
                Association na = new Association(res);
                assocs.add(na);
            }

            return assocs.toArray(new Association[0]);
        } finally {
            try {
                if (res != null)
                    res.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }        
    }
    
    @Override
    public Competition[] readEvents() throws SQLException {
        return readEvents(null, 0);
    }
    
    @Override
    public Competition[] readEvents(Timestamp datetime) throws SQLException {        
        return readEvents(datetime, 0);
    }
    
    @Override
    public Competition[] readEvents(int cpType) throws SQLException {
        return readEvents(null, cpType);
    }
    
    @Override
    public Competition[] readEvents(Timestamp datetime, int cpType) throws SQLException {            
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        String sql = Competition.getSelectString();
        
        sql += " WHERE 1 = 1";
        
        if (datetime != null)
            sql += " AND CpList.cpID IN (SELECT MtList.cpID FROM MtList WHERE DAY(MtList.mtDateTime) = ?)";
        
        if (cpType != 0)
            sql += " AND cpType = ?";
        
        sql += " ORDER BY cpDesc";        
        
        try {
            int idx = 0;
            stmt = prepareStatement(sql);
            if (datetime != null)
                stmt.setInt(++idx, datetime.getDate());
            if (cpType != 0)
                stmt.setInt(++idx, cpType);
            
            List<Competition> events = new java.util.ArrayList<>();

            res = stmt.executeQuery();
            while (res.next()) {
                Competition event = new Competition(res);
                events.add(event);
            }

            return events.toArray(new Competition[0]);
        } finally {
            try {
                if (res != null)
                    res.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public Competition readEvent(String cpName) throws SQLException {
        Competition cp = null;

        PreparedStatement stmt = null;
        ResultSet res = null;
        
        String sql = Competition.getSelectString() + " WHERE cpName = ?";

        try {
            stmt = prepareStatement(sql);
            stmt.setString(1, cpName);
            
            res = stmt.executeQuery();
            if (res.next())
                cp = new Competition(res);

            return cp;
        } finally {
            try {
                if (res != null) {
                    while (res.next()) {}
                    res.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    
    @Override
    public Group[] readPublishedGroups() throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        String sql = Group.getSelectString() +
                " WHERE grPublished > 0 " +
                " ORDER BY cp.cpName, gr.grSortOrder, gr.grStage, gr.grDesc";

        try {
            stmt = prepareStatement(sql);
            
            Competition[] events = readEvents();
            List<Group> groups = new java.util.ArrayList<>();

            res = stmt.executeQuery();

            while (res.next()) {
                groups.add(new Group(null, res));
            }

            Map<Integer, Competition> cpMap = new java.util.HashMap<>();
            for (int i = 0; i < events.length; i++)
                cpMap.put(events[i].cpID, events[i]);

            for (Iterator<Group> it = groups.iterator(); it.hasNext(); ) {
                Group gr = it.next();
                gr.cp = cpMap.get(gr.cpID);
            }

            return groups.toArray(new Group[0]);
        } finally {
            try {
                if (res != null)
                    res.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }    
    }

    
    // Read groups with played matches
    @Override
    public Group[] readCurrentGroups() throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        String sql = Group.getSelectString() +
                " WHERE (SELECT COUNT(*) FROM MtList mt " +
                "         WHERE mt.grID = gr.grID AND (mtResA > 0 OR mtResX > 0)) > 0 " +
                " ORDER BY cp.cpName, gr.grSortOrder, gr.grStage, gr.grDesc";

        try {
            stmt = prepareStatement(sql);
            
            Competition[] events = readEvents();
            List<Group> groups = new java.util.ArrayList<>();

            res = stmt.executeQuery();

            while (res.next()) {
                groups.add(new Group(null, res));
            }

            Map<Integer, Competition> cpMap = new java.util.HashMap<>();
            for (int i = 0; i < events.length; i++)
                cpMap.put(events[i].cpID, events[i]);

            for (Iterator<Group> it = groups.iterator(); it.hasNext(); ) {
                Group gr = it.next();
                gr.cp = cpMap.get(gr.cpID);
            }

            return groups.toArray(new Group[0]);
        } finally {
            try {
                if (res != null)
                    res.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public Group[] readGroups(Competition cp) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        if (cp == null)
            return new Group[0];
        
        String sql = Group.getSelectString() +
                    "WHERE cp.cpID = ? "+
                    "ORDER BY cp.cpName, gr.grSortOrder, gr.grStage, gr.grDesc";

        try {
            stmt = prepareStatement(sql);
            stmt.setInt(1, cp.cpID);
            
            List<Group> groups = new java.util.ArrayList<>();

            res = stmt.executeQuery();
            
            while (res.next()) {
                Group group = new Group(cp, res);
                groups.add(group);
            }

            return groups.toArray(new Group[0]);
        } finally {
            try {
                if (res != null)
                    res.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public Group[] readGroups(String date) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        String sql = 
                Group.getSelectString() +
                "WHERE grID IN (SELECT grID FROM MtList WHERE FORMAT(mtDateTime, 'yyyy-MM-dd') = ?) " +
                "ORDER BY cp.cpName, gr.grSortOrder, gr.grStage, gr.grDesc";

        try {
            stmt = prepareStatement(sql);
            stmt.setString(1, date);
            Competition[] events = readEvents();
            List<Group> groups = new java.util.ArrayList<>();

            res = stmt.executeQuery();
            
            while (res.next()) {
                Group group = new Group(null, res);
                groups.add(group);
            }

            Map<Integer, Competition> cpMap = new java.util.HashMap<>();
            for (int i = 0; i < events.length; i++)
                cpMap.put(events[i].cpID, events[i]);

            for (Iterator<Group> it = groups.iterator(); it.hasNext(); ) {
                Group gr = it.next();
                gr.cp = cpMap.get(gr.cpID);
            }

            return groups.toArray(new Group[0]);
        } finally {
            try {
                if (res != null)
                    res.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public Group readGroup(String cpName, String grName) throws SQLException {
        Competition cp = readEvent(cpName);

        if (cp == null)
            return null;

        return readGroup(cp, grName);
    }


    @Override
    public Group readGroup(Competition cp, String grName) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;

        Group group = null;
        
        String sql = Group.getSelectString() + "WHERE gr.cpID = ?  AND gr.grName = ?";
        
        try {
            stmt = prepareStatement(sql); 
            stmt.setInt(1, cp.cpID);
            stmt.setString(2, grName);
            
            res = stmt.executeQuery();

            if (res.next()) {
                group = new Group(cp, res);
            }

            return group;
        } finally {
            try {
                if (res != null) {
                    while (res.next()) {}
                    res.close();
                }
                
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void updateTimestamp(Group group) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        // Kein Timestamp, wenn Gruppe begonnen hat (mtPrinted) aber nicht geprueft wurde (mtChecked)
        // Liveticker aendert naemlich jedesmal mtTimestamp
        String sql = 
                "SELECT MAX(mtTimestamp) AS timestamp FROM MtList WHERE (mtPrinted = 0 OR mtChecked = 1) AND grID = ? " +
                "UNION " +
                "SELECT MAX(stTimestamp) AS timestmap FROM StList WHERE grID = ? " +
                "ORDER BY timestamp DESC";
        try {
            stmt = prepareStatement(sql);
            stmt.setInt(1, group.grID);
            stmt.setInt(2, group.grID);
            
            Timestamp ts = null;

            res  = stmt.executeQuery();

            if (res.next())
                ts = res.getTimestamp(1);

            if (group.timestamp == null || ts != null && group.timestamp.compareTo(ts) < 0)
                group.timestamp = ts;

        } finally {
            try {
                if (res != null) {
                    while (res.next()) {}
                    res.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public List<Match> readMatches(Group gr, String date) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;

        String sql;

        if (gr.cp.isSingle())
            sql = SingleMatch.getSelectString();
        else if (gr.cp.isDouble() || gr.cp.isMixed())
            sql = DoubleMatch.getSelectString();
        else if (gr.cp.isTeam())
            sql = TeamMatch.getSelectString();
        else
            return null;
        
        sql += 
                "WHERE gr.grID = ? AND FORMAT(mtDateTime, 'yyyy-MM-dd') = ? ORDER BY mt.mtRound, mt.mtMatch ";
        try {

            stmt = prepareStatement(sql);
            stmt.setInt(1, gr.grID);
            stmt.setString(2, date);
            
            res = stmt.executeQuery();

            List<Match> list = new java.util.ArrayList<>();

            while (res.next()) {

                Match match = null;

                if (gr.cp.isSingle())
                    match = new SingleMatch(gr, res);
                else if (gr.cp.isDouble() || gr.cp.isMixed())
                    match = new DoubleMatch(gr, res);
                else if (gr.cp.isTeam())
                    match = new TeamMatch(gr, res);

                list.add(match);
            }

            return list;
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @Override
    public List<List<Match>> readMatches(Group gr) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        String sql;

        if (gr.cp.isSingle())
            sql = SingleMatch.getSelectString();
        else if (gr.cp.isDouble() || gr.cp.isMixed())
            sql = DoubleMatch.getSelectString();
        else if (gr.cp.isTeam())
            sql = TeamMatch.getSelectString();
        else
            return null;
        
        sql += "WHERE gr.grID = ? ORDER BY mt.mtRound, mt.mtMatch ";

        try {
            stmt = prepareStatement(sql);            
            stmt.setInt(1, gr.grID);

            List<List<Match>> list = new java.util.ArrayList<>();

            List<Match> roundList = null;
            
            res = stmt.executeQuery();
            
            while (res.next()) {

                Match match = null;

                if (gr.cp.isSingle())
                    match = new SingleMatch(gr, res);
                else if (gr.cp.isDouble() || gr.cp.isMixed())
                    match = new DoubleMatch(gr, res);
                else if (gr.cp.isTeam())
                    match = new TeamMatch(gr, res);
                else
                    continue;

                if (roundList == null || roundList.get(0).mtRound != match.mtRound) {
                    roundList = new java.util.ArrayList<>();
                    list.add(roundList);
                }

                roundList.add(match);
            }

            return list;
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public List<Match> readIndividualMatches(Match teamMatch) throws SQLException {
        ResultSet res = null;
        List<Match> list = new java.util.ArrayList<>();

        String sql = IndividualMatch.getSelectString() +
                " WHERE mt.mtNr = ?  AND mt.mtMS <> 0 " +
                " ORDER BY mt.mtMS";
            
        try  {
            PreparedStatement stmt = prepareStatement(sql);
            stmt.setInt(1, teamMatch.mtNr);

            res = stmt.executeQuery();
            while (res.next()) {
                list.add(new IndividualMatch(teamMatch.gr, res));
            }

            return list;
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @Override
    public List<Match> readTeamMatches(int tmID) throws SQLException {
        List<Match> list = new java.util.ArrayList<>();
        
        String sql = TeamMatch.getSelectString();
        sql += " WHERE (tmAtmID = ? OR tmXtmID = ?) AND cp.cpType = 4";
        
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        try {
            stmt = prepareStatement(sql);  
            stmt.setInt(1, tmID);
            stmt.setInt(2, tmID);
            
            res = stmt.executeQuery();
            
            while (res.next()) {

                Match match = new TeamMatch();
                match.read(res);
                
                list.add(match);
            }
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return list;        
    }
    
    @Override
    public List<Match> readPlayersMatches(int plNr) throws SQLException {
        List<Match> list = new java.util.ArrayList<>();
        
        list.addAll(readPlayersMatchesSingles(plNr));
        list.addAll(readPlayersMatchesDoubles(plNr));
        list.addAll(readPlayersMatchesIndividual(plNr));
        
        return list;
    }
        
    private List<Match> readPlayersMatchesSingles(int plNr) throws SQLException {
        List<Match> list = new java.util.ArrayList<>();
        
        String sql = SingleMatch.getSelectString();
        sql += " WHERE ( (plAplNr % 10000) = ? OR (plXplNr % 10000) = ?) AND cp.cpType = 1";
        
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        try {
            stmt = prepareStatement(sql);  
            stmt.setInt(1, plNr);
            stmt.setInt(2, plNr);
            
            res = stmt.executeQuery();
            
            while (res.next()) {

                Match match = new SingleMatch();
                match.read(res);
                
                list.add(match);
            }
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return list;
    }

    private List<Match> readPlayersMatchesDoubles(int plNr) throws SQLException {
        List<Match> list = new java.util.ArrayList<>();
        
        String sql = DoubleMatch.getSelectString();
        
        // WHERE condition as SELECT: testing for player numbers of MtDoubleList takes mucht (5s) longer than testing them in StDoubleList
        sql +=  " WHERE (cp.cpType = 2 OR cp.cpType = 3) AND (" +
                " stA IN (SELECT stID FROM StDoubleList st WHERE st.grID = gr.grID AND ((plplNr % 10000) = ? OR (bdplNr % 10000) = ?)) OR " +
                " stX IN (SELECT stID FROM StDoubleList st WHERE st.grID = gr.grID AND ((plplNr % 10000) = ? OR (bdplNr % 10000) = ?)) ) "
        ;
        
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        try {
            stmt = prepareStatement(sql);  
            stmt.setInt(1, plNr);
            stmt.setInt(2, plNr);
            stmt.setInt(3, plNr);
            stmt.setInt(4, plNr);
            
            res = stmt.executeQuery();
            
            while (res.next()) {

                Match match = new DoubleMatch();
                match.read(res);
                
                list.add(match);
            }
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return list;
    }

    private List<Match> readPlayersMatchesIndividual(int plNr) throws SQLException {
        List<Match> list = new java.util.ArrayList<>();
        
        String sql = IndividualMatch.getSelectString();
        sql +=  " WHERE " +
                "  ((plAplNr % 10000) = ? OR (plBplNr % 10000) = ? OR " +
                "   (plXplNr % 10000) = ? OR (plYplNr % 10000) = ?) " +
                " AND cp.cpType = 4 ";
        
        // Bessere Filterung
        sql +=  "AND (tmAtmID IN (SELECT tmID FROM NtEntryList WHERE (plNr % 10000) = ?) OR " +
                "tmXtmID IN (SELECT tmID FROM NtEntryList WHERE (plNr % 10000) = ?))";
        
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        try {
            stmt = prepareStatement(sql);  
            stmt.setInt(1, plNr);
            stmt.setInt(2, plNr);
            stmt.setInt(3, plNr);
            stmt.setInt(4, plNr);
            stmt.setInt(5, plNr);
            stmt.setInt(6, plNr);
            
            res = stmt.executeQuery();
            
            while (res.next()) {

                Match match = new IndividualMatch();
                match.read(res);
                
                list.add(match);
            }
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return list;
    }

    @Override
    public List<Groupposition> readGroupposition(Group gr) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;

        String sql = null;
        if (gr.cp.isSingle())
            sql = SingleGroupposition.getSelectString();
        else if (gr.cp.isDouble() || gr.cp.isMixed())
            sql = DoubleGroupposition.getSelectString();
        else if (gr.cp.isTeam())
            sql = TeamGroupposition.getSelectString();
        else
            return null;

        sql += " ORDER BY stNr";

        List<Groupposition> list = new java.util.ArrayList<>();

        try {
            stmt = prepareStatement(sql);
            stmt.setInt(1, gr.grID);
            
            res = stmt.executeQuery();

            while (res.next()) {
                if (gr.cp.isSingle())
                    list.add(new SingleGroupposition(res));
                else if (gr.cp.isDouble() || gr.cp.isMixed())
                    list.add(new DoubleGroupposition(res));
                else if (gr.cp.isTeam())
                    list.add(new TeamGroupposition(gr.cp, res));
            }

            return list;
        } finally {
            try {
                if (res != null)
                    res.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public int getTableType() throws SQLException {
        Connection conn = getConnection();
        java.sql.Statement stmt = null;
        ResultSet res = null;

        int table = 0;

        try {
            stmt = conn.createStatement();
            res = stmt.executeQuery("SELECT idTable FROM IdRec");

            if (res.next())
                table = res.getInt(1);

            return table;
        } finally {
            try {
                if (res != null)
                    res.close();
                if (stmt != null)
                    stmt.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @Override
    public String getTitle() throws SQLException {
        Connection conn = getConnection();
        java.sql.Statement stmt = null;
        ResultSet res = null;

        String title = "";

        try {
            stmt = conn.createStatement();
            res = stmt.executeQuery("SELECT idTitle FROM IdRec");

            if (res.next())
                title = getString(res, 1);

            return title;
        } finally {
            try {
                if (res != null)
                    res.close();
                if (stmt != null)
                    stmt.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    

    @Override
    public List<Player> getPlayers() throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        String sql = 
                "SELECT plID, plNr, psLast, psFirst, psSex, naName, naDesc, naRegion, plExtID " +
                "  FROM PlList WHERE plDeleted = 0";

        List<Player> list = new java.util.ArrayList<>();

        try {
            stmt = prepareStatement(sql);
            res = stmt.executeQuery();

            while (res.next()) {
                int idx = 0;
                
                Player pl = new Player();
                pl.plID = res.getInt(++idx);
                pl.plNr = res.getInt(++idx);
                pl.psLastName = getString(res, ++idx); 
                pl.psFirstName = getString(res, ++idx); 
                pl.psSex = res.getInt(++idx);
                pl.naName = getString(res, ++idx); 
                pl.naDesc = getString(res, ++idx); 
                pl.naRegion = getString(res, ++idx);
                pl.plExtID = getString(res, ++idx);
                
                list.add(pl);
            }

            return list;
        } finally {
            try {
                if (res != null)
                    res.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public List<SinglePlayer> getSingles(Competition cp) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;

        if (!cp.isSingle())
            return null;
        
        String sql = 
                    "SELECT cpID, plID, plNr, psLast, psFirst, psSex, naName, naDesc, naRegion " +
                    "  FROM TmSingleList tm " +
                    " WHERE tm.cpID = ?";

        List<SinglePlayer> list = new java.util.ArrayList<>();

        try {
            stmt = prepareStatement(sql);
            stmt.setInt(1, cp.cpID);
            
            res = stmt.executeQuery();

            while (res.next()) {
                int idx = 0;
                
                SinglePlayer pl = new SinglePlayer();
                pl.cp = cp;
                pl.cpID = res.getInt(++idx);
                pl.plID = pl.pl.plID = res.getInt(++idx);
                pl.pl.plNr = res.getInt(++idx);
                pl.pl.psLastName = getString(res, ++idx); 
                pl.pl.psFirstName = getString(res, ++idx); 
                pl.pl.psSex = res.getInt(++idx);
                pl.pl.naName = getString(res, ++idx); 
                pl.pl.naDesc = getString(res, ++idx); 
                pl.pl.naRegion = getString(res, ++idx); 

                list.add(pl);
            }

            return list;
        } finally {
            try {
                if (res != null)
                    res.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public List<SinglePlayer> getPartnerWanted(Competition cp) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;

        if (!cp.isDouble() && !cp.isMixed() && !cp.isTeam())
            return null;
        
        String sql = 
                    "SELECT plNr, psLast, psFirst, psSex, naName, naDesc, naRegion " +
                    "  FROM LtEntryList lt " +
                    " WHERE lt.cpID = ? AND NOT EXISTS (SELECT nt.ltID FROM NtList nt WHERE nt.ltID = lt.ltID)";

        List<SinglePlayer> list = new java.util.ArrayList<>();

        try {
            stmt = prepareStatement(sql);
            stmt.setInt(1, cp.cpID);
            
            res = stmt.executeQuery();

            while (res.next()) {
                int idx = 0;
                
                SinglePlayer pl = new SinglePlayer();
                pl.cp = cp;
                pl.pl.plNr = res.getInt(++idx);
                pl.pl.psLastName = getString(res, ++idx); 
                pl.pl.psFirstName = getString(res, ++idx); 
                pl.pl.psSex = res.getInt(++idx);
                pl.pl.naName = getString(res, ++idx); 
                pl.pl.naDesc = getString(res, ++idx); 
                pl.pl.naRegion = getString(res, ++idx); 

                list.add(pl);
            }

            return list;
        } finally {
            try {
                if (res != null)
                    res.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public List<DoublePlayer> getDoubles(Competition cp) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;

        if (!cp.isDouble() && !cp.isMixed())
            return null;
        
        String sql =
                "SELECT cpID, " +
                "       plplID, plplNr, plpsLast, plpsFirst, plpsSex, plnaName, plnaDesc, plnaRegion, " +
                "       bdplID, bdplNr, bdpsLast, bdpsFirst, bdpsSex, bdNaName, bdnaDesc, bdnaRegion  " +
                "  FROM TmDoubleList tm  " +
                " WHERE tm.cpID = ?";
                
        List<DoublePlayer> list = new java.util.ArrayList<>();

        try {
            stmt = prepareStatement(sql);
            stmt.setInt(1, cp.cpID);
            
            res = stmt.executeQuery();

            while (res.next()) {
                int idx = 0;
                
                DoublePlayer pair = new DoublePlayer();
                pair.cp = cp;
                pair.cpID = res.getInt(++idx);
                pair.plID = pair.pl.plID = res.getInt(++idx);
                pair.pl.plNr = res.getInt(++idx);
                pair.pl.psLastName = getString(res, ++idx); 
                pair.pl.psFirstName = getString(res, ++idx);
                pair.pl.psSex = res.getInt(++idx);
                pair.pl.naName = getString(res, ++idx); 
                pair.pl.naDesc = getString(res, ++idx); 
                pair.pl.naRegion = getString(res, ++idx); 

                pair.bd = new Player();
                pair.bdID = pair.bd.plID = res.getInt(++idx);
                pair.bd.plNr = res.getInt(++idx);
                pair.bd.psLastName = getString(res, ++idx); 
                pair.bd.psFirstName = getString(res, ++idx); 
                pair.bd.psSex = res.getInt(++idx);
                pair.bd.naName = getString(res, ++idx); 
                pair.bd.naDesc = getString(res, ++idx); 
                pair.bd.naRegion = getString(res, ++idx); 

                list.add(pair);
            }

            return list;
        } finally {
            try {
                if (res != null)
                    res.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public List<TeamPlayer> getTeamPlayers(Competition cp) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;

        if (!cp.isTeam())
            return null;

        String sql = 
                    "SELECT nt.plID, nt.plNr, nt.psLast, nt.psFirst, nt.psSex, nt.naName, nt.naDesc, nt.naRegion, " +
                    "       tm.tmID, tm.tmName, tm.tmDesc, tm.naName, tm.naDesc, tm.naRegion " +
                    "  FROM NtEntryList nt INNER JOIN TmTeamList tm ON nt.tmID = tm.tmID " +
                    " WHERE tm.cpID = ?";
        
        List<TeamPlayer> list = new java.util.ArrayList<>();

        try {
            stmt = prepareStatement(sql);
            stmt.setInt(1, cp.cpID);
            
            res = stmt.executeQuery();

            while (res.next()) {
                int idx = 0;
                
                TeamPlayer player = new TeamPlayer(cp);
                player.plID = player.pl.plID = res.getInt(++idx);
                player.pl.plNr = res.getInt(++idx);
                player.pl.psLastName = getString(res, ++idx);
                player.pl.psFirstName = getString(res, ++idx); 
                player.pl.psSex = res.getInt(++idx);
                player.pl.naName = getString(res, ++idx); 
                player.pl.naDesc = getString(res, ++idx); 
                player.pl.naRegion = getString(res, ++idx);

                player.tmID = player.tm.tmID   = res.getInt(++idx);
                player.tm.tmName = getString(res, ++idx); 
                player.tm.tmDesc = getString(res, ++idx); 
                player.tm.naName = getString(res, ++idx); 
                player.tm.naDesc = getString(res, ++idx); 
                player.tm.naRegion = getString(res, ++idx);

                list.add(player);
            }

            return list;
        } finally {
            try {
                if (res != null)
                    res.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @Override
    public List<Team> getTeams(Competition cp) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;

        if (!cp.isTeam())
            return null;

        String sql = 
                "SELECT cpID, tmID, tmName, tmDesc, naName, naDesc FROM TmTeamList tm WHERE tm.cpID = ?";
        
        List<Team> list = new java.util.ArrayList<>();

        try {
            stmt = prepareStatement(sql);
            stmt.setInt(1, cp.cpID);
            
            res = stmt.executeQuery();

            while (res.next()) {
                int idx = 0;
                
                Team tm = new Team(cp);
                
                tm.cpID = res.getInt(++idx);
                tm.tmID   = res.getInt(++idx);
                tm.tmName = getString(res, ++idx); 
                tm.tmDesc = getString(res, ++idx); 
                tm.naName = getString(res, ++idx); 
                tm.naDesc = getString(res, ++idx); 

                list.add(tm);
            }

            return list;
        } finally {
            try {
                if (res != null)
                    res.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @Override
    public Timestamp getPlayerTimestamp() throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        Timestamp ts = null;
        
        String sql = "SELECT MAX(psTimestamp) FROM PlList";

        try {
            stmt = prepareStatement(sql);
            res  = stmt.executeQuery();

            if (res.next())
                ts = res.getTimestamp(1);
        } finally {
            try {
                if (res != null) {
                    while (res.next()) {}
                    res.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return ts;
    }

    @Override
    public Timestamp getEntryTimestamp(int type) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        Timestamp ts = null;
        
        String sql = 
            "SELECT MAX(ltTimestamp) FROM LtList lt INNER JOIN CpList cp ON lt.cpID = cp.cpID WHERE cp.cpType = ?"; 

        try {
            stmt = prepareStatement(sql);
            stmt.setInt(1, type);
            res  = stmt.executeQuery();

            if (res.next())
                ts = res.getTimestamp(1);
        } finally {
            try {
                if (res != null) {
                    while (res.next()) {}
                    res.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        // Falls es keinen derartigen WB gibt
        if (ts == null)
            return null;
        
        Timestamp tspl = getPlayerTimestamp();
        return ts.before(tspl) ? tspl : ts;
    }

    @Override
    public Timestamp getMatchTimestamp(String date) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        Timestamp ts = null;
        
        String sql = 
            "SELECT MAX(mtTimestamp) " +
            "  FROM MtList LEFT OUTER JOIN MtSet ON MtList.mtID = MtSet.mtID AND MtSet.mtMS = 0 ANd MtSet.mtSet = 0 " +
             "WHERE FORMAT(mtDateTime, 'yyyy-MM-dd') = ? " +
             "  AND (MtSet.mtID IS NULL OR MtSet.mtResA = 0 AND MtSet.mtResX = 0 OR mtChecked > 0) ";
              

        try {
            stmt = prepareStatement(sql);
            stmt.setString(1, date);
            res  = stmt.executeQuery();
                                        
            if (res.next())
                ts = res.getTimestamp(1);
        } finally {
            try {
                if (res != null) {
                    while (res.next()) {}
                    res.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return ts;
    }

    @Override
    public Timestamp getMatchTimestamp(Group gr, String date) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        Timestamp ts = null;
        
        String sql = 
            "SELECT MAX(mtTimestamp) " +
            "  FROM MtList LEFT OUTER JOIN MtSet ON MtList.mtID = MtSet.mtID AND MtSet.mtMS = 0 ANd MtSet.mtSet = 0 " +
             "WHERE grID = ? AND FORMAT(mtDateTime, 'yyyy-MM-dd') = ? " +
             "  AND (MtSet.mtID IS NULL OR MtSet.mtResA = 0 AND MtSet.mtResX = 0 OR mtChecked > 0) ";
// Und noch im ersten Satz
        try {
            stmt = prepareStatement(sql);
            stmt.setInt(1, gr.grID);
            stmt.setString(2, date);
            
            res  = stmt.executeQuery();
                                        
            if (res.next())
                ts = res.getTimestamp(1);
        } finally {
            try {
                if (res != null) {
                    while (res.next()) {}
                    res.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return ts;
    }

    @Override
    public List<Timestamp> getMatchDates(Group gr) throws SQLException {
        PreparedStatement  stmt = null;
        ResultSet  res  = null;
        
        List<Timestamp> list = new java.util.ArrayList<>();
        
        String sql = "SELECT DISTINCT mtDateTime FROM MtList WHERE grID = ?"; 
        
        try {
            stmt = prepareStatement(sql);
            stmt.setInt(1, gr.grID);
            
            res  = stmt.executeQuery();
            
            while (res.next()) {
                Timestamp ts = res.getTimestamp(1);
                if (ts != null)
                    list.add(ts);
            }
            
            return list;
                
        } finally {
            try {
                if (res != null)
                    res.close();
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
    @Override
    public void closeConnection() {
        if (connection != null) {
            try {                
                connection.close();
            } catch (SQLException ex) {
                
            }
        }
        
        stmtMap.clear();
    }
        
    private Connection getConnection() throws SQLException {
        if (connection != null) {
            try {
                if (false && !connection.isValid(1000))
                    connection = null;
                else
                    connection.getMetaData();
            } catch (Throwable t) {
                connection = null;
            }
        }

        if (connection == null) {
            connection = java.sql.DriverManager.getConnection(connectionString);
        
            if (connection != null)
                connection.setAutoCommit(true);
            
            stmtMap.clear();
        }

        return connection;
    }
    
    private PreparedStatement prepareStatement(String sql) throws SQLException {
        if ( !stmtMap.containsKey(sql) )
            stmtMap.put(sql, getConnection().prepareStatement(sql));

        return stmtMap.get(sql);
    }

    String connectionString = null;

    Connection connection = null;
    
    Map<String, PreparedStatement> stmtMap = new java.util.HashMap<>();
}
