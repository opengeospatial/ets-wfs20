package org.opengis.cite.iso19142.querymgmt;

import static org.testng.Assert.assertEquals;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Provides test methods that verify the creation of stored queries. A
 * conforming implementation must support the query language
 * {@value org.opengis.cite.iso19142.querymgmt.StoredQueryManagement#LANG_WFS_QUERY}.
 * Other query languages may be listed in the capabilities document in the
 * context of the <code>CreateStoredQuery</code> request.
 */
public class CreateStoredQueryTests extends BaseFixture {

    public final static String QRY_GET_FEATURE_BY_TYPE = "urn:example:wfs2-query:GetFeatureByTypeName";
    public final static String QRY_GET_FEATURE_BY_NAME = "urn:example:wfs2-query:GetFeatureByName";
    public final static String QRY_INVALID_LANG = "urn:example:wfs2-query:InvalidLang";
    private List<String> createdStoredQueries = new ArrayList<>(2);

    /**
     * This configuration method deletes specific stored queries that may
     * already be known to the IUT; specifically:
     * <ul>
     * <li>{@value #QRY_GET_FEATURE_BY_TYPE}</li>
     * <li>{@value #QRY_GET_FEATURE_BY_NAME}</li>
     * <li>{@value #QRY_INVALID_LANG}</li>
     * </ul>
     * 
     * The remaining tests are skipped if this fails because a precondition
     * cannot be met.
     */
    @BeforeClass
    public void deleteQueriesAtStart() {
        List<String> knownQueries = this.wfsClient.listStoredQueries();
        for (String queryId : new String[] { QRY_GET_FEATURE_BY_TYPE, QRY_GET_FEATURE_BY_NAME, QRY_INVALID_LANG }) {
            if (!knownQueries.contains(queryId))
                continue;
            int status = this.wfsClient.deleteStoredQuery(queryId);
            if (status >= 400) {
                throw new SkipException(String.format("[%s] Error dropping stored query: %s (status code was %d)",
                        getClass().getName(), queryId, status));
            }
        }
    }

    /**
     * This configuration method drops any stored queries that may have been
     * created by a test method. If an error occurs a WARNING message is logged.
     */
    @AfterClass
    public void deleteQueriesAtEnd() {
        for (String queryId : this.createdStoredQueries) {
            int status = this.wfsClient.deleteStoredQuery(queryId);
            if (status >= 400) {
                TestSuiteLogger.log(Level.WARNING,
                        String.format("[%s] Error dropping stored query: %s (status code was %d)", getClass().getName(),
                                queryId, status));
            }
        }
        this.createdStoredQueries.clear();
    }

    /**
     * [{@code Test}] Submits a <code>CreateStoredQuery</code> request to
     * retrieve features by type name. The query identifier is
     * {@value #QRY_GET_FEATURE_BY_TYPE}. The response is expected to contain an
     * XML entity with "CreateStoredQueryResponse" as the document element. The
     * query is then invoked for all feature types for which data exist.
     */
    @Test(description = "See OGC 09-025: 14.2, 14.5.2")
    public void createGetFeatureByTypeName() {
        this.reqEntity = WFSMessage.createRequestEntity(ETS_PKG + "/querymgmt/CreateStoredQuery-GetFeatureByTypeName",
                this.wfsVersion);
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.CREATE_STORED_QRY,
                ProtocolBinding.POST);
        ClientResponse rsp = this.wfsClient.submitRequest(new DOMSource(this.reqEntity), ProtocolBinding.POST,
                endpoint);
        this.rspEntity = rsp.getEntity(Document.class);
        assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        ETSAssert.assertQualifiedName(this.rspEntity.getDocumentElement(),
                new QName(WFS2.NS_URI, "CreateStoredQueryResponse"));
        this.createdStoredQueries.add(QRY_GET_FEATURE_BY_TYPE);
        for (QName featureTypeName : this.featureInfo.keySet()) {
            if (!this.featureInfo.get(featureTypeName).isInstantiated())
                continue;
            Map<String, Object> params = Collections.singletonMap("typeName", featureTypeName);
            Document doc = this.wfsClient.invokeStoredQuery(QRY_GET_FEATURE_BY_TYPE, params);
            ETSAssert.assertResultSetNotEmpty(doc, featureTypeName);
        }
    }

    /**
     * [{@code Test}] Submits a <code>CreateStoredQuery</code> request that
     * contains a query expressed in an unsupported query language. An exception
     * report is expected in response containing the error code
     * "InvalidParameterValue".
     */
    @Test(description = "See OGC 09-025: 14.2.2.5.3, 14.7")
    public void createStoredQueryWithUnsupportedQueryLanguage() {
        this.reqEntity = WFSMessage.createRequestEntity(ETS_PKG + "/querymgmt/CreateStoredQuery-GetFeatureByTypeName",
                this.wfsVersion);
        Element qryDefn = (Element) this.reqEntity.getElementsByTagNameNS(WFS2.NS_URI, "StoredQueryDefinition").item(0);
        qryDefn.setAttribute("id", QRY_INVALID_LANG);
        Element qryExpr = (Element) this.reqEntity.getElementsByTagNameNS(WFS2.NS_URI, "QueryExpressionText").item(0);
        qryExpr.setAttribute("language", "http://qry.example.org");
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.CREATE_STORED_QRY,
                ProtocolBinding.POST);
        ClientResponse rsp = this.wfsClient.submitRequest(new DOMSource(this.reqEntity), ProtocolBinding.POST,
                endpoint);
        this.rspEntity = rsp.getEntity(Document.class);
        ETSAssert.assertExceptionReport(this.rspEntity, "InvalidParameterValue", "language");
    }

    /**
     * [{@code Test}] Submits a <code>CreateStoredQuery</code> request
     * containing a query definition that is identical to an existing one. An
     * exception report is expected in response containing the error code
     * "DuplicateStoredQueryIdValue".
     */
    @Test(description = "See OGC 09-025: Table 3")
    public void duplicateQuery() {
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.CREATE_STORED_QRY,
                ProtocolBinding.POST);
        this.reqEntity = WFSMessage.createRequestEntity(ETS_PKG + "/querymgmt/CreateStoredQuery-GetFeatureByName",
                this.wfsVersion);
        ClientResponse rsp = this.wfsClient.submitRequest(new DOMSource(this.reqEntity), ProtocolBinding.POST,
                endpoint);
        this.rspEntity = rsp.getEntity(Document.class);
        ETSAssert.assertQualifiedName(this.rspEntity.getDocumentElement(),
                new QName(WFS2.NS_URI, "CreateStoredQueryResponse"));
        this.createdStoredQueries.add(QRY_GET_FEATURE_BY_NAME);
        // resubmit
        rsp = this.wfsClient.submitRequest(new DOMSource(this.reqEntity), ProtocolBinding.POST, endpoint);
        this.rspEntity = rsp.getEntity(Document.class);
        ETSAssert.assertExceptionReport(this.rspEntity, "DuplicateStoredQueryIdValue", QRY_GET_FEATURE_BY_NAME);
    }
}
