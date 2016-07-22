package org.opengis.cite.iso19142.util;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSTypeDefinition;
import org.geotoolkit.temporal.factory.DefaultTemporalFactory;
import org.geotoolkit.temporal.object.DefaultPosition;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.validation.XSModelBuilder;
import org.opengis.cite.validation.XmlSchemaCompiler;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalFactory;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class VerifyTimeUtils {

    private static final String EX_NS = "http://example.org/ns1";
    private static TemporalFactory tmFactory;
    private static XSModel model;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void initFixture() throws Exception {
        URL entityCatalog = VerifyAppSchemaUtils.class.getResource("/schema-catalog.xml");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        InputStream xis = VerifyAppSchemaUtils.class.getResourceAsStream("/xsd/simple.xsd");
        Schema schema = xsdCompiler.compileXmlSchema(new StreamSource(xis));
        model = XSModelBuilder.buildXMLSchemaModel(schema, EX_NS);
        tmFactory = new DefaultTemporalFactory();
    }

    @Test
    public void parseDateTime() {
        XSTypeDefinition typeDef = model.getTypeDefinition("dateTime", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        TemporalGeometricPrimitive result = TimeUtils.parseTemporalValue("2016-05-15T12:00:00Z", typeDef);
        assertTrue("Expected result: " + Instant.class.getName(), Instant.class.isInstance(result));
        Instant instant = Instant.class.cast(result);
        assertTrue(instant.getPosition().getDateTime().toString().startsWith("2016-05"));
    }

    @Test
    public void parseDate() {
        XSTypeDefinition typeDef = model.getTypeDefinition("date", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        TemporalGeometricPrimitive result = TimeUtils.parseTemporalValue("2016-05-15", typeDef);
        assertTrue("Expected result: " + Period.class.getName(), Period.class.isInstance(result));
        Period period = Period.class.cast(result);
        assertTrue(period.getBeginning().getPosition().getDateTime().toString().startsWith("2016-05-15T00:00:00"));
    }

    @Test
    public void parseDateWithOffset() {
        XSTypeDefinition typeDef = model.getTypeDefinition("date", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        TemporalGeometricPrimitive result = TimeUtils.parseTemporalValue("2016-05-15-04:00", typeDef);
        assertTrue("Expected result: " + Period.class.getName(), Period.class.isInstance(result));
        Period period = Period.class.cast(result);
        DateTimeFormatter xsdDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        ZonedDateTime actualStart = ZonedDateTime.parse(period.getBeginning().getPosition().getDateTime().toString(),
                xsdDateTimeFormatter);
        ZonedDateTime expectedStart = ZonedDateTime.parse("2016-05-15T00:00:00-04:00");
        assertTrue(actualStart.isEqual(expectedStart));
    }

    @Test
    public void periodInUTCAsGML() {
        ZonedDateTime t1 = ZonedDateTime.of(2016, 05, 3, 10, 15, 30, 0, ZoneId.of("Z"));
        Instant startPeriod = tmFactory.createInstant(new DefaultPosition(Date.from(t1.minusMonths(1).toInstant())));
        Instant endPeriod = tmFactory.createInstant(new DefaultPosition(Date.from(t1.plusMonths(1).toInstant())));
        Period period = tmFactory.createPeriod(startPeriod, endPeriod);
        Document doc = TimeUtils.periodAsGML(period);
        Node endPosition = doc.getElementsByTagNameNS(Namespaces.GML, "endPosition").item(0);
        assertTrue("Expected end date 2016-06-03", endPosition.getTextContent().startsWith("2016-06-03"));
    }

    @Test
    public void periodWithOffsetAsGML() {
        ZonedDateTime t1 = ZonedDateTime.of(2016, 05, 3, 10, 15, 30, 0, ZoneOffset.of("-07:00"));
        Instant startPeriod = tmFactory.createInstant(new DefaultPosition(Date.from(t1.minusMonths(1).toInstant())));
        Instant endPeriod = tmFactory.createInstant(new DefaultPosition(Date.from(t1.plusMonths(1).toInstant())));
        Period period = tmFactory.createPeriod(startPeriod, endPeriod);
        Document doc = TimeUtils.periodAsGML(period);
        Node beginPosition = doc.getElementsByTagNameNS(Namespaces.GML, "beginPosition").item(0);
        assertTrue("Expected begin time 17:15:30Z", beginPosition.getTextContent().endsWith("17:15:30Z"));
    }
}
