package org.opengis.cite.iso19142.transaction;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.joda.time.DateTime;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.AppSchemaUtils;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.iso19142.util.WFSRequest;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the response to a Transaction request that includes one or more update
 * actions. If the WFS supports feature versioning, then the wfs:UpdateResults
 * element must be present (to convey the new identifiers); however, this is not
 * currently checked.
 * 
 * @see "ISO 19142:2010, cl. 15.2.5: Update action"
 */
public class Update extends TransactionFixture {

    /** List containing original representations of modified features */
    private List<Element> modifiedFeatures = new ArrayList<Element>();

    /**
     * Attempts to restore the WFS data store to its previous state by replacing
     * all modified features with their original representations. If this does
     * not succeed a log message (WARNING) is written to the test suite logger.
     */
    @AfterClass
    public void restoreModifiedFeatures() {
        if (modifiedFeatures.isEmpty()) {
            return;
        }
        Document req = WFSRequest.createRequestEntity(WFS2.TRANSACTION);
        WFSRequest.addReplaceStatements(req, modifiedFeatures);
        ClientResponse rsp = wfsClient.submitRequest(req, ProtocolBinding.ANY);
        Document rspEntity = rsp.getEntity(Document.class);
        String expr = String.format("//wfs:totalReplaced = '%d'",
                modifiedFeatures.size());
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
     * [{@code Test}] Submits a Transaction request to update the first name
     * (gml:name[1]) of an existing feature instance. The test is run for all
     * supported transaction bindings and available feature types. The response
     * entity (wfs:TransactionResponse) must be schema-valid.
     * 
     * <h6 style="margin-bottom: 0.5em">Sources</h6>
     * <ul>
     * <li>ISO 19142:2010, cl. 15.3.3: TransactionSummary element</li>
     * <li>ISO 19142:2010, cl. 15.3.5: UpdateResults element</li>
     * </ul>
     * 
     * @param binding
     *            A supported transaction request binding.
     * @param featureType
     *            A QName representing the name of some feature type for which
     *            data are available.
     */
    @Test(description = "See ISO 19142: 15.3.3, 15.3.5", dataProvider = "binding+availFeatureType")
    public void updateGMLName(ProtocolBinding binding, QName featureType) {
        Document doc = wfsClient.getFeatureByType(featureType, 1, null);
        NodeList features = doc.getElementsByTagNameNS(
                featureType.getNamespaceURI(), featureType.getLocalPart());
        Element originalFeature = (Element) features.item(0);
        String gmlId = originalFeature.getAttributeNS(Namespaces.GML, "id");
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("gml:name[1]",
                "Ce n&apos;est pas Vieux-Port de Montr&#xe9;al!");
        this.rspEntity = wfsClient.updateFeature(this.reqEntity, gmlId,
                featureType, properties, binding);
        if (this.rspEntity.getDocumentElement().getLocalName()
                .equals(WFS2.TRANSACTION_RSP)) {
            modifiedFeatures.add(originalFeature);
        }
        ETSAssert.assertFeatureProperties(gmlId, properties, null, wfsClient);
    }

    /**
     * [{@code Test}] Submits a Transaction request to update a property
     * (gml:boundedBy) with an invalid value (kml:Point). An ExceptionReport
     * (with status code 400) containing the exception code {@code InvalidValue}
     * is expected in response.
     * 
     * <h6 style="margin-bottom: 0.5em">Sources</h6>
     * <ul>
     * <li>ISO 19142:2010, 15.2.5.2.1: Property element</li>
     * <li>ISO 19142:2010, Table 3: WFS exception codes</li>
     * </ul>
     * 
     */
    @Test(description = "See ISO 19142: 7.5, 15.2.5.2.1")
    public void updateBoundedByWithKMLPoint() {
        try {
            this.reqEntity = docBuilder.parse(getClass().getResourceAsStream(
                    "UpdateInvalidFeatureProperty.xml"));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse XML resource from classpath", e);
        }
        Element update = (Element) this.reqEntity.getElementsByTagNameNS(
                Namespaces.WFS, WFS2.UPDATE).item(0);
        WFSRequest.setTypeName(update, featureTypes.get(0));
        ProtocolBinding binding = wfsClient.getAnyTransactionBinding();
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(
                this.wfsMetadata, WFS2.TRANSACTION, binding);
        ClientResponse rsp = wfsClient.submitRequest(new DOMSource(
                this.reqEntity), binding, endpoint);
        this.rspEntity = rsp.getEntity(Document.class);
        Assert.assertEquals(rsp.getStatus(),
                ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        String xpath = "//ows:Exception[@exceptionCode = 'InvalidValue']";
        ETSAssert.assertXPath(xpath, this.rspEntity.getDocumentElement(), null);
    }

    /**
     * [{@code Test}] Submits a request to update a simple property value, one
     * that is based on a built-in XML Schema datatype (including enumerated
     * types). The test is run for all supported transaction bindings and
     * available feature types.
     * 
     * <h6 style="margin-bottom: 0.5em">Sources</h6>
     * <ul>
     * <li>ISO 19142:2010, cl. 15.3.3: TransactionSummary element</li>
     * <li>ISO 19142:2010, cl. 15.3.5: UpdateResults element</li>
     * </ul>
     * 
     * @param binding
     *            A supported message binding.
     * @param featureType
     *            A QName representing the name of some feature type for which
     *            data are available.
     */
    @Test(description = "See ISO 19142: 15.3.3, 15.3.5", dataProvider = "binding+availFeatureType")
    public void updateSimplePropertyValue(ProtocolBinding binding,
            QName featureType) {
        List<XSElementDeclaration> simpleProps = AppSchemaUtils
                .getSimpleFeatureProperties(this.model, featureType);
        // position iterator at end of list so previous() returns last item
        ListIterator<XSElementDeclaration> propItr = simpleProps
                .listIterator(simpleProps.size());
        XSElementDeclaration prop = propItr.previous();
        Set<String> identifiers = this.dataSampler
                .selectRandomFeatureIdentifiers(featureType, 1);
        String featureId = identifiers.iterator().next();
        QName propName = new QName(prop.getNamespace(), prop.getName(), "tns");
        List<String> propValues = this.dataSampler.getSimplePropertyValues(
                featureType, propName, featureId);
        String newVal = newPropertyValue(prop, propValues);
        WFSRequest.addNamespaceBinding(this.reqEntity, propName);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(propName.getPrefix() + ":" + propName.getLocalPart()
                + "[1]", newVal);
        this.rspEntity = wfsClient.updateFeature(this.reqEntity, featureId,
                featureType, properties, binding);
        if (this.rspEntity.getDocumentElement().getLocalName()
                .equals(WFS2.TRANSACTION_RSP)) {
            modifiedFeatures.add(this.dataSampler.getFeatureById(featureId));
        }
        ETSAssert.assertFeatureProperties(
                featureId,
                properties,
                Collections.singletonMap(propName.getNamespaceURI(),
                        propName.getPrefix()), wfsClient);
    }

    /**
     * Returns a new property value that conforms to the applicable simple type
     * definition (xs:string, xs:dateTime, xs:double). If the type contains an
     * enumeration facet, the new value will be one that does not occur in the
     * original feature instance.
     * 
     * @param prop
     *            A property (element) declaration.
     * @param propValues
     *            A List of known property values (may be empty).
     * @return A String representing the new property value.
     */
    String newPropertyValue(XSElementDeclaration prop, List<String> propValues) {
        XSSimpleTypeDefinition propType;
        if (prop.getTypeDefinition().getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
            propType = (XSSimpleTypeDefinition) prop.getTypeDefinition();
        } else {
            XSComplexTypeDefinition complexType = (XSComplexTypeDefinition) prop
                    .getTypeDefinition();
            propType = complexType.getSimpleType();
        }
        // empty StringList if not an enumerated data type
        StringList enums = propType.getLexicalEnumeration();
        for (int i = 0; i < enums.getLength(); i++) {
            if (!propValues.contains(enums.item(i))) {
                return enums.item(i);
            }
        }
        String newValue = null;
        switch (propType.getBuiltInKind()) {
        case XSConstants.STRING_DT:
            newValue = "TEST_VALUE";
            break;
        case XSConstants.DATETIME_DT:
            DateTime now = new DateTime();
            newValue = now.toString();
            break;
        case XSConstants.DOUBLE_DT:
            Double val = Double.valueOf(propValues.get(0));
            newValue = Double.toString(Math.log(val));
            break;
        default:
            newValue = "";
        }
        return newValue;
    }
}
