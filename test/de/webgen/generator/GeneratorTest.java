/* Copyright (C) 2020 Christoph Theis */
package de.webgen.generator;

import de.webgen.database.Competition;
import de.webgen.database.Group;
import de.webgen.database.match.Match;
import de.webgen.database.match.SingleMatch;
import java.sql.SQLException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
        System.out.println(parser.getErrors());
    }
    
    @Test
    public void test_010_generateMatchItem() throws SQLException {
        Match mt = new SingleMatch();
        mt.gr = new Group();
        mt.gr.cp = new Competition();
        
        String html = Generator.generateMatchItem(mt, testdb, true);
        Document doc = Jsoup.parseBodyFragment(html);
        assertEquals(0L, parser.getErrors().size());
        System.out.println(doc.toString());
    }
    
    @Test
    public void test_999_dummy() {
        Document doc = Jsoup.parseBodyFragment("<div></div>");
        System.out.println(doc.toString());
        
        assertTrue(true);
    }
}
