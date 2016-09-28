package org.opengis.cite.iso19142.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.iso19142.Namespaces;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;

/**
 * Verifies the behavior of the XMLUtils class.
 */
public class VerifyXMLUtils {

    private static final String ATOM_NS = "http://www.w3.org/2005/Atom";
    private static final String EX_NS = "http://example.org/ns1";
    private static DocumentBuilder docBuilder;

    public VerifyXMLUtils() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        docBuilder = dbf.newDocumentBuilder();
    }

    @Test
    public void writeDocToString() throws SAXException, IOException {
        Document doc = docBuilder.parse(this.getClass().getResourceAsStream("/atom-feed.xml"));
        String content = XMLUtils.writeNodeToString(doc);
        assertTrue("String should start with '<feed'", content.startsWith("<feed"));
    }

    @Test
    public void evaluateXPathExpression_match() throws XPathExpressionException, SAXException, IOException {
        Document doc = docBuilder.parse(this.getClass().getResourceAsStream("/atom-feed.xml"));
        String expr = "/tns:feed/tns:author[ns1:phone]";
        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put(ATOM_NS, "tns");
        nsBindings.put(EX_NS, "ns1");
        NodeList results = XMLUtils.evaluateXPath(doc, expr, nsBindings);
        assertTrue("Expected 1 node in results.", results.getLength() == 1);
        assertEquals("author", results.item(0).getLocalName());
    }

    @Test
    public void evaluateXPathExpression_noMatch() throws XPathExpressionException, SAXException, IOException {
        Document doc = docBuilder.parse(this.getClass().getResourceAsStream("/atom-feed.xml"));
        String expr = "/tns:feed/tns:author[ns1:blog]";
        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put(ATOM_NS, "tns");
        nsBindings.put(EX_NS, "ns1");
        NodeList results = XMLUtils.evaluateXPath(doc, expr, nsBindings);
        assertTrue("Expected empty results.", results.getLength() == 0);
    }

    @Test(expected = XPathExpressionException.class)
    public void evaluateXPathExpression_booleanResult() throws XPathExpressionException, SAXException, IOException {
        Document doc = docBuilder.parse(this.getClass().getResourceAsStream("/atom-feed.xml"));
        String expr = "count(//tns:entry) > 0";
        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put(ATOM_NS, "tns");
        NodeList results = XMLUtils.evaluateXPath(doc, expr, nsBindings);
        assertNull(results);
    }

    @Test
    public void createElement_Alpha() {
        QName qName = new QName("http://example.org", "Alpha");
        Element elem = XMLUtils.createElement(qName);
        assertEquals("Alpha", elem.getLocalName());
        assertNull(elem.getParentNode());
        assertNotNull(elem.getOwnerDocument());
    }

    @Test
    public void transformGMLEnvelopeToPolygon() throws SAXException, IOException {
        Document source = docBuilder.parse(this.getClass().getResourceAsStream("/Envelope.xml"));
        Source xslt = new StreamSource(this.getClass().getResourceAsStream("/xslt/bbox2polygon.xsl"));
        Document result = XMLUtils.transform(xslt, source, null);
        assertEquals("Document element has unexpected [local name].", "Polygon",
                result.getDocumentElement().getLocalName());
        String posList = result.getElementsByTagNameNS(Namespaces.GML, "posList").item(0).getTextContent();
        assertTrue("Expected gml:posList to end with coord '49.0 -124.0'", posList.endsWith("49.0 -124.0"));
    }

    @Test
    public void evaluateXPath2ExpressionAgainstDocument()
            throws SAXException, IOException, SaxonApiException, XPathException {
        Document doc = docBuilder.parse(this.getClass().getResourceAsStream("/atom-feed.xml"));
        String expr = "matches(//tns:entry/tns:title, '.*Robots')";
        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put(ATOM_NS, "tns");
        XdmValue result = XMLUtils.evaluateXPath2(new DOMSource(doc), expr, nsBindings);
        assertTrue("Expected non-empty result.", result.size() > 0);
        assertEquals("Result has unexpected string value.", "true", result.getUnderlyingValue().getStringValue());
    }

    @Test
    public void evaluateXPath2ExpressionAgainstElement()
            throws SAXException, IOException, SaxonApiException, XPathException {
        Document doc = docBuilder.parse(this.getClass().getResourceAsStream("/atom-feed.xml"));
        Node entry = doc.getElementsByTagNameNS(ATOM_NS, "entry").item(0);
        String expr = "matches(tns:title, '.*Robots')";
        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put(ATOM_NS, "tns");
        XdmValue result = XMLUtils.evaluateXPath2(new DOMSource(entry), expr, nsBindings);
        assertTrue("Expected non-empty result.", result.size() > 0);
        assertEquals("Result has unexpected string value.", "true", result.getUnderlyingValue().getStringValue());
    }

    @Test
    public void writeNodeToString_Latin1Char() throws SAXException, IOException {
        Document doc = docBuilder.parse(this.getClass().getResourceAsStream("/atom-feed.xml"));
        Node author = doc.getElementsByTagNameNS(ATOM_NS, "author").item(0);
        String result = XMLUtils.writeNodeToString(author);
        assertTrue("Expected result to contain 'Montréal'", result.contains("Montréal"));
    }

    @Test
    public void expandCharacterEntity() {
        String text = "Ce n&apos;est pas";
        String result = XMLUtils.expandReferencesInText(text);
        assertTrue("Expected result to contain an apostrophe (')", result.contains("'"));
    }

    @Test
    public void expandNumericCharacterReference() {
        String text = "Montr&#xe9;al";
        String result = XMLUtils.expandReferencesInText(text);
        assertEquals("Expected result to contain character é (U+00E9)", "Montréal", result);
    }

    @Test
    public void domResultToString() throws SAXException, IOException {
        Document svrl = docBuilder.parse(this.getClass().getResourceAsStream("/sch/result.xml"));
        Result result = new DOMResult(svrl.getDocumentElement());
        String strResult = XMLUtils.resultToString(result);
        assertTrue("Expected 'successful-report' in result", strResult.contains("successful-report"));
    }

    @Test
    public void streamResultToString() throws SAXException, IOException, URISyntaxException {
        URL url = getClass().getResource("/sch/result.txt");
        Result result = new StreamResult(url.toURI().toString());
        String strResult = XMLUtils.resultToString(result);
        assertTrue("Expected 'Error' in result", strResult.contains("Error"));
    }
}
