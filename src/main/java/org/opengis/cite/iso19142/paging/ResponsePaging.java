package org.opengis.cite.iso19142.paging;

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
 * requirements of the <strong>Response paging</strong> conformance class. All
 * tests are skipped if any preconditions are not met. The service constraint
 * {@value #IMPL_RESULT_PAGING} must be set to "TRUE" in the capabilities
 * document.
 * 
 * <pre>
 * {@code
 * <OperationsMetadata xmlns="http://www.opengis.net/ows/1.1">
 *   <Constraint name="ImplementsResultPaging">
 *     <AllowedValues>
 *       <Value>TRUE</Value>
 *     </AllowedValues>
 *   </Constraint>
 * </OperationsMetadata>
 * }
 * </pre>
 * 
 * @see <a target="_blank" href=
 *      "http://docs.opengeospatial.org/is/09-025r2/09-025r2.html#requirement_10">ATC
 *      A.1.10: Response Paging</a>
 */
public class ResponsePaging {

    public final static String IMPL_RESULT_PAGING = "ImplementsResultPaging";

    /**
     * This {@literal @BeforeTest} configuration method checks the
     * implementation status of the {@value #IMPL_RESULT_PAGING} conformance
     * class.
     * 
     * @param testContext
     *            Information about the test run environment.
     */
    @BeforeTest
    public void implementsResponsePaging(ITestContext testContext) {
        Document wfsMetadata = (Document) testContext.getSuite().getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        if (!ServiceMetadataUtils.implementsConformanceClass(wfsMetadata, IMPL_RESULT_PAGING)) {
            throw new SkipException(ErrorMessage.format(ErrorMessageKeys.NOT_IMPLEMENTED, IMPL_RESULT_PAGING));
        }
    }
}
