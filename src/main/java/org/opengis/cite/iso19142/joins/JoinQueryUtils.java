package org.opengis.cite.iso19142.joins;

import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSElementDeclaration;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.FeatureProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Utility methods for constructing join queries.
 */
public class JoinQueryUtils {

	/**
	 * Inserts a spatial join query into a GetFeature request entity.
	 * @param req A Document node representing a GetFeature request.
	 * @param operator The name of a spatial operator.
	 * @param properties A sequence of (2) FeatureProperty descriptors that identify the
	 * feature properties in the join condition.
	 */
	public static void appendSpatialJoinQuery(Document req, String operator, List<FeatureProperty> properties) {
		if (!req.getDocumentElement().getLocalName().equals(WFS2.GET_FEATURE)) {
			throw new IllegalArgumentException("Not a GetFeature request: " + req.getDocumentElement().getNodeName());
		}
		if (null == properties) {
			throw new NullPointerException("Feature properties are required.");
		}
		if (properties.size() != 2) {
			throw new IllegalArgumentException(
					"Two feature properties are required, but received " + properties.size());
		}
		Element docElem = req.getDocumentElement();
		Element qryElem = req.createElementNS(Namespaces.WFS, "wfs:Query");
		docElem.appendChild(qryElem);
		StringBuilder typeNames = new StringBuilder();
		for (FeatureProperty property : properties) {
			QName featureType = property.getFeatureType();
			// look for prefix already bound to this namespace URI
			String nsPrefix = qryElem.lookupPrefix(featureType.getNamespaceURI());
			if (null == nsPrefix) {
				nsPrefix = "ns" + Integer.toString((int) (Math.random() * 100));
				qryElem.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + nsPrefix,
						featureType.getNamespaceURI());
			}
			typeNames.append(nsPrefix).append(':').append(featureType.getLocalPart()).append(' ');
		}
		qryElem.setAttribute("typeNames", typeNames.toString().trim());
		Element filter = req.createElementNS(Namespaces.FES, "Filter");
		qryElem.appendChild(filter);
		Element predicateElem = req.createElementNS(Namespaces.FES, operator);
		filter.appendChild(predicateElem);
		appendValueRefToPredicate(predicateElem, properties.get(0));
		appendValueRefToPredicate(predicateElem, properties.get(1));
	}

	/**
	 * Appends a fes:ValueReference element to a filter predicate.
	 * @param predicateElem An Element representing a predicate (operator).
	 * @param featureProperty A FeatureProperty descriptor.
	 */
	public static void appendValueRefToPredicate(Element predicateElem, FeatureProperty featureProperty) {
		Element valueRef = predicateElem.getOwnerDocument().createElementNS(Namespaces.FES, "ValueReference");
		predicateElem.appendChild(valueRef);
		XSElementDeclaration propDecl = featureProperty.getDeclaration();
		String nsURI = propDecl.getNamespace();
		String propPrefix = predicateElem.lookupPrefix(nsURI);
		if (null == propPrefix) {
			propPrefix = "ns-" + Integer.toString((int) (Math.random() * 100));
			valueRef.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + propPrefix, nsURI);
		}
		QName featureType = featureProperty.getFeatureType();
		String typePrefix = predicateElem.lookupPrefix(featureType.getNamespaceURI());
		StringBuilder xpath = new StringBuilder();
		xpath.append(typePrefix).append(':').append(featureType.getLocalPart());
		xpath.append('/').append(propPrefix).append(':');
		xpath.append(propDecl.getName());
		valueRef.setTextContent(xpath.toString());
	}

}
