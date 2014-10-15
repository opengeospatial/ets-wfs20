package org.opengis.cite.iso19142.transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.iso19142.util.WFSRequest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the response to a Transaction request that includes one or more replace
 * actions. If the WFS supports feature versioning, then the wfs:ReplaceResults
 * element must be present (to convey the new identifiers); however, this is not
 * currently checked.
 * 
 * @see "ISO 19142:2010, cl. 15.2.6: Replace action"
 */
public class ReplaceTests extends TransactionFixture {

    /** Identifier for the collection of replacement property values. */
    public static final String REPL_PROPS = "replProps";
    /** List containing original representations of modified features */
    private List<Element> originalFeatures = new ArrayList<Element>();

    /**
     * Restores the WFS data store to its previous state by replacing all
     * modified features with their original representations.
     */
    @AfterClass
    public void restoreModifiedFeatures() {
        if (originalFeatures.isEmpty()) {
            return;
        }
        Document req = WFSRequest.createRequestEntity(WFS2.TRANSACTION);
        WFSRequest.addReplaceStatements(req, originalFeatures);
        ClientResponse rsp = wfsClient.submitRequest(req, ProtocolBinding.ANY);
        Document rspEntity = rsp.getEntity(Document.class);
        String expr = String.format("//wfs:totalReplaced = '%d'",
                originalFeatures.size());
        Boolean result;
        try {
            result = (Boolean) XMLUtils.evaluateXPath(rspEntity, expr, null,
                    XPathConstants.BOOLEAN);
        } catch (XPathExpressionException xpe) {
            throw new RuntimeException(xpe);
        }
        if (!result) {
            String msg = String.format(
                    "%s: Failed to replace modified features.\n%s", getClass()
                            .getName(), XMLUtils.writeNodeToString(rspEntity));
            TestSuiteLogger.log(Level.WARNING, msg);
        }
    }

    /**
     * [{@code Test}] Submits a Transaction request to replace an existing
     * feature instance. The test is run for all supported protocol bindings
     * (except HTTP GET) and feature types. The response entity
     * (wfs:TransactionResponse) must be schema-valid.
     * 
     * @see "ISO 19142:2010, cl. 15.3.3: TransactionSummary element"
     * @see "ISO 19142:2010, cl. 15.3.6: ReplaceResults element"
     * 
     * @param binding
     *            A supported transaction request binding.
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     * 
     */
    @Test(description = "See ISO 19142: 15.3.3, 15.3.6", dataProvider = "binding+availFeatureType")
    public void replaceFeature(ProtocolBinding binding, QName featureType) {
        Document doc = wfsClient.getFeatureByType(featureType, 1, null);
        NodeList features = doc.getElementsByTagNameNS(
                featureType.getNamespaceURI(), featureType.getLocalPart());
        Element originalFeature = (Element) features.item(0);
        Element replacement = createReplacementFeature(originalFeature);
        List<Element> replacements = Arrays.asList(replacement);
        WFSRequest.addReplaceStatements(this.reqEntity, replacements);
        ClientResponse rsp = wfsClient.submitRequest(this.reqEntity, binding);
        this.rspEntity = rsp.getEntity(Document.class);
        String xpath = String.format("//wfs:totalReplaced = '%d'",
                replacements.size());
        ETSAssert.assertXPath(xpath, this.rspEntity, null);
        originalFeatures.add(originalFeature);
        String gmlId = originalFeature.getAttributeNS(Namespaces.GML, "id");
        @SuppressWarnings("unchecked")
        Map<String, Object> replProps = (Map<String, Object>) replacement
                .getUserData(REPL_PROPS);
        ETSAssert.assertFeatureProperties(gmlId, replProps, null, wfsClient);
    }

    /**
     * Creates a new feature representation by cloning and modifying the
     * original instance. The following standard GML properties are added (or
     * replaced if already present):
     * 
     * <ul>
     * <li>gml:description ("Lorem ipsum dolor sit amet.")</li>
     * <li>gml:identifier (randomly generated UUID value)</li>
     * </ul>
     * 
     * <p>
     * The resulting Element node will have associated user data with the key
     * value {@link #REPL_PROPS}. The user data are represented by a
     * {@literal Map<String, Object>} object containing a collection of
     * name-value pairs, where each name is a property reference (XPath
     * expression) and the corresponding value is the replacement value.
     * </p>
     * 
     * @param originalFeature
     *            The original feature instance (unmodified).
     * @return A new Element node representing the modified feature.
     */
    Element createReplacementFeature(Element originalFeature) {
        Element replacement = (Element) originalFeature.cloneNode(true);
        Map<String, Object> replProps = new HashMap<String, Object>();
        QName propName = new QName(Namespaces.GML, "identifier");
        Element identifier = XMLUtils.createElement(propName);
        identifier.setAttribute("codeSpace", "http://cite.opengeospatial.org/");
        String idValue = UUID.randomUUID().toString();
        identifier.setTextContent(idValue);
        WFSRequest.insertGMLProperty(replacement, identifier);
        replProps.put("gml:identifier[1]", idValue);
        propName = new QName(Namespaces.GML, "description");
        Element desc = XMLUtils.createElement(propName);
        String description = "Lorem ipsum dolor sit amet.";
        desc.setTextContent(description);
        WFSRequest.insertGMLProperty(replacement, desc);
        replProps.put("gml:description[1]", description);
        replacement.setUserData(REPL_PROPS, replProps, null);
        return replacement;
    }
}
