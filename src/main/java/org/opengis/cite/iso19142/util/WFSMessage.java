package org.opengis.cite.iso19142.util;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.opengis.cite.iso19136.util.XMLSchemaModelUtils;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Provides various utility methods for constructing, accessing, or manipulating
 * the content of WFS request and response messages.
 */
public class WFSMessage {

    private static final Logger LOGR = Logger.getLogger(WFSMessage.class.getPackage().getName());
    private static final String TNS_PREFIX = "tns";
    private static final DocumentBuilder BUILDER = initDocBuilder();

    private static DocumentBuilder initDocBuilder() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            TestSuiteLogger.log(Level.WARNING, "Failed to create parser", e);
        }
        return builder;
    }

    /**
     * Transforms the XML representation of a WFS request entity to its
     * corresponding KVP serialization format.
     * 
     * @param xmlSource
     *            A Source representing the XML request entity.
     * @return A String containing the resulting query component.
     */
    public static String transformEntityToKVP(Source xmlSource) {
        Source xsltSource = new StreamSource(WFSMessage.class.getResourceAsStream("xml2kvp.xsl"));
        TransformerFactory factory = TransformerFactory.newInstance();
        StringWriter writer = new StringWriter();
        try {
            Transformer transformer = factory.newTransformer(xsltSource);
            transformer.transform(xmlSource, new StreamResult(writer));
        } catch (Exception e) {
            TestSuiteLogger.log(Level.WARNING, "Failed to generate KVP result from Source " + xmlSource.getSystemId(),
                    e);
        }
        return writer.toString();
    }

    /**
     * Wraps the given XML request entity in the body of a SOAP envelope.
     * 
     * @param xmlSource
     *            The Source providing the XML request entity.
     * @param version
     *            The version of the SOAP protocol (either "1.1" or "1.2"); if
     *            not specified the latest version is assumed.
     * @return A DOM Document node representing a SOAP request message.
     */
    public static Document wrapEntityInSOAPEnvelope(Source xmlSource, String version) {
        String soapNS;
        if (null != version && version.equals("1.1")) {
            soapNS = Namespaces.SOAP11;
        } else {
            soapNS = Namespaces.SOAP_ENV;
        }
        Document soapDoc = BUILDER.newDocument();
        Element soapEnv = soapDoc.createElementNS(soapNS, "soap:Envelope");
        soapDoc.appendChild(soapEnv);
        Element soapBody = soapDoc.createElementNS(soapNS, "soap:Body");
        soapEnv.appendChild(soapBody);
        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer idTransformer = tFactory.newTransformer();
            Document wfsReq = BUILDER.newDocument();
            idTransformer.transform(xmlSource, new DOMResult(wfsReq));
            soapBody.appendChild(soapDoc.importNode(wfsReq.getDocumentElement(), true));
        } catch (Exception e) {
            TestSuiteLogger.log(Level.WARNING, "Failed to create SOAP envelope from Source " + xmlSource.getSystemId(),
                    e);
        }
        return soapDoc;
    }

    /**
     * Adds a simple wfs:Query element (without a filter) to the given request
     * entity. The typeNames attribute value is set using the supplied QName
     * objects. Namespace bindings are added if necessary.
     * 
     * @param doc
     *            A Document representing a WFS request entity that accepts
     *            wfs:Query elements as children of the document element
     *            (GetFeature, GetPropertyValue, GetFeatureWithLock,
     *            LockFeature).
     * @param qNames
     *            A sequence of QName objects representing (qualified) feature
     *            type names recognized by the IUT.
     * @return The Element representing the query expression (wfs:Query); it
     *         will be empty.
     */
    public static Element appendSimpleQuery(Document doc, QName... qNames) {
        Element docElement = doc.getDocumentElement();
        Element newQuery = doc.createElementNS(Namespaces.WFS, "wfs:Query");
        StringBuilder typeNames = new StringBuilder();
        for (QName qName : qNames) {
            // look for prefix already bound to this namespace URI
            String nsPrefix = docElement.lookupPrefix(qName.getNamespaceURI());
            if (null == nsPrefix) {
                nsPrefix = "ns" + Integer.toString((int) (Math.random() * 100));
                newQuery.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + nsPrefix,
                        qName.getNamespaceURI());
            }
            typeNames.append(nsPrefix).append(':').append(qName.getLocalPart()).append(' ');
        }
        newQuery.setAttribute("typeNames", typeNames.toString().trim());
        docElement.appendChild(newQuery);
        return newQuery;
    }

    /**
     * Adds a wfs:StoredQuery element to the given request entity.
     * 
     * @param doc
     *            A Document representing a WFS request entity that accepts
     *            wfs:StoredQuery elements as children of the document element
     *            (GetFeature, GetPropertyValue, GetFeatureWithLock,
     *            LockFeature).
     * @param queryId
     *            A URI that identifies the stored query to invoke.
     * @param params
     *            A Map containing query parameters (may be empty, e.g.
     *            {@literal Collections.<String, Object>.emptyMap()}). A
     *            parameter name is associated with an Object (String or QName)
     *            representing its value.
     */
    public static void appendStoredQuery(Document doc, String queryId, Map<String, Object> params) {
        Element docElement = doc.getDocumentElement();
        Element newQuery = doc.createElementNS(Namespaces.WFS, "wfs:StoredQuery");
        newQuery.setAttribute("id", queryId);
        docElement.appendChild(newQuery);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Element param = doc.createElementNS(Namespaces.WFS, WFS2.PARAM_ELEM);
            param.setPrefix("wfs");
            param.setAttribute("name", entry.getKey());
            newQuery.appendChild(param);
            Object value = entry.getValue();
            if (QName.class.isInstance(value)) {
                QName qName = QName.class.cast(value);
                String prefix = (qName.getPrefix().isEmpty()) ? "tns" : qName.getPrefix();
                param.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + prefix, qName.getNamespaceURI());
                param.setTextContent(prefix + ":" + qName.getLocalPart());
            } else {
                param.setTextContent(value.toString());
            }
        }
    }

    /**
     * Creates an XML request entity of the specified request type.
     * 
     * @param reqResource
     *            The name of a classpath resource containing an XML request
     *            entity.
     * @param wfsVersion
     *            A WFS version identifier ("2.0.0" if not specified).
     * @return A Document representing a WFS request entity.
     */
    public static Document createRequestEntity(String reqResource, String wfsVersion) {
        String resourceName = reqResource + ".xml";
        Document doc = null;
        try {
            doc = BUILDER.parse(WFSMessage.class.getResourceAsStream(resourceName));
        } catch (Exception e) {
            TestSuiteLogger.log(Level.WARNING, "Failed to parse request entity from classpath: " + resourceName, e);
        }
        Attr verAttr = doc.getDocumentElement().getAttributeNode("version");
        if (null != verAttr && null != wfsVersion && !wfsVersion.isEmpty()) {
            doc.getDocumentElement().getAttributeNode("version").setValue(wfsVersion);
        }
        return doc;
    }

    /**
     * Sets the value of the typeName attribute on an action element
     * (wfs:Update, wfs:Delete) contained in a Transaction request entity.
     * 
     * @param elem
     *            An action element in a transaction request.
     * @param qName
     *            The qualified name of a feature type.
     */
    public static void setTypeName(Element elem, QName qName) {
        List<String> actions = Arrays.asList(WFS2.UPDATE, WFS2.DELETE);
        if (!actions.contains(elem.getLocalName())) {
            return;
        }
        StringBuilder typeNames = new StringBuilder();
        // look for prefix already bound to this namespace URI
        String nsPrefix = elem.lookupPrefix(qName.getNamespaceURI());
        if (null == nsPrefix) { // check document element
            nsPrefix = elem.getOwnerDocument().lookupPrefix(qName.getNamespaceURI());
        }
        if (null == nsPrefix) {
            nsPrefix = "ns" + Integer.toString((int) (Math.random() * 100));
            elem.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + nsPrefix, qName.getNamespaceURI());
        }
        typeNames.append(nsPrefix).append(':').append(qName.getLocalPart());
        elem.setAttribute("typeName", typeNames.toString());
    }

    /**
     * Builds a filter predicate containing a fes:ResourceId element that
     * identifies the feature instance to be modified.
     * 
     * @param id
     *            A String denoting a GML object identifier (gml:id).
     * @return An Element node (fes:Filter).
     */
    public static Element newResourceIdFilter(String id) {
        Element filter = XMLUtils.createElement(new QName(Namespaces.FES, "Filter", "fes"));
        Element resourceId = XMLUtils.createElement(new QName(Namespaces.FES, "ResourceId", "fes"));
        resourceId.setAttribute("rid", id);
        filter.appendChild(filter.getOwnerDocument().adoptNode(resourceId));
        return filter;
    }

    /**
     * Inserts a standard GML property into a given feature instance. If the
     * property node already exists it is replaced.
     * 
     * @param feature
     *            An Element node representing a GML feature
     * @param gmlProperty
     *            An Element node representing a standard (non-deprecated) GML
     *            feature property.
     */
    public static void insertGMLProperty(Element feature, Element gmlProperty) {
        Document doc = feature.getOwnerDocument();
        QName gmlPropName = new QName(gmlProperty.getNamespaceURI(), gmlProperty.getLocalName());
        NodeList existing = feature.getElementsByTagNameNS(gmlPropName.getNamespaceURI(), gmlPropName.getLocalPart());
        if (existing.getLength() > 0) {
            Node oldProp = existing.item(0);
            gmlProperty.setPrefix(oldProp.getPrefix());
            feature.replaceChild(doc.adoptNode(gmlProperty), oldProp);
            return;
        }
        // Create map associating GML property with collection of following
        // siblings to help determine insertion point (before nextSibling).
        Map<QName, List<String>> followingSiblingsMap = new HashMap<QName, List<String>>();
        followingSiblingsMap.put(new QName(Namespaces.GML, "description"),
                Arrays.asList("descriptionReference", "identifier", "name", "boundedBy", "location"));
        followingSiblingsMap.put(new QName(Namespaces.GML, "identifier"),
                Arrays.asList("name", "boundedBy", "location"));
        followingSiblingsMap.put(new QName(Namespaces.GML, "name"), Arrays.asList("boundedBy", "location"));
        if (!followingSiblingsMap.containsKey(gmlPropName))
            return; // ignore deprecated properties
        List<String> followingSibs = followingSiblingsMap.get(gmlPropName);
        NodeList properties = feature.getChildNodes();
        Node nextSibling = null;
        for (int i = 0; i < properties.getLength(); i++) {
            Node property = properties.item(i);
            if (property.getNodeType() != Node.ELEMENT_NODE)
                continue;
            String nsURI = property.getNamespaceURI();
            String propName = property.getLocalName();
            // check if application-defined prop or a following GML prop
            if (!nsURI.equals(Namespaces.GML) || (followingSibs.contains(propName) && nsURI.equals(Namespaces.GML))) {
                nextSibling = property;
                break;
            }
        }
        if (nextSibling.getNamespaceURI().equals(Namespaces.GML)) {
            gmlProperty.setPrefix(nextSibling.getPrefix());
        } else {
            gmlProperty.setPrefix("gml");
        }
        feature.insertBefore(doc.adoptNode(gmlProperty), nextSibling);
    }

    /**
     * Creates an Element node (fes:ValueReference) containing an XPath
     * expression derived from a property element declaration.
     * 
     * @param propertyElem
     *            An element declaration that defines some feature property.
     * @return An Element containing an XPath expression and an appropriate
     *         namespace binding.
     */
    public static Element createValueReference(XSElementDeclaration propertyElem) {
        Element valueRef = XMLUtils.createElement(new QName(Namespaces.FES, "ValueReference", "fes"));
        valueRef.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + TNS_PREFIX,
                propertyElem.getNamespace());
        valueRef.setTextContent(TNS_PREFIX + ":" + propertyElem.getName());
        return valueRef;
    }

    /**
     * Creates a GML envelope covering the area of use for the "WGS 84" CRS
     * (srsName="urn:ogc:def:crs:EPSG::4326").
     * 
     * @return A Document containing gml:Envelope as the document element.
     */
    public static Document createGMLEnvelope() {
        Document doc;
        try {
            doc = BUILDER.parse(WFSMessage.class.getResourceAsStream("Envelope.xml"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return doc;
    }

    /**
     * Adds a namespace binding to the document element.
     * 
     * @param doc
     *            A Document representing a request entity.
     * @param qName
     *            A QName containing a namespace URI and prefix; the local part
     *            is ignored.
     */
    public static void addNamespaceBinding(Document doc, QName qName) {
        Element docElem = doc.getDocumentElement();
        docElem.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + qName.getPrefix(),
                qName.getNamespaceURI());
    }

    /**
     * Adds a sequence of wfs:Replace statements to the given transaction
     * request entity.
     * 
     * @param trxRequest
     *            A Document node representing a wfs:Transaction request entity.
     * @param replacements
     *            A List containing replacement feature representations (as
     *            GML).
     */
    public static void addReplaceStatements(Document trxRequest, List<Element> replacements) {
        Element docElem = trxRequest.getDocumentElement();
        if (!docElem.getLocalName().equals(WFS2.TRANSACTION)) {
            throw new IllegalArgumentException("Document node is not a Transaction request: " + docElem.getNodeName());
        }
        for (Element feature : replacements) {
            Element replace = trxRequest.createElementNS(Namespaces.WFS, "Replace");
            replace.setPrefix("wfs");
            replace.appendChild(trxRequest.importNode(feature, true));
            Element filter = WFSMessage.newResourceIdFilter(feature.getAttributeNS(Namespaces.GML, "id"));
            replace.appendChild(trxRequest.adoptNode(filter));
            docElem.appendChild(replace);
        }
        if (TestSuiteLogger.isLoggable(Level.FINE)) {
            TestSuiteLogger.log(Level.FINE, XMLUtils.writeNodeToString(trxRequest));
        }
    }

    /**
     * Appends a wfs:Insert element to the document element in the given request
     * entity. The wfs:Insert element contains the supplied feature instance.
     * 
     * @param request
     *            A Document node representing a wfs:Transaction request entity.
     * @param feature
     *            A Node representing a GML feature instance.
     */
    public static void addInsertStatement(Document request, Node feature) {
        Element docElem = request.getDocumentElement();
        if (!docElem.getLocalName().equals(WFS2.TRANSACTION)) {
            throw new IllegalArgumentException("Document node is not a Transaction request: " + docElem.getNodeName());
        }
        Element insert = request.createElementNS(Namespaces.WFS, "Insert");
        docElem.appendChild(insert);
        insert.appendChild(request.importNode(feature, true));
    }

    /**
     * Adds a ResourceId predicate to a GetFeature (or GetFeatureWithLock)
     * request entity that contains a simple query expression without a filter.
     * The identifiers should match features of the indicated type.
     * 
     * @param request
     *            The request entity (/wfs:GetFeature/[wfs:Query]).
     * @param idSet
     *            A {@literal Set<String>} of feature identifiers that conform
     *            to the xsd:ID datatype.
     */
    public static void addResourceIdPredicate(Document request, Set<String> idSet) {
        if (idSet.isEmpty())
            return;
        if (!request.getDocumentElement().getLocalName().startsWith(WFS2.GET_FEATURE)) {
            throw new IllegalArgumentException(
                    "Expected a GetFeature(WithLock) request: " + request.getDocumentElement().getNodeName());
        }
        NodeList queryList = request.getElementsByTagNameNS(Namespaces.WFS, WFS2.QUERY_ELEM);
        if (queryList.getLength() == 0) {
            throw new IllegalArgumentException(
                    "No wfs:Query element found in request: " + request.getDocumentElement().getNodeName());
        }
        Element filter = request.createElementNS(Namespaces.FES, "Filter");
        queryList.item(0).appendChild(filter);
        for (String id : idSet) {
            Element resourceId = request.createElementNS(Namespaces.FES, "ResourceId");
            resourceId.setAttribute("rid", id);
            filter.appendChild(resourceId);
        }
    }

    /**
     * Checks the given list of objects for the presence of a
     * {@link ProtocolBinding#GET} object.
     * 
     * @param testParams
     *            A list of objects representing test method parameters.
     * @return true if a {@literal ProtocolBinding#GET} object was found; false
     *         otherwise.
     */
    public static boolean containsGetProtocolBinding(Object[] testParams) {
        if (null == testParams || testParams.length == 0) {
            return false;
        }
        boolean foundGetBinding = false;
        for (Object param : testParams) {
            if (ProtocolBinding.class.isInstance(param)
                    && ProtocolBinding.class.cast(param).equals(ProtocolBinding.GET)) {
                foundGetBinding = true;
                break;
            }
        }
        return foundGetBinding;
    }

    /**
     * Finds elements in a DOM Document that correspond to the given collection
     * of element declarations.
     * 
     * @param doc
     *            A Document node containing an XML entity.
     * @param elemDeclarations
     *            A collection of element declarations.
     * @return A list of matching element nodes (it may be empty).
     */
    public static List<Node> findMatchingElements(Document doc, XSElementDeclaration... elemDeclarations) {
        LOGR.log(Level.FINE, String.format("In %s, find %s", doc.getDocumentElement().getNodeName(),
                Arrays.toString(elemDeclarations)));
        List<Node> nodes = new ArrayList<>();
        for (XSElementDeclaration decl : elemDeclarations) {
            NodeList matches = doc.getElementsByTagNameNS(decl.getNamespace(), decl.getName());
            LOGR.log(Level.FINE, String.format("Found %d instances of %s", matches.getLength(), decl));
            for (int i = 0; i < matches.getLength(); i++) {
                nodes.add(matches.item(i));
            }
        }
        return nodes;
    }

    /**
     * Finds elements in a DOM Document that occur as values of the specified
     * feature property.
     * 
     * @param doc
     *            A Document node containing a WFS response entity.
     * @param propertyDecl
     *            An element declaration that defines a feature property.
     * @param schema
     *            A representation of an application schema.
     * @return A list of matching element nodes (it may be empty).
     */
    public static List<Node> findPropertyValues(Document doc, XSElementDeclaration propertyDecl, XSModel schema) {
        LOGR.log(Level.FINE,
                String.format("In %s, find values of %s", doc.getDocumentElement().getNodeName(), propertyDecl));
        XSElementDeclaration propValue = AppSchemaUtils.getComplexPropertyValue(propertyDecl);
        XSElementDeclaration[] expectedValues = new XSElementDeclaration[1];
        if (propValue.getAbstract()) {
            List<XSElementDeclaration> allowedValues = XMLSchemaModelUtils.getElementsByAffiliation(schema, propValue);
            if (allowedValues.isEmpty()) {
                throw new AssertionError(String.format(
                        "For property %s, no substitutable elements found for abstract property value: %s",
                        propertyDecl, propValue));
            }
            expectedValues = allowedValues.toArray(expectedValues);
        } else {
            expectedValues[0] = propValue;
        }
        List<Node> valueNodes = findMatchingElements(doc, expectedValues);
        return valueNodes;
    }

    /**
     * Returns the set of feature identifiers found in the given WFS response
     * entity.
     * 
     * @param doc
     *            A WFS response entity that may contain feature instances.
     * @param featureType
     *            The feature type of interest.
     * @return A set of feature identifiers (gml:id attribute values); it may be
     *         empty.
     */
    public static Set<String> extractFeatureIdentifiers(Document doc, QName featureType) {
        Set<String> idSet = new HashSet<>();
        if (null == featureType) { // alternative: use XPath if null
            throw new IllegalArgumentException("featureType is null");
        }
        NodeList features = doc.getElementsByTagNameNS(featureType.getNamespaceURI(), featureType.getLocalPart());
        for (int i = 0; i < features.getLength(); i++) {
            Element feature = (Element) features.item(i);
            idSet.add(feature.getAttributeNS(Namespaces.GML, "id"));
        }
        return idSet;
    }

    /**
     * Adds a temporal predicate to a GetFeature request entity. If the given
     * temporal element has no temporal reference (frame) it is assumed to use
     * the default frame (ISO 8601).
     * 
     * @param request
     *            The request entity (wfs:GetFeature).
     * @param temporalOp
     *            The name of a spatial operator.
     * @param gmlTime
     *            A Document containing a GML temporal primitive.
     * @param valueRef
     *            An Element (fes:ValueReference) that specifies the temporal
     *            property to check. If it is {@code null}, the predicate
     *            applies to all temporal properties.
     */
    public static void addTemporalPredicate(Document request, String temporalOp, Document gmlTime, Element valueRef) {
        if (!request.getDocumentElement().getLocalName().equals(WFS2.GET_FEATURE)) {
            throw new IllegalArgumentException(
                    "Not a GetFeature request: " + request.getDocumentElement().getNodeName());
        }
        Element queryElem = (Element) request.getElementsByTagNameNS(Namespaces.WFS, WFS2.QUERY_ELEM).item(0);
        if (null == queryElem) {
            throw new IllegalArgumentException("No Query element found in GetFeature request entity.");
        }
        Element filter = request.createElementNS(Namespaces.FES, "fes:Filter");
        queryElem.appendChild(filter);
        Element predicate = request.createElementNS(Namespaces.FES, "fes:" + temporalOp);
        filter.appendChild(predicate);
        if (null != valueRef) {
            predicate.appendChild(request.importNode(valueRef, true));
        }
        // import temporal element to avoid WRONG_DOCUMENT_ERR
        predicate.appendChild(request.importNode(gmlTime.getDocumentElement(), true));
    }

}
