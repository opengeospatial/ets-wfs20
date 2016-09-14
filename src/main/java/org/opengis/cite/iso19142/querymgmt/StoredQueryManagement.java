package org.opengis.cite.iso19142.querymgmt;

import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeTest;
import org.w3c.dom.Document;

/**
 * Checks preconditions for running tests to verify that the IUT satisfies the
 * requirements of the <strong>Manage stored queries</strong> conformance class.
 * All tests are skipped if any preconditions are not met. The service
 * constraint {@value #MANAGE_STORED_QRY} must be set to "TRUE" in the
 * capabilities document.
 * 
 * <pre>
 * {@code
 * <OperationsMetadata xmlns="http://www.opengis.net/ows/1.1">
 *   <Constraint name="ManageStoredQueries">
 *     <AllowedValues>
 *       <Value>TRUE</Value>
 *     </AllowedValues>
 *   </Constraint>
 * </OperationsMetadata>
 * }
 * </pre>
 * 
 * @see <a target="_blank" href=
 *      "http://docs.opengeospatial.org/is/09-025r2/09-025r2.html#requirement_15">ATC
 *      A.1.15: Manage stored queries</a>
 */
public class StoredQueryManagement {

    public final static String MANAGE_STORED_QRY = "ManageStoredQueries";

    /**
     * This {@literal @BeforeTest} configuration method checks the
     * implementation status of the {@value #MANAGE_STORED_QRY} conformance
     * class.
     * 
     * @param testContext
     *            Information about the test run environment.
     */
    @BeforeTest
    public void implementsManageStoredQueries(ITestContext testContext) {
        Document wfsMetadata = (Document) testContext.getSuite().getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        if (!ServiceMetadataUtils.implementsConformanceClass(wfsMetadata, MANAGE_STORED_QRY)) {
            throw new SkipException(ErrorMessage.format(ErrorMessageKeys.NOT_IMPLEMENTED, MANAGE_STORED_QRY));
        }
    }
}
