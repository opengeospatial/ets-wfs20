package org.opengis.cite.iso19142.paging;

import static org.testng.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.FeatureTypeInfo;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * Provides test methods that verify the pagination of search results. This
 * capability allows a client to iterate over the items in a result set. The
 * lifetime of a cached result set is indicated by the value of the operation
 * constraint <code>ResponseCacheTimeout</code> (duration in seconds); if this
 * constraint is not specified then the response cache never times out.
 * 
 * @see "OGC 09-025: Table 14"
 */
public class PagingTests extends BaseFixture {

    private int cacheTimeout = 0;

    @BeforeClass
    public void getPagingConstraints(ITestContext testContext) {
        Object obj = testContext.getAttribute(ResponsePaging.CACHE_TIMEOUT);
        if (null != obj) {
            this.cacheTimeout = Integer.class.cast(obj);
        }
    }

    /**
     * [{@code Test}] Submits a GetFeature request with a very small page size
     * (count="2") and resultType="hits". The initial response entity shall be
     * an empty feature collection with numberReturned = 0. The value of the
     * <code>next</code> attribute shall be a URI that refers to the first page
     * of results. Furthermore, the <code>previous</code> attribute must not
     * appear.
     */
    @Test(description = "See OGC 09-025: 7.7.4.2")
    public void getFeatureWithHitsOnly() {
        this.reqEntity = WFSMessage.createRequestEntity("GetFeature-Minimal", this.wfsVersion);
        this.reqEntity.getDocumentElement().setAttribute("count", "2");
        this.reqEntity.getDocumentElement().setAttribute("resultType", "hits");
        QName featureType = anyFeatureType(this.featureInfo);
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.GET_FEATURE,
                ProtocolBinding.GET);
        ClientResponse rsp = wfsClient.submitRequest(new DOMSource(this.reqEntity), ProtocolBinding.GET, endpoint);
        this.rspEntity = extractBodyAsDocument(rsp);
        ETSAssert.assertQualifiedName(rspEntity.getDocumentElement(),
                new QName(Namespaces.WFS, WFS2.FEATURE_COLLECTION));
        String numReturned = this.rspEntity.getDocumentElement().getAttribute("numberReturned");
        assertEquals(Integer.parseInt(numReturned), 0, ErrorMessage.get(ErrorMessageKeys.NUM_RETURNED));
        String prev = this.rspEntity.getDocumentElement().getAttribute("previous");
        assertTrue(prev.isEmpty(), "Unexpected attribute found in response entity: 'previous'.");
        String next = this.rspEntity.getDocumentElement().getAttribute("next");
        assertFalse(next.isEmpty(), "Expected attribute not found in response entity: 'next'.");
        rsp = retrieveResource(next);
        this.rspEntity = extractBodyAsDocument(rsp);
        assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        ETSAssert.assertQualifiedName(rspEntity.getDocumentElement(),
                new QName(Namespaces.WFS, WFS2.FEATURE_COLLECTION));
        numReturned = this.rspEntity.getDocumentElement().getAttribute("numberReturned");
        assertEquals(Integer.parseInt(numReturned), 2, ErrorMessage.get(ErrorMessageKeys.NUM_RETURNED));
    }

    /**
     * [{@code Test}] Submits a GetFeature request with count = "4" and
     * resultType = "results". The feature identifiers in the first page of
     * results are collected and the next page is retrieved. Then the previous
     * (first) page is retrieved and the feature identifiers are extracted; they
     * must match the known values from the first page.
     */
    @Test(description = "See OGC 09-025: 7.7.4.4.1")
    public void traverseResultSetInBothDirections() {
        this.reqEntity = WFSMessage.createRequestEntity("GetFeature-Minimal", this.wfsVersion);
        int count = 4;
        this.reqEntity.getDocumentElement().setAttribute("count", Integer.toString(count));
        QName featureType = anyFeatureType(this.featureInfo);
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.GET_FEATURE,
                ProtocolBinding.GET);
        ClientResponse rsp = wfsClient.submitRequest(new DOMSource(this.reqEntity), ProtocolBinding.GET, endpoint);
        this.rspEntity = extractBodyAsDocument(rsp);
        ETSAssert.assertQualifiedName(rspEntity.getDocumentElement(),
                new QName(Namespaces.WFS, WFS2.FEATURE_COLLECTION));
        ETSAssert.assertFeatureCount(this.rspEntity, featureType, count);
        Set<String> initialMembers = WFSMessage.extractFeatureIdentifiers(this.rspEntity, featureType);
        // retrieve second page
        String next = this.rspEntity.getDocumentElement().getAttribute("next");
        assertFalse(next.isEmpty(), "Expected attribute not found in response entity: 'next'.");
        rsp = retrieveResource(next);
        this.rspEntity = extractBodyAsDocument(rsp);
        // previous should return first page
        String prev = this.rspEntity.getDocumentElement().getAttribute("previous");
        assertFalse(prev.isEmpty(), "Expected attribute not found in response entity: 'previous'.");
        rsp = retrieveResource(prev);
        this.rspEntity = extractBodyAsDocument(rsp);
        Set<String> prevMembers = WFSMessage.extractFeatureIdentifiers(this.rspEntity, featureType);
        assertTrue(prevMembers.containsAll(initialMembers),
                String.format(
                        "Expected members of previous page to include all members of first page. \nFeature Identifiers: %s",
                        initialMembers));
    }

    // startIndex
    // getLastPage: request last page, NO NEXT
    // GetPropertyValue

    /**
     * Dereferences the given URI reference and returns the response message.
     * 
     * @param uriRef
     *            A String denoting an absolute 'http' URI.
     * @return A representation of the HTTP response message.
     */
    ClientResponse retrieveResource(String uriRef) {
        Logger.getLogger(getClass().getName()).log(Level.FINE, "Attempting to retrieve XML resource from {0}", uriRef);
        URI uri;
        try {
            uri = new URI(uriRef);
        } catch (URISyntaxException e) {
            throw new AssertionError(e.getMessage());
        }
        WebResource resource = this.wfsClient.getClient().resource(uri);
        ClientResponse rsp = resource.accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        return rsp;
    }

    /**
     * Returns the name of a feature type for which data exist.
     * 
     * @param featureInfo
     *            Information about feature types gleaned from the SUT.
     * @return The qualified name of a feature type.
     */
    QName anyFeatureType(Map<QName, FeatureTypeInfo> featureInfo) {
        QName qName = null;
        for (QName featureType : featureInfo.keySet()) {
            if (featureInfo.get(featureType).isInstantiated()) {
                qName = featureType;
                break;
            }
        }
        return qName;
    }
}
