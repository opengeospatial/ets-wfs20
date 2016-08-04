package org.opengis.cite.iso19142.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.opengis.cite.geomatics.Extents;
import org.opengis.cite.geomatics.SpatialRelationship;
import org.opengis.cite.iso19142.ConformanceClass;
import org.opengis.cite.iso19142.FeatureTypeInfo;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Provides various utility methods for accessing service metadata.
 */
public class ServiceMetadataUtils {

    private static final Logger LOGR = Logger.getLogger(ServiceMetadataUtils.class.getPackage().getName());

    /**
     * Extracts a request endpoint from a WFS capabilities document. If the
     * request URI contains a query component it is removed (but not from the
     * source document).
     * 
     * @param wfsMetadata
     *            A DOM Document node containing service metadata (OGC
     *            capabilities document).
     * @param opName
     *            The operation (request) name.
     * @param binding
     *            The message binding to use (if {@code null} any supported
     *            binding will be used).
     * @return A URI referring to a request endpoint; the URI is empty if no
     *         matching endpoint is found.
     */
    public static URI getOperationEndpoint(final Document wfsMetadata, String opName, ProtocolBinding binding) {
        if (null == binding || binding.equals(ProtocolBinding.ANY)) {
            binding = getOperationBindings(wfsMetadata, opName).iterator().next();
        }
        if (binding.equals(ProtocolBinding.SOAP)) {
            // use POST method for SOAP request
            binding = ProtocolBinding.POST;
        }
        // method name in OGC capabilities doc has initial capital
        StringBuilder method = new StringBuilder(binding.toString());
        method.replace(1, method.length(), method.substring(1).toLowerCase());
        NamespaceBindings nsBindings = new NamespaceBindings();
        nsBindings.addNamespaceBinding(Namespaces.OWS, "ows");
        nsBindings.addNamespaceBinding(Namespaces.XLINK, "xlink");
        String expr = String.format("//ows:Operation[@name='%s']//ows:%s/@xlink:href", opName, method.toString());
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(nsBindings);
        URI endpoint = null;
        try {
            String href = xpath.evaluate(expr, wfsMetadata);
            endpoint = URI.create(href);
        } catch (XPathExpressionException ex) {
            // XPath expression is correct
            TestSuiteLogger.log(Level.INFO, ex.getMessage());
        }
        if (null != endpoint.getQuery()) {
            // prune query component if present
            String uri = endpoint.toString();
            endpoint = URI.create(uri.substring(0, uri.indexOf('?')));
        }
        return endpoint;
    }

    /**
     * Returns a Map containing the HTTP endpoints for a given service request.
     * 
     * @param wfsMetadata
     *            A DOM Document node containing service metadata (WFS
     *            capabilities document).
     * @param reqName
     *            The (local) name of the service request.
     * @return A {@literal Map<String, URI>} object that associates an HTTP
     *         method name with a URI, or {@code null} if the request is not
     *         implemented.
     */
    public static Map<String, URI> getRequestEndpoints(final Document wfsMetadata, String reqName) {
        NamespaceBindings nsBindings = NamespaceBindings.withStandardBindings();
        String expr = String.format("//ows:Operation[@name='%s']/descendant::*[@xlink:href]", reqName);
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(nsBindings);
        Map<String, URI> endpoints = null;
        try {
            NodeList methodNodes = (NodeList) xpath.evaluate(expr, wfsMetadata, XPathConstants.NODESET);
            if ((null == methodNodes) || methodNodes.getLength() == 0) {
                return null;
            }
            endpoints = new HashMap<String, URI>();
            for (int i = 0; i < methodNodes.getLength(); i++) {
                Element methodElem = (Element) methodNodes.item(i);
                String methodName = methodElem.getLocalName().toUpperCase();
                String href = methodElem.getAttributeNS(Namespaces.XLINK, "href");
                if (href.indexOf('?') > 0) {
                    // prune query component if present
                    href = href.substring(0, href.indexOf('?'));
                }
                endpoints.put(methodName, URI.create(href));
            }
        } catch (XPathExpressionException xpe) {
            TestSuiteLogger.log(Level.INFO, xpe.getMessage());
        }
        return endpoints;
    }

