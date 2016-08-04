package org.opengis.cite.iso19142.basic.filter.spatial;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.iso19142.CommonTestFixture;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.basic.filter.spatial.BBOXTests;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Verifies the behavior of the BBOXTests class.
 */
public class VerifyBBOXTests extends CommonTestFixture {

	private static final String NS1 = "http://example.org/ns1";
	private static ITestContext testContext;
	private static ISuite suite;

	public VerifyBBOXTests() {
	}

	@BeforeClass
	public static void initClass() throws Exception {
		testContext = mock(ITestContext.class);
		suite = mock(ISuite.class);
		when(testContext.getSuite()).thenReturn(suite);
	}

	@Test
	public void addBBOX() throws SAXException, IOException {
		Document req = docBuilder.parse(this.getClass().getResourceAsStream(
				"/GetFeature/GetFeature-Minimal.xml"));
		WFSMessage.appendSimpleQuery(req, new QName(NS1, "Type1"));
		Document env = docBuilder.parse(this.getClass().getResourceAsStream(
				"/Envelope.xml"));
		BBOXTests iut = new BBOXTests();
		iut.addBBOXPredicate(req, env.getDocumentElement(), null);
		NodeList envElems = req.getElementsByTagNameNS(Namespaces.GML,
				"Envelope");
		Assert.assertEquals("Unexpected number of gml:Envelope elements.", 1,
				envElems.getLength());
	}

	@Test
	public void addBBOXWithValueReference() throws SAXException, IOException {
		Document req = docBuilder.parse(this.getClass().getResourceAsStream(
				"/GetFeature/GetFeature-Minimal.xml"));
		WFSMessage.appendSimpleQuery(req, new QName(NS1, "Type1"));
		Document env = docBuilder.parse(this.getClass().getResourceAsStream(
				"/Envelope.xml"));
		BBOXTests iut = new BBOXTests();
		Element valueRef = XMLUtils.createElement(new QName(Namespaces.FES,
				"ValueReference", "fes"));
		valueRef.setTextContent("tns:geom");
		iut.addBBOXPredicate(req, env.getDocumentElement(), valueRef);
		Node node = req
				.getElementsByTagNameNS(Namespaces.FES, "ValueReference").item(
						0);
		Assert.assertEquals("Unexpected fes:ValueReference.", "tns:geom",
				node.getTextContent());
	}
}
