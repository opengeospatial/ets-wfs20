package org.opengis.cite.iso19142.util;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.opengis.cite.iso19142.Namespaces;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Provides various utility methods for working with representations of temporal
 * values.
 */
public class TimeUtils {

    private static final DocumentBuilder DOC_BUILDER = initDocBuilder();

    private static DocumentBuilder initDocBuilder() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            TestSuiteLogger.log(Level.WARNING, "TimeUtils: Failed to create DocumentBuilder", e);
        }
        return builder;
    }

    /**
     * Builds a GML representation of a time interval delimited by the given end
     * points. The temporal reference system is ISO 8601.
     * 
     * @param startTime
     *            The starting instant.
     * @param endTime
     *            The ending instant.
     * @return A Document with gml:TimePeriod as the document element, or null
     *         if it cannot be created.
     */
    public static Document periodAsGML(ZonedDateTime startTime, ZonedDateTime endTime) {
        Document gmlTimePeriod;
        try {
            gmlTimePeriod = DOC_BUILDER.parse(TimeUtils.class.getResourceAsStream("TimePeriod.xml"));
        } catch (SAXException | IOException e) {
            return null;
        }
        String beginPosition = startTime.format(DateTimeFormatter.ISO_INSTANT);
        gmlTimePeriod.getElementsByTagNameNS(Namespaces.GML, "beginPosition").item(0).setTextContent(beginPosition);
        String endPosition = endTime.format(DateTimeFormatter.ISO_INSTANT);
        gmlTimePeriod.getElementsByTagNameNS(Namespaces.GML, "endPosition").item(0).setTextContent(endPosition);
        return gmlTimePeriod;
    }
}