    /**
     * Extracts the list of feature type names from a WFS capabilities document.
     * 
     * @param wfsMetadata
     *            A service capabilities document (wfs:WFS_Capabilities).
     * @return A List containing one or more QName items.
     */
    public static List<QName> getFeatureTypes(final Document wfsMetadata) {
        String xpath = "//wfs:FeatureType/wfs:Name";
        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put(Namespaces.WFS, "wfs");
        NodeList typeNames = null;
        try {
            typeNames = XMLUtils.evaluateXPath(wfsMetadata, xpath, nsBindings);
        } catch (XPathExpressionException xpe) {
            TestSuiteLogger.log(Level.INFO, "Failed to evaluate XPath expression: " + xpath, xpe);
        }
        List<QName> featureTypes = new ArrayList<QName>();
        for (int i = 0; i < typeNames.getLength(); i++) {
            Node typeName = typeNames.item(i);
            featureTypes.add(buildQName(typeName));
        }
        LOGR.fine(featureTypes.toString());
        return featureTypes;
    }

    /**
     * Extracts information about feature types from the service metadata
     * document. The following information items are collected for each feature
     * type:
     * 
     * <ul>
     * <li>Qualified type name (wfs:Name)</li>
     * <li>Supported CRS identifiers (wfs:DefaultCRS, wfs:OtherCRS)</li>
     * <li>Spatial extent (ows:WGS84BoundingBox)</li>
     * </ul>
     * 
     * @param wfsCapabilities
     *            A Document (wfs:WFS_Capabilities).
     * @return A Map containing one or more entries where a feature type name
     *         (QName) is associated with a FeatureTypeInfo value object.
     */
    public static Map<QName, FeatureTypeInfo> extractFeatureTypeInfo(final Document wfsCapabilities) {
        Map<QName, FeatureTypeInfo> featureInfo = new HashMap<QName, FeatureTypeInfo>();
        NodeList featureTypes = wfsCapabilities.getElementsByTagNameNS(Namespaces.WFS, "FeatureType");
        for (int i = 0; i < featureTypes.getLength(); i++) {
            FeatureTypeInfo typeInfo = new FeatureTypeInfo();
            Element featureTypeElem = (Element) featureTypes.item(i);
            Node nameNode = featureTypeElem.getElementsByTagNameNS(WFS2.NS_URI, "Name").item(0);
            QName typeName = buildQName(nameNode);
            typeInfo.setTypeName(typeName);
            Node defaultCRSNode = featureTypeElem.getElementsByTagNameNS(WFS2.NS_URI, "DefaultCRS").item(0);
            if (null != defaultCRSNode) {
                typeInfo.addCRSIdentifiers(defaultCRSNode.getTextContent());
            }
            NodeList otherCRSNodes = featureTypeElem.getElementsByTagNameNS(WFS2.NS_URI, "OtherCRS");
            if (otherCRSNodes.getLength() > 0) {
                for (int n = 0; n < otherCRSNodes.getLength(); n++) {
                    typeInfo.addCRSIdentifiers(otherCRSNodes.item(n).getTextContent());
                }
            }
            Node bboxNode = featureTypeElem.getElementsByTagNameNS(Namespaces.OWS, "WGS84BoundingBox").item(0);
            try {
                if (null != bboxNode) {
                    Envelope envelope = Extents.createEnvelope(bboxNode);
                    typeInfo.setSpatialExtent(envelope);
                }
            } catch (FactoryException e) {
                TestSuiteLogger.log(Level.WARNING, e.getMessage());
            }
            featureInfo.put(typeInfo.getTypeName(), typeInfo);
        }
        return featureInfo;
    }

    /**
     * Builds a QName representing the qualified name conveyed by a node with
     * text content.
     * 
     * @param node
     *            A DOM node (Element) containing a qualified name (xsd:QName
     *            value); if it is an unprefixed name, a default namespace
     *            binding should be in scope.
     * @return A QName object.
     */
    public static QName buildQName(Node node) {
        String localPart;
        String nsName = null;
        String name = node.getTextContent();
        int indexOfColon = name.indexOf(':');
        if (indexOfColon > 0) {
            localPart = name.substring(indexOfColon + 1);
            nsName = node.lookupNamespaceURI(name.substring(0, indexOfColon));
        } else {
            localPart = name;
            // return default namespace URI if any
            nsName = node.lookupNamespaceURI(null);
        }
        return new QName(nsName, localPart);
    }

