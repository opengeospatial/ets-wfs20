package org.opengis.cite.iso19142.transaction;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.util.AppSchemaUtils;
import org.opengis.cite.iso19142.util.DataSampler;
import org.opengis.cite.validation.XSModelBuilder;
import org.opengis.cite.validation.XmlSchemaCompiler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.xml.sax.SAXException;

/**
 * Verifies the behavior of the Update test class.
 */
public class VerifyUpdate {

    private static final String NS1 = "http://example.org/ns1";
    private static ITestContext testContext;
    private static ISuite suite;
    private static XSModel model;
    private static DataSampler dataSampler;

    @BeforeClass
    public static void mockTestContext() throws ParserConfigurationException {
        testContext = mock(ITestContext.class);
        suite = mock(ISuite.class);
        when(testContext.getSuite()).thenReturn(suite);
        dataSampler = mock(DataSampler.class);
        when(suite.getAttribute(SuiteAttribute.SAMPLER.getName())).thenReturn(
                dataSampler);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.newDocumentBuilder();
    }

    @BeforeClass
    public static void buildSchemaModel() throws SAXException {
        URL entityCatalog = VerifyUpdate.class
                .getResource("/schema-catalog.xml");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        InputStream xis = VerifyUpdate.class
                .getResourceAsStream("/xsd/simple.xsd");
        Schema schema = xsdCompiler.compileXmlSchema(new StreamSource(xis));
        model = XSModelBuilder.buildXMLSchemaModel(schema, NS1);
    }

    @Test
    public void createNewStringEnumValue() throws SAXException, IOException {
        QName featureType = new QName(NS1, "ComplexFeature");
        List<XSElementDeclaration> simpleProps = AppSchemaUtils
                .getSimpleFeatureProperties(model, featureType);
        List<String> propValues = new ArrayList<String>();
        propValues.add("CA-AB");
        Update iut = new Update();
        String newVal = iut.newPropertyValue(
                simpleProps.get(simpleProps.size() - 1), propValues);
        assertEquals("CA-BC", newVal);
    }

    @Test
    public void createNewDateTimeValue() throws SAXException, IOException {
        QName featureType = new QName(NS1, "ComplexFeature");
        List<XSElementDeclaration> simpleProps = AppSchemaUtils
                .getSimpleFeatureProperties(model, featureType);
        List<String> propValues = new ArrayList<String>();
        propValues.add("2010-03-20T13:32:00-04:00");
        Update iut = new Update();
        String newVal = iut.newPropertyValue(
                simpleProps.get(simpleProps.size() - 2), propValues);
        assertTrue(String.format("Expected %s isBeforeNow", newVal),
                new DateTime(newVal).isBeforeNow());
    }

    @Test
    public void createNewDoubleValue() throws SAXException, IOException {
        QName featureType = new QName(NS1, "ComplexFeature");
        List<XSElementDeclaration> simpleProps = AppSchemaUtils
                .getSimpleFeatureProperties(model, featureType);
        List<String> propValues = new ArrayList<String>();
        propValues.add("49.25");
        Update iut = new Update();
        String newVal = iut.newPropertyValue(simpleProps.get(0), propValues);
        assertEquals(3.8969, Double.parseDouble(newVal), 0.00001);
    }
}
