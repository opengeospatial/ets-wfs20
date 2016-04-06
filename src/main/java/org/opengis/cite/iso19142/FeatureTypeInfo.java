package org.opengis.cite.iso19142;

import java.io.File;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import org.geotoolkit.geometry.Envelopes;
import org.geotoolkit.geometry.ImmutableEnvelope;
import org.geotoolkit.referencing.CRS;
import org.opengis.cite.geomatics.GeodesyUtils;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Provides information about a feature type managed by a WFS. Much of the
 * information is gleaned from the service description (wfs:WFS_Capabilities).
 */
public class FeatureTypeInfo {
	private QName typeName;
	private Envelope geoExtent;
	private boolean instantiated;
	private String defaultCRSRef;
	private CoordinateReferenceSystem defaultCRS;
	private File sampleData;

	/**
	 * Get the qualified name of the feature type.
	 * 
	 * @return A QName object.
	 */
	public QName getTypeName() {
		return typeName;
	}

	/**
	 * Sets the feature type name.
	 * 
	 * @param typeName
	 *            A QName object.
	 */
	public void setTypeName(QName typeName) {
		this.typeName = typeName;
	}

	/**
	 * Indicates whether or not there are any instances of this feature type
	 * available in the data store.
	 * 
	 * @return {@code true} if at least one feature instance exists;
	 *         {@code false} otherwise.
	 */
	public boolean isInstantiated() {
		return instantiated;
	}

	/**
	 * Sets the availability of this feature type.
	 * 
	 * @param available
	 *            A boolean value indicating if any instances of this type are
	 *            available in the data store.
	 */
	public void setInstantiated(boolean available) {
		this.instantiated = available;
	}

	/**
	 * Gets the identifier of the default CRS for this feature type.
	 * 
	 * @return A String representing a CRS reference.
	 */
	public String getDefaultCRS() {
		return defaultCRSRef;
	}

	/**
	 * Sets the identifier of the default CRS for this feature type which shall
	 * be assumed by a WFS if not otherwise explicitly identified within a
	 * request.
	 * 
	 * @param crsRef
	 *            A valid CRS reference; this should be an absolute URI (see OGC
	 *            09-048r3, 4.4).
	 * @throws FactoryException
	 *             If the CRS reference is invalid or unrecognized.
	 */
	public void setDefaultCRS(String crsRef) throws FactoryException {
		this.defaultCRS = CRS.decode(crsRef);
		this.defaultCRSRef = crsRef;
	}

	/**
	 * Gets the geographic extent for the instances of this feature type. The
	 * spatial extent is typically set as follows:
	 * <ol>
	 * <li>using the first ows:WGS84BoundingBox element appearing in the WFS
	 * capabilities document;</li>
	 * <li>from the valid area of the default CRS.</li>
	 * </ol>
	 * 
	 * @return An Envelope defining a bounding box.
	 */
	public Envelope getGeoExtent() {
		if (null == geoExtent) {
			this.geoExtent = defaultCRSEnvelope(this.defaultCRSRef);
		}
		return geoExtent;
	}

	/**
	 * Sets the geographic extent of the feature instances.
	 * 
	 * @param geoExtent
	 *            An envelope defining a bounding box in some CRS.
	 */
	public void setGeoExtent(Envelope geoExtent) {
		if (!geoExtent.getCoordinateReferenceSystem().equals(defaultCRS)) {
			Envelope bbox = null;
			try {
				bbox = Envelopes.transform(geoExtent, defaultCRS);
			} catch (TransformException e) {
				throw new IllegalArgumentException(
						"Failed to transform envelope coordinates to CRS "
								+ defaultCRSRef, e);
			}
			this.geoExtent = new ImmutableEnvelope(bbox);
		} else {
			this.geoExtent = geoExtent;
		}
	}

	/**
	 * Returns a File containing sample data. This is an XML entity where the
	 * document element is wfs:FeatureCollection.
	 * 
	 * @return A File for reading the GML data, or {@code null} if no data are
	 *         available.
	 */
	public File getSampleData() {
		return sampleData;
	}

	/**
	 * Sets the location of a sample data file containing instances of this
	 * feature type.
	 * 
	 * @param sampleData
	 *            A File object.
	 */
	public void setSampleData(File sampleData) {
		this.sampleData = sampleData;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("FeatureTypeInfo {");
		sb.append("\n typeName: '").append(typeName);
		sb.append("',\n defaultCRS: '").append(defaultCRSRef);
		sb.append("',\n instantiated: ").append(instantiated);
		sb.append(",\n envelope: '").append(Envelopes.toWKT(getGeoExtent()));
		if (sampleData != null && sampleData.exists()) {
			sb.append("',\n data: '").append(sampleData.toString());
		}
		sb.append("'\n}");
		return sb.toString();
	}

	/**
	 * Creates an envelope representing the valid area of use for the default
	 * coordinate reference system (CRS).
	 * 
	 * @param crsRef
	 *            An absolute URI ('http' or 'urn' scheme) that identifies a CRS
	 *            in accord with OGC 09-048r3.
	 * 
	 * @return An ImmutableEnvelope defining the domain of validity for the
	 *         default CRS, or {@code null} no CRS definition can be found.
	 */
	ImmutableEnvelope defaultCRSEnvelope(String crsRef) {
		ImmutableEnvelope envelope = null;
		try {
			envelope = GeodesyUtils.getDomainOfValidity(crsRef);
		} catch (FactoryException e) {
			TestSuiteLogger.log(Level.WARNING,
					"Cannot determine domain of validity for CRS "
							+ defaultCRSRef, e);
		}
		return envelope;
	}

}
