package org.opengis.cite.iso19142.locking;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Tests the response to a LockFeature request that attempts to lock feature
 * instances identified using one or more query expressions. A lock is active
 * until it either expires (default duration is 300 s) or it is released by a
 * subsequent transaction.
 * 
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li>ISO 19142:2010, cl. 12: LockFeature operation</li>
 * <li>ISO 19142:2010, cl. 12.4: Exceptions</li>
 * <li>ISO 19142:2010, cl. 15.2.3.1.1: Declaring support for locking</li>
 * </ul>
 */
public class LockFeatureTests extends LockingFixture {

	private static Random randomIndex = new Random(System.currentTimeMillis());

	/**
	 * Builds a DOM Document representing a LockFeature request entity.
	 */
	@BeforeMethod
	public void buildSimpleLockFeatureRequest() {
		this.reqEntity = WFSMessage.createRequestEntity("LockFeature",
				this.wfsVersion);
	}

	/**
	 * [{@code Test}] Submits a request to lock a feature instance; within this
	 * interval an attempt to delete the instance without the correct lock
	 * identifier should fail with exception code {@code MissingParameterValue}.
	 * 
	 * <p style="margin-bottom: 0.5em">
	 * <strong>Sources</strong>
	 * </p>
	 * <ul>
	 * <li>ISO 19142:2010, cl. 12.2.4.3: Lock expiry parameter</li>
	 * <li>ISO 19142:2010, cl. 15.2.3.1.2: lockId parameter</li>
	 * </ul>
	 */
	@Test(description = "See ISO 19142: 12.2.4.3, 15.2.3.1.2")
	public void lockFeatureAndAttemptDelete() {
		QName featureType = this.dataSampler.selectFeatureType();
		String gmlId = this.dataSampler.selectRandomFeatureIdentifiers( featureType, 1 ).iterator().next();
		WFSMessage.appendStoredQuery(reqEntity, this.storedQueryId,
				Collections.singletonMap("id", (Object) gmlId));
		reqEntity.getDocumentElement().setAttribute("expiry", "60");
		Response rsp = wfsClient.submitRequest(reqEntity,
				ProtocolBinding.ANY);
		this.rspEntity = rsp.readEntity(Document.class);
		Assert.assertEquals(rsp.getStatus(),
				Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		ETSAssert
				.assertXPath("//wfs:LockFeatureResponse", this.rspEntity, null);
		Element lockRsp = (Element) this.rspEntity.getElementsByTagNameNS(
				Namespaces.WFS, WFS2.LOCK_FEATURE_RSP).item(0);
		String lockId = lockRsp.getAttribute("lockId");
		Assert.assertFalse(lockId.isEmpty(), ErrorMessage.format(
				ErrorMessageKeys.MISSING_INFOSET_ITEM, "@lockId"));
		locks.add(lockId);
		String xpath = String.format(
				"//wfs:FeaturesLocked/fes:ResourceId/@rid = '%s'", gmlId);
		ETSAssert.assertXPath(xpath, lockRsp, null);
		Map<String, QName> idTofeatureType = new HashMap<>();
		idTofeatureType.put( gmlId, featureType );
		Document trxResponse = wfsClient.deleteFeatures( idTofeatureType, ProtocolBinding.ANY);
		String xpath2 = "//ows:Exception[@exceptionCode = 'MissingParameterValue']";
		ETSAssert.assertXPath(xpath2, trxResponse.getDocumentElement(), null);
	}

	/**
	 * [{@code Test}] A feature instance may be locked by only one lock. An
	 * attempt to establish another lock should fail with exception code
	 * {@code CannotLockAllFeatures} if lockAction = "ALL" (the default value).
	 * 
	 * <p style="margin-bottom: 0.5em">
	 * <strong>Sources</strong>
	 * </p>
	 * <ul>
	 * <li>ISO 19142:2010, Table 18: Keywords for LockFeature KVP encoding</li>
	 * <li>ISO 19142:2010, cl. 12.2.5: State machine for WFS locking</li>
	 * </ul>
	 */
	@Test(description = "See ISO 19142: 12.2.3, 12.2.5")
	public void lockFeatureAlreadyLocked() {
		QName featureType = this.dataSampler.selectFeatureType();
		String gmlId = this.dataSampler.selectRandomFeatureIdentifiers( featureType, 1 ).iterator().next();
		WFSMessage.appendStoredQuery(reqEntity, this.storedQueryId,
				Collections.singletonMap("id", (Object) gmlId));
		reqEntity.getDocumentElement().setAttribute("expiry", "60");
		Response rsp = wfsClient.submitRequest(reqEntity,
				ProtocolBinding.ANY);
		this.rspEntity = rsp.readEntity(Document.class);
		Assert.assertEquals(rsp.getStatus(),
				Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		Element lockRsp = (Element) this.rspEntity.getElementsByTagNameNS(
				Namespaces.WFS, WFS2.LOCK_FEATURE_RSP).item(0);
		locks.add(lockRsp.getAttribute("lockId"));
		// try to lock it again (without specifying lockId)
		reqEntity.getDocumentElement().setAttribute("expiry", "180");
		rsp = wfsClient.submitRequest(reqEntity, ProtocolBinding.ANY);
		this.rspEntity = rsp.readEntity(Document.class);
		Assert.assertEquals(rsp.getStatus(),
				Status.BAD_REQUEST.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		String xpath = "//ows:Exception[@exceptionCode = 'CannotLockAllFeatures']";
		ETSAssert.assertXPath(xpath, this.rspEntity.getDocumentElement(), null);
	}

	/**
	 * [{@code Test}] Locks all feature instances of a given type using default
	 * values for all locking options. If no data exist for a given feature type
	 * it is skipped. The response entity must include a lockId attribute and
	 * the wfs:FeaturesLocked element. The wfs:FeatureNotLocked element must not
	 * be present.
	 * 
	 * <p style="margin-bottom: 0.5em">
	 * <strong>Sources</strong>
	 * </p>
	 * <ul>
	 * <li>ISO 19142:2010, cl. 12.3.2: XML-encoding</li>
	 * </ul>
	 * 
	 * @param binding
	 *            The ProtocolBinding to use for the request.
	 * @param featureType
	 *            A QName object denoting the feature type name.
	 */
	@Test(description = "See ISO 19142: 12.3.2", dataProvider = "protocol-featureType")
	public void lockAllFeaturesByType(ProtocolBinding binding, QName featureType) {
		if (!this.featureInfo.get(featureType).isInstantiated()) {
			throw new SkipException("No data available for feature type "
					+ featureType);
		}
		WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
		URI endpoint = ServiceMetadataUtils.getOperationEndpoint(
				this.wfsMetadata, WFS2.LOCK_FEATURE, binding);
		Response rsp = wfsClient.submitRequest(new DOMSource(reqEntity),
				binding, endpoint);
		this.rspEntity = extractBodyAsDocument(rsp);
		Assert.assertEquals(rsp.getStatus(),
				Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		Element lockRsp = this.rspEntity.getDocumentElement();
		Assert.assertEquals(lockRsp.getLocalName(), WFS2.LOCK_FEATURE_RSP,
				ErrorMessage.get(ErrorMessageKeys.LOCAL_NAME));
		String lockId = lockRsp.getAttribute("lockId");
		Assert.assertFalse(lockId.isEmpty(), ErrorMessage.format(
				ErrorMessageKeys.MISSING_INFOSET_ITEM,
				"@lockId in " + lockRsp.getNodeName()));
		locks.add(lockId);
		ETSAssert.assertXPath("//wfs:FeaturesLocked", lockRsp, null);
		ETSAssert.assertXPath("not(//wfs:FeaturesNotLocked)", lockRsp, null);
	}

    /**
     * [{@code Test}] An attempt to reset a lock with locKId and fes:AbstractQueryExpression
     * should produce a service exception with error code "OperationParsingFailed" and HTTP
     * status code 400 (Bad Request).
     *
     * <p>
     * <strong>Note</strong>: The WFS 2.0.2 specification specifies this behaviour in detail (12.2.4.2 lockId parameter):
     *
     * "If both a lockId parameter and one or more fes:AbstractQueryExpression elements are
     * included in a LockFeature request then the server shall raise an OperationParsingFailed
     * exception (see 7.5).[24]"
     * </p>
     *
     * <p style="margin-bottom: 0.5em">
     * <strong>Sources</strong>
     * </p>
     * <ul>
     * <li>09-025r2, cl. 12.2.4.2: lockId parameter</li>
     * <li>09-025r2, Table D.2</li>
     * </ul>
     */
    @Test(description = "See 09-025r2: 12.2.4.2")
    public void lockFeatureWithLockIdAndQuery() {
        if(!"2.0.2".equals( this.wfsVersion) ){
            throw new SkipException( "Tested only for WFS 2.0.2" );
        }
		QName featureType = this.dataSampler.selectFeatureType();
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        this.reqEntity.getDocumentElement().setAttribute("expiry", "10");
        Response rsp = wfsClient.submitRequest(this.reqEntity,
                                                     ProtocolBinding.ANY);
        this.rspEntity = rsp.readEntity(Document.class);
        Assert.assertEquals(rsp.getStatus(),
                            Status.OK.getStatusCode(),
                            ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Element featureColl = (Element) this.rspEntity.getElementsByTagNameNS(
                                Namespaces.WFS, WFS2.FEATURE_COLLECTION).item(0);
        String lockId = featureColl.getAttribute("lockId");
        Assert.assertFalse(lockId.isEmpty(), ErrorMessage.format(
                                ErrorMessageKeys.MISSING_INFOSET_ITEM, "@lockId"));
        locks.add(lockId);

        // try to reset expired lock with LockFeature request
        this.reqEntity = WFSMessage.createRequestEntity("LockFeature",
                                                        this.wfsVersion);
        reqEntity.getDocumentElement().setAttribute("lockId", lockId);
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        rsp = wfsClient.submitRequest(reqEntity, ProtocolBinding.ANY);
        this.rspEntity = rsp.readEntity(Document.class);
        Assert.assertEquals(rsp.getStatus(),
                            Status.BAD_REQUEST.getStatusCode(),
                            ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        String xpath = "//ows:Exception[@exceptionCode = 'OperationParsingFailed']";
        ETSAssert.assertXPath(xpath, this.rspEntity.getDocumentElement(), null);
    }

}
