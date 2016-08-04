package org.opengis.cite.iso19142.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.iso19142.FeatureTypeInfo;
import org.opengis.cite.validation.XSModelBuilder;
import org.opengis.cite.validation.XmlSchemaCompiler;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.temporal.Period;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class VerifyDataSampler {

    private static final String TNS = "http://example.org/ns1";
    private static XSModel model;
    private static DocumentBuilder docBuilder;

    @BeforeClass
    public static void createBuilder() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        docBuilder = dbf.newDocumentBuilder();
    }

    @BeforeClass
    public static void createModel() throws Exception {
        URL entityCatalog = VerifyAppSchemaUtils.class.getResource("/schema-catalog.xml");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        InputStream xis = VerifyAppSchemaUtils.class.getResourceAsStream("/xsd/simple.xsd");
        Schema schema = xsdCompiler.compileXmlSchema(new StreamSource(xis));
        model = XSModelBuilder.buildXMLSchemaModel(schema, TNS);
    }

    @Test
    public void initDataSampler() throws SAXException, IOException {
        Document capabilitiesDoc = docBuilder.parse(getClass().getResourceAsStream("/wfs/capabilities-acme.xml"));
        DataSampler iut = new DataSampler(capabilitiesDoc);
        Set<QName> featureTypes = iut.getFeatureTypeInfo().keySet();
        assertEquals("Unexpected number of feature types.", featureTypes.size(), 1);
        QName simpleFeature = new QName(TNS, "SimpleFeature");
        assertTrue("Expected type: " + simpleFeature, featureTypes.contains(simpleFeature));
    }

    @Test
    public void getSpatialExtentOfSimpleFeature() throws URISyntaxException, SAXException, IOException {
        URL dataURL = getClass().getResource("/wfs/FeatureCollection-SimpleFeature.xml");
        File dataFile = new File(dataURL.toURI());
        Document capabilitiesDoc = docBuilder.parse(getClass().getResourceAsStream("/wfs/capabilities-acme.xml"));
        QName simpleFeature = new QName(TNS, "SimpleFeature");
        DataSampler iut = new DataSampler(capabilitiesDoc);
        FeatureTypeInfo typeInfo = iut.getFeatureTypeInfo().get(simpleFeature);
        typeInfo.setInstantiated(true);
        typeInfo.setSampleData(dataFile);
        Envelope bbox = iut.getSpatialExtent(model, simpleFeature);
        assertNotNull("Envelope is null.", bbox);
        DirectPosition upperCorner = bbox.getUpperCorner();
        assertEquals("Unexpected ordinate[0] for upper corner.", 51.92, upperCorner.getOrdinate(0), 0.005);
        assertEquals("Unexpected ordinate[1] for upper corner.", 9.70, upperCorner.getOrdinate(1), 0.005);
    }

    @Test
    public void getTemporalExtentOfSimpleFeature() throws URISyntaxException, SAXException, IOException {
        URL dataURL = getClass().getResource("/wfs/FeatureCollection-SimpleFeature.xml");
        File dataFile = new File(dataURL.toURI());
        Document capabilitiesDoc = docBuilder.parse(getClass().getResourceAsStream("/wfs/capabilities-acme.xml"));
        QName simpleFeature = new QName(TNS, "SimpleFeature");
        List<XSElementDeclaration> tmProps = AppSchemaUtils.getTemporalFeatureProperties(model, simpleFeature);
        XSElementDeclaration tmProp = tmProps.stream().filter(decl -> decl.getName().equals("dateTimeProperty"))
                .findAny().orElse(null);
        DataSampler iut = new DataSampler(capabilitiesDoc);
        FeatureTypeInfo typeInfo = iut.getFeatureTypeInfo().get(simpleFeature);
        typeInfo.setInstantiated(true);
        typeInfo.setSampleData(dataFile);
        Period period = iut.getTemporalExtentOfProperty(model, simpleFeature, tmProp);
        assertNotNull("Period is null.", period);
        assertTrue("Expected duration P8M", period.length().toString().startsWith("P8M"));
    }
}
