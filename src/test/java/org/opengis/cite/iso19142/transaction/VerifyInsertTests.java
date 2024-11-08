package org.opengis.cite.iso19142.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.iso19142.Namespaces;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class VerifyInsertTests {

	private static ITestContext testContext;

	private static ISuite suite;

	private static DocumentBuilder docBuilder;

	@BeforeClass
	public static void initFixture() throws ParserConfigurationException {
		testContext = mock(ITestContext.class);
		suite = mock(ISuite.class);
		when(testContext.getSuite()).thenReturn(suite);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		docBuilder = dbf.newDocumentBuilder();
	}

	@Test
	public void replaceFirstName() throws SAXException, IOException {
		Document doc = docBuilder.parse(getClass().getResourceAsStream("/Alpha-1.xml"));
		String name = InsertTests.addRandomName(doc.getDocumentElement());
		assertFalse("No name value", name.isEmpty());
		NodeList nameList = doc.getElementsByTagNameNS(Namespaces.GML, "name");
		assertEquals("Unexpected number of names", 2, nameList.getLength());
		assertEquals("Unexpected name", name, nameList.item(0).getTextContent());
	}

}
