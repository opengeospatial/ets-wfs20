package org.opengis.cite.iso19142.basic;

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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Verifies the behavior of the GetPropertyValueTests class.
 */
public class VerifyGetPropertyValueTests {

	private static ITestContext testContext;
	private static ISuite suite;
	private static DocumentBuilder docBuilder;

	public VerifyGetPropertyValueTests() {
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
	public void addTwoQueries() throws SAXException, IOException {
		Document doc = docBuilder.parse(this.getClass().getResourceAsStream(
				"/GetPropertyValue.xml"));
		GetPropertyValueTests iut = new GetPropertyValueTests();
		iut.addQuery(doc, new QName("http://cite.opengeospatial.org/gmlsf",
				"PrimitiveGeoFeature"));
		iut.addQuery(doc, new QName(
				"http://www.opengis.net/citygml/building/2.0", "Building"));
		NodeList qryElems = doc.getElementsByTagNameNS(Namespaces.WFS,
				WFS2.QUERY_ELEM);
		Assert.assertEquals("Unexpected number of wfs:Query elements.", 3,
				qryElems.getLength());
	}
}
