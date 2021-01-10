/* Copyright (C) 2020 Christoph Theis */

package de.webgen.generator.report;

import de.webgen.database.IDatabase;
import java.sql.SQLException;


abstract public class Report {
    
    protected static final String SEP = System.getProperty("line.separator");

    public static Report getReportByType(int type) {
        if (type < 0 || type >= ReportType.values().length)
            return null;

        return getReportByType(ReportType.values()[type]);
    }

    public static Report getReportByType(ReportType type) {
        switch (type) {
            case Players :
                return new PlayersReport(type);

            case Singles :
                return new SinglesReport(type);

            case Doubles :
                return new DoublesReport(type);

            case Mixed :
                return new MixedReport(type);

            case Teams :
                return new TeamsReport(type);
                
            case PartnerWanted :
                return new PartnerWantedReport(type);
        }

        return null;
    }

    public enum ReportType {
        Players,
        Singles,
        Doubles,
        Mixed,
        Teams,
        PartnerWanted
    }

    public Report(ReportType type) {
        this.type = type;
    }

    abstract public String generate(IDatabase database) throws SQLException;

    @Override
    public String toString() {
        return type.toString();
    }

    public int compareTo(Report r) {
        return type.ordinal() - r.type.ordinal();
    }


    public ReportType getType() {
        return type;
    }

    public String getFileName() {
        return type.toString().toLowerCase();
    }
    
    public abstract java.sql.Timestamp getTimestamp(IDatabase database) throws java.sql.SQLException;

    public boolean selected = false;

    protected ReportType type;
}
