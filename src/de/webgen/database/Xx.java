/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database;


public class Xx {
    public int    stID;
    public String grDesc;
    public int    grPos;
    
    @Override
    public String toString() {
        return grDesc == null ? "" : grDesc + "&nbsp;(" + grPos + ")";
    }
}
