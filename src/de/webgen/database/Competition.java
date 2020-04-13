/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Competition {

    public final static int CP_SINGLE = 1;
    public final static int CP_DOUBLE = 2;
    public final static int CP_MIXED  = 3;
    public final static int CP_TEAM   = 4;

    public static String getSelectString() {
        return "SELECT cpID, cpName, cpDesc, cpType FROM CpList ";
    }
    
    public Competition() {

    }

    public Competition(ResultSet rs) throws SQLException {
        int idx = 0;
        cpID = rs.getInt(++idx);
        cpName = Database.getString(rs, ++idx);
        cpDesc = Database.getString(rs, ++idx);
        cpType = rs.getInt(++idx);
    }

    public int compareTo(Competition cp) {
        return cpName.compareTo(cp.cpName);
    }

    @Override
    public String toString() {
        return cpDesc + " (" + cpName + ")";
    }

    public boolean isSingle() {
        return cpType == CP_SINGLE;
    }

    public boolean isDouble() {
        return cpType == CP_DOUBLE;
    }

    public boolean isMixed() {
        return cpType == CP_MIXED;
    }

    public boolean isTeam() {
        return cpType == CP_TEAM;
    }

    public String getFileName() {
        String fileName = cpName;
        fileName = fileName.replace('\\', '_');
        fileName = fileName.replace('/', '_');
        fileName = fileName.replace(':', '_');
        fileName = fileName.replace(' ', '_');

        return fileName;
    }

    public int    cpID;
    public String cpName;
    public String cpDesc;
    public int    cpType;
}
