package org.opengis.cite.iso19142.locking;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.util.DataSampler;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.iso19142.util.WFSRequest;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.w3c.dom.Document;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Provides configuration methods that facilitate the testing of locking
 * behavior specified for the "Locking WFS" conformance level.
 */
public class LockingFixture extends BaseFixture {

    /** List containing lock identifiers */
    protected List<String> locks = new ArrayList<String>();
    /** Acquires and saves sample feature data. */
    protected DataSampler dataSampler;

    public LockingFixture() {
        super();
    }

    /**
     * Obtains a DataSampler object from the test run context (the value of the
     * {@link SuiteAttribute#SAMPLER SuiteAttribute.SAMPLER attribute}).
     * 
     * @param testContext
     *            The test run context.
     */
    @BeforeClass(alwaysRun = true)
    public void initLockingFixture(ITestContext testContext) {
        this.dataSampler = (DataSampler) testContext.getSuite().getAttribute(
                SuiteAttribute.SAMPLER.getName());
        this.featureInfo = this.dataSampler.getFeatureTypeInfo();
    }

    /**
     * Releases all locks by submitting empty Transaction requests that include
     * the lockId and releaseAction (="ALL") attributes. An unsuccessful request
     * is logged (as a {@link Level#WARNING}).
     */
    @AfterMethod(alwaysRun = true)
    public void releaseAllLocks() {
        if (locks.isEmpty()) {
            return;
        }
        Document trxEntity = WFSRequest.createRequestEntity("Transaction");
        trxEntity.getDocumentElement().setAttribute("releaseAction", "ALL");
        for (String lockId : locks) {
            trxEntity.getDocumentElement().setAttribute("lockId", lockId);
            ClientResponse rsp = this.wfsClient.submitRequest(trxEntity,
                    ProtocolBinding.ANY);
            if (rsp.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
                String entity = rsp.getEntity(String.class);
                TestSuiteLogger.log(Level.WARNING, "Failed to release lock "
                        + lockId + "\n" + entity);
            }
        }
        locks.clear();
    }
}
