package org.opengis.cite.iso19142.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.geomatics.Extents;
import org.opengis.cite.geomatics.SpatialOperator;
import org.opengis.cite.iso19142.ConformanceClass;
import org.opengis.cite.iso19142.FeatureTypeInfo;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Verifies the behavior of the ServiceMetadataUtils class.
 */
public class VerifyServiceMetadataUtils {

    private static DocumentBuilder docBuilder;
    private static final String TNS = "http://example.org/tns";

    public VerifyServiceMetadataUtils() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        docBuilder = dbf.newDocumentBuilder();
    }

    @Test
    public void findDescribeFeatureTypeUsingGET() throws SAXException, IOException {
        File xmlFile = new File("src/test/resources/capabilities-simple.xml");
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(docBuilder.parse(xmlFile), WFS2.DESCRIBE_FEATURE_TYPE,
                ProtocolBinding.GET);
        assertEquals("Unexpected endpoint for DescribeFeatureType(GET)", "http://localhost/wfs2", endpoint.toString());
    }

    @Test
    public void findDescribeFeatureTypeUsingPOST_notFound() throws SAXException, IOException {
        File xmlFile = new File("src/test/resources/capabilities-simple.xml");
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(docBuilder.parse(xmlFile), WFS2.DESCRIBE_FEATURE_TYPE,
                ProtocolBinding.POST);
        assertEquals("Expected empty URI reference.", URI.create(""), endpoint);
    }

    @Test
    public void getFeatureTypeList() throws SAXException, IOException {
        File xmlFile = new File("src/test/resources/capabilities-simple.xml");
        List<QName> typeNames = ServiceMetadataUtils.getFeatureTypes(docBuilder.parse(xmlFile));
        assertEquals("Unexpected size of type name list.", 1, typeNames.size());
        QName typeName = typeNames.get(0);
        assertEquals("Feature type has unexpected [namespace name].", "http://example.org/ns1",
                typeName.getNamespaceURI());
        assertEquals("Feature type has unexpected [local name].", "Alpha", typeName.getLocalPart());
    }

    @Test
    public void acquireFeatureTypeInfo() throws SAXException, IOException {
        File xmlFile = new File("src/test/resources/capabilities-simple.xml");
        Map<QName, FeatureTypeInfo> typeInfo = ServiceMetadataUtils.extractFeatureTypeInfo(docBuilder.parse(xmlFile));
        assertEquals("Unexpected size of type info collection.", 1, typeInfo.size());
        QName qName = new QName("http://example.org/ns1", "Alpha");
        assertEquals("Unexpected default CRS.", "urn:ogc:def:crs:EPSG::4326", typeInfo.get(qName).getDefaultCRS());
        Document gmlEnv = Extents.envelopeAsGML(typeInfo.get(qName).getSpatialExtent());
        assertEquals("Unexpected [local name] for extent.", "Envelope", gmlEnv.getDocumentElement().getLocalName());
    }

    @Test
    public void getRequestEndpoints_getCapabilities() throws SAXException, IOException {
        File xmlFile = new File("src/test/resources/capabilities-simple.xml");
        Map<String, URI> endpoints = ServiceMetadataUtils.getRequestEndpoints(docBuilder.parse(xmlFile),
                "GetCapabilities");
        assertEquals("Unexpected number of endpoints.", 2, endpoints.size());
        assertEquals("Unexpected GET endpoint.", "http://localhost/wfs2/capabilities", endpoints.get("GET").toString());
    }

    @Test
    public void getOperationBindings_getFeature() throws SAXException, IOException {
        File xmlFile = new File("src/test/resources/capabilities-simple.xml");
        Set<ProtocolBinding> bindings = ServiceMetadataUtils.getOperationBindings(docBuilder.parse(xmlFile),
                WFS2.GET_FEATURE);
        assertEquals("Unexpected number of GetFeature bindings (request encodings).", 2, bindings.size());
    }

    @Test
    public void getConformanceClaims() throws SAXException, IOException {
        File xmlFile = new File("src/test/resources/capabilities-simple.xml");
        Set<ConformanceClass> claims = ServiceMetadataUtils.getConformanceClaims(docBuilder.parse(xmlFile));
        assertEquals("Unexpected number of conformance claims", 3, claims.size());
        assertTrue("Expected 'Simple WFS' conformance claim", claims.contains(ConformanceClass.SIMPLE_WFS));
    }

    @Test
    public void buildQName_unprefixedWithoutDefaultBinding() {
        Document doc = docBuilder.newDocument();
        doc.appendChild(doc.createElementNS(TNS, "tns:alpha"));
        doc.getDocumentElement().setTextContent("localPart");
        QName qName = ServiceMetadataUtils.buildQName(doc.getDocumentElement());
        assertEquals("Unexpected local name", "localPart", qName.getLocalPart());
        assertEquals("Unexpected namespace name", XMLConstants.NULL_NS_URI, qName.getNamespaceURI());
    }

    @Test
    public void buildQName_prefixed() {
        Document doc = docBuilder.newDocument();
        doc.appendChild(doc.createElementNS(TNS, "tns:alpha"));
        doc.getDocumentElement().setTextContent("tns:localPart");
        QName qName = ServiceMetadataUtils.buildQName(doc.getDocumentElement());
        assertEquals("Unexpected local name", "localPart", qName.getLocalPart());
        assertEquals("Unexpected namespace name", TNS, qName.getNamespaceURI());
    }

    @Test
    public void simpleSpatialCapabilities() throws SAXException, IOException {
        Document wfsDescr = docBuilder.parse(getClass().getResourceAsStream("/capabilities-simple.xml"));
        Map<SpatialOperator, Set<QName>> capabilities = ServiceMetadataUtils.getSpatialCapabilities(wfsDescr);
        Set<SpatialOperator> operators = capabilities.keySet();
        assertEquals("Unexpected number of spatial operators.", 3, operators.size());
        assertTrue("Expected INTERSECTS in set.", operators.contains(SpatialOperator.INTERSECTS));
        Set<QName> geomOperands = capabilities.get(SpatialOperator.INTERSECTS);
        assertEquals("Unexpected number of geometry operands (INTERSECTS).", 6, geomOperands.size());
    }

}
