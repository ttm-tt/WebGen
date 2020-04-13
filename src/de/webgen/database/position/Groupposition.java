/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database.position;

import de.webgen.database.Xx;


abstract public class Groupposition {
    public int tmID;
    public int stID;
    public int stNr;
    public int stPos;

    public int mtMatchPoints;
    public int mtMatchCount;
    public int mtPointsA;
    public int mtPointsX;
    public int mtMatchesA;
    public int mtMatchesX;
    public int mtSetsA;
    public int mtSetsX;
    public int mtBallsA;
    public int mtBallsX;
    
    public Xx  xx = new Xx();

    abstract public String getEntry();
}
