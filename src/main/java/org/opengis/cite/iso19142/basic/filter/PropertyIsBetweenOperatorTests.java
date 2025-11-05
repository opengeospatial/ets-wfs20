package org.opengis.cite.iso19142.basic.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class PropertyIsBetweenOperatorTests extends QueryFilterFixture {

	@Test(description = "See ISO 19143: 7.7.3.6, A.6", dataProvider = "protocol-featureType")
	public void propertyIsBetween(ProtocolBinding binding, QName featureType) {
		Set<XSTypeDefinition> dataTypes = getNumericDataTypes(getModel());
		dataTypes.addAll(AppSchemaUtils.getSimpleTemporalDataTypes(getModel()));
		Map<XSElementDeclaration, String[]> propRangeMap = findFeaturePropertyValue(getModel(), featureType, dataTypes);
		if (propRangeMap.isEmpty()) {
			throw new SkipException("No numeric or temporal property values found for " + featureType);
		}
		Entry<XSElementDeclaration, String[]> propRange = propRangeMap.entrySet().iterator().next();
		String minValue = propRange.getValue()[0];
		String maxValue = propRange.getValue()[1];
		XSElementDeclaration propDecl = propRange.getKey();
		QName propName = new QName(propDecl.getNamespace(), propDecl.getName());
		WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
		this.reqEntity = WFSMessage.createRequestEntity(GET_FEATURE_MINIMAL, this.wfsVersion);
		WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
		addPropertyIsBetweenPredicate(this.reqEntity, propName, minValue, maxValue);
		Response rsp = wfsClient.submitRequest(reqEntity, binding);
		this.rspEntity = extractBodyAsDocument(rsp);
		Assert.assertEquals(rsp.getStatus(), Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		NodeList features = this.rspEntity.getElementsByTagNameNS(featureType.getNamespaceURI(),
				featureType.getLocalPart());
		QName dataType = AppSchemaUtils.getBuiltInDatatype(propDecl);
		String propTypeName = dataType.getLocalPart();
		String xpath = String.format("(xs:%s(ns1:%s) >= xs:%s('%s')) and (xs:%s(ns1:%s) <= xs:%s('%s'))", propTypeName,
				propName.getLocalPart(), propTypeName, minValue, propTypeName, propName.getLocalPart(), propTypeName,
				maxValue);
		Map<String, String> nsBindings = new HashMap<String, String>();
		nsBindings.put(propName.getNamespaceURI(), "ns1");
		for (int i = 0; i < features.getLength(); i++) {
			ETSAssert.assertXPath2(xpath, new DOMSource(features.item(i)), nsBindings);
		}
	}

	void addPropertyIsBetweenPredicate(Document request, QName propertyName, String minValue, String maxValue) {
		if (!request.getDocumentElement().getLocalName().equals(WFS2.GET_FEATURE)) {
			throw new IllegalArgumentException(
					"Not a GetFeature request: " + request.getDocumentElement().getNodeName());
		}
		if (null == propertyName) {
			throw new IllegalArgumentException("propertyName is required.");
		}
		Element queryElem = (Element) request.getElementsByTagNameNS(Namespaces.WFS, WFS2.QUERY_ELEM).item(0);
		Element filter = request.createElementNS(Namespaces.FES, "Filter");
		queryElem.appendChild(filter);
		Element predicate = request.createElementNS(Namespaces.FES, "PropertyIsBetween");
		filter.appendChild(predicate);
		Element valueRef = request.createElementNS(Namespaces.FES, "ValueReference");
		predicate.appendChild(valueRef);
		String prefix = (propertyName.getPrefix().length() > 0) ? propertyName.getPrefix() : TNS_PREFIX;
		String nsURI = request.lookupNamespaceURI(prefix);
		if (null == nsURI) {
			valueRef.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix, propertyName.getNamespaceURI());
		}
		valueRef.setTextContent(prefix + ":" + propertyName.getLocalPart());
		Element lowerBoundary = request.createElementNS(Namespaces.FES, "LowerBoundary");
		Element lowerLiteral = request.createElementNS(Namespaces.FES, "Literal");
		lowerBoundary.appendChild(lowerLiteral);
		lowerLiteral.setTextContent(minValue);
		predicate.appendChild(lowerBoundary);
		Element upperBoundary = request.createElementNS(Namespaces.FES, "UpperBoundary");
		Element upperLiteral = request.createElementNS(Namespaces.FES, "Literal");
		upperBoundary.appendChild(upperLiteral);
		upperLiteral.setTextContent(maxValue);
		predicate.appendChild(upperBoundary);
	}

}