    /**
     * Discovers which protocol bindings are broadly implemented by a WFS. These
     * global constraints may be overridden for a particular operation. The
     * values of the standard request encoding constraints are checked:
     * 
     * <ul>
     * <li>KVPEncoding</li>
     * <li>XMLEncoding</li>
     * <li>SOAPEncoding</li>
     * </ul>
     * 
     * @param wfsMetadata
     *            A service metadata document (wfs:WFS_Capabilities).
     * @return A Set of protocol bindings implemented by the SUT.
     */
    public static Set<ProtocolBinding> getGlobalBindings(final Document wfsMetadata) {
        if (null == wfsMetadata) {
            throw new NullPointerException("WFS metadata document is null.");
        }
        Set<ProtocolBinding> globalBindings = EnumSet.noneOf(ProtocolBinding.class);
        String xpath = "//ows:OperationsMetadata/ows:Constraint[@name='%s' and (ows:DefaultValue = 'TRUE')]";
        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put(Namespaces.OWS, "ows");
        try {
            if (XMLUtils.evaluateXPath(wfsMetadata, String.format(xpath, WFS2.KVP_ENC), nsBindings).getLength() > 0) {
                globalBindings.add(ProtocolBinding.GET);
            }
            if (XMLUtils.evaluateXPath(wfsMetadata, String.format(xpath, WFS2.XML_ENC), nsBindings).getLength() > 0) {
                globalBindings.add(ProtocolBinding.POST);
            }
            if (XMLUtils.evaluateXPath(wfsMetadata, String.format(xpath, WFS2.SOAP_ENC), nsBindings).getLength() > 0) {
                globalBindings.add(ProtocolBinding.SOAP);
            }
        } catch (XPathExpressionException xpe) {
            throw new RuntimeException("Error evaluating XPath expression against capabilities doc. ", xpe);
        }
        return globalBindings;
    }

    /**
     * Determines which protocol bindings are supported for a given operation.
     * This method will currently not handle the case where a global binding is
     * disabled for an operation (a per-operation constraint overrides a global
     * constraint).
     * 
     * @param wfsMetadata
     *            A service metadata document (wfs:WFS_Capabilities).
     * @param opName
     *            The name of a WFS operation.
     * @return A Set of protocol bindings supported for the operation.
     */
    public static Set<ProtocolBinding> getOperationBindings(final Document wfsMetadata, String opName) {
        Set<ProtocolBinding> protoBindings = new HashSet<ProtocolBinding>();
        String expr = "//ows:Operation[@name='%s']/ows:Constraint[@name='%s' and (ows:DefaultValue = 'TRUE')]";
        for (ProtocolBinding binding : EnumSet.allOf(ProtocolBinding.class)) {
            String xpath = String.format(expr, opName, binding.getConstraintName());
            try {
                if (XMLUtils.evaluateXPath(wfsMetadata, xpath, null).getLength() > 0) {
                    protoBindings.add(ProtocolBinding.GET);
                }
            } catch (XPathExpressionException xpe) {
                throw new RuntimeException("Error evaluating XPath expression against capabilities doc. ", xpe);
            }
        }
        // union with globally declared bindings
        protoBindings.addAll(getGlobalBindings(wfsMetadata));
        if (opName.equals(WFS2.TRANSACTION)) {
            // KVP content type not defined for Transaction requests
            protoBindings.remove(ProtocolBinding.GET);
        }
        return protoBindings;
    }

