package org.opengis.cite.iso19142.basic.filter.temporal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.geotoolkit.temporal.factory.DefaultTemporalFactory;
import org.geotoolkit.temporal.object.DefaultPosition;
import org.opengis.cite.iso19136.util.XMLSchemaModelUtils;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.opengis.temporal.TemporalFactory;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Utility methods to facilitate the creation and verification of temporal
 * queries.
 */
public class TemporalQuery {

    /**
     * Extracts the values of the specified temporal property from the given XML
     * document.
     *
     * @param rspEntity
     *            A Document representing a WFS response entity containing
     *            feature instances.
     * @param timeProperty
     *            An element declaration for a temporal feature property.
     * @param model
     *            A representation of an application schema.
     * @return A sequence of (element) nodes that are either (a) simple
     *         properties with temporal values as text content, or b) complex
     *         temporal values that can substitute for
     *         gml:AbstractTimeGeometricPrimitive (e.g. gml:Instant,
     *         gml:TimePeriod).
     */
    public static List<Node> extractTemporalNodes(Document rspEntity, XSElementDeclaration timeProperty,
            XSModel model) {
        List<Node> temporalNodes = null;
        XSTypeDefinition timePropType = timeProperty.getTypeDefinition();
        if ( timePropType.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE
             || ( (XSComplexTypeDefinition) timePropType ).getContentType() == XSComplexTypeDefinition.CONTENTTYPE_SIMPLE ) {
            temporalNodes = WFSMessage.findMatchingElements(rspEntity, timeProperty);
        } else {
            // elements that substitute for gml:AbstractTimeGeometricPrimitive
            XSElementDeclaration gmlAbstractTimePrimitive = model
                    .getElementDeclaration("AbstractTimeGeometricPrimitive", Namespaces.GML);
            List<XSElementDeclaration> expectedValues = XMLSchemaModelUtils.getElementsByAffiliation(model,
                    gmlAbstractTimePrimitive);
            temporalNodes = WFSMessage.findMatchingElements(rspEntity,
                    expectedValues.toArray(new XSElementDeclaration[expectedValues.size()]));
        }
        return temporalNodes;
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
        if ( typeDefinition.getTypeCategory() != XSTypeDefinition.SIMPLE_TYPE
             && !( ( (XSComplexTypeDefinition) typeDefinition ).getContentType() == XSComplexTypeDefinition.CONTENTTYPE_SIMPLE ) ) {
            throw new IllegalArgumentException( "Not a simple type definition: " + typeDefinition.getName() );
        }
        TemporalGeometricPrimitive tmPrimitive = null;
        TemporalFactory tmFactory = new DefaultTemporalFactory();
        XSSimpleTypeDefinition simpleTypeDefinition;
        if ( typeDefinition.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE ) {
            simpleTypeDefinition = XSSimpleTypeDefinition.class.cast( typeDefinition );
        } else
            simpleTypeDefinition = ( (XSComplexTypeDefinition) typeDefinition ).getSimpleType();

        switch ( simpleTypeDefinition.getBuiltInKind() ) {
        case XSConstants.DATETIME_DT:
            DateTimeFormatter xsdDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX]");
            TemporalAccessor tm = xsdDateTimeFormatter.parseBest(value, ZonedDateTime::from, LocalDateTime::from);
            if (tm instanceof LocalDateTime) {
                // set local time zone
                tm = LocalDateTime.class.cast(tm).atZone(ZonedDateTime.now().getOffset());
            }
            ZonedDateTime dateTime = (ZonedDateTime) tm;
            tmPrimitive = tmFactory.createInstant(new DefaultPosition(Date.from(dateTime.toInstant())));
            break;
        case XSConstants.DATE_DT:
            ZoneOffset zone = DateTimeFormatter.ISO_DATE.parse(value, TemporalQueries.offset());
            if (null == zone) {
                zone = ZonedDateTime.now().getOffset();
            }
            LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
            Temporal startOfDay = date.atStartOfDay(zone);
            // date is top-open interval (ends at 23:59:59.999)
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
