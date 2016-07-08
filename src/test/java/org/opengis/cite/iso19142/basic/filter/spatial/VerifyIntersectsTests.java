package org.opengis.cite.iso19142.basic.filter.spatial;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.apache.xerces.xs.XSModel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.util.VerifyAppSchemaUtils;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.opengis.cite.validation.XSModelBuilder;
import org.opengis.cite.validation.XmlSchemaCompiler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class VerifyIntersectsTests {

    private static final String NS1 = "http://example.org/ns1";
    private static DocumentBuilder docBuilder;
    private static XSModel model;

    @BeforeClass
    public static void initParser() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
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
    public void addIntersectsPredicate() throws SAXException, IOException {
        Document reqEntity = WFSMessage.createRequestEntity("GetFeature-Minimal", "2.0.2");
        WFSMessage.appendSimpleQuery(reqEntity, new QName(NS1, "SimpleFeature"));
        Element gmlGeom = docBuilder.parse(getClass().getResourceAsStream("Polygon-01.xml")).getDocumentElement();
        Element valueRef = WFSMessage.createValueReference(model.getElementDeclaration("lineProperty", NS1));
        IntersectsTests iut = new IntersectsTests();
        iut.addSpatialPredicate(reqEntity, "Intersects", gmlGeom, valueRef);
        Node predicate = reqEntity.getElementsByTagNameNS(Namespaces.FES, "Intersects").item(0);
        assertNotNull(predicate);
        assertEquals("Unexpected number of operands", 2, predicate.getChildNodes().getLength());
    }
}
