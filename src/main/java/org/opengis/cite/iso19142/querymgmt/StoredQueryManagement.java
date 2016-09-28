package org.opengis.cite.iso19142.querymgmt;

import static org.testng.Assert.*;

import javax.xml.xpath.XPathExpressionException;

import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.XMLUtils;

import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

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

    public final static String LANG_WFS_QUERY = "urn:ogc:def:queryLanguage:OGC-WFS::WFSQueryExpression";
    public final static String MANAGE_STORED_QRY = "ManageStoredQueries";

    /**
     * This {@literal @BeforeTest} configuration method checks the
     * implementation status of the {@value #MANAGE_STORED_QRY} conformance
     * class.
     * 
     * @param testContext
     *            Information about the test run.
     */
    @BeforeTest
    public void implementsManageStoredQueries(ITestContext testContext) {
        Document wfsMetadata = (Document) testContext.getSuite().getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        if (!ServiceMetadataUtils.implementsConformanceClass(wfsMetadata, MANAGE_STORED_QRY)) {
            throw new SkipException(ErrorMessage.format(ErrorMessageKeys.NOT_IMPLEMENTED, MANAGE_STORED_QRY));
        }
    }

    /**
     * [{@code Test}] Confirms that the capabilities document advertises support
     * for the (stored) query language {@value #LANG_WFS_QUERY} in the context
     * of <code>CreateStoredQuery</code>.
     * 
     * @param testContext
     *            Information about the test run.
     */
    @Test(description = "See OGC 09-025: Table 12, 14.2.2.5.3")
    public void supportedStoredQueryLanguages(ITestContext testContext) {
        Document wfsMetadata = (Document) testContext.getSuite().getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        NodeList result = null;
        try {
            result = XMLUtils.evaluateXPath(wfsMetadata,
                    "//ows:Operation[name='CreateStoredQuery']/ows:Parameter[@name='language']", null);
        } catch (XPathExpressionException e) { // valid expression
        }
        assertTrue(result.getLength() > 0, "Missing 'language' parameter for CreateStoredQuery.");
        assertTrue(result.item(0).getTextContent().contains(LANG_WFS_QUERY),
                ErrorMessage.format(ErrorMessageKeys.QRY_LANG_NOT_SUPPORTED, LANG_WFS_QUERY));
    }
}
