package org.opengis.cite.iso19142.transaction;

import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMSource;

import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.WFS2;
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

/**
 * Tests the service response to a GetCapabilities request for "Transactional
 * WFS" conformance.
 * 
 * @see "ISO 19142:2010, cl. 8: GetCapabilities operation"
 */
public class TransactionCapabilitiesTests extends BaseFixture {

    static final String TRX_WFS_PHASE = "TransactionalWFSPhase";
    static final String SCHEMATRON_METADATA = "wfs-capabilities-2.0.sch";

    @BeforeTest
    public void checkSuitePreconditions(ITestContext context) {
        Object failedPreconditions = context.getSuite().getAttribute(SuiteAttribute.FAILED_PRECONDITIONS.getName());
        if (null != failedPreconditions) {
            throw new SkipException("One or more test suite preconditions were not satisfied: " + failedPreconditions);
        }
    }

    /**
     * Run the tests for the "Transactional WFS" conformance class only if the
     * service constraint {@value org.opengis.cite.iso19142.WFS2#TRX_WFS} has
     * the value 'TRUE'. Otherwise they will be skipped.
     * 
     * @param testContext
     *            The test (set) context.
     */
    @BeforeTest
    public void implementsTransactionalWFS(ITestContext testContext) {
        this.wfsMetadata = (Document) testContext.getSuite().getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        String xpath = String.format("//ows:Constraint[@name='%s']/ows:DefaultValue = 'TRUE'", WFS2.TRX_WFS);
        ETSAssert.assertXPath(xpath, this.wfsMetadata, null);
    }

    /**
     * Builds a DOM Document representing a GetCapabilities request for a
     * complete service metadata document.
     */
    @BeforeClass
    public void buildGetCapabilitiesRequest() {
        this.reqEntity = this.docBuilder.newDocument();
        Element docElem = this.reqEntity.createElementNS(Namespaces.WFS, WFS2.GET_CAPABILITIES);
        docElem.setAttribute(WFS2.SERVICE_PARAM, WFS2.SERVICE_TYPE_CODE);
        this.reqEntity.appendChild(docElem);
    }

    /**
     * [{@code Test}] Verifies that the content of the complete service metadata
     * document (wfs:WFS_Capabilities) satisfies the requirements for
     * "Transactional WFS" conformance. Additional service endpoints and
     * properties (constraints) must be implemented. The applicable rules are
     * incorporated into the {@value #TRX_WFS_PHASE} phase of the Schematron
     * schema {@code wfs-capabilities-2.0.sch}.
     * 
     * <p style="margin-bottom: 0.5em">
     * <strong>Sources</strong>
     * </p>
     * <ul>
     * <li>ISO 19142:2010, Table 1: Conformance classes</li>
     * <li>ISO 19142:2010, Table 13: Service constraints</li>
     * <li>ISO 19142:2010, cl. A.1.3: Transactional WFS</li>
     * </ul>
     * 
     */
    @Test(description = "See ISO 19142: Table 13, A.2.23")
    public void capabilitiesDescribesTransactionalWFS() {
        SchematronValidator validator = ValidationUtils.buildSchematronValidator(SCHEMATRON_METADATA, TRX_WFS_PHASE);
        Result result = validator.validate(new DOMSource(this.wfsMetadata, this.wfsMetadata.getDocumentURI()), false);
        Assert.assertFalse(validator.ruleViolationsDetected(), ErrorMessage.format(ErrorMessageKeys.NOT_SCHEMA_VALID,
                validator.getRuleViolationCount(), XMLUtils.resultToString(result)));
    }
}
