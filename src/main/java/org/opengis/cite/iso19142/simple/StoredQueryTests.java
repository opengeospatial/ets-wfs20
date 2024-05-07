package org.opengis.cite.iso19142.simple;

import java.net.URI;
import java.util.Collections;
import java.util.UUID;

import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;

import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.DataSampler;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.ValidationUtils;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.opengis.cite.validation.SchematronValidator;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Tests the service response to a GetFeature request that invokes a stored
 * query. A WFS implementation is required to support stored queries at this
 * conformance level.
 * 
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li>ISO 19142:2010, cl. A.1.1: Simple WFS</li>
 * <li>ISO 19142:2010, cl. 11: GetFeature operation</li>
 * <li>ISO 19142:2010, cl. 7.9.3: Stored query expression</li>
 * </ul>
 * 
 */
public class StoredQueryTests extends BaseFixture {

    private Schema wfsSchema;
    private String queryId;
    private DataSampler dataSampler;

    /**
     * Initializes the test class fixture. The (pre-compiled) WFS schema is retrieved from the test run context.
     * 
     * @param testContext
     *            The test run context.
     */
    @BeforeClass
    public void initClassFixture(ITestContext testContext) {
        this.wfsSchema = (Schema) testContext.getSuite().getAttribute(SuiteAttribute.WFS_SCHEMA.getName());
        Assert.assertNotNull(this.wfsSchema, "WFS schema not found in suite fixture.");
        this.queryId = (this.wfsVersion.equals(WFS2.V2_0_0)) ? WFS2.QRY_GET_FEATURE_BY_ID_URN
                : WFS2.QRY_GET_FEATURE_BY_ID;
        this.dataSampler = (DataSampler) testContext.getSuite().getAttribute(SuiteAttribute.SAMPLER.getName());
    }

    /**
     * Builds a DOM Document representing a GetFeature request entity that
     * contains no query expressions.
     */
    @BeforeMethod
    public void buildGetFeatureRequestEntity() {
        this.reqEntity = WFSMessage.createRequestEntity("GetFeature", this.wfsVersion);
    }

    /**
     * [{@code Test}] If no stored query matches the given identifier then an
     * exception report with exception code "InvalidParameterValue" is expected.
     * 
     * @param binding
     *            The ProtocolBinding to use.
     * 
     * @see "ISO 19142:2010, cl. 7.9.3.4: Stored query identifier"
     */
    @Test(description = "See ISO 19142: 7.9.3.4", dataProvider = "protocol-binding")
    public void unknownStoredQuery(ProtocolBinding binding) {
        WFSMessage.appendStoredQuery(this.reqEntity, "http://docbook.org/ns/docbook",
                Collections.<String, Object>emptyMap());
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.GET_FEATURE, binding);
        Response rsp = wfsClient.submitRequest(new DOMSource(this.reqEntity), binding, endpoint);
        Assert.assertTrue(rsp.hasEntity(), ErrorMessage.get(ErrorMessageKeys.MISSING_XML_ENTITY));
        this.rspEntity = extractBodyAsDocument(rsp);
        SchematronValidator validator = ValidationUtils.buildSchematronValidator("ExceptionReport.sch",
                "InvalidParameterValuePhase");
        Result invalidaParamValueResult = validator.validate(new DOMSource(this.rspEntity), false);
	boolean ruleViolated = validator.ruleViolationsDetected();
	
	SchematronValidator operationParsingvalidator = ValidationUtils.buildSchematronValidator("ExceptionReport.sch", "OperationParsingFailedPhase");
        Result operationParsingResult = operationParsingvalidator.validate(new DOMSource(this.rspEntity), false);
        
