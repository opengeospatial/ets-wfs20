package org.opengis.cite.iso19142.basic.filter.spatial;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSTypeDefinition;
import org.opengis.cite.geomatics.Extents;
import org.opengis.cite.geomatics.SpatialOperator;
import org.opengis.cite.geomatics.TopologicalRelationships;
import org.opengis.cite.iso19136.GML32;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.basic.filter.QueryFilterFixture;
import org.opengis.cite.iso19142.util.AppSchemaUtils;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.opengis.geometry.Envelope;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the response to a GetFeature request that includes the spatial
 * predicate <em>Intersects</em>. This predicate is the logical complement of
 * the <em>Disjoint</em> predicate; that is:
 * 
 * <pre>
 * a.Intersects(b) &#8660; ! a.Disjoint(b)
 * </pre>
 * 
 * @see "OGC 09-026r2, A.8: Test cases for spatial filter"
 * @see "ISO 19125-1, 6.1.15.3"
 */
public class IntersectsTests extends QueryFilterFixture {

    public final static String IMPL_SPATIAL_FILTER = "ImplementsSpatialFilter";
    private static final String INTERSECTS_OP = "Intersects";
    private Map<QName, List<XSElementDeclaration>> allGeomProperties;
    private Set<QName> geomOperands;
    private static final String XSLT_ENV2POLYGON = "/org/opengis/cite/iso19142/util/bbox2polygon.xsl";

    /**
     * Checks the value of the filter constraint {@value #IMPL_SPATIAL_FILTER}
     * in the capabilities document. All tests are skipped if this is not
     * "TRUE".
     * 
     * @param testContext
     *            Information about the test run environment.
     */
    @BeforeTest
    public void implementsSpatialFilter(ITestContext testContext) {
        this.wfsMetadata = (Document) testContext.getSuite().getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        String xpath = String.format("//fes:Constraint[@name='%s' and (ows:DefaultValue = 'TRUE')]",
                IMPL_SPATIAL_FILTER);
        NodeList result;
        try {
            result = XMLUtils.evaluateXPath(this.wfsMetadata, xpath, null);
        } catch (XPathExpressionException e) {
            throw new AssertionError(e.getMessage());
        }
        if (result.getLength() == 0) {
            throw new SkipException(ErrorMessage.format(ErrorMessageKeys.NOT_IMPLEMENTED, IMPL_SPATIAL_FILTER));
        }
    }

    /**
     * Finds the geometry properties for all feature types recognized by the
     * SUT.
     */
    @BeforeClass
    public void findAllGeometryProperties() {
        XSTypeDefinition geomBaseType = model.getTypeDefinition("AbstractGeometryType", Namespaces.GML);
        this.allGeomProperties = new HashMap<>();
        for (QName featureType : this.featureTypes) {
            List<XSElementDeclaration> geomProps = AppSchemaUtils.getFeaturePropertiesByType(model, featureType,
                    geomBaseType);
            if (!geomProps.isEmpty()) {
                this.allGeomProperties.put(featureType, geomProps);
            }
        }
    }

    /**
     * Checks if the spatial operator "Intersects" is implemented and which
     * geometry operands are supported. If it's not implemented, all tests for
     * this operator are skipped.
     */
    @BeforeClass
    public void implementsIntersectsOp() {
        Map<SpatialOperator, Set<QName>> capabilities = ServiceMetadataUtils.getSpatialCapabilities(this.wfsMetadata);
        if (!capabilities.containsKey(SpatialOperator.INTERSECTS)) {
            throw new SkipException(ErrorMessage.format(ErrorMessageKeys.NOT_IMPLEMENTED, SpatialOperator.INTERSECTS));
        }
        this.geomOperands = capabilities.get(SpatialOperator.INTERSECTS);
    }

