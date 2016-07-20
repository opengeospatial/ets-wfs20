package org.opengis.cite.iso19142.util;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalQueries;
import java.util.Date;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.xs.XSTypeDefinition;
import org.geotoolkit.temporal.factory.DefaultTemporalFactory;
import org.geotoolkit.temporal.object.DefaultPosition;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.temporal.TemporalFactory;
import org.opengis.temporal.TemporalGeometricPrimitive;
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

    /**
     * Creates a primitive temporal object from the given temporal value and
     * type definition.
     * 
     * @param value
     *            A string representation of a temporal value.
     * @param typeDefinition
     *            A simple type definition to which the value conforms
     *            (xsd:dateTime, xsd:date, xsd:gYear, xsd:gYearMonth).
     * @return A TemporalGeometricPrimitive instance (instant or period).
     */
    public static TemporalGeometricPrimitive parseTemporalValue(String value, XSTypeDefinition typeDefinition) {
        if (typeDefinition.getTypeCategory() != XSTypeDefinition.SIMPLE_TYPE) {
            throw new IllegalArgumentException("Not a simple type definition: " + typeDefinition.getName());
        }
        TemporalGeometricPrimitive tmPrimitive = null;
        TemporalFactory tmFactory = new DefaultTemporalFactory();
        XSSimpleType simpleType = XSSimpleType.class.cast(typeDefinition);
        switch (simpleType.getPrimitiveKind()) {
        case XSSimpleType.PRIMITIVE_DATETIME:
            ZonedDateTime dateTime = ZonedDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
            tmPrimitive = tmFactory.createInstant(new DefaultPosition(Date.from(dateTime.toInstant())));
            break;
        case XSSimpleType.PRIMITIVE_DATE:
            ZoneOffset zone = DateTimeFormatter.ISO_DATE.parse(value, TemporalQueries.offset());
            if (null == zone) {
                zone = ZonedDateTime.now().getOffset();
            }
            LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
            Temporal startOfDay = date.atStartOfDay(zone);
            // date is top-open interval (end at 23:59:59.999)
            Temporal endOfDay = startOfDay.plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.MILLIS);
            tmPrimitive = tmFactory.createPeriod(
                    tmFactory.createInstant(new DefaultPosition(Date.from(Instant.from(startOfDay)))),
                    tmFactory.createInstant(new DefaultPosition(Date.from(Instant.from(endOfDay)))));
            break;
        default:
            throw new IllegalArgumentException("Unsupported datatype: " + typeDefinition.getName());
        }
        return tmPrimitive;
    }
}
