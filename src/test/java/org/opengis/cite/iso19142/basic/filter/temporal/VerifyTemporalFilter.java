package org.opengis.cite.iso19142.basic.filter.temporal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class VerifyTemporalFilter {

    private static ITestContext testContext;
    private static ISuite suite;
    private static DocumentBuilder docBuilder;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUpClass() throws Exception {
        testContext = mock(ITestContext.class);
        suite = mock(ISuite.class);
        when(testContext.getSuite()).thenReturn(suite);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        docBuilder = dbf.newDocumentBuilder();
    }

    @Test
    public void implementsMinTemporalFilter() throws SAXException, IOException {
        Document doc = docBuilder.parse(getClass().getResourceAsStream("/wfs/capabilities-acme.xml"));
        when(suite.getAttribute(SuiteAttribute.TEST_SUBJECT.getName())).thenReturn(doc);
        TemporalFilter iut = new TemporalFilter();
        iut.implementsMinimumTemporalFilter(testContext);
    }

    @Test
    public void doesNotImplementMinTemporalFilter() throws SAXException, IOException {
        thrown.expect(SkipException.class);
        thrown.expectMessage("Capability not implemented");
        Document doc = docBuilder.parse(getClass().getResourceAsStream("/capabilities-simple.xml"));
        when(suite.getAttribute(SuiteAttribute.TEST_SUBJECT.getName())).thenReturn(doc);
        TemporalFilter iut = new TemporalFilter();
        iut.implementsMinimumTemporalFilter(testContext);
    }

}
