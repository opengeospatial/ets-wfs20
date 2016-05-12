package org.opengis.cite.iso19142.basic.filter.joins;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.apache.xerces.xs.XSModel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.iso19142.CommonTestFixture;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.util.FeatureProperty;
import org.opengis.cite.iso19142.util.VerifyAppSchemaUtils;
import org.opengis.cite.iso19142.util.WFSRequest;
import org.opengis.cite.validation.XSModelBuilder;
import org.opengis.cite.validation.XmlSchemaCompiler;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class VerifyJoinQueryUtils extends CommonTestFixture {

	private static XSModel model;
	private static final String NS1 = "http://example.org/ns1";
	private static final String FES = Namespaces.FES;

	@BeforeClass
	public static void buildSchemaModel() throws SAXException {
		URL entityCatalog = VerifyAppSchemaUtils.class
				.getResource("/schema-catalog.xml");
		XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
		InputStream inStream = VerifyAppSchemaUtils.class
				.getResourceAsStream("/xsd/simple.xsd");
		Schema schema = xsdCompiler
				.compileXmlSchema(new StreamSource(inStream));
		model = XSModelBuilder.buildXMLSchemaModel(schema, NS1);
	}

	@Test
	public void noFeatureProperties() {
		thrown.expect(NullPointerException.class);
		thrown.expectMessage("Feature properties are required");
		Document reqEntity = WFSRequest.createRequestEntity(
				"GetFeature-Minimal", "2.0.0");
		List<FeatureProperty> geomProps = null;
		JoinQueryUtils.appendSpatialJoinQuery(reqEntity, "Intersects",
				geomProps);
	}

	@Test
	public void buildIntersectsQuery() {
		Document reqEntity = WFSRequest.createRequestEntity(
				"GetFeature-Minimal", "2.0.0");
		List<FeatureProperty> geomProps = new ArrayList<FeatureProperty>();
		geomProps.add(new FeatureProperty(new QName(NS1, "SimpleFeature"),
				model.getElementDeclaration("lineProperty", NS1)));
		geomProps.add(new FeatureProperty(new QName(NS1, "ComplexFeature"),
				model.getElementDeclaration("multiGeomProperty", NS1)));
		JoinQueryUtils.appendSpatialJoinQuery(reqEntity, "Intersects",
				geomProps);
		NodeList valueRefs = reqEntity.getElementsByTagNameNS(FES,
				"ValueReference");
		assertEquals("Unexpected number of fes:ValueReference elements.", 2,
				valueRefs.getLength());
		Node valueRef = valueRefs.item(0);
		String xpath = valueRef.getTextContent();
		String propName = xpath.substring(xpath.lastIndexOf('/') + 1);
		assertEquals("Unexpected local name", "lineProperty",
				propName.split(":")[1]);
		assertEquals("Unexpected namespace name", NS1,
				valueRef.lookupNamespaceURI(propName.split(":")[0]));
	}
}
