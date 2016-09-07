package org.opengis.cite.iso19142.joins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathExpressionException;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSTypeDefinition;
import org.opengis.cite.geomatics.SpatialOperator;
import org.opengis.cite.iso19142.ConformanceClass;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.basic.filter.QueryFilterFixture;
import org.opengis.cite.iso19142.util.AppSchemaUtils;
import org.opengis.cite.iso19142.util.FeatureProperty;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.sun.jersey.api.client.ClientResponse;

/**
 * A spatial join includes a spatial predicate. One or more of the following
 * spatial predicates must be supported:
 * <ul>
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
 * <p>
 * A sample GetFeature request entity is shown below, where the "Intersects"
 * predicate refers to the geometry properties of two different feature types.
 * </p>
 * 
 * <pre>
 * &lt;wfs:GetFeature version="2.0.0" service="WFS" 
 *   xmlns:tns="http://example.org/ns1" 
 *   xmlns:wfs="http://www.opengis.net/wfs/2.0"
 *   xmlns:fes="http://www.opengis.net/fes/2.0"&gt;
 *   &lt;wfs:Query typeNames="tns:Parks tns:Lakes"&gt;
 *     &lt;fes:Filter&gt;
 *       &lt;fes:Intersects&gt;
 *         &lt;fes:ValueReference&gt;tns:Parks/tns:geometry&lt;/fes:ValueReference&gt;
 *         &lt;fes:ValueReference&gt;tns:Lakes/tns:geometry&lt;/fes:ValueReference&gt;
 *       &lt;/fes:Intersects&gt;
 *     &lt;/fes:Filter&gt;
 *   &lt;/wfs:Query&gt;
 * &lt;/wfs:GetFeature&gt;
 * </pre>
 *
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li>OGC 09-025r2, 7.9.2.5.3: Join processing</li>
 * <li>OGC 09-025r2, A.1.12: Spatial joins</li>
 * <li>OGC 09-026r2, A.8: Test cases for spatial filter</li>
 * </ul>
 */
public class SpatialJoinTests extends QueryFilterFixture {

    public final static String IMPL_SPATIAL_JOINS = "ImplementsSpatialJoins";
    private static final Logger LOGR = Logger.getLogger(SpatialJoinTests.class.getPackage().getName());
    private Map<QName, List<XSElementDeclaration>> surfaceProps;
    private Map<QName, List<XSElementDeclaration>> curveProps;
    private Map<QName, List<XSElementDeclaration>> pointProps;
    private Map<SpatialOperator, Set<QName>> spatialCapabilities;

    /**
     * Searches the application schema for geometry properties where the value
     * is an instance of the given type.
     * 
     * @param gmlTypeName
     *            The name of a GML geometry type (may be abstract).
     * @return A Map containing, for each feature type name (key), a list of
     *         matching geometry properties (value).
     */
    Map<QName, List<XSElementDeclaration>> findGeometryProperties(String gmlTypeName) {
        Map<QName, List<XSElementDeclaration>> geomProps = new HashMap<QName, List<XSElementDeclaration>>();
        XSTypeDefinition gmlGeomBaseType = model.getTypeDefinition(gmlTypeName, Namespaces.GML);
        for (QName featureType : this.featureTypes) {
            List<XSElementDeclaration> geomPropsList = AppSchemaUtils.getFeaturePropertiesByType(model, featureType,
                    gmlGeomBaseType);
            if (!geomPropsList.isEmpty()) {
                geomProps.put(featureType, geomPropsList);
            }
        }
        return geomProps;
    }

    /**
     * Checks the value of the service constraint {@value #IMPL_SPATIAL_JOINS}
     * in the capabilities document. All tests are skipped if this is not
     * "TRUE".
     * 
     * @param testContext
     *            Information about the test run environment.
     */
    @BeforeTest
    public void implementsSpatialJoins(ITestContext testContext) {
        this.wfsMetadata = (Document) testContext.getSuite().getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        String xpath = String.format("//ows:Constraint[@name='%s' and (ows:DefaultValue = 'TRUE')]",
                IMPL_SPATIAL_JOINS);
        NodeList result;
        try {
            result = XMLUtils.evaluateXPath(this.wfsMetadata, xpath, null);
        } catch (XPathExpressionException e) {
            throw new AssertionError(e.getMessage());
        }
        if (result.getLength() == 0) {
            throw new SkipException(ErrorMessage.format(ErrorMessageKeys.NOT_IMPLEMENTED,
                    ConformanceClass.SPATIAL_JOINS.getConstraintName()));
        }
    }

