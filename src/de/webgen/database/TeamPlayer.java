/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database;


public class TeamPlayer {
    public TeamPlayer(Competition cp) {
        tm = new Team(cp);
        pl = new Player();        
    }
    
    public int tmID;
    public int plID;
    public Team tm;
    public Player pl;
}
