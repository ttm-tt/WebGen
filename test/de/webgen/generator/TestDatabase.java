/* Copyright (C) 2020 Christoph Theis */
package de.webgen.generator;

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
import de.webgen.database.position.Groupposition;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 *
 * @author chtheis
 */
public class TestDatabase implements IDatabase {

    @Override
    public void closeConnection() {
        throw new UnsupportedOperationException("Not supported yet."); 
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
        throw new UnsupportedOperationException("Not supported yet."); 
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
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Competition[] readEvents(Timestamp datetime) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Competition[] readEvents(int cpType) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Competition[] readEvents(Timestamp datetime, int cpType) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Group readGroup(String cpName, String grName) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Group readGroup(Competition cp, String grName) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public List<Groupposition> readGroupposition(Group gr) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Group[] readGroups(Competition cp) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
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
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public List<List<Match>> readMatches(Group gr) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); 
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
