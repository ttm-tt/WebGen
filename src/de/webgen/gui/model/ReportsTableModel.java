/* Copyright (C) 2020 Christoph Theis */

package de.webgen.gui.model;

import de.webgen.generator.report.Report;

public class ReportsTableModel extends javax.swing.table.AbstractTableModel {
    @Override
    public int getRowCount() {
        return reports == null ? 0 : reports.length;
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (reports.length <= rowIndex)
            return null;

        return reports[rowIndex].toString();
    }

    public void setReports(Report[] reports) {
        if (this.reports != null && this.reports.length > 0)
            fireTableRowsDeleted(0, this.reports.length - 1);

        this.reports = reports;

        if (this.reports != null && this.reports.length > 0)
            fireTableRowsInserted(0, this.reports.length - 1);
    }
    
    
    public Report[] getReports() {
        return reports;
    }


    public Report getReport(int row) {
        if (reports == null || reports.length <= row)
            return null;

        return reports[row];
    }

    public Report getReport(Report.ReportType type) {
        if (reports == null)
            return null;

        for (int i = 0; i < reports.length; i++) {
            if (reports[i].getType().equals(type))
                return reports[i];
        }

        return null;
    }

    Report[] reports;
}
