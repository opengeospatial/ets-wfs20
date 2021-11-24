package org.opengis.cite.iso19142.basic.filter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the response to a GetFeature request that includes a ResourceId filter
 * predicate, which is used to identify a feature instance (or some version of
 * it). The resource identifier maps to the standard gml:id attribute common to
 * all features; its value is assigned by the server when the instance is
 * created. A feature identifier cannot be reused once it has been assigned,
 * even after the feature is deleted.
 * 
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li>ISO 19142:2010, Table 1: Conformance classes</li>
 * <li>ISO 19142:2010, cl. 7.2.2: Encoding resource identifiers</li>
 * <li>ISO 19143:2010, cl. 7.11: Object identifiers</li>
 * <li>ISO 19143:2010, cl. A.4: Test cases for resource identification</li>
 * </ul>
 */
public class ResourceIdFilterTests extends QueryFilterFixture {

    /**
     * [{@code Test}] Submits a GetFeature request containing a ResourceId
     * predicate with two resource identifiers. The response entity must include
     * only instances of the requested type with matching gml:id attribute
     * values.
     * 
     * @param binding
     *            The ProtocolBinding to use for this request.
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     */
    @Test(description = "See ISO 19142: 7.2.2; ISO 19143: 7.11", dataProvider = "protocol-featureType")
    public void twoValidFeatureIdentifiers(ProtocolBinding binding, QName featureType) {
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        Set<String> idSet = this.dataSampler.selectRandomFeatureIdentifiers(featureType, 2);
        WFSMessage.addResourceIdPredicate(this.reqEntity, idSet);
        ClientResponse rsp = wfsClient.submitRequest(reqEntity, binding);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        ETSAssert.assertDescendantElementCount(this.rspEntity, new QName(Namespaces.WFS, WFS2.MEMBER), idSet.size());
    }

    /**
     * [{@code Test}] Submits a GetFeature request containing a ResourceId
     * predicate with an unknown feature identifier. The response entity is
     * expected to be a wfs:FeatureCollection that has no matching feature
     * member.
     * 
     * @param binding
     *            The ProtocolBinding to use for this request (e.g. GET, POST).
     * @param featureType
     *            A QName representing the qualified name of some supported
     *            feature type.
     */
    @Test(description = "See ISO 19142: 7.2.2, Table 8", dataProvider = "protocol-featureType")
    public void unknownFeatureIdentifier(ProtocolBinding binding, QName featureType) {
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        Set<String> idSet = new HashSet<String>();
        idSet.add("test-" + UUID.randomUUID());
        WFSMessage.addResourceIdPredicate(this.reqEntity, idSet);
        ClientResponse rsp = wfsClient.submitRequest(reqEntity, binding);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        ETSAssert.assertQualifiedName(this.rspEntity.getDocumentElement(),
                new QName(Namespaces.WFS, WFS2.FEATURE_COLLECTION));
        ETSAssert.assertFeatureCount(this.rspEntity, featureType, 0);
    }

    /**
     * [{@code Test}] If a feature instance identified by the RESOURCEID
     * parameter is not of the type specified by the TYPENAMES parameter, the
     * server shall raise an InvalidParameterValue exception where the "locator"
     * attribute value shall be set to "RESOURCEID".
     * 
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     * 
     * @see "OGC 09-025r1, cl. 7.9.2.4.1: typeNames parameter"
     */
    @Test(description = "See ISO 19142: 7.9.2.4.1", dataProvider = "instantiated-feature-types")
    public void inconsistentFeatureIdentifierAndType(QName featureType) {
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        Set<String> idSet = new HashSet<String>();
        String featureId = this.dataSampler.getFeatureIdNotOfType(featureType);
        if (null == featureId) {
            throw new SkipException("Unable to find id of feature instance that is NOT of type " + featureType);
        }
        idSet.add(featureId);
        WFSMessage.addResourceIdPredicate(this.reqEntity, idSet);
        ClientResponse rsp = wfsClient.submitRequest(reqEntity, ProtocolBinding.GET);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        ETSAssert.assertExceptionReport(this.rspEntity, "InvalidParameterValue", "RESOURCEID");
    }
}
