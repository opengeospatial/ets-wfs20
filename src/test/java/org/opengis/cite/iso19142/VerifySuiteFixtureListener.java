package org.opengis.cite.iso19142;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.testng.ISuite;
import org.testng.xml.XmlSuite;
import org.w3c.dom.Document;

public class VerifySuiteFixtureListener {

    private static XmlSuite xmlSuite;
    private static ISuite suite;

    public VerifySuiteFixtureListener() {
    }

    @BeforeClass
    public static void setUpClass() {
        xmlSuite = mock(XmlSuite.class);
        suite = mock(ISuite.class);
        when(suite.getXmlSuite()).thenReturn(xmlSuite);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test(expected = IllegalArgumentException.class)
    public void noSuiteParameters() {
        Map<String, String> params = new HashMap<String, String>();
        when(xmlSuite.getParameters()).thenReturn(params);
        SuiteFixtureListener iut = new SuiteFixtureListener();
        iut.onStart(suite);
    }

    @Test
    public void processWFSParameter() throws URISyntaxException {
        URL url = this.getClass().getResource("/capabilities-simple.xml");
        Map<String, String> params = new HashMap<String, String>();
        params.put(TestRunArg.WFS.toString(), url.toURI().toString());
        when(xmlSuite.getParameters()).thenReturn(params);
        SuiteFixtureListener iut = new SuiteFixtureListener();
        iut.onStart(suite);
        verify(suite).setAttribute(
                Matchers.eq(SuiteAttribute.TEST_SUBJECT.getName()),
                Matchers.isA(Document.class));
    }
}
