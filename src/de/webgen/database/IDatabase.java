/* Copyright (C) 2020 Christoph Theis */
package de.webgen.database;

import de.webgen.database.match.Match;
import de.webgen.database.position.Groupposition;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 *
 * @author chtheis
 */
public interface IDatabase {

    void closeConnection();

    List<DoublePlayer> getDoubles(Competition cp) throws SQLException;

    Timestamp getEntryTimestamp(int type) throws SQLException;

    List<Timestamp> getMatchDates(Group gr) throws SQLException;

    Timestamp getMatchTimestamp(String date) throws SQLException;

    Timestamp getMatchTimestamp(Group gr, String date) throws SQLException;

    List<SinglePlayer> getPartnerWanted(Competition cp) throws SQLException;

    Timestamp getPlayerTimestamp() throws SQLException;

    List<Player> getPlayers() throws SQLException;

    List<SinglePlayer> getSingles(Competition cp) throws SQLException;

    int getTableType() throws SQLException;

    List<TeamPlayer> getTeamPlayers(Competition cp) throws SQLException;

    List<Team> getTeams(Competition cp) throws SQLException;

    String getTitle() throws SQLException;

    Association[] readAssociations() throws SQLException;

    // Read groups with played matches
    Group[] readCurrentGroups() throws SQLException;

    Competition readEvent(String cpName) throws SQLException;

    Competition[] readEvents() throws SQLException;

    Competition[] readEvents(Timestamp datetime) throws SQLException;

    Competition[] readEvents(int cpType) throws SQLException;

    Competition[] readEvents(Timestamp datetime, int cpType) throws SQLException;

    Group readGroup(String cpName, String grName) throws SQLException;

    Group readGroup(Competition cp, String grName) throws SQLException;

    List<Groupposition> readGroupposition(Group gr) throws SQLException;

    Group[] readGroups(Competition cp) throws SQLException;

    Group[] readGroups(String date) throws SQLException;

    List<Match> readIndividualMatches(Match teamMatch) throws SQLException;

    List<Match> readMatches(Group gr, String date) throws SQLException;

    List<List<Match>> readMatches(Group gr) throws SQLException;

    List<Match> readPlayersMatches(int plNr) throws SQLException;

    Group[] readPublishedGroups() throws SQLException;

    List<Match> readTeamMatches(int tmID) throws SQLException;

    boolean testConnection();

    void updateTimestamp(Group group) throws SQLException;
    
}
