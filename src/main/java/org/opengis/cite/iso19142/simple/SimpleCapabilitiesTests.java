package org.opengis.cite.iso19142.simple;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.ValidationUtils;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.opengis.cite.validation.SchematronValidator;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Tests the service response to a GetCapabilities request for "Simple WFS" conformance.
 * The HTTP GET method must be supported by all conforming implementations.
 *
 * @see "ISO 19142:2010, cl. 8: GetCapabilities operation"
 */
public class SimpleCapabilitiesTests extends BaseFixture {

	private URI reqEndpointUsingGET;

	private Client client;

	@BeforeTest
	public void checkSuitePreconditions(ITestContext context) {
		Object failedPreconditions = context.getSuite().getAttribute(SuiteAttribute.FAILED_PRECONDITIONS.getName());
		if (null != failedPreconditions) {
			throw new SkipException("One or more test suite preconditions were not satisfied: " + failedPreconditions);
		}
	}

	/**
	 * Extracts the GET request endpoint from the capabilities document.
	 * @param testContext The test (set) context.
	 */
	@BeforeClass
	public void extractEndpoint(ITestContext testContext) {
		this.client = ClientBuilder.newClient();
		this.reqEndpointUsingGET = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.GET_CAPABILITIES,
				ProtocolBinding.GET);
	}

	/**
	 * A GET request that omits a mandatory query parameter must produce a response with
	 * status code 400 (Bad Request) and an exception report containing the exception code
	 * {@code MissingParameterValue}.
	 *
	 * @see "ISO 19142:2010, cl. 7.5: Exception reporting"
	 * @see "OGC 06-121r3, cl. 8: Exception reports"
	 * @see "OGC 06-121r3, cl. A.4.1.5: HTTP response status code"
	 */
	@Test(description = "See ISO 19142: 7.5")
	public void getCapabilities_missingServiceParam() {
		WebTarget target = client.target(reqEndpointUsingGET);
		target = target.queryParam(WFS2.REQUEST_PARAM, WFS2.GET_CAPABILITIES);
		Response rsp = target.request(MediaType.APPLICATION_XML).get();
		Assert.assertEquals(rsp.getStatus(), Response.Status.BAD_REQUEST.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		Assert.assertTrue(rsp.hasEntity(), ErrorMessage.get(ErrorMessageKeys.MISSING_XML_ENTITY));
		Object entity = rsp.getEntity();
		if (entity instanceof InputStream) {
			try {
				this.rspEntity = docBuilder.parse((InputStream) entity);
			}
			catch (SAXException | IOException e) {
				throw new AssertionError(e.getMessage());
			}
		}
		SchematronValidator validator = ValidationUtils.buildSchematronValidator("ExceptionReport.sch",
				"MissingParameterValuePhase");
		Result result = validator.validate(new DOMSource(this.rspEntity));
		Assert.assertFalse(validator.ruleViolationsDetected(), ErrorMessage.format(ErrorMessageKeys.NOT_SCHEMA_VALID,
				validator.getRuleViolationCount(), XMLUtils.resultToString(result)));
	}

	/**
	 * A minimally valid GetCapabilities request must produce a complete representation of
	 * the service capabilities document. The content of the document must correspond to
	 * the "Simple WFS" conformance level.
	 *
	 * @see "OGC 06-121r3, cl. 7.2: GetCapabilities request"
	 */
	@Test(description = "See ISO 19142: 7.2")
	public void getFullCapabilities() {
		WebTarget target = client.target(reqEndpointUsingGET);
		target = target.queryParam(WFS2.REQUEST_PARAM, WFS2.GET_CAPABILITIES);
		target = target.queryParam(WFS2.SERVICE_PARAM, WFS2.SERVICE_TYPE_CODE);
		Response rsp = target.request(MediaType.APPLICATION_XML).get();
		Assert.assertEquals(rsp.getStatus(), Response.Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		Object entity = rsp.getEntity();
		if (entity instanceof InputStream) {
			try {
				this.rspEntity = docBuilder.parse((InputStream) entity);
			}
			catch (SAXException | IOException e) {
				throw new AssertionError(e.getMessage());
			}
		}
		Assert.assertNotNull(this.rspEntity, ErrorMessage.get(ErrorMessageKeys.MISSING_XML_ENTITY));
		SchematronValidator validator = ValidationUtils.buildSchematronValidator("wfs-capabilities-2.0.sch",
				"SimpleWFSPhase");
		Result result = validator.validate(new DOMSource(this.rspEntity, this.rspEntity.getDocumentURI()), false);
		Assert.assertFalse(validator.ruleViolationsDetected(), ErrorMessage.format(ErrorMessageKeys.NOT_SCHEMA_VALID,
				validator.getRuleViolationCount(), XMLUtils.resultToString(result)));
	}

	/**
	 * Acceptable versions of the capabilities document are specified in order of
	 * preference (10.0.0, 2.0.0, 1.1.0). The response document must conform to the first
	 * version number that the SUT supports. All WFS implementations must be able to
	 * perform rudimentary version negotiation in this manner.
	 * @param binding The ProtocolBinding to use.
	 *
	 * @see "OGC 06-121r3, cl. 7.2: GetCapabilities request"
	 * @see "OGC 06-121r3, cl. 7.3.2: Version negotiation"
	 */
	@Test(description = "See ISO 19142: 7.2, 7.3.2", dataProvider = "protocol-binding")
	public void getCapabilities_acceptVersions(ProtocolBinding binding) {
		InputStream entityStream = getClass().getResourceAsStream("getCapabilities_acceptVersions.xml");
		URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.GET_CAPABILITIES, binding);
		Response rsp = wfsClient.submitRequest(new StreamSource(entityStream), binding, endpoint);
		Assert.assertTrue(rsp.hasEntity(), ErrorMessage.get(ErrorMessageKeys.MISSING_XML_ENTITY));
		this.rspEntity = extractBodyAsDocument(rsp);
		String xpath = "/wfs:WFS_Capabilities/@version = '2.0.0'";
		ETSAssert.assertXPath(xpath, this.rspEntity.getDocumentElement(), null);
	}

}
