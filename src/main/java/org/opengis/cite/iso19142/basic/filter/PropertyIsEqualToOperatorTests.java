package org.opengis.cite.iso19142.basic.filter;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSTypeDefinition;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.AppSchemaUtils;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the response to a GetFeature request that includes a
 * {@code PropertyIs[Not]EqualTo} filter that compares the value of a property
 * against some specified value. The comparison may or may not be done in a
 * case-sensitive manner. If a property has multiple values, the
 * {@code matchAction} parameter affects the scope of the comparison ("All",
 * "Any", "One").
 * 
 * <p style="margin-bottom: 0.5em"><strong>Sources</strong></p>
 * <ul>
 * <li>ISO 19142:2010, cl. A.1.2: Basic WFS</li>
 * <li>ISO 19143:2010, cl. 7.7: Comparison operators</li>
 * <li>ISO 19143:2010, cl. A.5: Test cases for minimum standard filter</li>
 * </ul>
 */
public class PropertyIsEqualToOperatorTests extends QueryFilterFixture {

    private static String MATCH_ALL = "All";
    private static String MATCH_ANY = "Any";

    /**
     * [{@code Test}] Submits a GetFeature request containing a
     * {@code PropertyIsEqualTo} predicate that applies to some simple feature
     * property. The response entity must include only feature instances with
     * matching (case-sensitive) property values; if multiple values exist, at
     * least one must match (matchAction="Any").
     * 
     * @param binding
     *            The ProtocolBinding to use for this request.
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     */
    @Test(description = "See ISO 19143: 7.7.3.2", dataProvider = "protocol-featureType")
    public void propertyIsEqualTo_caseSensitive(ProtocolBinding binding,
            QName featureType) {
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        Map<XSElementDeclaration, String> propValueMap = findMatchingPropertyValue(featureType);
        if (propValueMap.isEmpty()) {
            throw new SkipException(
                    "No simple property values found for feature type "
                            + featureType);
        }
        Entry<XSElementDeclaration, String> propValue = propValueMap.entrySet()
                .iterator().next();
        XSElementDeclaration propDecl = propValue.getKey();
        QName propName = new QName(propDecl.getNamespace(), propDecl.getName());
        addPropertyIsEqualToPredicate(this.reqEntity, propName,
                propValue.getValue(), true, null, false);
        ClientResponse rsp = wfsClient.submitRequest(reqEntity, binding);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        NodeList features = this.rspEntity.getElementsByTagNameNS(
                featureType.getNamespaceURI(), featureType.getLocalPart());
        // Add constructor functions for property type (XML Schema datatype)
        QName dataType = AppSchemaUtils.getBuiltInDatatype(propDecl);
        String propTypeName = dataType.getLocalPart();
        String xpath = String.format("xs:%s(ns1:%s[1]) = xs:%s('%s')",
                propTypeName, propName.getLocalPart(), propTypeName,
                propValue.getValue());
        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put(propName.getNamespaceURI(), "ns1");
        for (int i = 0; i < features.getLength(); i++) {
            ETSAssert.assertXPath2(xpath, new DOMSource(features.item(i)),
                    nsBindings);
        }
    }

    /**
     * [{@code Test}] Submits a GetFeature request containing a
     * {@code PropertyIsNotEqualTo} predicate that applies to some simple
     * feature property. The response entity must not include any features with
     * a matching (case-sensitive) property value; if multiple values exist, all
     * must satisfy the predicate (matchAction="All").
     * 
     * @param binding
     *            The ProtocolBinding to use for this request.
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     */
    @Test(description = "See ISO 19143: 7.7.3.2", dataProvider = "protocol-featureType")
    public void propertyIsNotEqualTo_caseSensitive(ProtocolBinding binding,
            QName featureType) {
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        Map<XSElementDeclaration, String> propValueMap = findMatchingPropertyValue(featureType);
        if (propValueMap.isEmpty()) {
            throw new SkipException(
                    "No simple property values found for feature type "
                            + featureType);
        }
        Entry<XSElementDeclaration, String> propValue = propValueMap.entrySet()
                .iterator().next();
        XSElementDeclaration propDecl = propValue.getKey();
        QName propName = new QName(propDecl.getNamespace(), propDecl.getName());
        addPropertyIsEqualToPredicate(this.reqEntity, propName,
                propValue.getValue(), true, MATCH_ALL, true);
        ClientResponse rsp = wfsClient.submitRequest(reqEntity, binding);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        NodeList features = this.rspEntity.getElementsByTagNameNS(
                featureType.getNamespaceURI(), featureType.getLocalPart());
        // Add constructor functions for property type (XML Schema datatype)
        QName dataType = AppSchemaUtils.getBuiltInDatatype(propDecl);
        String propTypeName = dataType.getLocalPart();
        // expect no matches even if multiple values exist
        String xpath = String.format("not(xs:%s(ns1:%s[1]) = xs:%s('%s'))",
                propTypeName, propName.getLocalPart(), propTypeName,
                propValue.getValue());
        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put(propName.getNamespaceURI(), "ns1");
        for (int i = 0; i < features.getLength(); i++) {
            ETSAssert.assertXPath2(xpath, new DOMSource(features.item(i)),
                    nsBindings);
        }
    }

