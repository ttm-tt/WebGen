/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;


public class Group {

    public static final int MOD_RR = 1;
    public static final int MOD_SKO = 2;
    public static final int MOD_DKO = 3;
    public static final int MOD_PLO = 4;

    public static String getSelectString() {
        return
            "SELECT grID, grName, grDesc, grStage, grModus, grSize, grWinner, gr.cpID, " +
            "       grQualRounds, grNofRounds, grNofMatches, grNoThirdPlace, grOnlyThirdPlace,  " +
            "       grPublished, grNotes, grSortOrder " +
            "  FROM GrList gr INNER JOIN CpList cp ON gr.cpID = cp.cpID ";
    }

    public Group() {

    }


    public Group(Competition cp, ResultSet rs) throws SQLException {
        this.cp = cp;

        int idx = 0;
        grID = rs.getInt(++idx);
        grName = Database.getString(rs, ++idx);
        grDesc = Database.getString(rs, ++idx);
        grStage = Database.getString(rs, ++idx);
        grModus = rs.getInt(++idx);
        grSize = rs.getInt(++idx);
        grWinner = rs.getInt(++idx);
        cpID = rs.getInt(++idx);
        grQualRounds = rs.getInt(++idx);
        grNofRounds = rs.getInt(++idx);
        grNofMatches = rs.getInt(++idx);
        grNoThirdPlace = rs.getInt(++idx) != 0;
        grOnlyThirdPlace = rs.getInt(++idx) != 0;
        grPublished = rs.getInt(++idx) != 0;
        grNotes = Database.getString(rs, ++idx);
        grSortOrder = rs.getInt(++idx);
        
        // Remove comments (starting with '#' until EOL)
        if (grNotes != null) {
            grNotes = grNotes.replaceAll("#[^\n]*[\n]?", "").trim();
            if (grNotes.isEmpty())
                grNotes = null;
        }
    }

    public int compareTo(Group g) {
        int ret = 0;
        if (ret == 0)
            ret = cp.compareTo(g.cp);
        if (ret == 0)
            ret =  -(grSortOrder - g.grSortOrder);
        if (ret == 0)
            ret = grStage.compareTo(g.grStage);
        if (ret == 0)
            ret = grName.compareTo(g.grName);

        return ret;
    }

    public String getFileName() {
        String fileName = cp.cpName + "_" + grName;
        fileName = fileName.replace('\\', '_');
        fileName = fileName.replace('/', '_');
        fileName = fileName.replace(':', '_');
        fileName = fileName.replace(' ', '_');

        return fileName;
    }

    public int      grID;
    public String   grName;
    public String   grDesc;
    public String   grStage;
    public int      grModus;
    public int      grSize;
    public int      grWinner;
    public int      cpID;
    public int      grQualRounds;
    public int      grNofRounds;
    public int      grNofMatches;
    public boolean  grNoThirdPlace;
    public boolean  grOnlyThirdPlace;
    public boolean  grPublished;
    public String   grNotes;
    public int      grSortOrder;

    public Competition cp;
    public Timestamp   timestamp;
    
    public boolean selected;
}
