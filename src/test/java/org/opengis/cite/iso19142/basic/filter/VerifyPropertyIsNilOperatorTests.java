package org.opengis.cite.iso19142.basic.filter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.util.DataSampler;
import org.opengis.cite.iso19142.util.VerifyAppSchemaUtils;
import org.opengis.cite.validation.XSModelBuilder;
import org.opengis.cite.validation.XmlSchemaCompiler;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.xml.sax.SAXException;

/**
 * Verifies the behavior of the PropertyIsNilOperatorTests class.
 */
public class VerifyPropertyIsNilOperatorTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();
	private static final String NS1 = "http://example.org/ns1";
	private static ITestContext testContext;
	private static ISuite suite;
	private static XSModel model;
	private static DataSampler dataSampler;

	public VerifyPropertyIsNilOperatorTests() {
	}

	@BeforeClass
	public static void prepareMockTestContext() {
		testContext = mock(ITestContext.class);
		suite = mock(ISuite.class);
		when(testContext.getSuite()).thenReturn(suite);
		dataSampler = mock(DataSampler.class);
		when(suite.getAttribute(SuiteAttribute.SAMPLER.getName())).thenReturn(
				dataSampler);
	}

	@BeforeClass
	public static void buildSchemaModel() throws SAXException {
		URL entityCatalog = VerifyAppSchemaUtils.class
				.getResource("/schema-catalog.xml");
		XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
		InputStream xis = VerifyAppSchemaUtils.class
				.getResourceAsStream("/xsd/simple.xsd");
		Schema schema = xsdCompiler.compileXmlSchema(new StreamSource(xis));
		model = XSModelBuilder.buildXMLSchemaModel(schema, NS1);
	}

	@Test
	public void noNillableProperties() {
		thrown.expect(AssertionError.class);
		thrown.expectMessage("No feature type for which instances exist has nillable properties");
		when(
				suite.getAttribute(org.opengis.cite.iso19136.SuiteAttribute.XSMODEL
						.getName())).thenReturn(model);
		PropertyIsNilOperatorTests iut = new PropertyIsNilOperatorTests();
		iut.initQueryFilterFixture(testContext);
		iut.nillableProperties = new HashMap<QName, List<XSElementDeclaration>>();
		ProtocolBinding binding = ProtocolBinding.ANY;
		iut.propertyIsNil(binding);
	}
}
