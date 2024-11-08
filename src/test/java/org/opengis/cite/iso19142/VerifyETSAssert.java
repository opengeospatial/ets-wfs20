package org.opengis.cite.iso19142;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.cite.iso19142.util.VerifyAppSchemaUtils;
import org.opengis.cite.validation.XSModelBuilder;
import org.opengis.cite.validation.XmlSchemaCompiler;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertThat;

public class VerifyETSAssert extends CommonTestFixture {

	private static final String WADL_NS = "http://wadl.dev.java.net/2009/02";

	private static final String EX_NS = "http://example.org/ns1";

	private static XSModel model;

	private static SchemaFactory factory;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	public VerifyETSAssert() {
	}

	@BeforeClass
	public static void setUpClass() throws ParserConfigurationException, SAXException {
		factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		URL entityCatalog = VerifyAppSchemaUtils.class.getResource("/schema-catalog.xml");
		XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
		InputStream xis = VerifyAppSchemaUtils.class.getResourceAsStream("/xsd/simple.xsd");
		Schema schema = xsdCompiler.compileXmlSchema(new StreamSource(xis));
		model = XSModelBuilder.buildXMLSchemaModel(schema, EX_NS);
	}

	@Test
	public void validateUsingSchemaHints_expect2Errors() throws SAXException {
		thrown.expect(AssertionError.class);
		thrown.expectMessage("2 schema validation error(s) detected");
		URL url = this.getClass().getResource("/Gamma.xml");
		Schema schema = factory.newSchema();
		Validator validator = schema.newValidator();
		ETSAssert.assertSchemaValid(validator, new StreamSource(url.toString()));
	}

	@Test
	public void assertXPathWithNamespaceBindings() throws SAXException, IOException {
		Document doc = BUILDER.parse(this.getClass().getResourceAsStream("/capabilities-simple.xml"));
		Map<String, String> nsBindings = new HashMap<String, String>();
		nsBindings.put(WADL_NS, "ns1");
		String xpath = "//ns1:resources";
		ETSAssert.assertXPath(xpath, doc, nsBindings);
	}

	@Test
	public void assertXPath_expectFalse() throws SAXException, IOException {
		thrown.expect(AssertionError.class);
		thrown.expectMessage("Unexpected result evaluating XPath expression");
		Document doc = BUILDER.parse(this.getClass().getResourceAsStream("/capabilities-simple.xml"));
		// using built-in namespace binding
		String xpath = "//ows:OperationsMetadata/ows:Constraint[@name='SOAPEncoding']/ows:DefaultValue = 'TRUE'";
		ETSAssert.assertXPath(xpath, doc, null);
	}

	@Test
	public void evaluateXPathToBoolean() throws SAXException, IOException {
		Document doc = BUILDER.parse(this.getClass().getResourceAsStream("/capabilities-simple.xml"));
		Map<String, String> nsBindings = new HashMap<>();
		nsBindings.put(WADL_NS, "ns1");
		String xpath = "//ns1:resources";
		boolean result = ETSAssert.evaluateXPathToBoolean(xpath, doc, nsBindings);
		assertThat(result, CoreMatchers.is(true));
	}

	@Test
	public void evaluateXPathToBoolean_expectFalse() throws SAXException, IOException {
		Document doc = BUILDER.parse(this.getClass().getResourceAsStream("/capabilities-simple.xml"));
		Map<String, String> nsBindings = new HashMap<>();
		nsBindings.put(WADL_NS, "ns1");
		String xpath = "//ns1:unknown";
		boolean result = ETSAssert.evaluateXPathToBoolean(xpath, doc, nsBindings);
		assertThat(result, CoreMatchers.is(false));
	}

	@Test
	public void assertStatusCodeMatches() {
		ETSAssert.assertStatusCode(400, new int[] { 500, 403, 400 });
	}

	@Test
	public void assertDateTimeProperty() throws SAXException, IOException {
		Document gml = BUILDER.parse(this.getClass().getResourceAsStream("/wfs/SimpleFeature-SF01.xml"));
		XSElementDeclaration propDecl = model.getElementDeclaration("dateTimeProperty", EX_NS);
		Map<XSElementDeclaration, Object> expectedValues = Collections.singletonMap(propDecl, "2016-07-21T14:47:51Z");
		ETSAssert.assertSimpleProperties(gml.getDocumentElement(), expectedValues,
				Collections.singletonMap(EX_NS, "tns"));
	}

}
