package org.opengis.cite.iso19142.simple;

import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.util.ValidationUtils;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.opengis.cite.validation.SchematronValidator;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Contains tests regarding service metadata resources, especially the content
 * of the WFS capabilities document. The capabilities document must be
 * well-formed and schema-valid.
 */
public class ServiceMetadataTests extends BaseFixture {

    static final String ROOT_PKG = "/org/opengis/cite/iso19142/";
    static final String SIMPLE_WFS_PHASE = "SimpleWFSPhase";
    private Schema wfsSchema;

    /**
     * Obtains the WFS 2.0 message schema from the test context.
     * 
     * @param testContext
     *            The test (group) context.
     */
    @BeforeClass(alwaysRun = true)
    public void obtainWFSSchema(ITestContext testContext) {
        this.wfsSchema = (Schema) testContext.getSuite().getAttribute(SuiteAttribute.WFS_SCHEMA.getName());
        Assert.assertNotNull(this.wfsSchema, "WFS schema not found in suite fixture.");
    }

    /**
     * Verifies that the WFS capabilities document is valid with respect to the
     * official {@code wfs.xsd} schema.
     * 
     * @see "ISO 19142:2010, cl. 8.3.2: GetCapabilities - Response"
     */
    @Test(description = "See ISO 19142: 8.3.2")
    public void capabilitiesDocIsXmlSchemaValid() {
        Validator validator = this.wfsSchema.newValidator();
        ETSAssert.assertSchemaValid(validator, new DOMSource(this.wfsMetadata, this.wfsMetadata.getDocumentURI()));
    }

    /**
     * Checks that the content of the WFS capabilities document reflects the
     * {@code Simple WFS} conformance class. The applicable rules are
     * incorporated into the {@value #SIMPLE_WFS_PHASE} phase of the Schematron
     * schema {@code wfs-capabilities-2.0.sch}.
     * 
     * @see "ISO 19142:2010, cl. A.1.1: Simple WFS"
     * @see "ISO 19142:2010, cl. A.2.23: Declaring conformance"
     */
    @Test(description = "See ISO 19142: A.1.1, A.2.23")
    public void capabilitiesDocCorrespondsToWfsSimple() {
        SchematronValidator validator = ValidationUtils.buildSchematronValidator("wfs-capabilities-2.0.sch",
                SIMPLE_WFS_PHASE);
        Result result = validator.validate(new DOMSource(this.wfsMetadata, this.wfsMetadata.getDocumentURI()), false);
        Assert.assertFalse(validator.ruleViolationsDetected(), ErrorMessage.format(ErrorMessageKeys.NOT_SCHEMA_VALID,
                validator.getRuleViolationCount(), XMLUtils.resultToString(result)));
    }
}
