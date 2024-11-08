package org.opengis.cite.iso19142.transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.xerces.xs.XSElementDeclaration;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.FES2;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.ws.rs.core.Response;

/**
 * Tests the response to a Transaction request that includes one or more replace actions.
 * If the WFS supports feature versioning, then the wfs:ReplaceResults element must be
 * present (to convey the new identifiers); however, this is not currently checked.
 *
 * @see "ISO 19142:2010, cl. 15.2.6: Replace action"
 */
public class ReplaceTests extends TransactionFixture {

	/** Identifier for the collection of replacement property values. */
	public static final String REPL_PROPS = "replProps";

	/** List containing original representations of modified features */
	private List<Element> originalFeatures = new ArrayList<Element>();

	/**
	 * Restores the WFS data store to its previous state by replacing all modified
	 * features with their original representations.
	 */
	@AfterClass
	public void restoreModifiedFeatures() {
		if (originalFeatures.isEmpty()) {
			return;
		}
		Document req = WFSMessage.createRequestEntity(WFS2.TRANSACTION, this.wfsVersion);
		WFSMessage.addReplaceStatements(req, originalFeatures);
		Response rsp = wfsClient.submitRequest(req, ProtocolBinding.ANY);
		Document rspEntity = rsp.readEntity(Document.class);
		String expr = String.format("//wfs:totalReplaced = '%d'", originalFeatures.size());
		Boolean result;
		try {
			result = (Boolean) XMLUtils.evaluateXPath(rspEntity, expr, null, XPathConstants.BOOLEAN);
		}
		catch (XPathExpressionException xpe) {
			throw new RuntimeException(xpe);
		}
		if (!result) {
			String msg = String.format("%s: Failed to replace modified features.\n%s", getClass().getName(),
					XMLUtils.writeNodeToString(rspEntity));
			TestSuiteLogger.log(Level.WARNING, msg);
		}
	}

	/**
	 * [{@code Test}] Submits a Transaction request to replace an existing feature
	 * instance. The test is run for all supported protocol bindings (except HTTP GET) and
	 * feature types. The response entity (wfs:TransactionResponse) must be schema-valid.
	 *
	 * @see "ISO 19142:2010, cl. 15.3.3: TransactionSummary element"
	 * @see "ISO 19142:2010, cl. 15.3.6: ReplaceResults element"
	 * @param binding A supported transaction request binding.
	 * @param featureType A QName representing the qualified name of some feature type.
	 *
	 */
	@Test(description = "See ISO 19142: 15.3.3, 15.3.6", dataProvider = "binding+availFeatureType")
	public void replaceFeature(ProtocolBinding binding, QName featureType) {
		Document doc = wfsClient.getFeatureByType(featureType, 1, null);
		NodeList features = doc.getElementsByTagNameNS(featureType.getNamespaceURI(), featureType.getLocalPart());
		Element originalFeature = (Element) features.item(0);
		Element replacement = createReplacementFeature(originalFeature);
		List<Element> replacements = Arrays.asList(replacement);
		WFSMessage.addReplaceStatements(this.reqEntity, replacements);
		Response rsp = wfsClient.submitRequest(this.reqEntity, binding);
		this.rspEntity = rsp.readEntity(Document.class);
		String xpath = String.format("//wfs:totalReplaced = '%d'", replacements.size());
		ETSAssert.assertXPath(xpath, this.rspEntity, null);
		originalFeatures.add(originalFeature);
		// feature versioning may be enabled, so get fes:ResourceId/@rid
		Element resourceId = (Element) this.rspEntity.getElementsByTagNameNS(FES2.NS, FES2.RESOURCE_ID).item(0);
		Assert.assertNotNull(resourceId, ErrorMessage.format(ErrorMessageKeys.MISSING_INFOSET_ITEM, FES2.RESOURCE_ID));
		String gmlId = resourceId.getAttribute("rid");
		@SuppressWarnings("unchecked")
		Map<XSElementDeclaration, Object> replProps = (Map<XSElementDeclaration, Object>) replacement
			.getUserData(REPL_PROPS);
		this.rspEntity = wfsClient.invokeStoredQuery(WFS2.QRY_GET_FEATURE_BY_ID, Collections.singletonMap("id", gmlId));
		Element feature = this.rspEntity.getDocumentElement();
		ETSAssert.assertQualifiedName(feature, featureType);
		ETSAssert.assertSimpleProperties(feature, replProps, null);
	}

	/**
	 * Creates a new feature representation by cloning and modifying the original
	 * instance. The following standard GML properties are added (or replaced if already
	 * present):
	 *
	 * <ul>
	 * <li>gml:description ("Lorem ipsum dolor sit amet.")</li>
	 * <li>gml:identifier (randomly generated UUID value)</li>
	 * </ul>
	 *
	 * <p>
	 * The resulting Element node will have associated user data with the key value
	 * {@link #REPL_PROPS}. The user data are represented by a
	 * {@literal Map<XSElementDeclaration, Object>} object containing a collection of
	 * property-value pairs, where each key is an element declaration denoting a property
	 * and the corresponding value is the replacement value.
	 * </p>
	 * @param originalFeature The original feature instance (unmodified).
	 * @return A new Element node representing the modified feature.
	 */
	Element createReplacementFeature(Element originalFeature) {
		Element replacement = (Element) originalFeature.cloneNode(true);
		Map<XSElementDeclaration, Object> replProps = new HashMap<>();
		QName propName = new QName(Namespaces.GML, "identifier");
		Element identifier = XMLUtils.createElement(propName);
		identifier.setAttribute("codeSpace", "http://cite.opengeospatial.org/");
		String idValue = UUID.randomUUID().toString();
		identifier.setTextContent(idValue);
		WFSMessage.insertGMLProperty(replacement, identifier);
		replProps.put(this.model.getElementDeclaration("identifier", Namespaces.GML), idValue);
		propName = new QName(Namespaces.GML, "description");
		Element desc = XMLUtils.createElement(propName);
		String description = "Lorem ipsum dolor sit amet.";
		desc.setTextContent(description);
		WFSMessage.insertGMLProperty(replacement, desc);
		replProps.put(this.model.getElementDeclaration("description", Namespaces.GML), description);
		replacement.setUserData(REPL_PROPS, replProps, null);
		return replacement;
	}

}
