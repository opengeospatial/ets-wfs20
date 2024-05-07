package org.opengis.cite.iso19142.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
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
import javax.xml.transform.dom.DOMSource;

import org.glassfish.jersey.client.ClientConfig;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.basic.filter.ResourceId;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;


/**
 * A WFS 2.0 client component.
 */
public class WFSClient {

    private static final Logger LOGR = Logger.getLogger(WFSClient.class.getPackage().getName());
    protected Client client;
    /** A Document that describes the service under test. */
    protected Document wfsMetadata;
    /** The set of message bindings broadly implemented by the SUT. */
    protected Set<ProtocolBinding> globalBindings;
    /** The list of feature types recognized by the SUT. */
    protected List<QName> featureTypes;
    /** The WFS version supported by the IUT. */
    private String wfsVersion;

    /**
     * Default client constructor. The client is configured to consume SOAP
     * message entities. The request and response may be logged to a default JDK
     * logger (in the namespace "com.sun.jersey.api.client"). TODO
     */
    public WFSClient() {
        ClientConfig config = new ClientConfig();
//        config.getClasses().add(SOAPMessageConsumer.class);
        this.client = ClientBuilder.newClient(config);
        this.client.register(new LoggingFilter());
        this.client.register(new SOAPMessageConsumer());
    }

    /**
     * Constructs a client that is aware of the capabilities of a WFS
     * implementation.
     * 
     * @param wfsMetadata
     *            A service description (e.g. WFS capabilities document).
     */
    public WFSClient(Document wfsMetadata) {
        this();
        String docElemName = wfsMetadata.getDocumentElement().getLocalName();
        if (!docElemName.equals(WFS2.WFS_CAPABILITIES)) {
            throw new IllegalArgumentException("Not a WFS service description: " + docElemName);
        }
        this.wfsMetadata = wfsMetadata;
        this.wfsVersion = wfsMetadata.getDocumentElement().getAttribute("version");
        this.featureTypes = ServiceMetadataUtils.getFeatureTypes(wfsMetadata);
        this.globalBindings = ServiceMetadataUtils.getGlobalBindings(wfsMetadata);
    }

    /**
     * Returns the WFS service description set for this client.
     * 
     * @return A WFS capabilities document (wfs:WFS_Capabilities).
     */
    public Document getServiceDescription() {
        return wfsMetadata;
    }

    /**
     * Returns the underlying JAX-RS client.
     * 
     * @return A JAX-RS client component.
     */
    public Client getClient() {
        return client;
    }

