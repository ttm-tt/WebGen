/* Copyright (C) 2020 Christoph Theis */
package de.webgen.generator;

import de.webgen.WebGenTestClass;
import de.webgen.database.Competition;
import de.webgen.database.Group;
import de.webgen.database.match.Match;
import java.sql.SQLException;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


public class KOGeneratorTest extends WebGenTestClass {
    protected Parser parser;
    
    @BeforeClass
    public static void setUpClass() {
        WebGenTestClass.setUpClass();
    }
    
    @AfterClass
    public static void tearDownClass() {
        WebGenTestClass.tearDownClass();
    }
    
    @Before
    public void setUp() {
        parser = Parser.htmlParser();
        parser.setTrackErrors(10);
    }
    
    @After
    public void tearDown() {
        // System.out.println(parser.getErrors());
    }
    
    @Test
    public void test_010_generate() throws SQLException {
        Group gr = testdb.readGroup("TEST", "KO");
        
        List<List<Match>> matches = testdb.readMatches(gr);
        
        String html = new KOGenerator().generate(matches, testdb);
        Document doc = Jsoup.parse(html, "", parser);
        // System.out.println(html);
        // System.out.println(doc.toString());
        System.out.println(parser.getErrors());
        assertEquals(0L, parser.getErrors().size());
    }
}
