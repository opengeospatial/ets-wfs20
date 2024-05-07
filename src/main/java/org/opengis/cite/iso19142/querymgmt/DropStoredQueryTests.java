package org.opengis.cite.iso19142.querymgmt;

import static org.testng.Assert.assertEquals;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.DataSampler;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Provides test methods that verify the deletion of stored queries.
 */
public class DropStoredQueryTests extends BaseFixture {

    private DataSampler dataSampler;

    @BeforeClass()
    public void initQueryFilterFixture( ITestContext testContext ) {
        ISuite suite = testContext.getSuite();
        this.dataSampler = (DataSampler) suite.getAttribute( SuiteAttribute.SAMPLER.getName() );
    }
    
    /**
     * [{@code Test}] Submits a <code>DropStoredQuery</code> request to remove
     * an existing stored query. The response is expected to contain an XML
     * entity with "DropStoredQueryResponse" as the document element. A
     * subsequent attempt to invoke the query should fail with an exception
     * report ("InvalidParameterValue").
     */
    @Test(description = "See OGC 09-025: 14.6.2")
    public void dropStoredQuery() {
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.CREATE_STORED_QRY,
                ProtocolBinding.POST);
        this.reqEntity = WFSMessage.createRequestEntity(ETS_PKG + "/querymgmt/CreateStoredQuery-GetFeatureByName",
                this.wfsVersion);
        WFSMessage.setReturnTypesAndTypeNamesAttribute( this.reqEntity, this.dataSampler.selectFeatureType() );
        Response rsp = this.wfsClient.submitRequest(new DOMSource(this.reqEntity), ProtocolBinding.POST,
                endpoint);
        assertEquals(rsp.getStatus(), Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        this.reqEntity = WFSMessage.createRequestEntity("DropStoredQuery", this.wfsVersion);
        this.reqEntity.getDocumentElement().setAttribute("id", CreateStoredQueryTests.QRY_GET_FEATURE_BY_NAME);
        endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.DROP_STORED_QRY,
                ProtocolBinding.POST);
        rsp = this.wfsClient.submitRequest(new DOMSource(this.reqEntity), ProtocolBinding.POST, endpoint);
        this.rspEntity = rsp.readEntity(Document.class);
        assertEquals(rsp.getStatus(), Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        ETSAssert.assertQualifiedName(this.rspEntity.getDocumentElement(),
                new QName(WFS2.NS_URI, "DropStoredQueryResponse"));
        Map<String, Object> params = Collections.singletonMap("name", "Irrelevant");
        this.rspEntity = this.wfsClient.invokeStoredQuery(CreateStoredQueryTests.QRY_GET_FEATURE_BY_NAME, params);
        ETSAssert.assertExceptionReport(this.rspEntity, "InvalidParameterValue", "id");
    }

    /**
     * [{@code Test}] Submits a <code>DropStoredQuery</code> request that
     * identifies a nonexistent query. An exception report is expected in
     * response containing the error code "InvalidParameterValue".
     */
    @Test(description = "See OGC 09-025: 14.6.1, 14.7")
    public void dropNonexistentQuery() {
        this.reqEntity = WFSMessage.createRequestEntity("DropStoredQuery", this.wfsVersion);
        this.reqEntity.getDocumentElement().setAttribute("id", "urn:uuid:" + UUID.randomUUID().toString());
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.DROP_STORED_QRY,
                ProtocolBinding.POST);
        Response rsp = this.wfsClient.submitRequest(new DOMSource(this.reqEntity), ProtocolBinding.POST,
                endpoint);
        this.rspEntity = rsp.readEntity(Document.class);
        ETSAssert.assertExceptionReport(this.rspEntity, "InvalidParameterValue", "id");
    }

}
