/* Copyright (C) 2020 Christoph Theis */
package de.webgen.generator;

// Setup a database for testing
public class WebGenTestClass {

    static public TestDatabase testdb;
    public static void setUpClass() {      
        testdb = new TestDatabase();
    }

    public static void tearDownClass() { 
        // System.out.println("Database tearDown");
    }

}