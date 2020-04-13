/* Copyright (C) 2020 Christoph Theis */

package de.webgen.xml;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Hilfsklasse mit statischen Methoden, die fuer den allgemeinen Umgang mit XML-Dokumenten
 * (DOM) nuetzlich sind.
 */
public class XMLUtilities {

    public static Document readXmlFile(String filename) {
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return docBuilder.parse(new File(filename));
        } catch (SAXException ex) {
            Logger.getLogger(XMLUtilities.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(XMLUtilities.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(XMLUtilities.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
    
    public static void writeXmlFile(Document doc, String filename) {
        try {
            // Prepare the DOM document for writing
            Source source = new DOMSource(doc);
            // Prepare the output file
            File file = new File(filename);
            Result result = new StreamResult(file);
            // Write the DOM document to the file
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);
        } catch (TransformerException ex) {
            Logger.getLogger(XMLUtilities.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** Liefert die Anzahl der Kindelemente (Node vom Typ <code>ELEMENT_NODE</code>) von <code>e</code>. */
    public static int getChildElementCount(Element e) {
        int count = 0;
        final NodeList nl = e.getChildNodes();
        for ( int i = nl.getLength()-1; i >= 0; i-- ) {
            if ( nl.item(i).getNodeType() == Node.ELEMENT_NODE ) count++;
        }

        return( count );
    }


    /**
     * Liefert den Wert eines Elements. <br>
     * Fuer einen Node vom Typ <code>Element</code> kann der Wert nicht mit <code>getValue()</code> abgefragt werden
     * (liefert null). Der Wert ist in den Kindnodes vom Typ <code>TEXT_NODE</code> enthalten. Die Whitespaces aufgrund der
     * textuellen Formatierung des XML-Dokuments sind auch mit enthalten (LF, CR, Blanks), diese werden von dieser Funktion
     * entfernt.
     */
    public static String getElementValue(Element e) {
        final StringBuffer sb = new StringBuffer();
        final NodeList nl = e.getChildNodes();

        // den Wert des Elements aus den TEXT_NODE's holen
        for ( int i = 0; i < nl.getLength(); i++ ) {
            Node n = nl.item( i );
            if ( n.getNodeType() == Node.TEXT_NODE )
                sb.append( n.getNodeValue().trim() );
        }

        return( sb.toString() );
    }


    /**
     * Liefert alle Kindelemente von <code>e</code> mit dem Tagnamen <code>tagName</code>, in der Reihenfolge ihrer Definition.
     * Es werden nur direkte Kinder der ersten Ebene beruecksichtigt.
     *
     * @return  Array der Kindelemente, leeres Array falls keine Kindelemente mit <code>tagName</code> in <code>e</code>.
     */
    public static List<Element> getChildElementsWithTagName(Element e, String tagName) {
        if ( tagName == null )
            throw new NullPointerException( "tagName==null" );

        final NodeList nl = e.getChildNodes();
        final int count = nl.getLength();
        final List<Element> eList = new java.util.ArrayList<>( count );
        for ( int i = 0; i < count; i++ ) {
            final Node child = nl.item( i );
            if ( (child.getNodeType() == Node.ELEMENT_NODE) && tagName.equals(child.getNodeName()) ) {
                eList.add( (Element) child );
            }
        }

        return eList;
    }
}
