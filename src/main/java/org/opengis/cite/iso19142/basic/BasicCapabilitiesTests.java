package org.opengis.cite.iso19142.basic;

import java.net.URI;

import javax.xml.soap.SOAPException;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMSource;

import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the service response to a GetCapabilities request for "Basic WFS"
 * conformance.
 * 
 * @see "ISO 19142:2010, cl. 8: GetCapabilities operation"
 */
public class BasicCapabilitiesTests extends BaseFixture {

    static final String BASIC_WFS_PHASE = "BasicWFSPhase";
    private static final String SCHEMATRON_METADATA = "wfs-capabilities-2.0.sch";

    @BeforeTest
    public void checkSuitePreconditions(ITestContext context) {
        Object failedPreconditions = context.getSuite().getAttribute(SuiteAttribute.FAILED_PRECONDITIONS.getName());
        if (null != failedPreconditions) {
            throw new SkipException("One or more test suite preconditions were not satisfied: " + failedPreconditions);
        }
    }

    /**
     * Run the tests for the "Basic WFS" conformance class only if the service
     * constraint {@value org.opengis.cite.iso19142.WFS2#BASIC_WFS} has the
     * value 'TRUE'. Otherwise the constituent tests will be skipped.
     * 
     * @param testContext
     *            The test (set) context.
     */
    @BeforeTest
    public void implementsBasicWFS(ITestContext testContext) {
        this.wfsMetadata = (Document) testContext.getSuite().getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        String xpath = String.format("//ows:Constraint[@name='%s']/ows:DefaultValue = 'TRUE'", WFS2.BASIC_WFS);
        ETSAssert.assertXPath(xpath, this.wfsMetadata, null);
    }

    /**
     * Builds a DOM Document representing a GetCapabilities request for a
     * complete service metadata document.
     */
    @BeforeClass
    public void buildGetCapabilitiesRequest() {
        this.reqEntity = this.docBuilder.newDocument();
        Element docElem = reqEntity.createElementNS(Namespaces.WFS, WFS2.GET_CAPABILITIES);
        docElem.setAttribute(WFS2.SERVICE_PARAM, WFS2.SERVICE_TYPE_CODE);
        this.reqEntity.appendChild(docElem);
    }

    /**
     * Verifies that the content of the service metadata (wfs:WFS_Capabilities)
     * document satisfies the requirements for "Basic WFS" conformance.
     * Additional service endpoints, service properties (constraints), and
     * filtering options must be implemented. The applicable rules are
     * incorporated into the {@value #BASIC_WFS_PHASE} phase of the Schematron
     * schema {@code wfs-capabilities-2.0.sch}.
     * 
     * @param binding
     *            The ProtocolBinding to use.
     * @throws SOAPException
     *             If an error occurs while processing a SOAP response message.
     * 
     * @see "ISO 19142:2010, Table 1: Conformance classes"
     * @see "ISO 19142:2010, Table 13: Service constraints"
     * @see "ISO 19142:2010, cl. A.1.2: Basic WFS"
     * @see "ISO 19143:2010, Table 5: Names of conformance class constraints"
     */
    @Test(description = "See ISO 19142: Table 1, Table 13, A.1.2", dataProvider = "protocol-binding")
    public void describesBasicWFS(ProtocolBinding binding) throws SOAPException {
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.GET_CAPABILITIES, binding);
        ClientResponse rsp = wfsClient.submitRequest(new DOMSource(reqEntity), binding, endpoint);
        Assert.assertTrue(rsp.hasEntity(), ErrorMessage.get(ErrorMessageKeys.MISSING_XML_ENTITY));
        Document entity = extractBodyAsDocument(rsp);
        SchematronValidator validator = ValidationUtils.buildSchematronValidator(SCHEMATRON_METADATA, BASIC_WFS_PHASE);
        Result result = validator.validate(new DOMSource(entity, entity.getDocumentURI()), false);
        Assert.assertFalse(validator.ruleViolationsDetected(), ErrorMessage.format(ErrorMessageKeys.NOT_SCHEMA_VALID,
                validator.getRuleViolationCount(), XMLUtils.resultToString(result)));
    }
}