        if (!ruleViolated) {
            //Assertion for InvalidParameterException
            Assert.assertFalse(ruleViolated, ErrorMessage.format(ErrorMessageKeys.NOT_SCHEMA_VALID, validator.getRuleViolationCount(),
                            XMLUtils.resultToString(invalidaParamValueResult)));
        } else if(!operationParsingvalidator.ruleViolationsDetected()) {     
            //Assertion for OperationParsingFailed
            Assert.assertFalse(operationParsingvalidator.ruleViolationsDetected(), ErrorMessage.format(ErrorMessageKeys.NOT_SCHEMA_VALID, operationParsingvalidator.getRuleViolationCount(),
                            XMLUtils.resultToString(operationParsingResult)));
        } else {
            //Assertion fail if both exception code not found.
            Assert.fail("Required exception code not found in response. Test expects either InvalidParameterException or OperationParsingFailed exception code.");
        }
    }

    /**
     * [{@code Test}] Invoking the {@code GetFeatureById} query with an
     * {@code id} parameter value that does not match any feature should produce
     * an error response with status code 404 (Not Found). The corresponding OGC
     * exception code in the response entity, if present, must be "NotFound".
     * 
     * In the WFS 2.0.0 specification, clause 11.4 stipulates that "In the event
     * that a web feature service encounters an error processing a GetFeature
     * request, it shall raise an OperationProcessingFailed exception as
     * described in 7.5." In Table D.2 this exception code is mapped to status
     * code 403 for some reason.
     * 
     * @param binding
     *            The ProtocolBinding to use.
     *
     * @see "OGC 09-026r2, cl. 11.3.5: GetFeatureById response"
     * @see "OGC 09-026r1, cl. 7.9.3.6: GetFeatureById stored query"
     */
    @Test(description = "See ISO 19142: 7.9.3.6, 11.4", dataProvider = "protocol-binding")
    public void invokeGetFeatureByIdWithUnknownID(ProtocolBinding binding) {
        String id = "uuid-" + UUID.randomUUID().toString();
        WFSMessage.appendStoredQuery(this.reqEntity, this.queryId, Collections.singletonMap("id", (Object) id));
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.GET_FEATURE, binding);
        Response rsp = wfsClient.submitRequest(new DOMSource(this.reqEntity), binding, endpoint);
        this.rspEntity = extractBodyAsDocument(rsp);
        int statusCode = rsp.getStatus();
        if (this.wfsVersion.equals("2.0.0")) {
            Assert.assertTrue(
                    statusCode == Status.NOT_FOUND.getStatusCode()
                             || statusCode == Status.FORBIDDEN.getStatusCode(),
                    "Expected status code 404 or 403. Received: " + statusCode);
        } else {
            Assert.assertTrue(
                    statusCode == Status.INTERNAL_SERVER_ERROR.getStatusCode()
                            || statusCode == Status.FORBIDDEN.getStatusCode()
                            || statusCode == Status.NOT_FOUND.getStatusCode(),
                    ErrorMessageKeys.UNEXPECTED_STATUS);
        }
    }

    /**
     * [{@code Test}] Invoking the {@code GetFeatureById} query with a known
     * feature identifier shall produce the matching feature representation
     * (@gml:id) as the response entity. If there is no matching feature, an
     * error response with a status code 404 (Not Found) is expected.
     * 
     * @param binding
     *            The ProtocolBinding to use.
     * 
     * @see "ISO 19142:2010, cl. 7.9.3.6: GetFeatureById stored query"
     */
    @Test(description = "See ISO 19142: 7.9.3.6", dataProvider = "protocol-binding")
    public void invokeGetFeatureById( ProtocolBinding binding ) {
        String featureIdToRequest = this.dataSampler.getFeatureId();
        Assert.assertTrue( featureIdToRequest != null && !featureIdToRequest.isEmpty(),
                           ErrorMessage.get( ErrorMessageKeys.FID_NOT_FOUND ) );
        WFSMessage.appendStoredQuery( this.reqEntity, this.queryId, Collections.singletonMap( "id", featureIdToRequest ) );
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint( this.wfsMetadata, WFS2.GET_FEATURE, binding );
        Response rsp = wfsClient.submitRequest( new DOMSource( this.reqEntity ), binding, endpoint );
        Assert.assertTrue( rsp.hasEntity(), ErrorMessage.get( ErrorMessageKeys.MISSING_XML_ENTITY ) );
        this.rspEntity = extractBodyAsDocument( rsp );
        Element feature = this.rspEntity.getDocumentElement();
        Assert.assertEquals( feature.getAttributeNS( Namespaces.GML, "id" ), featureIdToRequest,
                             ErrorMessage.get( ErrorMessageKeys.UNEXPECTED_ID ) );
    }

}