    /**
     * [{@code Test}] Submits a GetFeature request containing an Intersects
     * predicate with a gml:Polygon operand. The response entity must be
     * schema-valid and contain only matching instances.
     * 
     * @param binding
     *            The ProtocolBinding to use for this request.
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     */
    @Test(description = "See OGC 09-026r2, A.8", dataProvider = "protocol-featureType")
    public void intersectsPolygon(ProtocolBinding binding, QName featureType) {
        if (!this.allGeomProperties.keySet().contains(featureType)) {
            throw new SkipException("Feature type has no geometry properties: " + featureType);
        }
        QName gmlPolygon = new QName(Namespaces.GML, GML32.POLYGON);
        if (!this.geomOperands.contains(gmlPolygon)) {
            throw new SkipException("Unsupported geometry operand: " + gmlPolygon);
        }
        Envelope extent = this.dataSampler.getSpatialExtent(this.model, featureType);
        Document gmlEnv = Extents.envelopeAsGML(extent);
        Element gmlPolygonElem = XMLUtils
                .transform(new StreamSource(getClass().getResourceAsStream(XSLT_ENV2POLYGON)), gmlEnv, null)
                .getDocumentElement();
        List<XSElementDeclaration> geomProps = this.allGeomProperties.get(featureType);
        XSElementDeclaration geomProperty = geomProps.get(0);
        this.reqEntity = buildGetFeatureRequest(featureType, INTERSECTS_OP, geomProperty, gmlPolygonElem);
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.GET_FEATURE, binding);
        ClientResponse rsp = wfsClient.submitRequest(new DOMSource(reqEntity), binding, endpoint);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        List<Node> geomNodes = WFSMessage.findPropertyValues(this.rspEntity, geomProperty, this.model);
        if (geomNodes.isEmpty()) {
            TestSuiteLogger.log(Level.INFO, String.format(
                    "intersectsPolygon: No values found for geometry property %s (in %s)", geomProperty, featureType));
        }
        for (Node geom : geomNodes) {
            boolean intersects = TopologicalRelationships.isSpatiallyRelated(SpatialOperator.INTERSECTS, gmlPolygonElem,
                    geom);
            Assert.assertTrue(intersects, ErrorMessage.format(ErrorMessageKeys.PREDICATE_NOT_SATISFIED, INTERSECTS_OP,
                    XMLUtils.writeNodeToString(gmlPolygonElem), XMLUtils.writeNodeToString(geom)));
        }
    }

    /**
     * [{@code Test}] Submits a GetFeature request containing an
     * <strong>Intersects</strong> predicate with a gml:LineString or gml:Curve
     * (LineStringSegment) operand. The response entity must be schema-valid and
     * contain only matching instances.
     * 
     * @param binding
     *            The ProtocolBinding to use for this request.
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     */
    @Test(description = "See OGC 09-026r2, A.8", dataProvider = "protocol-featureType")
    public void intersectsCurve(ProtocolBinding binding, QName featureType) {
        if (!this.allGeomProperties.keySet().contains(featureType)) {
            throw new SkipException("Feature type has no geometry properties: " + featureType);
        }
        QName gmlCurve = new QName(Namespaces.GML, GML32.LINE_STRING);
        if (!this.geomOperands.contains(gmlCurve)) {
            gmlCurve = new QName(Namespaces.GML, GML32.CURVE);
            if (!this.geomOperands.contains(gmlCurve)) {
                throw new SkipException("Unsupported geometry operands: gml:LineString, gml:Curve");
            }
        }
        Envelope extent = this.dataSampler.getSpatialExtent(this.model, featureType);
        Document gmlEnvelope = Extents.envelopeAsGML(extent);
        Element gmlCurveElem = XMLUtils
                .transform(new StreamSource(getClass().getResourceAsStream("envelopeTocurve.xsl")), gmlEnvelope,
                        Collections.singletonMap("curveType", gmlCurve.getLocalPart()))
                .getDocumentElement();
        List<XSElementDeclaration> geomProps = this.allGeomProperties.get(featureType);
        Iterator<XSElementDeclaration> itr = geomProps.iterator();
        XSElementDeclaration geomProperty = null;
        while (itr.hasNext()) {
            geomProperty = itr.next();
            XSElementDeclaration value = AppSchemaUtils.getComplexPropertyValue(geomProperty);
            if (!value.getName().equals(GML32.POINT))
                break; // ignore point property--unlikely to intersect line
        }
        this.reqEntity = buildGetFeatureRequest(featureType, INTERSECTS_OP, geomProperty, gmlCurveElem);
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.GET_FEATURE, binding);
        ClientResponse rsp = wfsClient.submitRequest(new DOMSource(this.reqEntity), binding, endpoint);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        List<Node> geomNodes = WFSMessage.findPropertyValues(this.rspEntity, geomProperty, this.model);
        if (geomNodes.isEmpty()) {
            TestSuiteLogger.log(Level.INFO, String.format(
                    "intersectsCurve: No values found for geometry property %s (in %s)", geomProperty, featureType));
        }
        for (Node geom : geomNodes) {
            boolean intersects = TopologicalRelationships.isSpatiallyRelated(SpatialOperator.INTERSECTS, gmlCurveElem,
                    geom);
            Assert.assertTrue(intersects, ErrorMessage.format(ErrorMessageKeys.PREDICATE_NOT_SATISFIED, INTERSECTS_OP,
                    XMLUtils.writeNodeToString(gmlCurveElem), XMLUtils.writeNodeToString(geom)));
        }
    }

    // intersectsCurve (corner points of bbox) - other CRS
    // intersectsCircle (corner points of bbox) - default CRS

    /**
     * Builds a simple GetFeature request entity that contains a basic filter
     * expression.
     * 
     * @param featureType
     *            The name of the feature type.
     * @param operator
     *            The name of the filter predicate (operator).
     * @param propertyDecl
     *            The declaration of the feature property to be queried.
     * @param literal
     *            An element representing the literal value to match.
     * @return A Document containing a request entity with wfs:GetFeature as the
     *         document element.
     */
    Document buildGetFeatureRequest(QName featureType, String operator, XSElementDeclaration propertyDecl,
            Element literal) {
        Document entity = WFSMessage.createRequestEntity(GET_FEATURE_MINIMAL, this.wfsVersion);
        WFSMessage.appendSimpleQuery(entity, featureType);
        Element valueRef = WFSMessage.createValueReference(propertyDecl);
        addSpatialPredicate(entity, operator, literal, valueRef);
        return entity;
    }

    /**
     * Adds a spatial predicate to a GetFeature request entity. If the given
     * geometry element has no spatial reference (srsName) it is assumed to use
     * the default CRS specified in the capabilities document.
     * 
     * @param request
     *            The request entity (wfs:GetFeature).
     * @param spatialOp
     *            The name of a spatial operator.
     * @param gmlGeom
     *            A DOM Element representing a GML geometry.
     * @param valueRef
     *            An Element (fes:ValueReference) that specifies the spatial
     *            property to check. If it is {@code null}, the predicate
     *            applies to all spatial properties.
     */
    void addSpatialPredicate(Document request, String spatialOp, Element gmlGeom, Element valueRef) {
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
        Element predicate = request.createElementNS(Namespaces.FES, "fes:" + spatialOp);
        filter.appendChild(predicate);
        if (null != valueRef) {
            predicate.appendChild(request.importNode(valueRef, true));
        }
        // import geometry element to avoid WRONG_DOCUMENT_ERR
        predicate.appendChild(request.importNode(gmlGeom, true));
    }
}
