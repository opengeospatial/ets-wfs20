package org.opengis.cite.iso19142.transaction;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import static org.junit.Assert.*;

import org.apache.xerces.xs.XSModel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.iso19136.GML32;
import org.opengis.cite.validation.XSModelBuilder;
import org.opengis.cite.validation.XmlSchemaCompiler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class VerifyReplaceTests {

    private static final String NS1 = "http://example.org/ns1";
    private static DocumentBuilder docBuilder;
    private static XSModel model;

    @BeforeClass
    public static void initFixture() throws ParserConfigurationException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        docBuilder = dbf.newDocumentBuilder();
        URL entityCatalog = VerifyUpdate.class.getResource("/schema-catalog.xml");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        InputStream xis = VerifyUpdate.class.getResourceAsStream("/xsd/simple.xsd");
        Schema schema = xsdCompiler.compileXmlSchema(new StreamSource(xis));
        model = XSModelBuilder.buildXMLSchemaModel(schema, NS1);
    }

    @Test
    public void createReplacementFeature() throws SAXException, IOException {
        Document featureDoc = docBuilder.parse(getClass().getResourceAsStream("/Feature-River.xml"));
        Element original = featureDoc.getDocumentElement();
        ReplaceTests iut = new ReplaceTests();
        iut.setModel(model);
        Element repl = iut.createReplacementFeature(original);
        NodeList idNodes = repl.getElementsByTagNameNS(GML32.NS_NAME, "identifier");
        assertEquals("Unexpected number of gml:identifier elements.", 1, idNodes.getLength());
        Element id = (Element) idNodes.item(0);
        assertEquals("Unexpected value for @codeSpace.", "http://cite.opengeospatial.org/",
                id.getAttribute("codeSpace"));
        Object userData = repl.getUserData(ReplaceTests.REPL_PROPS);
        assertNotNull(userData);
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) userData;
        assertEquals("Unexpected number of Map entries in user data", 2, props.size());
    }
}
