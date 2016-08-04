package org.opengis.cite.iso19142.basic.filter.spatial;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSTypeDefinition;
import org.opengis.cite.geomatics.Extents;
import org.opengis.cite.geomatics.SpatialRelationship;
import org.opengis.cite.geomatics.TopologicalRelationships;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.basic.filter.QueryFilterFixture;
import org.opengis.cite.iso19142.util.AppSchemaUtils;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the response to a GetFeature request that includes a BBOX predicate.
 * All conforming "Basic WFS" implementations must support this spatial
 * operator.
 * 
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li>OGC 09-025r2, Table 1: Conformance classes</li>
 * <li>OGC 09-026r2, cl. A.7: Test cases for minimum spatial filter</li>
 * </ul>
 */
public class BBOXTests extends QueryFilterFixture {

    private static final String XSLT_ENV2POLYGON = "/org/opengis/cite/iso19142/util/bbox2polygon.xsl";
    private XSTypeDefinition gmlGeomBaseType;

    /**
     * Creates an XSTypeDefinition object representing the
     * gml:AbstractGeometryType definition.
     */
    @BeforeClass
    public void createGeometryBaseType() {
        this.gmlGeomBaseType = model.getTypeDefinition("AbstractGeometryType", Namespaces.GML);
    }

    /**
     * [{@code Test}] Submits a GetFeature request with a non-specific BBOX
     * predicate. If no value reference is specified the predicate is applied to
     * all spatial properties. The response entity (wfs:FeatureCollection) must
     * be schema-valid and contain only instances of the requested type that
     * satisfy the spatial predicate.
     * 
     * @param binding
     *            The ProtocolBinding to use for this request.
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     * 
     * @see "ISO 19143:2010, 7.8.3.2: BBOX operator"
     */
    @Test(description = "See ISO 19143: 7.8.3.2", dataProvider = "protocol-featureType")
    public void nonSpecificBBOX(ProtocolBinding binding, QName featureType) {
        List<XSElementDeclaration> geomProps = AppSchemaUtils.getFeaturePropertiesByType(model, featureType,
                gmlGeomBaseType);
        if (geomProps.isEmpty()) {
            throw new SkipException("Feature type has no geometry properties: " + featureType);
        }
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        Document gmlEnv = Extents.envelopeAsGML(featureInfo.get(featureType).getSpatialExtent());
        addBBOXPredicate(this.reqEntity, gmlEnv.getDocumentElement(), null);
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.GET_FEATURE, binding);
        ClientResponse rsp = wfsClient.submitRequest(new DOMSource(reqEntity), binding, endpoint);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put(Namespaces.WFS, "wfs");
        NodeList members;
        String xpath = "//wfs:member/*";
        try {
            members = XMLUtils.evaluateXPath(this.rspEntity, xpath, nsBindings);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        Assert.assertTrue(members.getLength() > 0, ErrorMessage.format(ErrorMessageKeys.XPATH_RESULT,
                this.rspEntity.getDocumentElement().getNodeName(), xpath));
        for (int i = 0; i < members.getLength(); i++) {
            ETSAssert.assertQualifiedName(members.item(i), featureType);
        }
    }

