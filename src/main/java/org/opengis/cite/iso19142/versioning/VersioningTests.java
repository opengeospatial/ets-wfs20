package org.opengis.cite.iso19142.versioning;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.opengis.cite.iso19142.BaseFixture;
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

import com.sun.jersey.api.client.ClientResponse;

/**
 * Provides test methods that verify the behavior of the IUT with respect to
 * creating and navigating feature versions.
 */
public class VersioningTests extends BaseFixture {

    private DataSampler dataSampler;
    private Map<String, QName> insertedFeatures = new HashMap<String, QName>();

    @BeforeClass
    public void getDataSamplerFromContext(ITestContext testContext) {
        Object obj = testContext.getSuite().getAttribute(SuiteAttribute.SAMPLER.getName());
        if (null != obj) {
            this.dataSampler = DataSampler.class.cast(obj);
        }
    }

    @AfterClass
    public void deleteInsertedFeatures() {
        if (insertedFeatures.isEmpty()) {
            return;
        }
        Document rspEntity = this.wfsClient.deleteFeatures(this.insertedFeatures, ProtocolBinding.ANY);
        Element totalDeleted = (Element) rspEntity.getElementsByTagNameNS(Namespaces.WFS, "totalDeleted").item(0);
        if (null == totalDeleted || Integer.parseInt(totalDeleted.getTextContent().trim()) != insertedFeatures.size()) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING,
                    String.format("Failed to delete all new features: %s \n%s", this.insertedFeatures,
                            XMLUtils.writeNodeToString(rspEntity)));
        }
        this.insertedFeatures.clear();
    }

    /**
     * [{@code Test}] Submits a request to insert a feature. The response is
     * expected to contain a single fes:ResourceId element with
     * <code>version="1"</code> and the <code>previousRid</code> attribute NOT
     * set. A subsequent query to retrieve the LAST version shall reveal that
     * its state is "valid".
     */
    @Test(description = "See OGC 09-025: 11.3.3.2; OGC 09-026: 7.11.2")
    public void firstVersionHasNoPredecessor() {
        this.reqEntity = WFSMessage.createRequestEntity(WFS2.TRANSACTION, this.wfsVersion);
        Element feature = this.dataSampler.randomlySelectFeatureInstance();
        QName typeName = new QName(feature.getNamespaceURI(), feature.getLocalName());
        feature.setAttributeNS(Namespaces.GML, "id", "id-1");
        UUID uuid = InsertTests.insertRandomIdentifier(feature);
        WFSMessage.addInsertStatement(this.reqEntity, feature);
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.TRANSACTION,
                ProtocolBinding.POST);
        ClientResponse rsp = this.wfsClient.submitRequest(new DOMSource(this.reqEntity), ProtocolBinding.POST,
                endpoint);
        this.rspEntity = rsp.getEntity(Document.class);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        List<ResourceId> newFeatureIDs = InsertTests.extractFeatureIdentifiers(this.rspEntity);
        assertEquals(newFeatureIDs.size(), 1, "Unexpected number of fes:ResourceId elements in response entity.");
        ResourceId id = newFeatureIDs.get(0);
        this.insertedFeatures.put(id.getRid(), typeName);
        assertNotNull(id.getVersion(), String.format("ResourceId is missing version designator (rid=%s)", id.getRid()));
        assertEquals(id.getVersion(), "1", "Unexpected designator for first version.");
        assertNull(id.getPreviousRid(), String.format("New feature has previousRid (rid=%s)", id.getRid()));
        ResourceId qryId = new ResourceId(id.getRid());
        // get LAST version and check that state attribute is "valid"
        qryId.setVersion(FES2.VersionAction.LAST.name());
        rsp = this.wfsClient.GetFeatureVersion(qryId, typeName);
        this.rspEntity = rsp.getEntity(Document.class);
        XMLUtils.writeNode(this.rspEntity, System.out);
        int numReturned = Integer.parseInt(this.rspEntity.getDocumentElement().getAttribute("numberReturned"));
        assertEquals(numReturned, 1, ErrorMessage.get(ErrorMessageKeys.NUM_RETURNED));
        feature = (Element) this.rspEntity.getElementsByTagNameNS(typeName.getNamespaceURI(), typeName.getLocalPart())
                .item(0);
        Element member = (Element) feature.getParentNode();
        assertEquals(member.getAttribute("state"), WFS2.VersionState.VALID.toString(),
                ErrorMessage.get(ErrorMessageKeys.VERSION_STATE));
    }

    // updateFeatureCreatesVersion -> latest is "valid", prev is "superseded"
    // replaceFeatureCreatesVersion -> latest is "valid", prev is "superseded"
    // deleteFeatureRetiresVersion -> latest is "retired"
}