    /**
     * Adds a {@code PropertyIsEqualTo} predicate to a GetFeature request entity
     * with the given property name and literal value. The predicate is
     * structured as shown in the listing below.
     * 
     * <pre>
     * {@code
     * <Filter xmlns="http://www.opengis.net/fes/2.0">
     *   <PropertyIsEqualTo matchCase="true" matchAction="Any">
     *     <Literal>value</Literal>
     *     <ValueReference>tns:featureProperty</ValueReference>
     *   </PropertyIsEqualTo>
     * </Filter>
     * }
     * </pre>
     * 
     * @param request
     *            The request entity (/wfs:GetFeature).
     * @param propertyName
     *            A QName that specifies the feature property to check.
     * @param value
     *            The literal value to match the property value against.
     * @param matchCase
     *            A boolean value indicating whether or not the comparison
     *            should be case-sensitive.
     * @param matchAction
     *            A String specifying how the predicate should be applied to a
     *            multi-valued property; the default value is "Any".
     * @param negate
     *            Negates the predicate by using the
     *            {@code PropertyIsNotEqualTo} predicate instead.
     */
    void addPropertyIsEqualToPredicate(Document request, QName propertyName,
            String value, boolean matchCase, String matchAction, boolean negate) {
        if (!request.getDocumentElement().getLocalName()
                .equals(WFS2.GET_FEATURE)) {
            throw new IllegalArgumentException("Not a GetFeature request: "
                    + request.getDocumentElement().getNodeName());
        }
        if (null == propertyName) {
            throw new IllegalArgumentException("propertyName is required.");
        }
        Element queryElem = (Element) request.getElementsByTagNameNS(
                Namespaces.WFS, WFS2.QUERY_ELEM).item(0);
        Element filter = request.createElementNS(Namespaces.FES, "Filter");
        queryElem.appendChild(filter);
        Element predicate;
        if (negate) {
            predicate = request.createElementNS(Namespaces.FES,
                    "PropertyIsNotEqualTo");
        } else {
            predicate = request.createElementNS(Namespaces.FES,
                    "PropertyIsEqualTo");
        }
        filter.appendChild(predicate);
        String matchActionAttr = (null != matchAction && !matchAction.isEmpty()) ? matchAction
                : MATCH_ANY;
        predicate.setAttribute("matchCase", Boolean.toString(matchCase));
        predicate.setAttribute("matchAction", matchActionAttr);
        Element literal = request.createElementNS(Namespaces.FES, "Literal");
        literal.setTextContent(value);
        predicate.appendChild(literal);
        Element valueRef = request.createElementNS(Namespaces.FES,
                "ValueReference");
        predicate.appendChild(valueRef);
        String prefix = (propertyName.getPrefix().length() > 0) ? propertyName
                .getPrefix() : TNS_PREFIX;
        String nsURI = request.lookupNamespaceURI(prefix);
        if (null == nsURI) {
            valueRef.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix,
                    propertyName.getNamespaceURI());
        }
        valueRef.setTextContent(prefix + ":" + propertyName.getLocalPart());
    }

    /**
     * Inspects sample data retrieved from the SUT and finds a value that
     * matches at least one simple property for the specified feature type. If
     * the property occurs more than once only the first occurrrence is used.
     * 
     * @param featureType
     *            The qualified name of some feature type.
     * @return A Map containing a single entry where the key is an element
     *         declaration and the value is a String representing the property
     *         value.
     */
    Map<XSElementDeclaration, String> findMatchingPropertyValue(
            QName featureType) {
        XSTypeDefinition xsdSimpleType = getModel().getTypeDefinition(
                "anySimpleType", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        List<XSElementDeclaration> simpleProps = AppSchemaUtils
                .getFeaturePropertiesByType(getModel(), featureType, xsdSimpleType);
        ListIterator<XSElementDeclaration> listItr = simpleProps
                .listIterator(simpleProps.size());
        XSElementDeclaration prop = null;
        String propValue = null;
        // start with application-specific properties at end of list
        while (listItr.hasPrevious()) {
            prop = listItr.previous();
            QName propName = new QName(prop.getNamespace(), prop.getName());
            List<String> values = dataSampler.getSimplePropertyValues(
                    featureType, propName, null);
            if (!values.isEmpty()) {
                // select first value if multiple occurrences
                propValue = values.get(0);
                break;
            }
        }
        Map<XSElementDeclaration, String> map = new HashMap<XSElementDeclaration, String>();
        if (null != propValue) {
            map.put(prop, propValue);
        }
        return map;
    }
}
