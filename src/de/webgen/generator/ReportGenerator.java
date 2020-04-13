/* Copyright (C) 2020 Christoph Theis */

package de.webgen.generator;

import de.webgen.database.Database;
import de.webgen.database.match.Match;
import de.webgen.generator.report.Report;
import java.sql.SQLException;
import java.util.List;


public class ReportGenerator extends Generator {

    public ReportGenerator() {
    }


    @Override
    public String generate(List<List<Match>> matchList, Database database) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String generate(Report report, Database database) throws SQLException {
        StringBuilder sb = new StringBuilder();

        sb.append(report.generate(database));
        
        return sb.toString();
    }

}
