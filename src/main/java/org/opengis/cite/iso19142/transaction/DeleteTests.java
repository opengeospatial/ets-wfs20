package org.opengis.cite.iso19142.transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tests the response to a Transaction request that includes one or more delete
 * actions.
 * 
 * @see "ISO 19142:2010, cl. 15.2.7: Delete action"
 */
public class DeleteTests extends TransactionFixture {

    /** List containing original representations of deleted features */
    private List<Element> deletedFeatures = new ArrayList<Element>();

    /**
     * Restores the WFS data store to its previous state by replacing all
     * deleted features with their previous representations.
     */
    @AfterClass
    public void restoreDeletedFeatures() {
        if (deletedFeatures.isEmpty()) {
            return;
        }
        Document rspEntity = this.wfsClient.insert(deletedFeatures,
                ProtocolBinding.ANY);
        String xpath = String.format("//wfs:totalInserted = '%d'",
                deletedFeatures.size());
        Boolean result;
        try {
            result = (Boolean) XMLUtils.evaluateXPath(rspEntity, xpath, null,
                    XPathConstants.BOOLEAN);
        } catch (XPathExpressionException xpe) {
            throw new RuntimeException(xpe);
        }
        if (!result) {
            String msg = String.format(
                    "%s: Failed to insert deleted features.\n%s", getClass()
                            .getName(), XMLUtils.writeNodeToString(rspEntity));
            TestSuiteLogger.log(Level.WARNING, msg);
        }
    }

    /**
     * [{@code Test}] Submits a Transaction request to delete an existing
     * feature instance. The test is run for all supported transaction request
     * bindings and feature types. The response entity (wfs:TransactionResponse)
     * must be schema-valid and the wfs:TransactionSummary element shall report
     * the correct number of deleted features (totalDeleted).
     * 
     * @param binding
     *            A supported message binding (POST preferred over SOAP).
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     * 
     * @see "ISO 19142:2010, cl. 15.3.3: TransactionSummary element"
     */
    @Test(description = "See ISO 19142: 15.2.7", dataProvider = "binding+availFeatureType")
    public void deleteFeature(ProtocolBinding binding, QName featureType) {
        Document doc = wfsClient.getFeatureByType(featureType, 10, null);
        NodeList features = doc.getElementsByTagNameNS(
                featureType.getNamespaceURI(), featureType.getLocalPart());
        // randomly select an available feature to delete
        Random random = new Random();
        Element originalFeature = (Element) features.item(random
                .nextInt(features.getLength()));
        String gmlId = originalFeature.getAttributeNS(Namespaces.GML, "id");
        Map<String, QName> featuresToDelete = new HashMap<String, QName>();
        featuresToDelete.put(
                gmlId,
                new QName(originalFeature.getNamespaceURI(), originalFeature
                        .getLocalName()));
        this.rspEntity = wfsClient.deleteFeatures(featuresToDelete, binding);
        ETSAssert
                .assertXPath("//wfs:TransactionResponse", this.rspEntity, null);
        String xpath = String.format("//wfs:totalDeleted = '%d'",
                featuresToDelete.size());
        ETSAssert.assertXPath(xpath, this.rspEntity, null);
        deletedFeatures.add(originalFeature);
        ETSAssert.assertFeatureAvailability(gmlId, false, wfsClient);
    }
}
