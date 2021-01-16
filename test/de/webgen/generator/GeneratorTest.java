/* Copyright (C) 2020 Christoph Theis */
package de.webgen.generator;

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


public class GeneratorTest extends WebGenTestClass {
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
    public void test_010_generateMatchItem() throws SQLException {
        Competition cp = testdb.readEvents()[0];
        Group gr = testdb.readGroups(cp)[0];
        
        List<List<Match>> matches = testdb.readMatches(gr);
        Match mt = matches.get(0).get(0);
        
        String html = Generator.generateMatchItem(mt, testdb, true);
        // Our html string is a row in a table, to validate we need to add 
        // that wrapper around. Everything else can be done by Jsoup.
        Document doc = Jsoup.parse("<table>" + html + "</table>", "", parser);
        // System.out.println(html);
        // System.out.println(doc.toString());
        assertEquals(0L, parser.getErrors().size());
    }
    
    
    @Test
    public void test_020_generateMatchList() throws SQLException {
        Competition cp = testdb.readEvents()[0];
        Group gr = testdb.readGroups(cp)[0];
        
        List<List<Match>> matches = testdb.readMatches(gr);
        
        String html = Generator.generateMatchList(matches.get(0), testdb);
        // Our html string is a body in a table, to validate we need to add 
        // that wrapper around. Everything else can be done by Jsoup.
        Document doc = Jsoup.parse("<table>" + html + "</table>", "", parser);
        // System.out.println(html);
        // System.out.println(doc.toString());
        assertEquals(0L, parser.getErrors().size());
    }
    
    
    
    @Test
    public void test_999_dummy() {
        Document doc = Jsoup.parse("<div><span></span></div>", "", parser);
        assertEquals(0L, parser.getErrors());
        // System.err.println(parser.getErrors());
        // System.out.println(doc.toString());
        
        assertTrue(true);
    }
}