    /**
     * Initializes the test class fixture. Finds surface, curve, and point
     * properties defined in the application schema. Properties that use
     * primitive types are preferred, but if none are defined then aggregate
     * geometry types (Multi*) will be used instead.
     */
    @BeforeClass
    public void initFixture() {
        this.spatialCapabilities = ServiceMetadataUtils.getSpatialCapabilities(this.wfsMetadata);
        this.surfaceProps = findGeometryProperties("AbstractSurfaceType");
        if (this.surfaceProps.isEmpty()) {
            this.surfaceProps = findGeometryProperties("MultiSurfaceType");
        }
        LOGR.info(this.surfaceProps.toString());
        this.curveProps = findGeometryProperties("AbstractCurveType");
        if (this.curveProps.isEmpty()) {
            this.curveProps = findGeometryProperties("MultiCurveType");
        }
        LOGR.info(this.curveProps.toString());
        this.pointProps = findGeometryProperties("PointType");
        if (this.pointProps.isEmpty()) {
            this.pointProps = findGeometryProperties("MultiPointType");
        }
        LOGR.info(this.pointProps.toString());
    }

    /**
     * [{@code Test}] Submits a basic join query that includes the
     * <code>Intersects</code> operator. A projection clause (wfs:PropertyName)
     * is omitted, so the response entity is expected to contain instances of
     * both feature types.
     */
    @Test(description = "See OGC 09-025r2: 7.9.2.5.3, A.1.12")
    public void joinWithIntersects() {
        if (!this.spatialCapabilities.keySet().contains(SpatialOperator.INTERSECTS)) {
            throw new SkipException(ErrorMessage.format(ErrorMessageKeys.NOT_IMPLEMENTED, SpatialOperator.INTERSECTS));
        }
        List<FeatureProperty> joinProperties = new ArrayList<FeatureProperty>();
        if (this.surfaceProps.size() > 1) {
            Iterator<Map.Entry<QName, List<XSElementDeclaration>>> itr = this.surfaceProps.entrySet().iterator();
            Entry<QName, List<XSElementDeclaration>> entry = itr.next();
            joinProperties.add(new FeatureProperty(entry.getKey(), entry.getValue().get(0)));
            entry = itr.next();
            joinProperties.add(new FeatureProperty(entry.getKey(), entry.getValue().get(0)));
        } else if (!this.surfaceProps.isEmpty() && !this.curveProps.isEmpty()) {
            // Surface propertry and curve prop
        } else if (!this.surfaceProps.isEmpty() && !this.pointProps.isEmpty()) {
            // Surface propertry and point prop
        } else if (this.curveProps.size() > 1) {
            // Two curve properties
        }
        JoinQueryUtils.appendSpatialJoinQuery(this.reqEntity, "Intersects", joinProperties);
        ClientResponse rsp = wfsClient.submitRequest(this.reqEntity, ProtocolBinding.ANY);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        // TODO check entity body: F1 intersects F2
    }

    // self-join T1 BBOX T1 (gml:boundedBy)??
    // self-join T1 INTERSECTS T1
    // self-join T1 BEFORE T1
    // join T1 BBOX T2 (gml;boundedBy) ??
    // join T1 AFTER T2
    // join T1 BEFORE T2

    public void selfJoinWithIntersects() {
        if (!ServiceMetadataUtils.implementsSpatialOperator(this.wfsMetadata, "Intersects")) {
            throw new SkipException(ErrorMessage.format(ErrorMessageKeys.NOT_IMPLEMENTED, "Intersects operator"));
        }
        if (!this.surfaceProps.isEmpty()) {
            // TODO
        }
        if (!this.curveProps.isEmpty()) {
            // TODO
        }
    }

}
