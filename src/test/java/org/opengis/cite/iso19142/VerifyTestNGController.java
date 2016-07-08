package org.opengis.cite.iso19142;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Verifies the results of executing a test run using the main controller
 * (TestNGController).
 */
public class VerifyTestNGController {

    private static DocumentBuilder docBuilder;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void initParser() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        docBuilder = dbf.newDocumentBuilder();
    }

    @Test
    public void etsCode() throws Exception {
        TestNGController controller = new TestNGController();
        String etsCode = controller.getCode();
        assertEquals("Unexpected ETS code.", "wfs20", etsCode);
    }

    @Test
    public void missingArgument() throws URISyntaxException, IOException, SAXException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("'iut' or 'wfs' must be present");
        Properties testRunProps = new Properties();
        URL sut = getClass().getResource("/wfs/capabilities-acme.xml");
        testRunProps.setProperty("sut", sut.toURI().toString());
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);
        testRunProps.storeToXML(outStream, "Integration test");
        Document testRunArgs = docBuilder.parse(new ByteArrayInputStream(outStream.toByteArray()));
        TestNGController controller = new TestNGController();
        controller.validateTestRunArgs(testRunArgs);
    }
}
