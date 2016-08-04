package org.opengis.cite.iso19142.util;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSTypeDefinition;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.validation.XSModelBuilder;
import org.opengis.cite.validation.XmlSchemaCompiler;

public class VerifyFeatureProperty {

    private static final String EX_NS = "http://example.org/ns1";
    private static XSModel model;

    @BeforeClass
    public static void setUpClass() throws Exception {
        URL entityCatalog = VerifyAppSchemaUtils.class.getResource("/schema-catalog.xml");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        InputStream xis = VerifyAppSchemaUtils.class.getResourceAsStream("/xsd/simple.xsd");
        Schema schema = xsdCompiler.compileXmlSchema(new StreamSource(xis));
        model = XSModelBuilder.buildXMLSchemaModel(schema, EX_NS);
    }

    @Test
    public void curveProperty() {
        QName featureTypeName = new QName(EX_NS, "SimpleFeature");
        XSTypeDefinition typeDef = model.getTypeDefinition("AbstractCurveType", Namespaces.GML);
        List<XSElementDeclaration> props = AppSchemaUtils.getFeaturePropertiesByType(model, featureTypeName, typeDef);
        FeatureProperty prop = new FeatureProperty(featureTypeName, props.get(0));
        assertEquals(new QName(Namespaces.GML, "LineString"), prop.getValueType());
    }

    @Test
    public void decimalProperty() {
        QName featureTypeName = new QName(EX_NS, "SimpleFeature");
        XSTypeDefinition typeDef = model.getTypeDefinition("decimal", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        List<XSElementDeclaration> props = AppSchemaUtils.getFeaturePropertiesByType(model, featureTypeName, typeDef);
        FeatureProperty prop = new FeatureProperty(featureTypeName, props.get(0));
        assertEquals(new QName(Namespaces.XSD.toString(), "integer"), prop.getValueType());
    }
}
