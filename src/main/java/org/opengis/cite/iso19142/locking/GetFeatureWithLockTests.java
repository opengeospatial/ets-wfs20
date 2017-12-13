package org.opengis.cite.iso19142.locking;

import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;

import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.WFSMessage;
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
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
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
	 * 
	 * @param testContext
	 *            Supplies details about the test run.
	 */
	@BeforeClass(alwaysRun = true)
	public void sutImplementsGetFeatureWithLock(ITestContext testContext) {
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
		this.reqEntity = WFSMessage.createRequestEntity("GetFeatureWithLock",
				this.wfsVersion);
	}

	/**
	 * [{@code Test}] The only valid value for the resultType attribute in a
	 * GetFeatureWithLock request is "results". Any other value ("hits") shall
	 * produce an exception report with error code "InvalidParameterValue".
	 * 
	 * @see "ISO 19142:2010, cl. 13.2.4.3: resultType parameter"
	 */
	@Test(description = "See ISO 19142: 13.2.4.3")
	public void lockQueryResults_hits() {
		QName featureType = LockFeatureTests
				.selectRandomFeatureType(this.featureInfo);
		WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
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
	 * selected feature type for 20 seconds. After this time has elapsed, an
	 * attempt is made to reset the lock; this LockFeature request should fail
	 * with a 'LockHasExpired' exception and HTTP status code 403 (Forbidden).
	 * 
	 * <p style="margin-bottom: 0.5em">
	 * <strong>Sources</strong>
	 * </p>
	 * <ul>
	 * <li>ISO 19142:2010, cl. 12.2.4.2: lockId parameter</li>
	 * <li>ISO 19142:2010, Table D.2</li>
	 * </ul>
	 */
	@Test(description = "See ISO 19142: 12.2.4.2, Table D.2")
	public void lockAllQueryResults_20Seconds() {
		QName featureType = LockFeatureTests
				.selectRandomFeatureType(this.featureInfo);
		WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
		this.reqEntity.getDocumentElement().setAttribute("expiry", "20");
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
		this.reqEntity = WFSMessage.createRequestEntity("LockFeature",
				this.wfsVersion);
		reqEntity.getDocumentElement().setAttribute("lockId", lockId);
		rsp = wfsClient.submitRequest(reqEntity, ProtocolBinding.ANY);
		this.rspEntity = rsp.getEntity(Document.class);
		Assert.assertEquals(rsp.getStatus(),
				ClientResponse.Status.FORBIDDEN.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		String xpath = "//ows:Exception[@exceptionCode = 'LockHasExpired']";
		ETSAssert.assertXPath(xpath, this.rspEntity.getDocumentElement(), null);
	}

	/**
	 * [{@code Test}] Verifies that a feature can be protected by only one lock
	 * at a time (i.e. a mutual exclusion lock). Two
	 * <code>GetFeatureWithLock</code> requests are submitted:
	 * <ol>
	 * <li>A request to lock a single feature instance for 60 seconds (Q1)</li>
	 * <li>A request to lock SOME of Q1 plus other feature instances (a proper
	 * superset of the Q1).</li>
	 * </ol>
	 * 
	 * <p>
	 * The last request should succeed. However, the response entity shall not
	 * contain the feature that was previously locked in Q1.
	 * </p>
	 * 
	 * @see "ISO 19142:2010, cl. 13.2.4.2: lockAction parameter"
	 */
	@Test(description = "See ISO 19142: 13.2.4.2")
	public void lockSomeFeatures() {
		QName featureType = LockFeatureTests
				.selectRandomFeatureType(this.featureInfo);
		Set<String> featureIdSet = this.dataSampler
				.selectRandomFeatureIdentifiers(featureType, 10);
		// Submit Q1 to lock one feature
		Set<String> singleton = Collections.singleton(featureIdSet.iterator()
				.next());
		WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
		WFSMessage.addResourceIdPredicate(this.reqEntity, singleton);
		this.reqEntity.getDocumentElement().setAttribute("expiry", "60");
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
		// Submit Q2 to lock all features in set
		buildGetFeatureWithLockRequest();
		WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
		WFSMessage.addResourceIdPredicate(this.reqEntity, featureIdSet);
		this.reqEntity.getDocumentElement().setAttribute("lockAction", "SOME");
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
		String xpath1 = "/wfs:FeatureCollection/@numberReturned > 1";
		ETSAssert
				.assertXPath(xpath1, this.rspEntity.getDocumentElement(), null);
		// response must exclude feature that was previously locked
		xpath1 = String.format("not(//*[@gml:id = '%s'])", singleton.iterator()
				.next());
		ETSAssert
				.assertXPath(xpath1, this.rspEntity.getDocumentElement(), null);
	}
}
