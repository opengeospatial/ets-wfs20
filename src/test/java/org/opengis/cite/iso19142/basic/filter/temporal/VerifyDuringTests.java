package org.opengis.cite.iso19142.basic.filter.temporal;

import java.io.InputStream;
import java.net.URL;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.apache.xerces.xs.XSModel;
import org.junit.BeforeClass;
import org.opengis.cite.iso19142.util.VerifyAppSchemaUtils;
import org.opengis.cite.validation.XSModelBuilder;
import org.opengis.cite.validation.XmlSchemaCompiler;
import org.xml.sax.SAXException;

public class VerifyDuringTests {

	private static final String NS1 = "http://example.org/ns1";

	private static XSModel model;

	@BeforeClass
	public static void buildSchemaModel() throws SAXException {
		URL entityCatalog = VerifyAppSchemaUtils.class.getResource("/schema-catalog.xml");
		XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
		InputStream xis = VerifyAppSchemaUtils.class.getResourceAsStream("/xsd/simple.xsd");
		Schema schema = xsdCompiler.compileXmlSchema(new StreamSource(xis));
		model = XSModelBuilder.buildXMLSchemaModel(schema, NS1);
	}

}
