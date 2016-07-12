package org.opengis.cite.iso19142.basic.filter.temporal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.apache.xerces.xs.XSModel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.util.TimeUtils;
import org.opengis.cite.iso19142.util.VerifyAppSchemaUtils;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.opengis.cite.validation.XSModelBuilder;
import org.opengis.cite.validation.XmlSchemaCompiler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class VerifyDuringTests {

    private static final String NS1 = "http://example.org/ns1";
    private static XSModel model;

    @BeforeClass
    public static void buildSchemaModel() throws SAXException {
        URL entityCatalog = VerifyAppSchemaUtils.class.getResource("/schema-catalog.xml");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        InputStream xis = VerifyAppSchemaUtils.class.getResourceAsStream("/xsd/simple.xsd");
        Schema schema = xsdCompiler.compileXmlSchema(new StreamSource(xis));
        model = XSModelBuilder.buildXMLSchemaModel(schema, NS1);
    }

    @Test
    public void addDuringPredicate() throws SAXException, IOException {
        Document reqEntity = WFSMessage.createRequestEntity("GetFeature-Minimal", "2.0.2");
        WFSMessage.appendSimpleQuery(reqEntity, new QName(NS1, "SimpleFeature"));
        ZonedDateTime endTime = ZonedDateTime.now(ZoneId.of("Z"));
        ZonedDateTime startTime = endTime.minusYears(5);
        Document gmlTime = TimeUtils.periodAsGML(startTime, endTime);
        Element valueRef = WFSMessage.createValueReference(model.getElementDeclaration("dateTimeProperty", NS1));
        DuringTests iut = new DuringTests();
        iut.addTemporalPredicate(reqEntity, "During", gmlTime, valueRef);
        Node predicate = reqEntity.getElementsByTagNameNS(Namespaces.FES, "During").item(0);
        assertNotNull(predicate);
        assertEquals("Unexpected number of operands", 2, predicate.getChildNodes().getLength());
    }
}
