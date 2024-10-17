package org.opengis.cite.iso19142.util;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
		assertEquals("Unexpected number of feature types.", featureTypes.size(), 2);
		QName simpleFeature = new QName(TNS, "SimpleFeature");
		assertTrue("Expected type: " + simpleFeature, featureTypes.contains(simpleFeature));
	}

	@Test
	public void getSpatialExtentOfSimpleFeature() throws URISyntaxException, SAXException, IOException {
		Document capabilitiesDoc = docBuilder.parse(getClass().getResourceAsStream("/wfs/capabilities-acme.xml"));
		QName simpleFeature = new QName(TNS, "SimpleFeature");
		DataSampler iut = new DataSampler(capabilitiesDoc);
		setSampleData(iut, simpleFeature, "/wfs/FeatureCollection-SimpleFeature.xml");
		Envelope bbox = iut.getSpatialExtent(model, simpleFeature);
		assertNotNull("Envelope is null.", bbox);
		DirectPosition upperCorner = bbox.getUpperCorner();
		assertEquals("Unexpected ordinate[0] for upper corner.", 51.92, upperCorner.getOrdinate(0), 0.005);
		assertEquals("Unexpected ordinate[1] for upper corner.", 8.541, upperCorner.getOrdinate(1), 0.005);
	}

	@Test
	public void getTemporalExtentOfSimpleFeatures() throws URISyntaxException, SAXException, IOException {
		Document capabilitiesDoc = docBuilder.parse(getClass().getResourceAsStream("/wfs/capabilities-acme.xml"));
		QName simpleFeature = new QName(TNS, "SimpleFeature");
		List<XSElementDeclaration> tmProps = AppSchemaUtils.getTemporalFeatureProperties(model, simpleFeature);
		XSElementDeclaration tmProp = tmProps.stream()
			.filter(decl -> decl.getName().equals("dateTimeProperty"))
			.findAny()
			.orElse(null);
		DataSampler iut = new DataSampler(capabilitiesDoc);
		setSampleData(iut, simpleFeature, "/wfs/FeatureCollection-SimpleFeature.xml");
		Period period = iut.getTemporalExtentOfProperty(model, simpleFeature, tmProp);
		assertNotNull("Period is null.", period);
		assertTrue("Expected duration P8M", period.length().toString().startsWith("P8M"));
	}

	@Test
	public void getTemporalExtentOfComplexFeatures() throws URISyntaxException, SAXException, IOException {
		Document capabilitiesDoc = docBuilder.parse(getClass().getResourceAsStream("/wfs/capabilities-acme.xml"));
		QName featureType = new QName(TNS, "ComplexFeature");
		List<XSElementDeclaration> tmProps = AppSchemaUtils.getTemporalFeatureProperties(model, featureType);
		XSElementDeclaration tmProp = tmProps.stream()
			.filter(decl -> decl.getName().equals("validTime"))
			.findAny()
			.orElse(null);
		DataSampler iut = new DataSampler(capabilitiesDoc);
		setSampleData(iut, featureType, "/wfs/FeatureCollection-ComplexFeature.xml");
		Period period = iut.getTemporalExtentOfProperty(model, featureType, tmProp);
		assertNotNull("Period is null.", period);
		assertTrue("Expected duration P11M", period.length().toString().startsWith("P11M"));
	}

	@Test
	public void testSelectRandomFeatureType() throws Exception {
		Document capabilitiesDoc = docBuilder.parse(getClass().getResourceAsStream("/wfs/capabilities-acme.xml"));
		QName featureType = new QName(TNS, "ComplexFeature");
		DataSampler iut = new DataSampler(capabilitiesDoc);

		QName selectedFeatureTypeBeforeInstantiaed = iut.selectFeatureType();
		assertThat(selectedFeatureTypeBeforeInstantiaed, nullValue());

		FeatureTypeInfo typeInfo = iut.getFeatureTypeInfo().get(featureType);
		typeInfo.setInstantiated(true);

		QName selectedFeatureType = iut.selectFeatureType();
		assertThat(selectedFeatureType, is(featureType));
	}

	@Test
	public void testGetFeatureId() throws Exception {
		Document capabilitiesDoc = docBuilder.parse(getClass().getResourceAsStream("/wfs/capabilities-acme.xml"));
		QName simpleFt = new QName(TNS, "SimpleFeature");
		QName complexFt = new QName(TNS, "ComplexFeature");
		DataSampler iut = new DataSampler(capabilitiesDoc);
		setSampleData(iut, simpleFt, "/wfs/FeatureCollection-SimpleFeature.xml");
		setSampleData(iut, complexFt, "/wfs/FeatureCollection-ComplexFeature.xml");
		String id = iut.getFeatureId();
		assertThat(id, anyOf(is("CF01"), is("SF-01")));
	}

	@Test
	public void testGetFeatureId_matchFalse() throws Exception {
		Document capabilitiesDoc = docBuilder.parse(getClass().getResourceAsStream("/wfs/capabilities-acme.xml"));
		QName simpleFt = new QName(TNS, "SimpleFeature");
		QName complexFt = new QName(TNS, "ComplexFeature");
		DataSampler iut = new DataSampler(capabilitiesDoc);
		setSampleData(iut, simpleFt, "/wfs/FeatureCollection-SimpleFeature.xml");
		setSampleData(iut, complexFt, "/wfs/FeatureCollection-ComplexFeature.xml");
		String id = iut.getFeatureIdNotOfType(simpleFt);
		assertThat(id, is("CF01"));
	}

	private void setSampleData(DataSampler iut, QName featureType, String resource) throws URISyntaxException {
		URL dataURL = getClass().getResource(resource);
		File dataFile = new File(dataURL.toURI());
		FeatureTypeInfo typeInfo = iut.getFeatureTypeInfo().get(featureType);
		typeInfo.setInstantiated(true);
		typeInfo.setSampleData(dataFile);
	}

}