    /**
     * Sets the service description obtained using the given InputStream. The
     * standard description is an XML representation of a WFS capabilities
     * document.
     * 
     * @param srvMetadata
     *            An InputStream supplying the service metadata.
     * @throws SAXException
     *             If any I/O errors occur.
     * @throws IOException
     *             If any parsing errors occur.
     */
    public void setServiceDescription(InputStream srvMetadata) throws SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(srvMetadata);
            if (doc.getDocumentElement().getLocalName().equals(WFS2.WFS_CAPABILITIES))
                this.wfsMetadata = docBuilder.parse(srvMetadata);
        } catch (ParserConfigurationException e) {
            LOGR.log(Level.WARNING, e.getMessage());
        }
    }

    /**
     * Invokes a stored query using any supported protocol binding (request
     * encoding).
     * 
     * @param queryId
     *            A stored query identifier.
     * @param params
     *            A collection of query parameters distinguished by name (may be
     *            empty, e.g. {@literal Collections.<String, Object>.emptyMap()}
     *            ).
     * @return A Document representing the XML response entity, or {@code null}
     *         if the response doesn't contain one.
     */
    public Document invokeStoredQuery(String queryId, Map<String, Object> params) {
        if (this.wfsVersion.equals(WFS2.V2_0_0) && queryId.equals(WFS2.QRY_GET_FEATURE_BY_ID)) {
            // use deprecated URN identifier in WFS 2.0.0
            queryId = WFS2.QRY_GET_FEATURE_BY_ID_URN;
        }
        Document req = WFSMessage.createRequestEntity("GetFeature", this.wfsVersion);
        WFSMessage.appendStoredQuery(req, queryId, params);
        ProtocolBinding binding = globalBindings.iterator().next();
        return retrieveXMLResponseEntity(req, binding);
    }

    /**
     * Retrieves feature representations by type name.
     * 
     * @param typeName
     *            A QName denoting the feature type.
     * @param count
     *            The maximum number of features to fetch (&gt; 0). If count
     *            &lt; 1, the default value (10) applies.
     * @param binding
     *            The ProtocolBinding to use for this request; if {@code null} a
     *            global binding will be used.
     * @return A Document representing the XML response entity, or {@code null}
     *         if the response doesn't contain one.
     */
    public Document getFeatureByType(QName typeName, int count, ProtocolBinding binding) {
        if (null == binding) {
            binding = globalBindings.iterator().next();
        }
        Document req = WFSMessage.createRequestEntity("GetFeature", this.wfsVersion);
        if (count > 0) {
            req.getDocumentElement().setAttribute("count", Integer.toString(count));
        }
        WFSMessage.appendSimpleQuery(req, typeName);
        return retrieveXMLResponseEntity(req, binding);
    }

    /**
     * Submits a GetFeature request.
     * 
     * @param reqEntity
     *            A Source representing the content of the request entity; if
     *            necessary it will be transformed into the query component of a
     *            GET request.
     * @param binding
     *            The HTTP method binding to use.
     * @return A client response context.
     */
    public Response getFeature(Source reqEntity, ProtocolBinding binding) {
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.GET_FEATURE, binding);
        return submitRequest(reqEntity, binding, endpoint);
    }

    /**
     * Submits a request to delete a collection of features specified by
     * identifier and type name.
     * 
     * @param features
     *            A Map containing entries that specify a feature by identifier
     *            (gml:id attribute value) and type name (QName).
     * @param binding
     *            The ProtocolBinding to use.
     * @return A Document representing the XML response entity, or {@code null}
     *         if the response doesn't contain one.
     */
    public Document deleteFeatures(Map<String, QName> features, ProtocolBinding binding) {
        Document req = WFSMessage.createRequestEntity(WFS2.TRANSACTION, this.wfsVersion);
        for (Map.Entry<String, QName> entry : features.entrySet()) {
            QName typeName = entry.getValue();
            Element delete = req.createElementNS(Namespaces.WFS, "Delete");
            delete.setPrefix("wfs");
            delete.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":tns", typeName.getNamespaceURI());
            delete.setAttribute("typeName", "tns:" + typeName.getLocalPart());
            req.getDocumentElement().appendChild(delete);
            Element filter = req.createElementNS(Namespaces.FES, "Filter");
            delete.appendChild(filter);
            Element resourceId = req.createElementNS(Namespaces.FES, "ResourceId");
            resourceId.setAttribute("rid", entry.getKey());
            filter.appendChild(resourceId);
        }
        if (TestSuiteLogger.isLoggable(Level.FINE)) {
            TestSuiteLogger.log(Level.FINE, XMLUtils.writeNodeToString(req));
        }
        return executeTransaction(req, binding);
    }

    /**
     * Submits a request to delete a feature. A wfs:Delete element is appended
     * to the given transaction request entity.
     * 
     * @param reqEntity
     *            A WFS transaction request entity (wfs:Transaction).
     * @param id
     *            A feature identifier.
     * @param typeName
     *            The type name of the feature.
     * @return A representation of the HTTP response.
     */
    public Response deleteFeature(Document reqEntity, String id, QName typeName) {
        Element delete = reqEntity.createElementNS(Namespaces.WFS, "Delete");
        delete.setPrefix("wfs");
        delete.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":tns", typeName.getNamespaceURI());
        delete.setAttribute("typeName", "tns:" + typeName.getLocalPart());
        reqEntity.getDocumentElement().appendChild(delete);
        Element filter = reqEntity.createElementNS(Namespaces.FES, "Filter");
        delete.appendChild(filter);
        Element resourceId = reqEntity.createElementNS(Namespaces.FES, "ResourceId");
        resourceId.setAttribute("rid", id);
        filter.appendChild(resourceId);
        return submitRequest(reqEntity, ProtocolBinding.POST);
    }

    /**
     * Submits a request to retrieve one or more feature versions as specified
     * by the given resource identifier.
     * 
     * @param rid
     *            A resource identifier that selects members of the version
     *            chain to which this identified version belongs.
     * @param typeName
     *            The name of a feature type.
     * @return The (JAX-RS) client response message.
     */
    public Response GetFeatureVersion(ResourceId rid, QName typeName) {
        Document req = WFSMessage.createRequestEntity("GetFeature-Minimal", this.wfsVersion);
        Element qry = WFSMessage.appendSimpleQuery(req, typeName);
        Element filter = req.createElementNS(Namespaces.FES, "Filter");
        filter.appendChild(req.importNode(rid.toElement(), true));
        qry.appendChild(filter);
        return submitRequest(req, ProtocolBinding.ANY);
    }

    /**
     * Submits a request to insert a collection of GML feature instances.
     * 
     * @param features
     *            A {@literal List<Element>} containing one or more feature
     *            representations.
     * @param binding
     *            The ProtocolBinding to use.
     * @return A Document representing the XML response entity, or {@code null}
     *         if the response doesn't contain one.
     */
    public Document insert(List<Element> features, ProtocolBinding binding) {
        if (features.isEmpty()) {
            throw new IllegalArgumentException("No features instances to insert.");
        }
        Document req = WFSMessage.createRequestEntity(WFS2.TRANSACTION, this.wfsVersion);
        Element insert = req.createElementNS(Namespaces.WFS, "Insert");
        insert.setPrefix("wfs");
        req.getDocumentElement().appendChild(insert);
        for (Element feature : features) {
            insert.appendChild(req.importNode(feature, true));
        }
        if (TestSuiteLogger.isLoggable(Level.FINE)) {
            TestSuiteLogger.log(Level.FINE, XMLUtils.writeNodeToString(req));
        }
        return executeTransaction(req, binding);
    }

    /**
     * Submits a request to update a feature using the POST protocol binding.
     * 
     * @param id
     *            A feature identifier.
     * @param featureType
     *            The qualified name of the feature type.
     * @param properties
     *            A Map containing the feature properties to be updated.
     * @return A Document representing the XML response entity.
     * 
     * @see #updateFeature(Document, String, QName, Map, ProtocolBinding)
     */
    public Document updateFeature(String id, QName featureType, Map<String, Object> properties) {
        Document req = WFSMessage.createRequestEntity(WFS2.TRANSACTION, this.wfsVersion);
        return updateFeature(req, id, featureType, properties, ProtocolBinding.POST);
    }

    /**
     * Submits a request to update a feature.
     * 
     * @param req
     *            An empty wfs:Transaction request entity.
     * @param id
     *            The GML identifier of the feature to be updated (gml:id
     *            attribute).
     * @param featureType
     *            The type of the feature instance.
     * @param properties
     *            A Map containing the feature properties to be updated
     *            (replaced). Each entry consists of a value reference (an XPath
     *            expression) and a value object. The value may be a Node
     *            representing a complex property value; otherwise it is treated
     *            as a simple value by calling the object's toString() method.
     * @param binding
     *            The ProtocolBinding to use.
     * @return A Document representing the XML response entity, or {@code null}
     *         if the response doesn't contain one.
     */
    public Document updateFeature(Document req, String id, QName featureType, Map<String, Object> properties,
            ProtocolBinding binding) {
        Element update = req.createElementNS(Namespaces.WFS, "Update");
        update.setPrefix("wfs");
        update.setAttribute("handle", "Update");
        req.getDocumentElement().appendChild(update);
        WFSMessage.setTypeName(update, featureType);
        for (Map.Entry<String, Object> property : properties.entrySet()) {
            Element prop = req.createElementNS(Namespaces.WFS, "Property");
            prop.setPrefix("wfs");
            Element valueRef = req.createElementNS(Namespaces.WFS, "ValueReference");
            valueRef.setTextContent(property.getKey());
            valueRef.setPrefix("wfs");
            prop.appendChild(valueRef);
            Element value = req.createElementNS(Namespaces.WFS, "Value");
            value.setPrefix("wfs");
            if (Node.class.isInstance(property.getValue())) {
                value.appendChild((Node) property.getValue());
            } else {
                value.setTextContent(property.getValue().toString());
            }
            prop.appendChild(value);
            update.appendChild(prop);
        }
        Element filter = WFSMessage.newResourceIdFilter(id);
        update.appendChild(req.adoptNode(filter));
        if (TestSuiteLogger.isLoggable(Level.FINE)) {
            TestSuiteLogger.log(Level.FINE, XMLUtils.writeNodeToString(req));
        }
        return executeTransaction(req, binding);
    }

    /**
     * Submits an HTTP request message. For GET requests the XML request entity
     * is serialized to its corresponding KVP string format and added to the
     * query component of the Request-URI. For SOAP requests that adhere to the
     * "Request-Response" message exchange pattern, the outbound message entity
     * is a SOAP envelope containing the standard XML request in the body.
     * 
     * @param entity
     *            An XML representation of the request entity.
     * @param binding
     *            The {@link ProtocolBinding} to use.
     * @param endpoint
     *            The service endpoint.
     * @return A Response object representing the response message.
     */
    public Response submitRequest(Source entity, ProtocolBinding binding, URI endpoint) {
        
        WebTarget target = client.target(endpoint);
        target = target.queryParam(WFS2.REQUEST_PARAM, WFS2.GET_CAPABILITIES);
        Builder builder = target.request();
        LOGR.log(Level.FINE, String.format("Submitting %s request to URI %s", binding, target.getUri()));
        Response response = null;
        switch (binding) {
        case GET:
            String queryString = WFSMessage.transformEntityToKVP(entity);
            URI requestURI = UriBuilder.fromUri(target.getUri()).replaceQuery(queryString).build();
            LOGR.log(Level.FINE, String.format("Request URI: %s", requestURI));
            target = client.target(requestURI);
            builder = target.request();
            response = builder.accept(MediaType.APPLICATION_XML_TYPE).buildGet().invoke();
            break;
        case POST:
            response = builder.accept(MediaType.APPLICATION_XML_TYPE).buildPost(Entity.entity(entity, MediaType.APPLICATION_XML_TYPE)).invoke();
            break;
        case SOAP:
            Document soapEnv = WFSMessage.wrapEntityInSOAPEnvelope( entity, determineSoapVersion() );
            response = builder.accept(MediaType.valueOf(WFS2.APPLICATION_SOAP)).buildPost(Entity.entity(new DOMSource(soapEnv), MediaType.valueOf(WFS2.APPLICATION_SOAP))).invoke();
            break;
        default:
            throw new IllegalArgumentException("Unsupported message binding: " + binding);
        }
        return response;
    }

    /**
     * Submits a request using the specified message binding and the content of
     * the given XML request entity.
     * 
     * @param reqEntity
     *            A DOM Document representing the content of the request
     *            message.
     * @param binding
     *            The ProtocolBinding to use; may be {@link ProtocolBinding#ANY}
     *            if any supported binding can be used.
     * @return A Response object representing the response message.
     */
    public Response submitRequest(Document reqEntity, ProtocolBinding binding) {
        String requestName = reqEntity.getDocumentElement().getLocalName();
        Map<String, URI> endpoints = ServiceMetadataUtils.getRequestEndpoints(this.wfsMetadata, requestName);
        if (null == endpoints) {
            throw new IllegalArgumentException("No HTTP method bindings found for " + requestName);
        }
        if ((null == binding) || binding.equals(ProtocolBinding.ANY)) {
            String methodName = endpoints.keySet().iterator().next();
            binding = Enum.valueOf(ProtocolBinding.class, methodName);
        }
        // SOAP Request-Response MEP bound to HTTP POST
        String httpMethod = (binding == ProtocolBinding.SOAP) ? ProtocolBinding.POST.name() : binding.name();
        return submitRequest(new DOMSource(reqEntity), binding, endpoints.get(httpMethod));
    }

    /**
     * Retrieves a complete representation of the capabilities document from the
     * WFS implementation described by the service metadata. The
     * <code>acceptVersions</code> parameter is omitted, so the response shall
     * reflect the latest version supported by the SUT.
     * 
     * @return A Document containing the response to a GetCapabilities request,
     *         or {@code null} if one could not be obtained.
     */
    public Document getCapabilities() {
        if (null == this.wfsMetadata) {
            throw new IllegalStateException("Service description is unavailable.");
        }
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.GET_CAPABILITIES,
                ProtocolBinding.GET);
        WebTarget target = client.target(endpoint);
        target = target.queryParam(WFS2.REQUEST_PARAM, WFS2.GET_CAPABILITIES);
        target = target.queryParam(WFS2.SERVICE_PARAM, WFS2.SERVICE_TYPE_CODE);
        Builder builder = target.request();
        return builder.buildGet().invoke(Document.class);
    }

    /**
     * Returns a protocol binding suitable for transaction requests. Any binding
     * advertised in the service capabilities document is returned.
     * 
     * @return A supported ProtocolBinding instance (POST or SOAP).
     */
    public ProtocolBinding getAnyTransactionBinding() {
        Set<ProtocolBinding> trxBindings = ServiceMetadataUtils.getOperationBindings(this.wfsMetadata,
                WFS2.TRANSACTION);
        return trxBindings.iterator().next();
    }

    /**
     * Executes a WFS transaction.
     * 
     * @param request
     *            A Document node representing a wfs:Transaction request entity.
     * @param binding
     *            The ProtocolBinding to use
     * @return A Document node representing the response entity.
     */
    Document executeTransaction(Document request, ProtocolBinding binding) {
        if (binding == ProtocolBinding.ANY) {
            binding = getAnyTransactionBinding();
        }
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.TRANSACTION, binding);
        if (null == endpoint.getScheme()) {
            throw new IllegalArgumentException("No Transaction endpoint found for binding " + binding);
        }
        LOGR.log(Level.FINE, String.format("Submitting request entity to URI %s \n%s", endpoint,
                XMLUtils.writeNodeToString(request)));
        Response rsp = submitRequest(new DOMSource(request), binding, endpoint);
        Document entity = null;
        if (rsp.hasEntity()) {
            entity = rsp.readEntity(Document.class);
        }
        return entity;
    }

    /**
     * Submits the given request entity and returns the response entity as a DOM
     * Document.
     * 
     * @param request
     *            An XML representation of the request entity; the actual
     *            request depends on the message binding in use.
     * @param binding
     *            The ProtocolBinding to use (GET, POST, or SOAP).
     * @return A DOM Document containing the response entity, or {@code null} if
     *         the request failed or the message body could not be parsed.
     */
    Document retrieveXMLResponseEntity(Document request, ProtocolBinding binding) {
        if (LOGR.isLoggable(Level.FINE)) {
            LOGR.fine("Request entity:\n" + XMLUtils.writeNodeToString(request));
        }
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata,
                request.getDocumentElement().getLocalName(), binding);
        Response rsp = submitRequest(new DOMSource(request), binding, endpoint);
        Document rspEntity = null;
        if (rsp.hasEntity()) {
            MediaType mediaType = rsp.getMediaType();
            if (!mediaType.getSubtype().endsWith("xml")) {
                throw new RuntimeException("Did not receive an XML entity: " + mediaType);
            }
            rspEntity = rsp.readEntity(Document.class);
            if (LOGR.isLoggable(Level.FINE)) {
                LOGR.fine("Response entity:\n" + XMLUtils.writeNodeToString(rspEntity));
            }
        }
        return rspEntity;
    }

    /**
     * Submits a request to delete a stored query.
     * 
     * @param queryId
     *            A URI value that identifies the query to be dropped.
     * @return The HTTP status code.
     */
    public int deleteStoredQuery(String queryId) {
        Document req = WFSMessage.createRequestEntity("DropStoredQuery", this.wfsVersion);
        req.getDocumentElement().setAttribute("id", queryId);
        ProtocolBinding binding = ProtocolBinding.POST;
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata,
                req.getDocumentElement().getLocalName(), binding);
        Response rsp = submitRequest(new DOMSource(req), binding, endpoint);
        return rsp.getStatus();
    }

    /**
     * Requests a list of stored queries.
     * 
     * @return A list of query identifiers.
     */
    public List<String> listStoredQueries() {
        Document req = WFSMessage.createRequestEntity(WFS2.LIST_STORED_QUERIES, this.wfsVersion);
        // use any supported HTTP method
        ProtocolBinding binding = ServiceMetadataUtils.getOperationBindings(wfsMetadata, WFS2.LIST_STORED_QUERIES)
                .iterator().next();
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.LIST_STORED_QUERIES, binding);
        Response rsp = submitRequest(new DOMSource(req), binding, endpoint);
        Document rspEntity = rsp.readEntity(Document.class);
        NodeList qryList = rspEntity.getElementsByTagNameNS(Namespaces.WFS, "StoredQuery");
        List<String> idList = new ArrayList<>();
        for (int i = 0; i < qryList.getLength(); i++) {
            idList.add(Element.class.cast(qryList.item(i)).getAttribute("id"));
        }
        return idList;
    }

    private String determineSoapVersion() {
        if ( "2.0.2".equals( this.wfsVersion ) )
            return WFS2.SOAP_VERSION_1_2;
        return WFS2.SOAP_VERSION;
    }

}