    /**
     * Returns a set of conformance classes that the WFS under test claims to
     * satisfy.
     * 
     * @param wfsMetadata
     *            A service metadata document (wfs:WFS_Capabilities).
     * @return A Set containing at least two members: a fundamental conformance
     *         level and a message binding.
     * 
     * @see "ISO 19142:2010, Geographic information -- Web Feature Service: Table 13"
     */
    public static Set<ConformanceClass> getConformanceClaims(final Document wfsMetadata) {
        Set<ConformanceClass> conformanceSet = EnumSet.allOf(ConformanceClass.class);
        String expr = "//ows:Constraint[@name='%s' and (ows:DefaultValue = 'TRUE')]";
        Iterator<ConformanceClass> itr = conformanceSet.iterator();
        while (itr.hasNext()) {
            ConformanceClass conformClass = itr.next();
            String xpath = String.format(expr, conformClass.getConstraintName());
            NodeList result;
            try {
                result = XMLUtils.evaluateXPath(wfsMetadata, xpath, null);
            } catch (XPathExpressionException xpe) {
                throw new RuntimeException("Error evaluating XPath expression against capabilities doc. " + xpath, xpe);
            }
            if (result.getLength() == 0) {
                conformanceSet.remove(conformClass);
            }
        }
        return conformanceSet;
    }

    /**
     * Indicates whether or not the specified spatial operator is supported. The
     * standard operators are listed below.
     * <ul>
     * <li>BBOX (mandatory)</li>
     * <li>Equals</li>
     * <li>Disjoint</li>
     * <li>Intersects</li>
     * <li>Touches</li>
     * <li>Crosses</li>
     * <li>Within</li>
     * <li>Contains</li>
     * <li>Overlaps</li>
     * <li>Beyond</li>
     * <li>DWithin</li>
     * </ul>
     * 
     * @param wfsMetadata
     *            A WFS capabilities document.
     * @param operatorName
     *            The name of a spatial operator.
     * @return true if the operator is supported; false if not.
     */
    public static boolean implementsSpatialOperator(final Document wfsMetadata, String operatorName) {
        String expr = String.format("//fes:SpatialOperator[@name='%s']", operatorName);
        NodeList results = null;
        try {
            results = XMLUtils.evaluateXPath(wfsMetadata, expr, null);
        } catch (XPathExpressionException e) { // expr ok
        }
        return results.getLength() > 0;
    }

    /**
     * Returns the set of spatial operators (other than BBOX, DWithin, Beyond)
     * listed in a WFS service description.
     * 
     * @param wfsMetadata
     *            A WFS capabilities document.
     * @return A set containing all implemented spatial predicates.
     */
    public static Set<SpatialRelationship> getImplementedSpatialOperators(final Document wfsMetadata) {
        Set<SpatialRelationship> implementedOps = EnumSet.noneOf(SpatialRelationship.class);
        NodeList spatialOps = wfsMetadata.getElementsByTagNameNS(Namespaces.FES, "SpatialOperator");
        for (int i = 0; i < spatialOps.getLength(); i++) {
            Element spatialOp = (Element) spatialOps.item(i);
            String opName = spatialOp.getAttribute("name").toUpperCase();
            if (Arrays.asList(new String[] { "BBOX", "DWITHIN", "BEYOND" }).contains(opName)) {
                continue; // ignore analytic methods
            }
            implementedOps.add(SpatialRelationship.valueOf(opName));
        }
        return implementedOps;
    }

    /**
     * Indicates whether or not the specified temporal operator is supported.
     * The standard operators are listed below.
     * <ul>
     * <li>During (mandatory)</li>
     * <li>After</li>
     * <li>Before</li>
     * <li>Begins</li>
     * <li>BegunBy</li>
     * <li>TContains</li>
     * <li>TEquals</li>
     * <li>TOverlaps</li>
     * <li>Meets</li>
     * <li>OverlappedBy</li>
     * <li>MetBy</li>
     * <li>Ends</li>
     * <li>EndedBy</li>
     * </ul>
     * 
     * @param wfsMetadata
     *            A WFS capabilities document.
     * @param operatorName
     *            The name of a temporal operator.
     * @return true if the operator is supported; false if not.
     */
    public static boolean implementsTemporalOperator(final Document wfsMetadata, String operatorName) {
        String expr = String.format("//fes:TemporalOperator[@name='%s']", operatorName);
        NodeList results = null;
        try {
            results = XMLUtils.evaluateXPath(wfsMetadata, expr, null);
        } catch (XPathExpressionException e) { // expr ok
        }
        return results.getLength() > 0;
    }
}
