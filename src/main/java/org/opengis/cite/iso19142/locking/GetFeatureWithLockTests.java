package org.opengis.cite.iso19142.locking;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.WFSRequest;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the response to a GetFeatureWithLock request that attempts to lock
 * feature instances that belong to a set of query results. The lock is active
 * until it either expires (default duration is 300 s) or it is released by a
 * subsequent transaction.
 * 
 * <h6 style="margin-bottom: 0.5em">Sources</h6>
 * <ul>
 * <li>ISO 19142:2010, cl. 13: GetFeatureWithLock operation</li>
 * <li>ISO 19142:2010, cl. 13.4: Exceptions</li>
 * <li>ISO 19142:2010, cl. 15.2.3.1.1: Declaring support for locking</li>
 * </ul>
 */
public class GetFeatureWithLockTests extends LockingFixture {

    /**
     * Checks that the GetFeatureWithLock operation is implemented by the SUT.
     * If not, all test methods defined in this class are skipped.
     */
    @BeforeClass
    public void sutImplementsGetFeatureWithLock(ITestContext testContext) {
        this.wfsMetadata = (Document) testContext.getSuite().getAttribute(
                SuiteAttribute.TEST_SUBJECT.getName());
        String xpath = String.format("//ows:Operation[@name='%s']",
                WFS2.GET_FEATURE_WITH_LOCK);
        ETSAssert.assertXPath(xpath, this.wfsMetadata, null);
    }

    /**
     * Builds a DOM Document representing a GetFeatureWithLock request entity.
     * It contains default values for all lock-related attributes.
     */
    @BeforeMethod
    public void buildGetFeatureWithLockRequest() {
        this.reqEntity = WFSRequest.createRequestEntity("GetFeatureWithLock");
    }

