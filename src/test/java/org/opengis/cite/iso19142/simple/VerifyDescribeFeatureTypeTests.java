package org.opengis.cite.iso19142.simple;

import java.io.IOException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.WFS2;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Verifies the behavior of the DescribeFeatureTypeTests class.
 */
public class VerifyDescribeFeatureTypeTests {

	private static ITestContext testContext;
	private static ISuite suite;
	private static DocumentBuilder docBuilder;

	public VerifyDescribeFeatureTypeTests() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
		testContext = mock(ITestContext.class);
		suite = mock(ISuite.class);
		when(testContext.getSuite()).thenReturn(suite);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		docBuilder = dbf.newDocumentBuilder();
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void addOneFeatureType() throws SAXException, IOException {
		Document doc = docBuilder.parse(this.getClass().getResourceAsStream(
				"/DescribeFeatureType-Empty.xml"));
		DescribeFeatureTypeTests iut = new DescribeFeatureTypeTests();
		iut.addFeatureType(doc,
				new QName("http://example.org", "Unknown1.Type"));
		Element typeName = (Element) doc.getElementsByTagNameNS(Namespaces.WFS,
				WFS2.TYPENAME_ELEM).item(0);
		String[] qName = typeName.getTextContent().split(":");
		Assert.assertEquals("Qualified name should be 'prefix:localPart'.", 2,
				qName.length);
		Assert.assertEquals("Unexpected type name.", "Unknown1.Type", qName[1]);
	}
}
