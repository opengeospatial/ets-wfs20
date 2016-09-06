package org.opengis.cite.iso19142;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.opengis.cite.iso19142.util.DataSampler;
import org.opengis.cite.iso19142.util.WFSClient;
import org.testng.Assert;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Includes tests to confirm the readiness of the SUT to undergo testing. If any
 * of these test methods fail then its name is added to the suite attribute
 * {@link org.opengis.cite.iso19142.SuiteAttribute#FAILED_PRECONDITIONS}. The
 * presence of the attribute can be checked in a <code>@BeforeTest</code> method
 * to determine whether or not a set of tests should be skipped if one or more
 * preconditions were not satisfied.
 * 
 */
public class SuitePreconditions {

    private static final Logger LOGR = Logger.getLogger(SuitePreconditions.class.getName());

    @AfterMethod
    @SuppressWarnings("unchecked")
    public void preconditionNotSatisfied(ITestResult result) {
        if (!result.isSuccess()) {
            Object failedPreconditions = result.getTestContext().getSuite()
                    .getAttribute(SuiteAttribute.FAILED_PRECONDITIONS.getName());
            if (null == failedPreconditions) {
                failedPreconditions = new ArrayList<String>();
                result.getTestContext().getSuite().setAttribute(SuiteAttribute.FAILED_PRECONDITIONS.getName(),
                        failedPreconditions);
            }
            ArrayList.class.cast(failedPreconditions).add(result.getName());
        }
    }

    /**
     * [{@literal @Test}] Verifies that the test subject is a WFS 2.0 service.
     * The document element in the supplied metadata resource must be
     * "{http://www.opengis.net/wfs/2.0}WFS_Capabilities".
     * 
     * @param testContext
     *            The test run context (ITestContext).
     */
    @Test(description = "Test subject is WFS 2.0 service")
    public void verifyServiceDescription(ITestContext testContext) {
        Document wfsMetadata = (Document) testContext.getSuite().getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        ETSAssert.assertQualifiedName(wfsMetadata.getDocumentElement(),
                new QName(Namespaces.WFS, WFS2.WFS_CAPABILITIES));
    }

    /**
     * [{@literal @Test}] Confirms that the SUT is available and produces a
     * service description in response to a basic GetCapabilities request. The
     * document element is expected to have the following infoset properties:
     * <ul>
     * <li>[local name] = "WFS_Capabilities"</li>
     * <li>[namespace name] = "http://www.opengis.net/wfs/2.0"</li>
     * </ul>
     * 
     * @param testContext
     *            Supplies details about the test run.
     */
    @Test(description = "SUT produces GetCapabilities response", dependsOnMethods = { "verifyServiceDescription" })
    public void serviceIsAvailable(ITestContext testContext) {
        Document wfsMetadata = (Document) testContext.getSuite().getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        WFSClient wfsClient = new WFSClient(wfsMetadata);
        Document capabilities = wfsClient.getCapabilities();
        Assert.assertNotNull(capabilities, "No GetCapabilities response from SUT.");
        Element docElement = capabilities.getDocumentElement();
        Assert.assertEquals(docElement.getLocalName(), WFS2.WFS_CAPABILITIES,
                "Capabilities document element has unexpected [local name].");
        Assert.assertEquals(docElement.getNamespaceURI(), Namespaces.WFS,
                "Capabilities document element has unexpected [namespace name].");
    }

    /**
     * [{@literal @Test}] Confirms that the SUT can supply data for at least one
     * advertised feature type.
     * 
     * @param testContext
     *            Supplies details about the test run.
     */
    @Test(description = "SUT has data for at least one advertised feature type", dependsOnMethods = {
            "verifyServiceDescription" })
    public void dataAreAvailable(ITestContext testContext) {
        ISuite suite = testContext.getSuite();
        Document wfsMetadata = (Document) suite.getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        DataSampler sampler = new DataSampler(wfsMetadata);
        try {
            sampler.acquireFeatureData();
        } catch (RuntimeException rx) {
            StringBuilder msg = new StringBuilder("Failed to acquire feature data (");
            msg.append(rx.getClass().getName()).append("): ");
            msg.append(rx.getMessage());
            throw new AssertionError(msg.toString());
        }
        Map<QName, FeatureTypeInfo> featureTypeInfo = sampler.getFeatureTypeInfo();
        boolean sutHasData = false;
        for (FeatureTypeInfo typeInfo : featureTypeInfo.values()) {
            if (typeInfo.isInstantiated()) {
                sutHasData = true;
                break;
            }
        }
        if (!sutHasData) {
            String msg = ErrorMessage.get(ErrorMessageKeys.DATA_UNAVAILABLE);
            LOGR.warning(msg + featureTypeInfo.toString());
            throw new AssertionError(msg);
        }
        suite.setAttribute(SuiteAttribute.FEATURE_INFO.getName(), featureTypeInfo);
        suite.setAttribute(SuiteAttribute.SAMPLER.getName(), sampler);
    }
}
