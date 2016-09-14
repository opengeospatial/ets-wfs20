package org.opengis.cite.iso19142.basic.filter.temporal;

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
 * requirements of the <strong>Minimum Temporal filter</strong> conformance
 * class. All tests are skipped if any preconditions are not met. The filter
 * constraint {@value #IMPL_MIN_TEMPORAL_FILTER} must be set to "TRUE" in the
 * capabilities document. The During operator must be implemented.
 * 
 * <pre>
 * {@code
 * <Conformance xmlns="http://www.opengis.net/fes/2.0">
 *   <Constraint name="ImplementsMinTemporalFilter">
 *     <AllowedValues xmlns="http://www.opengis.net/ows/1.1">
 *       <Value>TRUE</Value>
 *     </AllowedValues>
 *   </Constraint>
 * </Conformance>
 * }
 * </pre>
 * 
 * @see <a target="_blank" href=
 *      "http://docs.opengeospatial.org/is/09-026r2/09-026r2.html#requirement_9">ATC
 *      A.9: Test cases for minimum temporal filter</a>
 */
public class TemporalFilter {

    public final static String IMPL_MIN_TEMPORAL_FILTER = "ImplementsMinTemporalFilter";

    /**
     * This {@literal @BeforeTest} configuration method checks the
     * implementation status of the {@value #IMPL_MIN_TEMPORAL_FILTER}
     * conformance class.
     * 
     * @param testContext
     *            Information about the test run environment.
     */
    @BeforeTest
    public void implementsMinimumTemporalFilter(ITestContext testContext) {
        Document wfsMetadata = (Document) testContext.getSuite().getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        if (!ServiceMetadataUtils.implementsConformanceClass(wfsMetadata, IMPL_MIN_TEMPORAL_FILTER)) {
            throw new SkipException(ErrorMessage.format(ErrorMessageKeys.NOT_IMPLEMENTED, IMPL_MIN_TEMPORAL_FILTER));
        }
    }
}
