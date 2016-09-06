package org.opengis.cite.iso19142;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import javax.xml.namespace.QName;

import org.geotoolkit.geometry.Envelopes;
import org.geotoolkit.geometry.ImmutableEnvelope;
import org.geotoolkit.referencing.CRS;
import org.opengis.cite.geomatics.GeodesyUtils;
import org.opengis.cite.geomatics.time.TemporalUtils;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.temporal.Period;
import org.opengis.util.FactoryException;

/**
 * Provides information about a feature type managed by a WFS. Much of the
 * information is gleaned from the content of the service description
 * (wfs:WFS_Capabilities).
 */
public class FeatureTypeInfo {
    private QName typeName;
    private Envelope spatialExtent;
    private boolean instantiated;
    private List<String> supportedCRSList;
    private File sampleData;
    private Period temporalExtent;

    public FeatureTypeInfo() {
        this.supportedCRSList = new ArrayList<String>();
    }

    /**
     * Returns of list of supported CRS identifiers. The first entry denotes the
     * default CRS.
     * 
     * @return A list of CRS identifiers (absolute URI values).
     */
    public List<String> getSupportedCRSIdentifiers() {
        return supportedCRSList;
    }

    /**
     * Adds the given sequence of identifiers to the list of supported
     * coordinate reference systems.
     * 
     * @param crsIdentifiers
     *            A sequence of CRS identifiers (absolute URI values that comply
     *            with OGC 09-048r3, 4.4).
     */
    public void addCRSIdentifiers(String... crsIdentifiers) {
        this.supportedCRSList.addAll(Arrays.asList(crsIdentifiers));
    }

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
     * @return A String representing a CRS reference (an absolute URI value).
     */
    public String getDefaultCRS() {
        return this.supportedCRSList.get(0);
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
    public Envelope getSpatialExtent() {
        if (null == spatialExtent) {
            this.spatialExtent = getValidAreaOfCRS(getDefaultCRS());
        }
        return spatialExtent;
    }

    /**
     * Sets the geographic extent of the feature instances. The CRS of the given
     * envelope will be changed to the default CRS if necessary.
     * 
     * @param geoExtent
     *            An envelope defining a bounding box in some CRS.
     */
    public void setSpatialExtent(Envelope geoExtent) {
        CoordinateReferenceSystem defaultCRS = null;
        try {
            // http-based identifier is not recognized by Geotk v3
            String crsId = GeodesyUtils.getAbbreviatedCRSIdentifier(getDefaultCRS());
            defaultCRS = CRS.decode(crsId);
        } catch (FactoryException fex) {
            throw new RuntimeException("Default CRS not recognized. " + fex.getMessage());
        }
        if (!geoExtent.getCoordinateReferenceSystem().equals(defaultCRS)) {
            Envelope bbox = null;
            try {
                bbox = Envelopes.transform(geoExtent, defaultCRS);
            } catch (TransformException e) {
                throw new IllegalArgumentException("Failed to transform envelope coordinates to CRS " + getDefaultCRS(),
                        e);
            }
            this.spatialExtent = new ImmutableEnvelope(bbox);
        } else {
            this.spatialExtent = geoExtent;
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
        sb.append("',\n supportedCRS: '").append(supportedCRSList);
        sb.append("',\n instantiated: ").append(instantiated);
        sb.append(",\n spatial extent: '").append(Envelopes.toWKT(getSpatialExtent()));
        if (temporalExtent != null) {
            sb.append("',\n temporal extent: '");
            sb.append(TemporalUtils.temporalGeometricPrimitiveToString(temporalExtent));
        }
        if (sampleData != null && sampleData.exists()) {
            sb.append("',\n data: '").append(sampleData.toString());
        }
        sb.append("'\n}");
        return sb.toString();
    }

    /**
     * Gets the temporal extent for the instances of this feature type.
     * 
     * @return A period representing a time interval, or null if the feature has
     *         no temporal properties.
     */
    public Period getTemporalExtent() {
        return this.temporalExtent;
    }

    /**
     * Sets the temporal extent of the feature instances.
     * 
     * @param period
     *            A period representing a time interval.
     */
    public void setTemporalExtent(Period period) {
        this.temporalExtent = period;
    }

    /**
     * Creates an envelope representing the valid area of use for the specified
     * coordinate reference system (CRS).
     * 
     * @param crsRef
     *            An absolute URI ('http' or 'urn' scheme) that identifies a CRS
     *            in accord with OGC 09-048r3.
     * 
     * @return An ImmutableEnvelope defining the domain of validity for the CRS,
     *         or {@code null} if no CRS definition can be found.
     */
    ImmutableEnvelope getValidAreaOfCRS(String crsRef) {
        ImmutableEnvelope envelope = null;
        try {
            envelope = GeodesyUtils.getDomainOfValidity(crsRef);
        } catch (FactoryException e) {
            TestSuiteLogger.log(Level.WARNING, "Cannot determine domain of validity for CRS " + crsRef, e);
        }
        return envelope;
    }

}
