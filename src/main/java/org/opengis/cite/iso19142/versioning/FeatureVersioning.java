package org.opengis.cite.iso19142.versioning;

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
 * requirements of the <strong>Feature versions</strong> conformance class. All
 * tests are skipped if any preconditions are not met. The implementation
 * constraints {@value #IMPL_FEATURE_VERSIONING} and {@value #IMPL_VERSION_NAV}
 * (FES) must be set to "TRUE" in the capabilities document.
 * 
 * <pre>
 * {@code
 * <OperationsMetadata xmlns="http://www.opengis.net/ows/1.1">
 *   <Constraint name="ImplementsFeatureVersioning">
 *     <AllowedValues>
 *       <Value>TRUE</Value>
 *     </AllowedValues>
 *   </Constraint>
 * </OperationsMetadata>
 * }
 * </pre>
 * 
 * @see <a target="_blank" href=
 *      "http://docs.opengeospatial.org/is/09-025r2/09-025r2.html#requirement_14">ATC
 *      A.1.14: Feature versions</a>
 */
public class FeatureVersioning {

    public final static String IMPL_FEATURE_VERSIONING = "ImplementsFeatureVersioning";
    public final static String IMPL_VERSION_NAV = "ImplementsVersionNav";

    /**
     * This {@literal @BeforeTest} configuration method checks the
     * implementation status of the {@value #IMPL_FEATURE_VERSIONING} and
     * {@value #IMPL_VERSION_NAV} (FES) conformance classes.
     * 
     * @param testContext
     *            Information about the test run environment.
     */
    @BeforeTest
    public void implementsFeatureVersioning(ITestContext testContext) {
        Document wfsMetadata = (Document) testContext.getSuite().getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        if (!ServiceMetadataUtils.implementsConformanceClass(wfsMetadata, IMPL_FEATURE_VERSIONING)) {
            throw new SkipException(ErrorMessage.format(ErrorMessageKeys.NOT_IMPLEMENTED, IMPL_FEATURE_VERSIONING));
        }
        if (!ServiceMetadataUtils.implementsConformanceClass(wfsMetadata, IMPL_FEATURE_VERSIONING)) {
            throw new SkipException(ErrorMessage.format(ErrorMessageKeys.NOT_IMPLEMENTED, IMPL_FEATURE_VERSIONING));
        }
        if (!ServiceMetadataUtils.implementsConformanceClass(wfsMetadata, IMPL_VERSION_NAV)) {
            throw new SkipException(ErrorMessage.format(ErrorMessageKeys.NOT_IMPLEMENTED, IMPL_VERSION_NAV));
        }
    }
}
