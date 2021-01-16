/* Copyright (C) 2020 Christoph Theis */
package de.webgen.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.webgen.database.IDatabase;

import de.webgen.database.Association;
import de.webgen.database.Competition;
import de.webgen.database.DoublePlayer;
import de.webgen.database.Group;
import de.webgen.database.Player;
import de.webgen.database.SinglePlayer;
import de.webgen.database.Team;
import de.webgen.database.TeamPlayer;
import de.webgen.database.match.Match;
import de.webgen.database.match.SingleMatch;
import de.webgen.database.position.Groupposition;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author chtheis
 */
public class TestDatabase implements IDatabase {
    
    Map<Integer, Player> players = new java.util.HashMap<>();
    Map<Integer, Competition> competitions = new java.util.HashMap<>();
    Map<Integer, Group> groups = new java.util.HashMap<>();
    Map<Integer, Match> matches = new java.util.HashMap<>();
    
    TestDatabase() {
        Gson json = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:MM")
                .create();
        
        // Read fixtures
        for (String str : Fixtures.players) {
            Player pl = json.fromJson(str, Player.class);
            players.put(pl.plNr, pl);
        }
        
        // Add empty player as place holder
        players.put(0, new Player());
        
        for (String str : Fixtures.competitions) {
            Competition cp = json.fromJson(str, Competition.class);
            competitions.put(cp.cpID, cp);
        }
        
        for (String str : Fixtures.groups) {
            Group gr = json.fromJson(str, Group.class);
            gr.cp = competitions.get(gr.cpID);
            groups.put(gr.grID, gr);
        }
        
        for (String str : Fixtures.singles) {
            SingleMatch mt = json.fromJson(str, SingleMatch.class);
            mt.gr = groups.get(mt.grID);
            mt.plA = players.get(mt.plAplID);
            mt.plX = players.get(mt.plXplID);
            matches.put(mt.mtID, mt);
        }
    }

    @Override
    public void closeConnection() {
        
    }

    @Override
    public List<DoublePlayer> getDoubles(Competition cp) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Timestamp getEntryTimestamp(int type) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public List<Timestamp> getMatchDates(Group gr) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Timestamp getMatchTimestamp(String date) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Timestamp getMatchTimestamp(Group gr, String date) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public List<SinglePlayer> getPartnerWanted(Competition cp) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Timestamp getPlayerTimestamp() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public List<Player> getPlayers() throws SQLException {
        return new java.util.ArrayList<Player>(players.values());
    }

    @Override
    public List<SinglePlayer> getSingles(Competition cp) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public int getTableType() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public List<TeamPlayer> getTeamPlayers(Competition cp) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public List<Team> getTeams(Competition cp) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public String getTitle() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Association[] readAssociations() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Group[] readCurrentGroups() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Competition readEvent(String cpName) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Competition[] readEvents() throws SQLException {
        return competitions.values().toArray(Competition[]::new);
    }

    @Override
    public Competition[] readEvents(Timestamp datetime) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Competition[] readEvents(int cpType) throws SQLException {
         return competitions.values()
                 .stream()
                 .filter(cp -> cp.cpType == cpType)
                 .collect(Collectors.toList())
                 .toArray(Competition[]::new);
    }

    @Override
    public Competition[] readEvents(Timestamp datetime, int cpType) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Group readGroup(String cpName, String grName) throws SQLException {
        int cpID = 0;
        for (Competition cp : competitions.values()) {
            if (cp.cpName.equals((cpName))) {
                cpID = cp.cpID;
                break;
            }
        }
        
        if (cpID == 0)
            return null;
        
        for (Group gr : groups.values()) {
            if (gr.cpID == cpID && gr.grName.equals(grName))
                return gr;
        }
        
        return null;
    }

    @Override
    public Group readGroup(Competition cp, String grName) throws SQLException {
        if (cp == null)
            return null;
        
        for (Group gr : groups.values()) {
            if (gr.cpID == cp.cpID && gr.grName.equals(grName))
                return gr;
        }
        
        return null;
    }

    @Override
    public List<Groupposition> readGroupposition(Group gr) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Group[] readGroups(Competition cp) throws SQLException {
         return groups.values()
                 .stream()
                 .filter(gr -> gr.cpID == cp.cpID)
                 .collect(Collectors.toList())
                 .toArray(Group[]::new);
    }

    @Override
    public Group[] readGroups(String date) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public List<Match> readIndividualMatches(Match teamMatch) throws SQLException {
        return new java.util.ArrayList<>(); 
    }

    @Override
    public List<Match> readMatches(Group gr, String date) throws SQLException {
        if (gr == null)
            return new java.util.ArrayList<>();
        
        return matches.values()
                    .stream()
                    .filter(mt -> mt.grID == gr.grID)
                    .collect(Collectors.toList());
    }

    @Override
    public List<List<Match>> readMatches(Group gr) throws SQLException {
         List<List<Match>> ret = new java.util.ArrayList<>();

         if (gr == null)
             return ret;
         
         Match[] list =
            matches.values()
                 .stream()
                 .filter(mt -> mt.grID == gr.grID)
                 .collect(Collectors.toList())
                 .toArray(Match[]::new);
         
         for (Match mt : list) {
             while (ret.size() < mt.mtRound)
                 ret.add(new java.util.ArrayList<>());
             
             List<Match> round = ret.get(mt.mtRound - 1);
             
             while (round.size() < mt.mtMatch)
                 round.add(null);
             
             round.set(mt.mtMatch - 1, mt);             
         }
         
         return ret;
    }

    @Override
    public List<Match> readPlayersMatches(int plNr) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Group[] readPublishedGroups() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public List<Match> readTeamMatches(int tmID) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public boolean testConnection() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void updateTimestamp(Group group) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    
}
