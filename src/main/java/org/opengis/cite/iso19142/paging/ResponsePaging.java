package org.opengis.cite.iso19142.paging;

import java.util.logging.Level;

import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
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
 * An implementation may ensure transactional consistency for response paging,
 * as indicated by the value of the operation constraint
 * <em>PagingIsTransactionSafe</em> (default: FALSE). This constraint applies to
 * GetFeature, GetFeatureWithLock, and GetPropertyValue requests.
 * 
 * @see <a target="_blank" href=
 *      "http://docs.opengeospatial.org/is/09-025r2/09-025r2.html#requirement_10">ATC
 *      A.1.10: Response Paging</a>
 */
public class ResponsePaging {

    public final static String IMPL_RESULT_PAGING = "ImplementsResultPaging";
    public final static String CACHE_TIMEOUT = "ResponseCacheTimeout";
    public final static String PAGING_IS_CONSISTENT = "PagingIsTransactionSafe";
    public final static String COUNT_DEFAULT = "CountDefault";

    /**
     * This {@literal @BeforeTest} configuration method checks the
     * implementation status of the {@value #IMPL_RESULT_PAGING} conformance
     * class and looks up the values of the relevant operation constraints
     * ({@value ResponsePaging#CACHE_TIMEOUT}, {@value #PAGING_IS_CONSISTENT},
     * and , {@value #COUNT_DEFAULT}).
     * 
     * @param testContext
     *            Information about the test run environment.
     * @see "OGC 09-025: Table 14, A.2.20.2"
     */
    @BeforeTest
    public void implementsResponsePaging(ITestContext testContext) {
        Document wfsMetadata = (Document) testContext.getSuite().getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        if (!ServiceMetadataUtils.implementsConformanceClass(wfsMetadata, IMPL_RESULT_PAGING)) {
            throw new SkipException(ErrorMessage.format(ErrorMessageKeys.NOT_IMPLEMENTED, IMPL_RESULT_PAGING));
        }
        String pagingIsConsistent = ServiceMetadataUtils.getConstraintValue(wfsMetadata, PAGING_IS_CONSISTENT);
        testContext.setAttribute(PAGING_IS_CONSISTENT, Boolean.valueOf(pagingIsConsistent));
        String cacheTimeout = ServiceMetadataUtils.getConstraintValue(wfsMetadata, CACHE_TIMEOUT);
        String countDefault = ServiceMetadataUtils.getConstraintValue(wfsMetadata, COUNT_DEFAULT);
        try {
            if (!cacheTimeout.isEmpty()) {
                testContext.setAttribute(CACHE_TIMEOUT, Integer.valueOf(cacheTimeout));
            }
            if (!countDefault.isEmpty()) {
                testContext.setAttribute(COUNT_DEFAULT, Integer.valueOf(countDefault));
            }
        } catch (NumberFormatException e) {
            // cache never times out or page size is unconstrained
            TestSuiteLogger.log(Level.WARNING,
                    String.format("Invalid constraint (expected integer value): %s", e.getMessage()));
        }
    }
}
