package org.opengis.cite.iso19142.util;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.opengis.cite.iso19142.Namespaces;
import org.opengis.temporal.Period;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
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
     * Builds a GML representation of a time interval delimited by the given
     * time instants. The temporal reference system is ISO 8601 (UTC).
     * 
     * @param startDateTime
     *            The starting instant.
     * @param endDateTime
     *            The ending instant.
     * @return A Document with gml:TimePeriod as the document element, or null
     *         if it cannot be created.
     */
    public static Document intervalAsGML(ZonedDateTime startDateTime, ZonedDateTime endDateTime) {
        Document gmlTimePeriod;
        try {
            gmlTimePeriod = DOC_BUILDER.parse(TimeUtils.class.getResourceAsStream("TimePeriod.xml"));
        } catch (SAXException | IOException e) {
            return null;
        }
        String beginPosition = startDateTime.format(DateTimeFormatter.ISO_INSTANT);
        gmlTimePeriod.getElementsByTagNameNS(Namespaces.GML, "beginPosition").item(0).setTextContent(beginPosition);
        String endPosition = endDateTime.format(DateTimeFormatter.ISO_INSTANT);
        gmlTimePeriod.getElementsByTagNameNS(Namespaces.GML, "endPosition").item(0).setTextContent(endPosition);
        return gmlTimePeriod;
    }

    /**
     * Builds a GML representation of the given time period.
     * 
     * @param period
     *            A Period representing a temporal interval (UTC).
     * @return A Document with gml:TimePeriod as the document element.
     */
    public static Document periodAsGML(Period period) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
        String startOfPeriod = period.getBeginning().getPosition().getDateTime().toString();
        ZonedDateTime startDateTime = ZonedDateTime.parse(startOfPeriod, dateTimeFormatter);
        String endOfPeriod = period.getEnding().getPosition().getDateTime().toString();
        ZonedDateTime endDateTime = ZonedDateTime.parse(endOfPeriod, dateTimeFormatter);
        return intervalAsGML(startDateTime, endDateTime);
    }

    /**
     * Builds a GML representation of the given time period.
     * 
     * @param period
     *            A Period representing a temporal interval (UTC).
     * @return A Document with gml:TimePeriod as the document element.
     * Subtracts one day from beginning and end, see https://github.com/opengeospatial/ets-wfs20/issues/199.
     */
    public static Document periodAsGMLSubtractOneDay(Period period) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
        String startOfPeriod = period.getBeginning().getPosition().getDateTime().toString();
        ZonedDateTime startDateTime = ZonedDateTime.parse(startOfPeriod, dateTimeFormatter);
        startDateTime = startDateTime.minus(1, ChronoUnit.DAYS);
        String endOfPeriod = period.getEnding().getPosition().getDateTime().toString();
        ZonedDateTime endDateTime = ZonedDateTime.parse(endOfPeriod, dateTimeFormatter);
        endDateTime = endDateTime.minus(1, ChronoUnit.DAYS);
        return intervalAsGML(startDateTime, endDateTime);
    }

    /**
     * Builds a GML representation of a time instant with the specified
     * time-zone offset.
     * 
     * @param instant
     *            An instant representing a position in time.
     * @param offset
     *            A time-zone offset from UTC ('Z' if null).
     * @return A Document with gml:TimeInstant as the document element.
     */
    public static Document instantAsGML(org.opengis.temporal.Instant instant, ZoneOffset offset) {
        if (null == offset) {
            offset = ZoneOffset.UTC;
        }
        Document gmlTimeInstant;
        try {
            gmlTimeInstant = DOC_BUILDER.parse(TimeUtils.class.getResourceAsStream("TimeInstant.xml"));
        } catch (SAXException | IOException e) {
            return null;
        }
        OffsetDateTime tPos = instant.getPosition().getDate().toInstant().atOffset(offset);
        String timePositionValue = tPos.format( DateTimeFormatter.ISO_OFFSET_DATE_TIME );
        Node timePosition = gmlTimeInstant.getElementsByTagNameNS( Namespaces.GML, "timePosition" ).item( 0 );
        timePosition.setTextContent( timePositionValue );
        return gmlTimeInstant;
    }

    /**
     * Builds a GML representation of a time instant with the specified
     * time-zone offset.
     * 
     * @param instant
     *            An instant representing a position in time.
     * @param offset
     *            A time-zone offset from UTC ('Z' if null).
     * @return A Document with gml:TimeInstant as the document element.
     * Subtracts one day from the instant, see https://github.com/opengeospatial/ets-wfs20/issues/199.
     */
    public static Document instantAsGMLSubtractOneDay(org.opengis.temporal.Instant instant, ZoneOffset offset) {
        if (null == offset) {
            offset = ZoneOffset.UTC;
        }
        Document gmlTimeInstant;
        try {
            gmlTimeInstant = DOC_BUILDER.parse(TimeUtils.class.getResourceAsStream("TimeInstant.xml"));
        } catch (SAXException | IOException e) {
            return null;
        }
        OffsetDateTime tPos = ogcInstantToJavaInstantSubtractOneDay(instant).atOffset(offset);
        String timePositionValue = tPos.format( DateTimeFormatter.ISO_OFFSET_DATE_TIME );
        Node timePosition = gmlTimeInstant.getElementsByTagNameNS( Namespaces.GML, "timePosition" ).item( 0 );
        timePosition.setTextContent( timePositionValue );
        return gmlTimeInstant;
    }
    
    private static Instant ogcInstantToJavaInstantSubtractOneDay(org.opengis.temporal.Instant instant) {
    	return instant.getPosition().getDate().toInstant().minus(1, ChronoUnit.DAYS);
    }

	public static Document periodAsGMLAddOneDay(Period period) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
        String startOfPeriod = period.getBeginning().getPosition().getDateTime().toString();
        ZonedDateTime startDateTime = ZonedDateTime.parse(startOfPeriod, dateTimeFormatter);
        startDateTime = startDateTime.plus(1, ChronoUnit.DAYS);
        String endOfPeriod = period.getEnding().getPosition().getDateTime().toString();
        ZonedDateTime endDateTime = ZonedDateTime.parse(endOfPeriod, dateTimeFormatter);
        endDateTime = endDateTime.plus(1, ChronoUnit.DAYS);
        return intervalAsGML(startDateTime, endDateTime);
	}
}
