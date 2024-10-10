package org.opengis.cite.iso19142.basic;

import java.net.URI;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;

import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.ws.rs.core.Response;


/**
 * Tests the service response to a GetPropertyValue request. The
 * GetPropertyValue operation allows the value of a feature property (or part of
 * the value of a complex feature property) to be retrieved.
 * 
 * @see "ISO 19142:2010, cl. 10: GetPropertyValue operation"
 */
public class GetPropertyValueTests extends BaseFixture {

	private Schema wfsSchema;

	/**
	 * Retrieves the (pre-compiled) WFS schema from the suite fixture.
	 * 
	 * @param testContext
	 *            The test (group) context.
	 */
	@BeforeClass
	public void setupClassFixture(ITestContext testContext) {
		this.wfsSchema = (Schema) testContext.getSuite().getAttribute(
				SuiteAttribute.WFS_SCHEMA.getName());
		Assert.assertNotNull(this.wfsSchema,
				"WFS schema not found in suite fixture.");
	}

	/**
	 * Builds a DOM Document node representing the entity body for a
	 * GetPropertyValue request. A minimal XML representation is read from the
	 * classpath (at util/GetPropertyValue.xml).
	 */
	@BeforeMethod
	public void buildRequestEntity() {
		this.reqEntity = WFSMessage.createRequestEntity("GetPropertyValue",
				this.wfsVersion);
	}

	/**
	 * Submits a GetPropertyValue request for a known feature type with
	 * valueReference="@gml:id". The members in the resulting ValueCollection
	 * element are expected to contain simple (atomic) text values representing
	 * the server-assigned object identifiers (xsd:ID).
	 * 
	 * @param binding
	 *            The ProtocolBinding to use.
	 * 
	 * @see "ISO 19142:2010, cl. 10.2.4.3: GetPropertyValue - valueReference parameter"
	 */
	@Test(description = "See ISO 19142: 10.2.4.3", dataProvider = "protocol-binding")
	public void getProperty_gmlId(ProtocolBinding binding) {
		setValueReference(reqEntity, "@gml:id");
		addQuery(this.reqEntity, this.featureTypes.get(0));
		URI endpoint = ServiceMetadataUtils.getOperationEndpoint(
				this.wfsMetadata, WFS2.GET_PROP_VALUE, binding);
		Response rsp = wfsClient.submitRequest(new DOMSource(reqEntity),
				binding, endpoint);
		Assert.assertTrue(rsp.hasEntity(),
				ErrorMessage.get(ErrorMessageKeys.MISSING_XML_ENTITY));
		Document entity = extractBodyAsDocument(rsp);
		ETSAssert.assertQualifiedName(entity.getDocumentElement(), new QName(
				Namespaces.WFS, WFS2.VALUE_COLLECTION));
		NodeList members = entity.getElementsByTagNameNS(Namespaces.WFS,
				"member");
		for (int i = 0; i < members.getLength(); i++) {
			String value = members.item(i).getTextContent();
			Assert.assertFalse(value.isEmpty(), "Found empty wfs:member[" + i
					+ "]");
		}
	}

	/**
	 * If the valueReference is an empty string, an ExceptionReport is expected
	 * to contain the error code "InvalidParameterValue".
	 * 
	 * @param binding
	 *            The ProtocolBinding to use.
	 * 
	 * @see "ISO 19142:2010, cl. 10.4: GetPropertyValue - Exceptions"
	 */
	@Test(description = "See ISO 19142: 7.5, 10.4", dataProvider = "protocol-binding")
	public void getProperty_emptyValueRef(ProtocolBinding binding) {
		setValueReference(reqEntity, "");
		addQuery(reqEntity, this.featureTypes.get(0));
		URI endpoint = ServiceMetadataUtils.getOperationEndpoint(
				this.wfsMetadata, WFS2.GET_PROP_VALUE, binding);
		Response rsp = wfsClient.submitRequest(new DOMSource(reqEntity),
				binding, endpoint);
		Assert.assertTrue(rsp.hasEntity(),
				ErrorMessage.get(ErrorMessageKeys.MISSING_XML_ENTITY));
		Document entity = extractBodyAsDocument(rsp);
		ETSAssert.assertXPath(
				"//ows:Exception/@exceptionCode = 'InvalidParameterValue'",
				entity.getDocumentElement(), null);
	}

	/**
	 * Sets the valueReference attribute on the request entity.
	 * 
	 * @param entity
	 *            The request entity (wfs:GetPropertyValue).
	 * @param xpath
	 *            A String representing an XPath expression.
	 */
	void setValueReference(Document entity, String xpath) {
		entity.getDocumentElement().setAttribute("valueReference", xpath);
	}

	/**
	 * Adds a simple query element to the request entity.
	 * 
	 * @param request
	 *            The request entity (wfs:GetPropertyValue).
	 * @param qName
	 *            A QName representing the qualified name of a feature type.
	 */
	void addQuery(Document request, QName qName) {
		Element docElem = request.getDocumentElement();
		String nsPrefix = docElem.lookupPrefix(qName.getNamespaceURI());
		if (null == nsPrefix) {
			nsPrefix = "ns" + Integer.toString((int) (Math.random() * 100));
		}
		Element query = request
				.createElementNS(Namespaces.WFS, WFS2.QUERY_ELEM);
		query.setPrefix("wfs");
		query.setAttribute("typeNames", nsPrefix + ":" + qName.getLocalPart());
		docElem.appendChild(query);
		docElem.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + nsPrefix,
				qName.getNamespaceURI());
	}
}
