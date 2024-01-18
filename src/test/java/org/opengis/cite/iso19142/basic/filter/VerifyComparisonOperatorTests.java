package org.opengis.cite.iso19142.basic.filter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSTypeDefinition;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.iso19142.FES2;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.util.DataSampler;
import org.opengis.cite.iso19142.util.VerifyAppSchemaUtils;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.opengis.cite.validation.XSModelBuilder;
import org.opengis.cite.validation.XmlSchemaCompiler;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.ISuite;
import org.testng.ITestContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Verifies the behavior of the ComparisonOperatorTests class.
 */
public class VerifyComparisonOperatorTests {

    private static final String NS1 = "http://example.org/ns1";
    private static ITestContext testContext;
    private static ISuite suite;
    private static XSModel model;
    private static DataSampler dataSampler;
    private static DocumentBuilder docBuilder;

    public VerifyComparisonOperatorTests() {
    }

    @BeforeClass
    public static void prepareMockTestContext() throws ParserConfigurationException {
        testContext = mock(ITestContext.class);
        suite = mock(ISuite.class);
        when(testContext.getSuite()).thenReturn(suite);
        dataSampler = mock(DataSampler.class);
        when(suite.getAttribute(SuiteAttribute.SAMPLER.getName())).thenReturn(dataSampler);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        docBuilder = dbf.newDocumentBuilder();
    }

    @BeforeClass
    public static void buildSchemaModel() throws SAXException {
        URL entityCatalog = VerifyAppSchemaUtils.class.getResource("/schema-catalog.xml");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        InputStream xis = VerifyAppSchemaUtils.class.getResourceAsStream("/xsd/simple.xsd");
        Schema schema = xsdCompiler.compileXmlSchema(new StreamSource(xis));
        model = XSModelBuilder.buildXMLSchemaModel(schema, NS1);
    }

    @Test
    public void findNumericPropertyOfSimpleFeature() {
        QName featureType = new QName( NS1, "SimpleFeature" );
        when( suite.getAttribute( org.opengis.cite.iso19136.SuiteAttribute.XSMODEL.getName() ) ).thenReturn( model );

        List<String> integerValueList = Arrays.asList( "600", "100", "200" );
        QName integerPropName = new QName( NS1, "intProperty2" );
        when( dataSampler.getSimplePropertyValues( eq( featureType ), eq( integerPropName ), anyString() ) ).thenReturn( integerValueList );

        List<String> doubleValueList = Arrays.asList( "600.68", "100.47", "200.54" );
        QName doublePropName = new QName( NS1, "measurand" );
        when( dataSampler.getSimplePropertyValues( eq( featureType ), eq( doublePropName ), anyString() ) ).thenReturn( doubleValueList );

        ComparisonOperatorTests iut = new ComparisonOperatorTests();
        iut.initQueryFilterFixture( testContext );
        Set<XSTypeDefinition> dataTypes = iut.getNumericDataTypes( model );
        Map<XSElementDeclaration, String[]> prop = iut.findFeaturePropertyValue( model, featureType, dataTypes );
        assertFalse( "Expected to find numeric property for SimpleFeature.", prop.isEmpty() );
        Entry<XSElementDeclaration, String[]> propRange = prop.entrySet().iterator().next();
        String[] range = propRange.getValue();
        assertThat( "Unexpected maximum value.", range[range.length - 1], anyOf( is( "600" ), is( "600.68" ) ) );
    }

    @Test
    public void addPropertyIsGreaterThanPredicate() throws SAXException, IOException {
        Document reqEntity = docBuilder
                .parse(this.getClass().getResourceAsStream("/GetFeature/GetFeature-Minimal.xml"));
        QName featureType = new QName(NS1, "SimpleFeature");
        WFSMessage.appendSimpleQuery(reqEntity, featureType);
        QName propName = new QName(NS1, "decimalProperty");
        String literalValue = "122.6";
        ComparisonOperatorTests iut = new ComparisonOperatorTests();
        iut.addComparisonPredicate(reqEntity, FES2.GREATER_THAN, propName, literalValue, true, null);
        Element predicate = (Element) reqEntity.getElementsByTagNameNS(Namespaces.FES, FES2.GREATER_THAN).item(0);
        assertEquals("Unexpected Literal value.", literalValue, predicate.getFirstChild().getTextContent());
    }

    @Test
    public void sortNumericValues() {
        String[] values = new String[] { "0.8", "1.314E+1", "-100.5" };
        ComparisonOperatorTests iut = new ComparisonOperatorTests();
        iut.sortValues(values);
        assertEquals("Unexpected values[0].", "-100.5", values[0]);
    }

    @Test
    public void sortDateTimeValues() {
        String[] values = new String[] { "2012-12-12T17:00:00+04:00", "2012-12-12T10:00:00-08:00",
                "2012-12-12T17:00:00Z" };
        ComparisonOperatorTests iut = new ComparisonOperatorTests();
        iut.sortValues(values);
        String latestValue = values[values.length - 1];
        ZonedDateTime latest = ZonedDateTime.parse(latestValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        ZonedDateTime expected = ZonedDateTime.parse("2012-12-12T18:00:00Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertTrue("Expected latest to be 2012-12-12T18:00:00Z", latest.isEqual(expected));
    }

    @Test
    public void sortDateValues() {
        String[] values = new String[] { "2011-12-31Z", "2011-12-01Z", "2011-12-10Z" };
        ComparisonOperatorTests iut = new ComparisonOperatorTests();
        iut.sortValues(values);
        String latestValue = values[values.length - 1];
        assertEquals("2011-12-31Z", latestValue);
    }

    @Test
    public void calculateIntegerRange() {
        String[] values = new String[] { "9", "-2", "7" };
        ComparisonOperatorTests iut = new ComparisonOperatorTests();
        String[] range = iut.calculateRange(values, new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "integer"));
        assertEquals("Unexpected min value.", -2, Integer.parseInt(range[0]));
    }

    @Test
    public void calculateDecimalRange() {
        String[] values = new String[] { "6919880000", "3571970000", "964773000", "45401800" };
        ComparisonOperatorTests iut = new ComparisonOperatorTests();
        String[] range = iut.calculateRange(values, new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "decimal"));
        assertEquals("Unexpected min value.", "45401800", range[0]);
        assertEquals("Unexpected max value.", 6919880000d, Double.parseDouble(range[1]), 0);
    }

    @Test
    public void sortNumericValuesWithExponents() {
        String[] values = new String[] { "0.8", "1.20528E5", "1.20528E3" };
        ComparisonOperatorTests iut = new ComparisonOperatorTests();
        iut.sortValues(values);
        assertEquals("Unexpected max value.", 120528, Double.parseDouble(values[values.length - 1]), 0);
    }
}
