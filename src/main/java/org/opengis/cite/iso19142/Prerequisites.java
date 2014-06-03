package org.opengis.cite.iso19142;

import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.opengis.cite.iso19142.util.DataSampler;
import org.opengis.cite.iso19142.util.WFSClient;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.Reporter;
import org.testng.annotations.BeforeSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Confirms the readiness of the SUT to undergo testing. If any of these tests
 * fail then all remaining tests in the suite are skipped.
 * 
 */
public class Prerequisites {

    private static final Logger LOGR = Logger.getLogger(Prerequisites.class
            .getName());

    /**
     * Confirms that the SUT is available and produces a service description in
     * response to a basic GetCapabilities request. The document element is
     * expected to have the following infoset properties:
     * <ul>
     * <li>[local name] = "WFS_Capabilities"</li>
     * <li>[namespace name] = "http://www.opengis.net/wfs/2.0"</li>
     * </ul>
     */
    @BeforeSuite(description = "prerequisite", alwaysRun = true)
    public void serviceIsAvailable(ITestContext testContext) {
        Document wfsMetadata = (Document) testContext.getSuite().getAttribute(
                SuiteAttribute.TEST_SUBJECT.getName());
        WFSClient wfsClient = new WFSClient(wfsMetadata);
        Document capabilities = wfsClient.getCapabilities();
        Assert.assertNotNull(capabilities,
                "No GetCapabilities response from SUT.");
        Element docElement = capabilities.getDocumentElement();
        Assert.assertEquals(docElement.getLocalName(), WFS2.WFS_CAPABILITIES,
                "Capabilities document element has unexpected [local name].");
        Assert.assertEquals(docElement.getNamespaceURI(), Namespaces.WFS,
                "Capabilities document element has unexpected [namespace name].");
    }

    /**
     * Confirms that the SUT can supply data for at least one advertised feature
     * type.
     */
    @BeforeSuite(description = "prerequisite", dependsOnMethods = { "serviceIsAvailable" })
    public void dataAreAvailable(ITestContext testContext) throws Exception {
        Document wfsMetadata = (Document) testContext.getSuite().getAttribute(
                SuiteAttribute.TEST_SUBJECT.getName());
        DataSampler sampler = new DataSampler(wfsMetadata);
        sampler.acquireFeatureData();
        Map<QName, FeatureTypeInfo> featureTypeInfo = sampler
                .getFeatureTypeInfo();
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
            Reporter.getOutput().add(msg);
            throw new AssertionError(msg);
        }
    }
}
