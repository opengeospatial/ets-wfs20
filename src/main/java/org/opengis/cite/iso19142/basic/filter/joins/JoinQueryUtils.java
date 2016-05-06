package org.opengis.cite.iso19142.basic.filter.joins;

import java.util.List;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSElementDeclaration;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.WFS2;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Utility methods for constructing join queries.
 */
public class JoinQueryUtils {

	/**
	 * Inserts a spatial join query into a GetFeature request entity.
	 * 
	 * @param req
	 *            A Document node representing a GetFeature request.
	 * @param operator
	 *            The name of a spatial operator.
	 * @param properties
	 *            A sequence of (2) map entries specifying the feature
	 *            properties in the join condition; the QName key identifies the
	 *            feature type for which a property is defined (the first
	 *            property is used if there is more than one in the list).
	 */
	@SafeVarargs
	public static void appendSpatialJoinQuery(Document req, String operator,
			Entry<QName, List<XSElementDeclaration>>... properties) {
		if (!req.getDocumentElement().getLocalName().equals(WFS2.GET_FEATURE)) {
			throw new IllegalArgumentException("Not a GetFeature request: "
					+ req.getDocumentElement().getNodeName());
		}
		if (null == properties) {
			throw new NullPointerException("Feature properties are required.");
		}
		if (properties.length != 2) {
			throw new IllegalArgumentException(
					"Two feature properties are required, but received "
							+ properties.length);
		}
		Element docElem = req.getDocumentElement();
		Element qryElem = req.createElementNS(Namespaces.WFS, "wfs:Query");
		docElem.appendChild(qryElem);
		StringBuilder typeNames = new StringBuilder();
		for (Entry<QName, List<XSElementDeclaration>> entry : properties) {
			QName featureType = entry.getKey();
			// look for prefix already bound to this namespace URI
			String nsPrefix = qryElem.lookupPrefix(featureType
					.getNamespaceURI());
			if (null == nsPrefix) {
				nsPrefix = "ns" + Integer.toString((int) (Math.random() * 100));
				qryElem.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
						"xmlns:" + nsPrefix, featureType.getNamespaceURI());
			}
			typeNames.append(nsPrefix).append(':')
					.append(featureType.getLocalPart()).append(' ');
		}
		qryElem.setAttribute("typeNames", typeNames.toString().trim());
		Element filter = req.createElementNS(Namespaces.FES, "Filter");
		qryElem.appendChild(filter);
		Element predicateElem = req.createElementNS(Namespaces.FES, operator);
		filter.appendChild(predicateElem);
		appendValueRefToPredicate(predicateElem, properties[0]);
		appendValueRefToPredicate(predicateElem, properties[1]);
	}

	/**
	 * Appends a fes:ValueReference element to a filter predicate.
	 * 
	 * @param predicateElem
	 *            An Element representing a predicate (operator).
	 * @param featureProperty
	 *            A map entry (key-value pair), where the key identifies the
	 *            feature type and the value is a list of properties; the first
	 *            property is used if there is more than one.
	 */
	public static void appendValueRefToPredicate(Element predicateElem,
			Entry<QName, List<XSElementDeclaration>> featureProperty) {
		Element valueRef = predicateElem.getOwnerDocument().createElementNS(
				Namespaces.FES, "ValueReference");
		predicateElem.appendChild(valueRef);
		XSElementDeclaration propDecl = featureProperty.getValue().get(0);
		String nsURI = propDecl.getNamespace();
		String propPrefix = predicateElem.lookupPrefix(nsURI);
		if (null == propPrefix) {
			propPrefix = "ns-" + Integer.toString((int) (Math.random() * 100));
			valueRef.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
					"xmlns:" + propPrefix, nsURI);
		}
		QName featureType = featureProperty.getKey();
		String typePrefix = predicateElem.lookupPrefix(featureType
				.getNamespaceURI());
		StringBuilder xpath = new StringBuilder();
		xpath.append(typePrefix).append(':').append(featureType.getLocalPart());
		xpath.append('/').append(propPrefix).append(':');
		xpath.append(propDecl.getName());
		valueRef.setTextContent(xpath.toString());
	}
}
