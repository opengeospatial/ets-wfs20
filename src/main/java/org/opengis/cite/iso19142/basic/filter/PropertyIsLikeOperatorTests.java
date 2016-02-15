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
import org.opengis.cite.iso19142.util.WFSRequest;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the response to a GetFeature request that includes a
 * {@code PropertyIsLike} filter that tests the value of a property using a
 * specified pattern--a combination of regular characters and metacharacters.
 * The {@code PropertyIsLike} predicate can be regarded as a very simple regular
 * expression operator.
 * 
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li>ISO 19142:2010, cl. A.1.2: Basic WFS</li>
 * <li>ISO 19143:2010, cl. 7.7.3.4: PropertyIsLike operator</li>
 * <li>ISO 19143:2010, cl. A.6: Test cases for standard filter</li>
 * </ul>
 */
public class PropertyIsLikeOperatorTests extends QueryFilterFixture {

	/**
	 * [{@code Test}] Submits a GetFeature request containing a
	 * {@code Not/PropertyIsLike} predicate that applies to some simple feature
	 * property (of type xsd:string). The response entity must not include any
	 * feature instances with matching property values.
	 * 
	 * @param binding
	 *            The ProtocolBinding to use for this request.
	 * @param featureType
	 *            A QName representing the qualified name of some feature type.
	 */
	@Test(description = "See ISO 19143: 7.7.3.4, 7.10", dataProvider = "protocol-featureType")
	public void propertyIsNotLike(ProtocolBinding binding, QName featureType) {
		WFSRequest.appendSimpleQuery(this.reqEntity, featureType);
		Map<QName, String> patternMap = generateMatchingStringPattern(featureType);
		if (patternMap.isEmpty()) {
			throw new SkipException(
					"No string property values found for feature type "
							+ featureType);
		}
		Entry<QName, String> propPattern = patternMap.entrySet().iterator()
				.next();
		addPropertyIsLikePredicate(this.reqEntity, propPattern.getKey(),
				propPattern.getValue(), true);
		ClientResponse rsp = wfsClient.submitRequest(reqEntity, binding);
		this.rspEntity = extractBodyAsDocument(rsp, binding);
		Assert.assertEquals(rsp.getStatus(),
				ClientResponse.Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		NodeList features = this.rspEntity.getElementsByTagNameNS(
				featureType.getNamespaceURI(), featureType.getLocalPart());
		// convert wildcards in pattern to proper regular expression
		String xpath = String.format("not(matches(ns1:%s[1], '%s'))",
				propPattern.getKey().getLocalPart(), propPattern.getValue()
						.replace("*", ".*"));
		Map<String, String> nsBindings = new HashMap<String, String>();
		nsBindings.put(propPattern.getKey().getNamespaceURI(), "ns1");
		for (int i = 0; i < features.getLength(); i++) {
			ETSAssert.assertXPath2(xpath, new DOMSource(features.item(i)),
					nsBindings);
		}
	}

	/**
	 * [{@code Test}] Submits a GetFeature request containing a
	 * {@code PropertyIsLike} predicate that applies to some simple feature
	 * property (of type xsd:string). The response entity must include only
	 * features with a property value matching the specified pattern.
	 * 
	 * @param binding
	 *            The ProtocolBinding to use for this request.
	 * @param featureType
	 *            A QName representing the qualified name of some feature type.
	 */
	@Test(description = "See ISO 19143: 7.7.3.4", dataProvider = "protocol-featureType")
	public void propertyIsLike(ProtocolBinding binding, QName featureType) {
		WFSRequest.appendSimpleQuery(this.reqEntity, featureType);
		Map<QName, String> patternMap = generateMatchingStringPattern(featureType);
		if (patternMap.isEmpty()) {
			throw new SkipException(
					"No string property values found for feature type "
							+ featureType);
		}
		Entry<QName, String> propPattern = patternMap.entrySet().iterator()
				.next();
		addPropertyIsLikePredicate(this.reqEntity, propPattern.getKey(),
				propPattern.getValue(), false);
		ClientResponse rsp = wfsClient.submitRequest(reqEntity, binding);
		this.rspEntity = extractBodyAsDocument(rsp, binding);
		Assert.assertEquals(rsp.getStatus(),
				ClientResponse.Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		NodeList features = this.rspEntity.getElementsByTagNameNS(
				featureType.getNamespaceURI(), featureType.getLocalPart());
		// convert wildcards in pattern to proper regular expression
		String xpath = String.format("matches(ns1:%s[1], '%s')", propPattern
				.getKey().getLocalPart(),
				propPattern.getValue().replace("*", ".*"));
		Map<String, String> nsBindings = new HashMap<String, String>();
		nsBindings.put(propPattern.getKey().getNamespaceURI(), "ns1");
		for (int i = 0; i < features.getLength(); i++) {
			ETSAssert.assertXPath2(xpath, new DOMSource(features.item(i)),
					nsBindings);
		}
	}

	/**
	 * Adds a {@code PropertyIsLike} predicate to a GetFeature request entity
	 * with the given property name and pattern. The metacharacters are shown in
	 * the following example.
	 * 
	 * <pre>
	 * {@code
	 * <Filter xmlns="http://www.opengis.net/fes/2.0">
	 *   <PropertyIsLike wildCard="*" singleChar="." escapeChar="\">
	 *     <ValueReference>tns:featureProperty</ValueReference>
	 *     <Literal>*pattern-.</Literal>
	 *   </PropertyIsLike>
	 * </Filter>
	 * }
	 * </pre>
	 * 
	 * @param request
	 *            The request entity (/wfs:GetFeature).
	 * @param propertyName
	 *            A QName that specifies the feature property to check.
	 * @param pattern
	 *            The pattern to match the property value against.
	 * @param negate
	 *            Negates the predicate by inserting a {@code <Not>} operator
	 *            (logical complement).
	 */
	void addPropertyIsLikePredicate(Document request, QName propertyName,
			String pattern, boolean negate) {
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
		Element predicate = request.createElementNS(Namespaces.FES,
				"PropertyIsLike");
		predicate.setAttribute("wildCard", "*");
		predicate.setAttribute("singleChar", "?");
		predicate.setAttribute("escapeChar", "\\");
		if (negate) {
			Element not = request.createElementNS(Namespaces.FES, "Not");
			filter.appendChild(not);
			not.appendChild(predicate);
		} else {
			filter.appendChild(predicate);
		}
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
		Element literal = request.createElementNS(Namespaces.FES, "Literal");
		literal.setTextContent(pattern);
		predicate.appendChild(literal);
	}

	/**
	 * Inspects sample data retrieved from the SUT and generates a pattern that
	 * matches at least one simple property value (of type xsd:string) for the
	 * specified feature type. If the property occurs more than once only the
	 * first occurrrence is used.
	 * 
	 * Complex properties are not inspected for constituent string elements.
	 * 
	 * @param featureType
	 *            The qualified name of some feature type.
	 * @return A Map containing a single entry where the key is a property name
	 *         and the value is a pattern (with metacharacters).
	 */
	Map<QName, String> generateMatchingStringPattern(QName featureType) {
		QName propName = null;
		String pattern = null;
		XSTypeDefinition stringType = this.model.getTypeDefinition("string",
				XMLConstants.W3C_XML_SCHEMA_NS_URI);
		List<XSElementDeclaration> strProps = AppSchemaUtils
				.getFeaturePropertiesByType(model, featureType, stringType);
		ListIterator<XSElementDeclaration> listItr = strProps
				.listIterator(strProps.size());
		// start with application-specific properties at end of list
		while (listItr.hasPrevious()) {
			XSElementDeclaration prop = listItr.previous();
			propName = new QName(prop.getNamespace(), prop.getName());
			List<String> values = this.dataSampler.getSimplePropertyValues(
					featureType, propName, null);
			if (!values.isEmpty()) {
				// just use first value and replace first two chars with '*'
				StringBuilder patternBuilder = new StringBuilder(values.get(0));
				patternBuilder.replace(0, 2, "*");
				pattern = patternBuilder.toString();
				break;
			}
		}
		Map<QName, String> map = new HashMap<QName, String>();
		if (null != pattern) {
			map.put(propName, pattern);
		}
		return map;
	}
}
