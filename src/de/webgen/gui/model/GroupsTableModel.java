/* Copyright (C) 2020 Christoph Theis */

package de.webgen.gui.model;

import de.webgen.database.Group;
import java.util.ResourceBundle;


public class GroupsTableModel extends javax.swing.table.AbstractTableModel {
    private static ResourceBundle bundle = ResourceBundle.getBundle("de/webgen/gui/resources/WebGen"); // NOI18N

    @Override
    public int getRowCount() {
        return groups == null ? 0 : groups.length;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0 :
                return bundle.getString("Name");

            case 1 :
                return bundle.getString("Stage");
        }

        return null;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (groups == null || groups.length <= rowIndex)
            return null;

        Group group = groups[rowIndex];

        switch (columnIndex) {
            case 0 :
                return group.grName;

            case 1 :
                return group.grStage;
        }

        return null;
    }

    public void setGroups(Group[] groups) {
        if (this.groups != null && this.groups.length > 0)
            fireTableRowsDeleted(0, this.groups.length - 1);

        this.groups = groups;

        if (this.groups != null && this.groups.length > 0)
            fireTableRowsInserted(0, this.groups.length - 1);

    }
    
    
    public Group[] getGroups() {
        return groups;
    }


    public Group getGroup(int row) {
        if (groups == null || groups.length <= row)
            return null;

        return groups[row];
    }

    Group[] groups;
}
