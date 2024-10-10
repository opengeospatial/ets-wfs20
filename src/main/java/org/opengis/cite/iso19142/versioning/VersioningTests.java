package org.opengis.cite.iso19142.versioning;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.FES2;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.basic.filter.ResourceId;
import org.opengis.cite.iso19142.transaction.InsertTests;
import org.opengis.cite.iso19142.util.DataSampler;
import org.opengis.cite.iso19142.util.Randomizer;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;


/**
 * Provides test methods that verify the behavior of the IUT with respect to
 * creating and navigating feature versions.
 */
public class VersioningTests extends BaseFixture {

    private DataSampler dataSampler;
    private Map<String, QName> modifiedFeatures = new HashMap<String, QName>();

    @BeforeClass
    public void getDataSamplerFromContext(ITestContext testContext) {
        Object obj = testContext.getSuite().getAttribute(SuiteAttribute.SAMPLER.getName());
        if (null != obj) {
            this.dataSampler = DataSampler.class.cast(obj);
        }
    }

    @AfterClass
    public void deleteModifiedFeatures() {
        if (modifiedFeatures.isEmpty()) {
            return;
        }
        Document rspEntity = this.wfsClient.deleteFeatures(this.modifiedFeatures, ProtocolBinding.ANY);
        Element totalDeleted = (Element) rspEntity.getElementsByTagNameNS(Namespaces.WFS, WFS2.TOTAL_DEL).item(0);
        if (null == totalDeleted || Integer.parseInt(totalDeleted.getTextContent().trim()) != modifiedFeatures.size()) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING,
                    String.format("Failed to delete all new features: %s \n%s", this.modifiedFeatures,
                            XMLUtils.writeNodeToString(rspEntity)));
        }
        this.modifiedFeatures.clear();
    }

    /**
     * [{@code Test}] Submits a request to insert a feature. The response is
     * expected to contain a single fes:ResourceId element with
     * <code>version="1"</code> and the <code>previousRid</code> attribute NOT
     * set. A subsequent query to retrieve the LAST version shall reveal that
     * its state is "valid".
     */
    @Test(description = "See OGC 09-025: 11.3.3.2, 15.3.4; OGC 09-026: 7.11.2")
    public void firstVersionHasNoPredecessor() {
        this.reqEntity = WFSMessage.createRequestEntity(WFS2.TRANSACTION, this.wfsVersion);
        Element feature = this.dataSampler.randomlySelectFeatureInstance();
        QName typeName = new QName(feature.getNamespaceURI(), feature.getLocalName());
        feature.setAttributeNS(Namespaces.GML, "id", "id-" + System.currentTimeMillis());
        InsertTests.insertRandomIdentifier(feature);
        WFSMessage.addInsertStatement(this.reqEntity, feature);
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.TRANSACTION,
                ProtocolBinding.POST);
        Response rsp = this.wfsClient.submitRequest(new DOMSource(this.reqEntity), ProtocolBinding.POST,
                endpoint);
        this.rspEntity = rsp.readEntity(Document.class);
        Assert.assertEquals(rsp.getStatus(), Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        List<ResourceId> newFeatureIDs = InsertTests.extractFeatureIdentifiers(this.rspEntity, WFS2.Transaction.INSERT);
        assertEquals(newFeatureIDs.size(), 1,
                "Unexpected number of fes:ResourceId elements in response entity (InsertResults).");
        ResourceId id = newFeatureIDs.get(0);
        this.modifiedFeatures.put(id.getRid(), typeName);
        assertNotNull(id.getVersion(), String.format("ResourceId is missing version designator (rid=%s)", id.getRid()));
        assertEquals(id.getVersion(), "1", "Unexpected designator for first version.");
        assertNull(id.getPreviousRid(), String.format("New feature has previousRid (rid=%s)", id.getRid()));
        ResourceId qryId = new ResourceId(id.getRid());
        // get LAST version and check that state attribute is "valid"
        qryId.setVersion(FES2.VersionAction.LAST.name());
        rsp = this.wfsClient.GetFeatureVersion(qryId, typeName);
        this.rspEntity = rsp.readEntity(Document.class);
        int numReturned = Integer.parseInt(this.rspEntity.getDocumentElement().getAttribute("numberReturned"));
        assertEquals(numReturned, 1, ErrorMessage.get(ErrorMessageKeys.NUM_RETURNED));
        feature = (Element) this.rspEntity.getElementsByTagNameNS(typeName.getNamespaceURI(), typeName.getLocalPart())
                .item(0);
        Element member = (Element) feature.getParentNode();
        assertEquals(member.getAttribute("state"), WFS2.VersionState.VALID.toString(),
                ErrorMessage.get(ErrorMessageKeys.VERSION_STATE));
    }

    /**
     * [{@code Test}] Submits a request to update a feature property (gml:name).
     * The response is expected to contain a single fes:ResourceId element with
     * the <code>previousRid</code> attribute set. A subsequent query to
     * retrieve the PREVIOUS version shall reveal that its state is
     * "superseded".
     */
    @Test(description = "See OGC 09-025: 15.3.5")
    public void updatedVersionHasSupersededPredecessor() {
        this.reqEntity = WFSMessage.createRequestEntity(WFS2.TRANSACTION, this.wfsVersion);
        Element feature = this.dataSampler.randomlySelectFeatureInstance();
        QName typeName = new QName(feature.getNamespaceURI(), feature.getLocalName());
        String oldId = feature.getAttributeNS(Namespaces.GML, "id");
        Map<String, Object> properties = new HashMap<String, Object>();
        String newName = Randomizer.generateWords(2);
        properties.put("gml:name[1]", newName);
        this.rspEntity = wfsClient.updateFeature(this.reqEntity, oldId, typeName, properties, ProtocolBinding.POST);
        List<ResourceId> updatedIDs = InsertTests.extractFeatureIdentifiers(this.rspEntity, WFS2.Transaction.UPDATE);
        assertEquals(updatedIDs.size(), 1,
                "Unexpected number of fes:ResourceId elements in response entity (UpdateResults).");
        ResourceId id = updatedIDs.get(0);
        this.modifiedFeatures.put(id.getRid(), typeName);
        assertNotNull(id.getPreviousRid(),
                String.format("Updated feature is missing previousRid (rid=%s)", id.getRid()));
        // get PREVIOUS version and check that state attribute is "superseded"
        ResourceId qryId = new ResourceId(id.getPreviousRid());
        Response rsp = this.wfsClient.GetFeatureVersion(qryId, typeName);
        this.rspEntity = rsp.readEntity(Document.class);
        int numReturned = Integer.parseInt(this.rspEntity.getDocumentElement().getAttribute("numberReturned"));
        assertEquals(numReturned, 1, ErrorMessage.get(ErrorMessageKeys.NUM_RETURNED));
        feature = (Element) this.rspEntity.getElementsByTagNameNS(typeName.getNamespaceURI(), typeName.getLocalPart())
                .item(0);
        Element member = (Element) feature.getParentNode();
        assertEquals(member.getAttribute("state"), WFS2.VersionState.SUPERSEDED.toString(),
                ErrorMessage.get(ErrorMessageKeys.VERSION_STATE));
    }

    /**
     * [{@code Test}] Submits a request to update a superseded version (whose
     * state is NOT "valid"). An exception report is expected in response; it
     * must contain the error code <code>OperationProcessingFailed</code> and
     * refer to the update handle.
     * 
     * TODO: Enable this when expected behavior is clarified.
     */
    @Test(description = "See OGC 09-025: Table 3, 15.4", enabled = false)
    public void updateSupersededVersion() {
        this.reqEntity = WFSMessage.createRequestEntity(WFS2.TRANSACTION, this.wfsVersion);
        Element feature = this.dataSampler.randomlySelectFeatureInstance();
        QName typeName = new QName(feature.getNamespaceURI(), feature.getLocalName());
        String oldId = feature.getAttributeNS(Namespaces.GML, "id");
        Map<String, Object> properties = new HashMap<String, Object>();
        String newName = Randomizer.generateWords(2);
        properties.put("gml:name[1]", newName);
        this.rspEntity = this.wfsClient.updateFeature(this.reqEntity, oldId, typeName, properties,
                ProtocolBinding.POST);
        List<ResourceId> updatedIDs = InsertTests.extractFeatureIdentifiers(this.rspEntity, WFS2.Transaction.UPDATE);
        assertEquals(updatedIDs.size(), 1,
                "Unexpected number of fes:ResourceId elements in response entity (UpdateResults).");
        ResourceId id = updatedIDs.get(0);
        this.modifiedFeatures.put(id.getRid(), typeName);
        assertFalse(id.getRid().equals(id.getPreviousRid()),
                String.format("The rid and previousRid values should not match. ", id));
        // resubmit update request against previous revision
        this.rspEntity = this.wfsClient.updateFeature(this.reqEntity, oldId, typeName, properties,
                ProtocolBinding.POST);
        ETSAssert.assertExceptionReport(this.rspEntity, "OperationProcessingFailed", "Update");
    }

    /**
     * [{@code Test}] Submits a request to replace a feature version. The
     * response is expected to contain a single fes:ResourceId element with the
     * <code>previousRid</code> attribute set. A subsequent query to retrieve
     * the NEXT version shall produce an empty response.
     */
    @Test(description = "See OGC 09-025: 15.3.6")
    public void replacementVersionHasNoSuccessor() {
        this.reqEntity = WFSMessage.createRequestEntity(WFS2.TRANSACTION, this.wfsVersion);
        Element feature = this.dataSampler.randomlySelectFeatureInstance();
        QName typeName = new QName(feature.getNamespaceURI(), feature.getLocalName());
        InsertTests.insertRandomIdentifier(feature);
        InsertTests.addRandomName(feature);
        WFSMessage.addReplaceStatements(this.reqEntity, Collections.singletonList(feature));
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.TRANSACTION,
                ProtocolBinding.POST);
        Response rsp = this.wfsClient.submitRequest(new DOMSource(this.reqEntity), ProtocolBinding.POST,
                endpoint);
        this.rspEntity = rsp.readEntity(Document.class);
        Assert.assertEquals(rsp.getStatus(), Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        List<ResourceId> newFeatureIDs = InsertTests.extractFeatureIdentifiers(this.rspEntity,
                WFS2.Transaction.REPLACE);
        assertEquals(newFeatureIDs.size(), 1,
                "Unexpected number of fes:ResourceId elements in response entity (ReplaceResults).");
        ResourceId id = newFeatureIDs.get(0);
        this.modifiedFeatures.put(id.getRid(), typeName);
        assertNotNull(id.getPreviousRid(),
                String.format("Replacement feature is missing previousRid (rid=%s)", id.getRid()));
        // get NEXT version and check that it doesn't exist
        ResourceId qryId = new ResourceId(id.getRid());
        qryId.setVersion(FES2.VersionAction.NEXT.name());
        rsp = this.wfsClient.GetFeatureVersion(qryId, typeName);
        this.rspEntity = rsp.readEntity(Document.class);
        int numMatched = Integer.parseInt(this.rspEntity.getDocumentElement().getAttribute("numberMatched"));
        assertEquals(numMatched, 0, ErrorMessage.get(ErrorMessageKeys.NUM_MATCHED));
    }

    /**
     * [{@code Test}] Submits a request to delete a feature. The response is
     * expected to report totalDeleted = 1. A subsequent query to retrieve the
     * LAST version shall reveal that its state is "retired".
     */
    @Test(description = "See OGC 09-025: 15.2.7.1")
    public void deletedFeatureIsRetired() {
        this.reqEntity = WFSMessage.createRequestEntity(WFS2.TRANSACTION, this.wfsVersion);
        Element feature = this.dataSampler.randomlySelectFeatureInstance();
        String gmlId = feature.getAttributeNS(Namespaces.GML, "id");
        QName typeName = new QName(feature.getNamespaceURI(), feature.getLocalName());
        Response rsp = wfsClient.deleteFeature(this.reqEntity, gmlId, typeName);
        this.rspEntity = rsp.readEntity(Document.class);
        assertEquals(rsp.getStatus(), Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        int totalDeleted = Integer.parseInt(
                this.rspEntity.getElementsByTagNameNS(Namespaces.WFS, WFS2.TOTAL_DEL).item(0).getTextContent());
        assertEquals(totalDeleted, 1, ErrorMessage.format(ErrorMessageKeys.UNEXPECTED_VALUE, WFS2.TOTAL_DEL));
        // get LAST version and check that state attribute is "superseded"
        ResourceId qryId = new ResourceId(gmlId);
        qryId.setVersion(FES2.VersionAction.LAST.name());
        rsp = this.wfsClient.GetFeatureVersion(qryId, typeName);
        this.rspEntity = rsp.readEntity(Document.class);
        int numReturned = Integer.parseInt(this.rspEntity.getDocumentElement().getAttribute("numberReturned"));
        assertEquals(numReturned, 1, ErrorMessage.get(ErrorMessageKeys.NUM_RETURNED));
        feature = (Element) this.rspEntity.getElementsByTagNameNS(typeName.getNamespaceURI(), typeName.getLocalPart())
                .item(0);
        Element member = (Element) feature.getParentNode();
        assertEquals(member.getAttribute("state"), WFS2.VersionState.RETIRED.toString(),
                ErrorMessage.get(ErrorMessageKeys.VERSION_STATE));
        // attempt to restore deleted feature
        Document doc = this.wfsClient.insert(Collections.singletonList(feature), ProtocolBinding.POST);
        int totalInserted = Integer
                .parseInt(doc.getElementsByTagNameNS(Namespaces.WFS, WFS2.TOTAL_INS).item(0).getTextContent());
        if (totalInserted != 1) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING,
                    String.format("Failed to restore deleted feature with id = %s", gmlId));
        }
    }
}
