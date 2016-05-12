package org.opengis.cite.iso19142.simple;

import com.sun.jersey.api.client.ClientResponse;
import java.net.URI;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.validation.ValidationErrorHandler;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests the service response to a ListStoredQueries request. A conforming
 * service must implement at least the GetFeatureById stored query, which has
 * the identifier {@value org.opengis.cite.iso19142.WFS2#QRY_GET_FEATURE_BY_ID}.
 * 
 * @see "ISO 19142:2010, cl. 14.3: ListStoredQueries operation"
 * @see "ISO 19142:2010, cl. 7.9.3.6: GetFeatureById stored query"
 */
public class ListStoredQueriesTests extends BaseFixture {

	private Schema wfsSchema;

	/**
	 * Retrieves the (pre-compiled) WFS schema from the suite fixture and builds
	 * the XML request entity.
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
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			this.reqEntity = builder.parse(getClass().getResourceAsStream(
					"ListStoredQueries.xml"));
		} catch (Exception e) {
			TestSuiteLogger.log(Level.WARNING,
					"Failed to parse request entity from classpath", e);
		}
	}

	/**
	 * The response to a ListStoredQueries request must include an XML entity
	 * having wfs:ListStoredQueriesResponse as the document element. The
	 * document must (a) be schema valid, and (b) contain one or more
	 * wfs:StoredQuery elements, including the mandatory GetFeatureById query.
	 * 
	 * @param binding
	 *            The ProtocolBinding to use.
	 * 
	 * @see "ISO 19142:2010, cl. 14.3.4: Response"
	 */
	@Test(description = "See ISO 19142: 14.3.4", dataProvider = "protocol-binding")
	public void listStoredQueries(ProtocolBinding binding) {
		URI endpoint = ServiceMetadataUtils.getOperationEndpoint(
				this.wfsMetadata, WFS2.LIST_STORED_QUERIES, binding);
		ClientResponse rsp = wfsClient.submitRequest(new DOMSource(
				this.reqEntity), binding, endpoint);
		Assert.assertTrue(rsp.hasEntity(),
				ErrorMessage.get(ErrorMessageKeys.MISSING_XML_ENTITY));
		this.rspEntity = extractBodyAsDocument(rsp);
		Validator validator = this.wfsSchema.newValidator();
		ValidationErrorHandler errHandler = new ValidationErrorHandler();
		validator.setErrorHandler(errHandler);
		try {
			validator.validate(new DOMSource(this.rspEntity, this.rspEntity
					.getDocumentURI()));
		} catch (Exception ex) {
			// unlikely with DOM object and ErrorHandler set
			TestSuiteLogger.log(Level.WARNING,
					"Failed to validate WFS capabilities document", ex);
		}
		Assert.assertFalse(errHandler.errorsDetected(), ErrorMessage.format(
				ErrorMessageKeys.NOT_SCHEMA_VALID, errHandler.getErrorCount(),
				errHandler.toString()));
		String xpath = String.format("//wfs:StoredQuery[@id='%s' or @id='%s']",
				WFS2.QRY_GET_FEATURE_BY_ID, WFS2.QRY_GET_FEATURE_BY_ID_URN);
		ETSAssert.assertXPath(xpath, this.rspEntity.getDocumentElement(), null);
	}
}
