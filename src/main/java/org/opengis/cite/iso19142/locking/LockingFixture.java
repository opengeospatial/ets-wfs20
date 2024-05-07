package org.opengis.cite.iso19142.locking;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.DataSampler;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.w3c.dom.Document;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Provides configuration methods that facilitate the testing of locking
 * behavior specified for the "Locking WFS" conformance level.
 */
public class LockingFixture extends BaseFixture {

    /** List containing lock identifiers */
    protected List<String> locks = new ArrayList<String>();
    /** Acquires and saves sample feature data. */
    protected DataSampler dataSampler;
    /** Identifier for GetFeatureById stored query */
    protected String storedQueryId;

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
    @BeforeClass
    public void initLockingFixture(ITestContext testContext) {
        this.dataSampler = (DataSampler) testContext.getSuite().getAttribute(SuiteAttribute.SAMPLER.getName());
        this.featureInfo = this.dataSampler.getFeatureTypeInfo();
        this.storedQueryId = (this.wfsVersion.equals(WFS2.V2_0_0)) ? WFS2.QRY_GET_FEATURE_BY_ID_URN
                : WFS2.QRY_GET_FEATURE_BY_ID;
    }

    /**
     * Releases all locks by submitting empty Transaction requests that include
     * the lockId and releaseAction (="ALL") attributes. An unsuccessful request
     * is logged (as a {@link Level#WARNING}).
     */
    @AfterMethod
    public void releaseAllLocks() {
        if (locks.isEmpty()) {
            return;
        }
        TestSuiteLogger.log(Level.CONFIG, "releaseAllLocks: " + this.locks);
        Document trxEntity = WFSMessage.createRequestEntity("Transaction", this.wfsVersion);
        trxEntity.getDocumentElement().setAttribute("releaseAction", "ALL");
        for (String lockId : locks) {
            trxEntity.getDocumentElement().setAttribute("lockId", lockId);
            Response rsp = this.wfsClient.submitRequest(trxEntity, ProtocolBinding.ANY);
            if (rsp.getStatus() != Status.OK.getStatusCode()) {
                String entity = rsp.readEntity(String.class);
                TestSuiteLogger.log(Level.WARNING, "Failed to release lock " + lockId + "\n" + entity);
            }
        }
        locks.clear();
    }
}
