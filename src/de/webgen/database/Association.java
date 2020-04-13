/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Association {
    public String naName;
    public String naDesc;
    
    public static String getSelectString() {
        return "SELECT naName, naDesc FROM NaList ";
    }
    
    public Association(ResultSet rs) throws SQLException {
        naName = Database.getString(rs, 1);
        naDesc = Database.getString(rs, 2);
    }
}
