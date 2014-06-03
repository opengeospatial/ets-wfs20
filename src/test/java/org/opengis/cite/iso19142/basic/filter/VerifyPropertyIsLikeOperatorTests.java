package org.opengis.cite.iso19142.basic.filter;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import junit.framework.Assert;

import org.apache.xerces.xs.XSModel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.util.DataSampler;
import org.opengis.cite.iso19142.util.VerifyAppSchemaUtils;
import org.opengis.cite.validation.XSModelBuilder;
import org.opengis.cite.validation.XmlSchemaCompiler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.xml.sax.SAXException;

/**
 * Verifies the behavior of the PropertyIsLikeOperatorTests class.
 */
public class VerifyPropertyIsLikeOperatorTests {

    private static final String NS1 = "http://example.org/ns1";
    private static ITestContext testContext;
    private static ISuite suite;
    private static XSModel model;
    private static DataSampler dataSampler;

    public VerifyPropertyIsLikeOperatorTests() {
    }

    @BeforeClass
    public static void prepareMockTestContext() {
        testContext = mock(ITestContext.class);
        suite = mock(ISuite.class);
        when(testContext.getSuite()).thenReturn(suite);
        dataSampler = mock(DataSampler.class);
        when(suite.getAttribute(SuiteAttribute.SAMPLER.getName())).thenReturn(
                dataSampler);
    }

    @BeforeClass
    public static void buildSchemaModel() throws SAXException {
        URL entityCatalog = VerifyAppSchemaUtils.class
                .getResource("/schema-catalog.xml");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        InputStream xis = VerifyAppSchemaUtils.class
                .getResourceAsStream("/xsd/simple.xsd");
        Schema schema = xsdCompiler.compileXmlSchema(new StreamSource(xis));
        model = XSModelBuilder.buildXMLSchemaModel(schema, NS1);
    }

    @Test
    public void generateStringPatternForLocationNameInSimpleFeature() {
        List<String> valueList = Arrays.asList("Haida Gwaii",
                "Queen Charlotte Islands");
        when(
                suite.getAttribute(org.opengis.cite.iso19136.SuiteAttribute.XSMODEL
                        .getName())).thenReturn(model);
        when(
                dataSampler.getSimplePropertyValues(Matchers.any(QName.class),
                        Matchers.any(QName.class), Matchers.anyString()))
                .thenReturn(valueList);
        QName featureType = new QName(NS1, "SimpleFeature");
        PropertyIsLikeOperatorTests iut = new PropertyIsLikeOperatorTests();
        iut.initQueryFilterFixture(testContext);
        Map<QName, String> prop = iut
                .generateMatchingStringPattern(featureType);
        Assert.assertFalse(
                "Expected to find string property value pattern for SimpleFeature.",
                prop.isEmpty());
        Entry<QName, String> entry = prop.entrySet().iterator().next();
        Assert.assertEquals("Unexpected property name", "locationName", entry
                .getKey().getLocalPart());
        Assert.assertEquals("Unexpected pattern.", "*ida Gwaii",
                entry.getValue());
    }

    @Test
    public void generateStringPatternForLocationCodeInComplexFeature() {
        List<String> valueList = Arrays.asList("CA-BC", "CA-AB");
        when(
                suite.getAttribute(org.opengis.cite.iso19136.SuiteAttribute.XSMODEL
                        .getName())).thenReturn(model);
        when(
                dataSampler.getSimplePropertyValues(Matchers.any(QName.class),
                        Matchers.any(QName.class), Matchers.anyString()))
                .thenReturn(valueList);
        QName featureType = new QName(NS1, "ComplexFeature");
        PropertyIsLikeOperatorTests iut = new PropertyIsLikeOperatorTests();
        iut.initQueryFilterFixture(testContext);
        Map<QName, String> prop = iut
                .generateMatchingStringPattern(featureType);
        Assert.assertFalse(
                "Expected to find string property value pattern for ComplexFeature.",
                prop.isEmpty());
        Entry<QName, String> entry = prop.entrySet().iterator().next();
        Assert.assertEquals("Unexpected property name", "locationCode", entry
                .getKey().getLocalPart());
        Assert.assertEquals("Unexpected pattern.", "*-BC", entry.getValue());
    }
}
