package org.opengis.cite.iso19142.basic.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSElementDeclaration;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.AppSchemaUtils;
import org.opengis.cite.iso19142.util.WFSRequest;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the response to a GetFeature request that includes a
 * {@code PropertyIsNull} filter predicate; this operator tests for the
 * existence of a specified feature property.
 * 
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li>ISO 19142:2010, cl. A.1.2: Basic WFS</li>
 * <li>ISO 19143:2010, cl. 7.7.3.5: PropertyIsNull operator</li>
 * <li>ISO 19143:2010, cl. A.6: Test cases for standard filter</li>
 * </ul>
 */
public class PropertyIsNullOperatorTests extends QueryFilterFixture {

	/**
	 * [{@code Test}] Submits a GetFeature request containing a
	 * {@code PropertyIsNull} predicate designating the gml:name property. The
	 * response entity must include only features that lack the specified
	 * property.
	 * 
	 * @param binding
	 *            The ProtocolBinding to use for this request.
	 * @param featureType
	 *            A QName representing the qualified name of some feature type.
	 */
	@Test(description = "See ISO 19143: 7.7.3.5", dataProvider = "protocol-featureType")
	public void gmlNameIsNull(ProtocolBinding binding, QName featureType) {
		WFSRequest.appendSimpleQuery(this.reqEntity, featureType);
		QName gmlName = new QName(Namespaces.GML, "name", "gml");
		addPropertyIsNullPredicate(this.reqEntity, gmlName, false);
		ClientResponse rsp = wfsClient.submitRequest(reqEntity, binding);
		this.rspEntity = extractBodyAsDocument(rsp, binding);
		Assert.assertEquals(rsp.getStatus(),
				ClientResponse.Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		Map<String, String> nsBindings = new HashMap<String, String>();
		nsBindings.put(featureType.getNamespaceURI(), "tns");
		String xpath = String.format("not(//tns:%s[gml:name])",
				featureType.getLocalPart());
		ETSAssert.assertXPath(xpath, this.rspEntity, nsBindings);
	}

	/**
	 * [{@code Test}] Submits a GetFeature request containing a
	 * {@code Not/PropertyIsNull} predicate designating some feature property
	 * (the last one in document order). The response entity must include only
	 * features that include the specified property.
	 * 
	 * @param binding
	 *            The ProtocolBinding to use for this request.
	 * @param featureType
	 *            A QName representing the qualified name of some feature type.
	 */
	@Test(description = "See ISO 19143: 7.7.3.5, 7.10", dataProvider = "protocol-featureType")
	public void propertyIsNotNull(ProtocolBinding binding, QName featureType) {
		WFSRequest.appendSimpleQuery(this.reqEntity, featureType);
		List<XSElementDeclaration> props = AppSchemaUtils
				.getAllFeatureProperties(model, featureType);
		// get last property in document order
		XSElementDeclaration lastProp = props.get(props.size() - 1);
		QName propName = new QName(lastProp.getNamespace(), lastProp.getName());
		addPropertyIsNullPredicate(this.reqEntity, propName, true);
		ClientResponse rsp = wfsClient.submitRequest(reqEntity, binding);
		this.rspEntity = extractBodyAsDocument(rsp, binding);
		Assert.assertEquals(rsp.getStatus(),
				ClientResponse.Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		Map<String, String> nsBindings = new HashMap<String, String>();
		nsBindings.put(featureType.getNamespaceURI(), "ns1");
		String nsPrefix = "ns1";
		if (!propName.getNamespaceURI().equals(featureType.getNamespaceURI())) {
			nsPrefix = "ns2";
			nsBindings.put(propName.getNamespaceURI(), nsPrefix);
		}
		String xpath = String.format(
				"count(//ns1:%s[%s:%s]) = count(//wfs:member)",
				featureType.getLocalPart(), nsPrefix, propName.getLocalPart());
		ETSAssert.assertXPath(xpath, this.rspEntity, nsBindings);
	}

	/**
	 * Adds a {@code PropertyIsNull} predicate to a GetFeature request entity
	 * using the given property name.
	 * 
	 * <pre>
	 * {@code
	 * <Filter xmlns="http://www.opengis.net/fes/2.0">
	 *   <PropertyIsNull>
	 *     <ValueReference>tns:featureProperty</ValueReference>
	 *   </PropertyIsNull>
	 * </Filter>
	 * }
	 * </pre>
	 * 
	 * @param request
	 *            The request entity (/wfs:GetFeature).
	 * @param propertyName
	 *            A QName that specifies the feature property to check.
	 * @param negate
	 *            Negates the predicate by inserting a {@code <Not>} operator
	 *            (logical complement).
	 */
	void addPropertyIsNullPredicate(Document request, QName propertyName,
			boolean negate) {
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
				"PropertyIsNull");
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
	}
}