    /**
     * [{@code Test}] The only valid value for the resultType attribute in a
     * GetFeatureWithLock request is "results". Any other value ("hits") shall
     * produce an exception report with error code "InvalidParameterValue".
     * 
     * @see "ISO 19142:2010, cl. 13.2.4.3: resultType parameter"
     */
    @Test
    public void lockQueryResults_hits() {
        QName featureType = LockFeatureTests
                .selectRandomFeatureType(this.featureInfo);
        WFSRequest.appendSimpleQuery(this.reqEntity, featureType);
        this.reqEntity.getDocumentElement().setAttribute("resultType", "hits");
        ClientResponse rsp = wfsClient.submitRequest(this.reqEntity,
                ProtocolBinding.ANY);
        this.rspEntity = rsp.getEntity(Document.class);
        Assert.assertEquals(rsp.getStatus(),
                ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        String xpath = "//ows:Exception[@exceptionCode = 'InvalidParameterValue']";
        ETSAssert.assertXPath(xpath, this.rspEntity.getDocumentElement(), null);
    }

    /**
     * [{@code Test}] Submits a request to lock all instances of a randomly
     * selected feature type for 30 seconds. After this time has elapsed, an
     * attempt is made to reset the lock; this LockFeature request should fail
     * with a 'LockHasExpired' exception.
     * 
     * @see "ISO 19142:2010, cl. 12.2.4.2: lockId parameter"
     */
    @Test
    public void lockAllQueryResults_30Seconds() {
        QName featureType = LockFeatureTests
                .selectRandomFeatureType(this.featureInfo);
        WFSRequest.appendSimpleQuery(this.reqEntity, featureType);
        this.reqEntity.getDocumentElement().setAttribute("expiry", "30");
        ClientResponse rsp = wfsClient.submitRequest(this.reqEntity,
                ProtocolBinding.ANY);
        this.rspEntity = rsp.getEntity(Document.class);
        Assert.assertEquals(rsp.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Element featureColl = (Element) this.rspEntity.getElementsByTagNameNS(
                Namespaces.WFS, WFS2.FEATURE_COLLECTION).item(0);
        String lockId = featureColl.getAttribute("lockId");
        Assert.assertFalse(lockId.isEmpty(), ErrorMessage.format(
                ErrorMessageKeys.MISSING_INFOSET_ITEM, "@lockId"));
        locks.add(lockId);
        try {
            Thread.sleep(34 * 1000);
        } catch (InterruptedException e) {
            // ignore interrupt should one occur
        }
        // try to reset expired lock with LockFeature request
        reqEntity = WFSRequest.createRequestEntity("LockFeature");
        WFSRequest.appendStoredQuery(reqEntity, WFS2.QRY_GET_FEATURE_BY_TYPE,
                Collections.singletonMap("typeName", (Object) featureType));
        reqEntity.getDocumentElement().setAttribute("lockId", lockId);
        rsp = wfsClient.submitRequest(reqEntity, ProtocolBinding.ANY);
        this.rspEntity = rsp.getEntity(Document.class);
        Assert.assertEquals(rsp.getStatus(),
                ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        String xpath = "//ows:Exception[@exceptionCode = 'LockHasExpired']";
        ETSAssert.assertXPath(xpath, this.rspEntity.getDocumentElement(), null);
    }

    /**
     * [{@code Test}] Submits a request to lock all instances of a randomly
     * selected feature type (Q1) for 60 seconds. Another request is then
     * submitted to lock SOME of Q1 plus the members of result set Q2 (different
     * feature type). This request should succeed; the response shall only
     * contain those features that were successfully locked (members of Q2
     * results but <strong>not</strong> Q1 results that were previously locked).
     * 
     * @see "ISO 19142:2010, cl. 13.2.4.2: lockAction parameter"
     */
    @Test
    public void lockSomeQueryResults() {
        QName featureType1 = LockFeatureTests
                .selectRandomFeatureType(this.featureInfo);
        WFSRequest.appendSimpleQuery(this.reqEntity, featureType1);
        this.reqEntity.getDocumentElement().setAttribute("expiry", "60");
        XMLUtils.writeNode(this.reqEntity, System.out);
        ClientResponse rsp = wfsClient.submitRequest(this.reqEntity,
                ProtocolBinding.ANY);
        this.rspEntity = rsp.getEntity(Document.class);
        Assert.assertEquals(rsp.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Element featureColl = (Element) this.rspEntity.getElementsByTagNameNS(
                Namespaces.WFS, WFS2.FEATURE_COLLECTION).item(0);
        String lockId = featureColl.getAttribute("lockId");
        Assert.assertFalse(lockId.isEmpty(), ErrorMessage.format(
                ErrorMessageKeys.MISSING_INFOSET_ITEM,
                "@lockId in response to GetFeatureWithLock"));
        locks.add(lockId);
        QName featureType2 = LockFeatureTests
                .selectRandomFeatureType(this.featureInfo);
        // WARNING: could be same feature type
        WFSRequest.appendSimpleQuery(this.reqEntity, featureType2);
        this.reqEntity.getDocumentElement().setAttribute("lockAction", "SOME");
        XMLUtils.writeNode(this.reqEntity, System.out);
        rsp = wfsClient.submitRequest(this.reqEntity, ProtocolBinding.ANY);
        this.rspEntity = rsp.getEntity(Document.class);
        Assert.assertEquals(rsp.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        featureColl = (Element) this.rspEntity.getElementsByTagNameNS(
                Namespaces.WFS, WFS2.FEATURE_COLLECTION).item(0);
        lockId = featureColl.getAttribute("lockId");
        Assert.assertFalse(lockId.isEmpty(), ErrorMessage.format(
                ErrorMessageKeys.MISSING_INFOSET_ITEM,
                "@lockId in response to GetFeatureWithLock"));
        locks.add(lockId);
        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put(featureType1.getNamespaceURI(), "ns1");
        // response must exclude features that were not locked (already locked)
        String xpath1 = String.format("not(//ns1:%s)",
                featureType1.getLocalPart());
        ETSAssert.assertXPath(xpath1, this.rspEntity.getDocumentElement(),
                nsBindings);
        nsBindings.put(featureType2.getNamespaceURI(), "ns2");
        // response must include features that were successfully locked
        String xpath2 = String.format("//ns2:%s", featureType2.getLocalPart());
        ETSAssert.assertXPath(xpath2, this.rspEntity.getDocumentElement(),
                nsBindings);
    }
}
