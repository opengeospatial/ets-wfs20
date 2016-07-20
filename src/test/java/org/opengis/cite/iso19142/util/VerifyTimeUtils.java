package org.opengis.cite.iso19142.util;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSTypeDefinition;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.cite.validation.XSModelBuilder;
import org.opengis.cite.validation.XmlSchemaCompiler;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalGeometricPrimitive;

public class VerifyTimeUtils {

    private static final String EX_NS = "http://example.org/ns1";
    private static XSModel model;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void initModel() throws Exception {
        URL entityCatalog = VerifyAppSchemaUtils.class.getResource("/schema-catalog.xml");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        InputStream xis = VerifyAppSchemaUtils.class.getResourceAsStream("/xsd/simple.xsd");
        Schema schema = xsdCompiler.compileXmlSchema(new StreamSource(xis));
        model = XSModelBuilder.buildXMLSchemaModel(schema, EX_NS);
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
}
