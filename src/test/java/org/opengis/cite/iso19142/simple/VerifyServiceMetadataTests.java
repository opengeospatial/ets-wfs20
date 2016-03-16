package org.opengis.cite.iso19142.simple;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.util.ValidationUtils;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Verifies the behavior of the ServiceMetadataTests class.
 */
public class VerifyServiceMetadataTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();
	private static ITestContext testContext;
	private static ISuite suite;
	private static DocumentBuilder docBuilder;
	private static Schema wfsSchema;

	public VerifyServiceMetadataTests() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
		testContext = mock(ITestContext.class);
		suite = mock(ISuite.class);
		when(testContext.getSuite()).thenReturn(suite);
		wfsSchema = ValidationUtils.createWFSSchema();
		when(suite.getAttribute(SuiteAttribute.WFS_SCHEMA.getName()))
				.thenReturn(wfsSchema);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		docBuilder = dbf.newDocumentBuilder();
	}

	@Test
	public void validateEmptyCapabilitiesDoc_valid() throws SAXException,
			IOException {
		Document doc = docBuilder.parse(this.getClass().getResourceAsStream(
				"/empty-wfs-capabilities.xml"));
		when(suite.getAttribute(SuiteAttribute.TEST_SUBJECT.getName()))
				.thenReturn(doc);
		ServiceMetadataTests iut = new ServiceMetadataTests();
		iut.initBaseFixture(testContext);
		iut.obtainWFSSchema(testContext);
		iut.capabilitiesDocIsXmlSchemaValid();
	}

	@Test
	public void atomFeed() throws SAXException, IOException {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Not a WFS service description");
		Document doc = docBuilder.parse(this.getClass().getResourceAsStream(
				"/atom-feed.xml"));
		when(suite.getAttribute(SuiteAttribute.TEST_SUBJECT.getName()))
				.thenReturn(doc);
		ServiceMetadataTests iut = new ServiceMetadataTests();
		iut.initBaseFixture(testContext);
	}
}
