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
import org.opengis.cite.iso19136.GML32;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.validation.XSModelBuilder;
import org.opengis.cite.validation.XmlSchemaCompiler;

/**
 * Verifies the behavior of the AppSchemaUtils class.
 */
public class VerifyAppSchemaUtils {

    private static final String EX_NS = "http://example.org/ns1";
    private static final QName DOUBLE_DT = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "double");
    private static final QName DATETIME_DT = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "dateTime");
    private static XSModel model;

    public VerifyAppSchemaUtils() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        URL entityCatalog = VerifyAppSchemaUtils.class.getResource("/schema-catalog.xml");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        InputStream xis = VerifyAppSchemaUtils.class.getResourceAsStream("/xsd/simple.xsd");
        Schema schema = xsdCompiler.compileXmlSchema(new StreamSource(xis));
        model = XSModelBuilder.buildXMLSchemaModel(schema, EX_NS);
    }

    @Test
    public void findSimpleProperties_doubleType() {
        QName featureTypeName = new QName(EX_NS, "SimpleFeature");
        XSTypeDefinition xsdDoubleType = model.getTypeDefinition("double", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        List<XSElementDeclaration> props = AppSchemaUtils.getFeaturePropertiesByType(model, featureTypeName,
                xsdDoubleType);
        assertEquals("Unexpected number of xsd:double properties.", 1, props.size());
        assertEquals("Unexpected property name.", "measurand", props.get(0).getName());
    }

    @Test
    public void findSimpleProperties_decimalType() {
        QName featureTypeName = new QName(EX_NS, "SimpleFeature");
        XSTypeDefinition xsdDoubleType = model.getTypeDefinition("decimal", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        List<XSElementDeclaration> props = AppSchemaUtils.getFeaturePropertiesByType(model, featureTypeName,
                xsdDoubleType);
        assertEquals("Unexpected number of xsd:decimal properties.", 2, props.size());
    }

    @Test
    public void findGeometryProperties_curveType() {
        QName featureTypeName = new QName(EX_NS, "SimpleFeature");
        XSTypeDefinition gmlCurveType = model.getTypeDefinition("AbstractCurveType", Namespaces.GML);
        List<XSElementDeclaration> props = AppSchemaUtils.getFeaturePropertiesByType(model, featureTypeName,
                gmlCurveType);
        assertEquals("Unexpected number of curve properties.", 1, props.size());
        assertEquals("Unexpected property name.", "lineProperty", props.get(0).getName());
    }

    @Test
    public void findAllGeometryProperties() {
        QName featureTypeName = new QName(EX_NS, "SimpleFeature");
        XSTypeDefinition gmlGeomType = model.getTypeDefinition("AbstractGeometryType", Namespaces.GML);
        List<XSElementDeclaration> props = AppSchemaUtils.getFeaturePropertiesByType(model, featureTypeName,
                gmlGeomType);
        assertEquals("Unexpected number of geometry properties.", 3, props.size());
    }

    @Test
    public void findBooleanProperty_none() {
        QName featureTypeName = new QName(EX_NS, "SimpleFeature");
        XSTypeDefinition xsdBooleanType = model.getTypeDefinition("boolean", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        List<XSElementDeclaration> props = AppSchemaUtils.getFeaturePropertiesByType(model, featureTypeName,
                xsdBooleanType);
        assertTrue("Expected empty property list.", props.isEmpty());
    }

    @Test
    public void findAllProperties_doubleType() {
        QName featureTypeName = new QName(EX_NS, "ComplexFeature");
        XSTypeDefinition doubleType = model.getTypeDefinition("double", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        List<XSElementDeclaration> props = AppSchemaUtils.getFeaturePropertiesByType(model, featureTypeName,
                doubleType);
        assertEquals("Unexpected number of properties derived from xsd:double.", 1, props.size());
        assertEquals("Unexpected property name.", "observation", props.get(0).getName());
    }

    @Test
    public void findFeatureAssociations() {
        QName featureTypeName = new QName(EX_NS, "ComplexFeature");
        XSTypeDefinition featureType = model.getTypeDefinition("AbstractFeatureType", Namespaces.GML);
        List<XSElementDeclaration> props = AppSchemaUtils.getFeaturePropertiesByType(model, featureTypeName,
                featureType);
        assertEquals("Unexpected number of feature associations.", 1, props.size());
        assertEquals("Unexpected property name.", "simpleFeature", props.get(0).getName());
    }

    @Test
    public void findNillableFeatureProperties() {
        QName featureTypeName = new QName(EX_NS, "ComplexFeature");
        List<XSElementDeclaration> props = AppSchemaUtils.getNillableProperties(model, featureTypeName);
        assertEquals("Unexpected number of nillable properties.", 2, props.size());
    }

    @Test
    public void findAllProperties_ComplexFeature() {
        QName featureTypeName = new QName(EX_NS, "ComplexFeature");
        List<XSElementDeclaration> props = AppSchemaUtils.getAllFeatureProperties(model, featureTypeName);
        assertEquals("Found unexpected number of feature properties.", 15, props.size());
        XSElementDeclaration elem = props.get(0);
        assertEquals("Unexpected name for first property in list.", "metaDataProperty", elem.getName());
    }

    @Test
    public void findRequiredProperties_ComplexFeature() {
        QName featureTypeName = new QName(EX_NS, "ComplexFeature");
        List<XSElementDeclaration> props = AppSchemaUtils.getRequiredProperties(model, featureTypeName);
        assertEquals("Found unexpected number of required feature properties.", 5, props.size());
        XSElementDeclaration elem = props.get(props.size() - 1);
        assertEquals("Unexpected name for last property in list.", "validTime", elem.getName());
    }

    @Test
    public void getBuiltInTypeOfObservationProperty() {
        QName featureTypeName = new QName(EX_NS, "ComplexFeature");
        List<XSElementDeclaration> simpleProps = AppSchemaUtils.getSimpleFeatureProperties(model, featureTypeName);
        XSElementDeclaration obsProp = simpleProps.get(0);
        QName qName = AppSchemaUtils.getBuiltInDatatype(obsProp);
        assertEquals("Unexpected datatype.", DOUBLE_DT, qName);
    }

    @Test
    public void getBuiltInTypeOfValidTimeProperty() {
        QName featureTypeName = new QName(EX_NS, "ComplexFeature");
        List<XSElementDeclaration> simpleProps = AppSchemaUtils.getSimpleFeatureProperties(model, featureTypeName);
        XSElementDeclaration timeProp = simpleProps.get(simpleProps.size() - 2);
        QName qName = AppSchemaUtils.getBuiltInDatatype(timeProp);
        assertEquals("Unexpected datatype.", DATETIME_DT, qName);
    }

    @Test
    public void getValueOfMultiGeomProperty() {
        XSElementDeclaration multiGeomProperty = model.getElementDeclaration("multiGeomProperty", EX_NS);
        XSElementDeclaration value = AppSchemaUtils.getComplexPropertyValue(multiGeomProperty);
        assertEquals("Unexpected namespace", GML32.NS_NAME, value.getNamespace());
        assertEquals("Unexpected name.", "AbstractGeometricAggregate", value.getName());
    }
}