    /**
     * [{@code Test}] Submits a GetFeature request with a BBOX predicate
     * referring to a valid geometry property. The response shall contain only
     * features possessing a geometry value that spatially interacts (i.e. is
     * not disjoint) with the given envelope.
     * 
     * <p style="margin-bottom: 0.5em">
     * <strong>Sources</strong>
     * </p>
     * <ul>
     * <li>ISO 19143:2010, cl. A.7: Test cases for minimum spatial filter</li>
     * <li>ISO 19143:2010, 7.8.3.2: BBOX operator</li>
     * </ul>
     * 
     * @param binding
     *            The ProtocolBinding to use for this request.
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     * 
     */
    @Test(description = "See ISO 19143: 7.8.3.2, A.7", dataProvider = "protocol-featureType")
    public void bboxWithDefaultExtent(ProtocolBinding binding, QName featureType) {
        List<XSElementDeclaration> geomProps = AppSchemaUtils.getFeaturePropertiesByType(model, featureType,
                gmlGeomBaseType);
        if (geomProps.isEmpty()) {
            throw new SkipException("Feature type has no geometry properties: " + featureType);
        }
        XSElementDeclaration geomProp = geomProps.get(0);
        Element valueRef = WFSMessage.createValueReference(geomProp);
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        Document gmlEnv = Extents.envelopeAsGML(featureInfo.get(featureType).getSpatialExtent());
        addBBOXPredicate(this.reqEntity, gmlEnv.getDocumentElement(), valueRef);
        ClientResponse rsp = wfsClient.submitRequest(reqEntity, binding);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put(featureType.getNamespaceURI(), "ns1");
        String xpath;
        if (!geomProp.getNamespace().equals(featureType.getNamespaceURI())) {
            nsBindings.put(geomProp.getNamespace(), "ns2");
            xpath = String.format("//ns1:%s/ns2:%s/*[1]", featureType.getLocalPart(), geomProp.getName());
        } else {
            xpath = String.format("//ns1:%s/ns1:%s/*[1]", featureType.getLocalPart(), geomProp.getName());
        }
        NodeList geometryNodes;
        try {
            geometryNodes = XMLUtils.evaluateXPath(this.rspEntity, xpath, nsBindings);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        Assert.assertTrue(geometryNodes.getLength() > 0, ErrorMessage.format(ErrorMessageKeys.XPATH_RESULT,
                this.rspEntity.getDocumentElement().getNodeName(), xpath));
        Document gmlPolygon = XMLUtils.transform(new StreamSource(getClass().getResourceAsStream(XSLT_ENV2POLYGON)),
                gmlEnv);
        for (int i = 0; i < geometryNodes.getLength(); i++) {
            Element geometry = (Element) geometryNodes.item(i);
            if (geometry.getElementsByTagNameNS(Namespaces.GML, "PolygonPatch").getLength() > 0) {
                // gml:Surface comprised of one or more PolygonPatch elements
                geometry = surfaceToPolygon(geometry);
            }
            boolean intersects = TopologicalRelationships.isSpatiallyRelated(SpatialRelationship.INTERSECTS,
                    gmlPolygon.getDocumentElement(), geometry);
            Assert.assertTrue(intersects, ErrorMessage.format(ErrorMessageKeys.PREDICATE_NOT_SATISFIED, "BBOX",
                    XMLUtils.writeNodeToString(gmlPolygon), XMLUtils.writeNodeToString(geometry)));
        }
    }

    /**
     * [{@code Test}] Submits a GetFeature request where the BBOX predicate
     * refers to a feature property (gml:description) that is not
     * geometry-valued. An exception is expected in response with status code
     * 400 and exception code {@code InvalidParameterValue}.
     * 
     * @param featureType
     *            A QName representing the qualified name of a feature type for
     *            which instances exist.
     * 
     * @see "ISO 19142:2010, 11.4: GetFeature - Exceptions"
     * @see "ISO 19143:2010, 8.3: Exceptions"
     */
    @Test(description = "See ISO 19142: 11.4; ISO 19143: 8.3", dataProvider = "instantiated-feature-types")
    public void invalidGeometryOperand(QName featureType) {
        XSElementDeclaration gmlDesc = this.model.getElementDeclaration("description", Namespaces.GML);
        Element valueRef = WFSMessage.createValueReference(gmlDesc);
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        Document gmlEnv = Extents.envelopeAsGML(featureInfo.get(featureType).getSpatialExtent());
        addBBOXPredicate(this.reqEntity, gmlEnv.getDocumentElement(), valueRef);
        ClientResponse rsp = wfsClient.submitRequest(reqEntity, ProtocolBinding.ANY);
        this.rspEntity = rsp.getEntity(Document.class);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        String xpath = "//ows:Exception[@exceptionCode='InvalidParameterValue']";
        ETSAssert.assertXPath(xpath, this.rspEntity, null);
    }

    // bboxCoversSampleData
    // bboxWithOtherCRS
    // bboxWithUnsupportedCRS

    /**
     * Replaces gml:Surface elements having a single gml:PolygonPatch with a
     * gml:Polygon element. Such a simplification facilitates a binding to JTS
     * geometry objects.
     * 
     * @param geometry
     *            A GML geometry collection.
     * @return A new DOM Element containing a simplified representation of the
     *         original geometry collection.
     * 
     * @see "ISO 19125-1: Geographic information -- Simple feature access --
     *      Part 1: Common architecture"
     */
    Element surfaceToPolygon(Element geometry) {
        Document result = XMLUtils.transform(new StreamSource(getClass().getResourceAsStream("surface2polygon.xsl")),
                geometry);
        return result.getDocumentElement();
    }

    /**
     * Adds a BBOX spatial predicate to a GetFeature request entity. If the
     * envelope has no spatial reference (srsName) it is assumed to be the
     * default CRS specified in the capabilities document.
     * 
     * @param request
     *            The request entity (/wfs:GetFeature).
     * @param envelope
     *            A DOM Element representing a gml:Envelope.
     * @param valueRef
     *            An Element (fes:ValueReference) that specifies the spatial
     *            property to check. If it is {@code null}, the predicate
     *            applies to all spatial properties.
     */
    void addBBOXPredicate(Document request, Element envelope, Element valueRef) {
        if (!request.getDocumentElement().getLocalName().equals(WFS2.GET_FEATURE)) {
            throw new IllegalArgumentException(
                    "Not a GetFeature request: " + request.getDocumentElement().getNodeName());
        }
        Element queryElem = (Element) request.getElementsByTagNameNS(Namespaces.WFS, WFS2.QUERY_ELEM).item(0);
        Element filter = request.createElementNS(Namespaces.FES, "fes:Filter");
        queryElem.appendChild(filter);
        Element bbox = request.createElementNS(Namespaces.FES, "fes:BBOX");
        filter.appendChild(bbox);
        if (null != valueRef) {
            bbox.appendChild(request.importNode(valueRef, true));
        }
        // import envelope node to avoid WRONG_DOCUMENT_ERR
        bbox.appendChild(request.importNode(envelope, true));
    }
}
