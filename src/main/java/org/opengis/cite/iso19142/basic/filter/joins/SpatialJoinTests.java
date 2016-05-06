package org.opengis.cite.iso19142.basic.filter.joins;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSTypeDefinition;
import org.opengis.cite.iso19142.ConformanceClass;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.basic.filter.QueryFilterFixture;
import org.opengis.cite.iso19142.util.AppSchemaUtils;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
 */
public class SpatialJoinTests extends QueryFilterFixture {

	private static final Logger LOGR = Logger.getLogger(SpatialJoinTests.class
			.getPackage().getName());
	private Map<QName, List<XSElementDeclaration>> surfaceProps;
	private Map<QName, List<XSElementDeclaration>> curveProps;
	private Map<QName, List<XSElementDeclaration>> pointProps;

	/**
	 * Searches the application schema for geometry properties where the value
	 * is consistent with the given type.
	 * 
	 * @param gmlTypeName
	 *            The name of a GML geometry type (may be abstract).
	 * @return A Map containing, for each feature type name (key), a list of
	 *         matching geometry properties (value).
	 */
	Map<QName, List<XSElementDeclaration>> findGeometryProperties(
			String gmlTypeName) {
		Map<QName, List<XSElementDeclaration>> geomProps = new HashMap<QName, List<XSElementDeclaration>>();
		XSTypeDefinition gmlGeomBaseType = model.getTypeDefinition(gmlTypeName,
				Namespaces.GML);
		for (QName featureType : this.featureTypes) {
			List<XSElementDeclaration> geomPropsList = AppSchemaUtils
					.getFeaturePropertiesByType(model, featureType,
							gmlGeomBaseType);
			if (!geomPropsList.isEmpty()) {
				geomProps.put(featureType, geomPropsList);
			}
		}
		return geomProps;
	}

	@BeforeClass
	public void findGeometryPropertiesToJoin() {
		if (!ServiceMetadataUtils.getConformanceClaims(this.wfsMetadata)
				.contains(ConformanceClass.SPATIAL_JOINS)) {
			throw new SkipException(ErrorMessage.format(
					ErrorMessageKeys.NOT_IMPLEMENTED,
					ConformanceClass.SPATIAL_JOINS.getConstraintName()));
		}
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

	@Test(description = "")
	public void joinWithIntersects() {
		if (!ServiceMetadataUtils.implementsSpatialOperator(this.wfsMetadata,
				"Intersects")) {
			throw new SkipException(ErrorMessage.format(
					ErrorMessageKeys.NOT_IMPLEMENTED, "Intersects operator"));
		}
		if (this.surfaceProps.size() > 1) {
			// Two surface properties
			Iterator<Map.Entry<QName, List<XSElementDeclaration>>> itr = this.surfaceProps
					.entrySet().iterator();
			JoinQueryUtils.appendSpatialJoinQuery(this.reqEntity, "Intersects",
					itr.next(), itr.next());
		} else if (!this.surfaceProps.isEmpty() && !this.curveProps.isEmpty()) {
			// Surface propertry and curve prop
		} else if (!this.surfaceProps.isEmpty() && !this.pointProps.isEmpty()) {
			// Surface propertry and point prop
		} else if (this.curveProps.size() > 1) {
			// Two curve properties
		}
	}

	public void selfJoinWithIntersects() {
		if (!ServiceMetadataUtils.implementsSpatialOperator(this.wfsMetadata,
				"Intersects")) {
			throw new SkipException(ErrorMessage.format(
					ErrorMessageKeys.NOT_IMPLEMENTED, "Intersects operator"));
		}
		if (!this.surfaceProps.isEmpty()) {
			// TODO
		}
		if (!this.curveProps.isEmpty()) {
			// TODO
		}
	}

}
