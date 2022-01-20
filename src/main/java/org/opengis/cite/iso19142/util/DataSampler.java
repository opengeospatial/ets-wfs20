package org.opengis.cite.iso19142.util;

import static org.opengis.cite.iso19142.Namespaces.GML;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmValue;

import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSTypeDefinition;
import org.opengis.cite.geomatics.Extents;
import org.opengis.cite.geomatics.gml.GmlUtils;
import org.opengis.cite.geomatics.time.TemporalComparator;
import org.opengis.cite.geomatics.time.TemporalUtils;
import org.opengis.cite.iso19142.FeatureTypeInfo;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.basic.filter.temporal.TemporalQuery;
import org.opengis.geometry.Envelope;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Obtains samples of the feature data available from the WFS under test.
 * Instances of all feature types advertised in the service description are
 * requested, but data need not exist for every type.
 */
public class DataSampler {

    private static final Logger LOGR = Logger.getLogger(DataSampler.class.getPackage().getName());
    public static final QName BOUNDED_BY = new QName( GML, "boundedBy" );
    private int maxFeatures = 25;
    private Document serviceDescription;
    private Map<QName, FeatureTypeInfo> featureInfo;
    private Map<QName, Envelope> spatialExtents;
    private Map<FeatureProperty, Period> temporalPropertyExtents;
    private final Map<QName, List<QName>> nillableProperties = new HashMap<>();

    /**
     * Constructs a new DataSampler for a particular WFS implementation.
     * 
     * @param wfsCapabilities
     *            A DOM Document representing the service metadata
     *            (/wfs:WFS_Capabilities).
     */
    public DataSampler(Document wfsCapabilities) {
        if (null == wfsCapabilities
                || !wfsCapabilities.getDocumentElement().getLocalName().equals(WFS2.WFS_CAPABILITIES)) {
            throw new IllegalArgumentException("Did not supply a WFS capabilities document");
        }
        this.serviceDescription = wfsCapabilities;
        this.featureInfo = ServiceMetadataUtils.extractFeatureTypeInfo(wfsCapabilities);
        if (this.featureInfo.isEmpty()) {
            throw new RuntimeException("No feature type info available.");
        }
        this.spatialExtents = new HashMap<>();
        this.temporalPropertyExtents = new HashMap<>();
        LOGR.config("Created DataSampler - GetCapabilities (GET) endpoint is " + ServiceMetadataUtils
                .getOperationEndpoint(wfsCapabilities, WFS2.GET_CAPABILITIES, ProtocolBinding.GET));
    }

    /**
     * Sets the maximum number of features to include in the response entity.
     * 
     * @param maxFeatures
     *            An integer value &gt; 0 (the default value is 25).
     */
    public void setMaxFeatures(int maxFeatures) {
        if (maxFeatures > 0) {
            this.maxFeatures = maxFeatures;
        }
    }

    /**
     * Returns a set of identifiers for available feature instances of a given
     * type. The identifiers are randomly selected from the sample data.
     * 
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     * @param numId
     *            The desired number of identifiers.
     * @return A Set containing zero or more feature identifiers.
     */
    public Set<String> selectRandomFeatureIdentifiers(QName featureType, int numId) {
        FeatureTypeInfo typeInfo = featureInfo.get(featureType);
        File dataFile = typeInfo.getSampleData();
        Set<String> idSet = new HashSet<String>();
        if (null == dataFile || !dataFile.exists()) {
            return idSet;
        }
        String xpath = "//wfs:member/*/@gml:id";
        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put(Namespaces.GML, "gml");
        nsBindings.put(Namespaces.WFS, "wfs");
        XdmValue result = null;
        try {
            result = XMLUtils.evaluateXPath2(new StreamSource(dataFile), xpath, nsBindings);
        } catch (SaxonApiException e) {
            LOGR.log(Level.WARNING, String.format("Failed to extract feature identifiers from data file at %s",
                    dataFile.getAbsolutePath()));
        }
        int sampleSize = result.size();
        numId = (numId > sampleSize) ? sampleSize : numId;
        Random random = new Random();
        while (idSet.size() < numId) {
            int randomInt = random.nextInt(sampleSize);
            idSet.add(result.itemAt(randomInt).getStringValue());
        }
        return idSet;
    }

    /**
     * Returns a list containing the values (in document order) of the specified
     * feature property in the sample data set. The property value is converted
     * to a string as if the XPath string() function were applied.
     * 
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     * @param propName
     *            The name of the property.
     * @param featureId
     *            A feature identifer (gml:id); if {@code null} or empty the
     *            evaluation context includes all members of the collection.
     * @return A List containing simple property values; the list is empty if no
     *         values are found.
     */
    public List<String> getSimplePropertyValues(QName featureType, QName propName, String featureId) {
        FeatureTypeInfo typeInfo = featureInfo.get(featureType);
        File dataFile = typeInfo.getSampleData();
        List<String> values = new ArrayList<String>();
        if (null == dataFile || !dataFile.exists()) {
            return values;
        }
        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put(Namespaces.WFS, "wfs");
        nsBindings.put(Namespaces.GML, "gml");
        nsBindings.put(Namespaces.XSI, "xsi");
        nsBindings.put(featureType.getNamespaceURI(), "ns1");
        StringBuilder xpath = new StringBuilder("//wfs:member/ns1:");
        xpath.append(featureType.getLocalPart());
        if (null != featureId && !featureId.isEmpty()) {
            xpath.append("[@gml:id='").append(featureId).append("']");
        }
        if (!propName.getNamespaceURI().equals(featureType.getNamespaceURI())) {
            xpath.append("/ns2:");
            nsBindings.put(propName.getNamespaceURI(), "ns2");
        } else {
            xpath.append("/ns1:");
        }
        xpath.append(propName.getLocalPart());
        // ignore nil property values
        xpath.append("[not(@xsi:nil)]");
        XdmValue result = null;
        try {
            result = XMLUtils.evaluateXPath2(new StreamSource(dataFile), xpath.toString(), nsBindings);
        } catch (SaxonApiException e) {
            LOGR.log(Level.WARNING, String.format("Failed to evaluate XPath expression %s against data at %s\n%s\n",
                    xpath, dataFile.getAbsolutePath(), nsBindings) + e.getMessage());
        }
        if (null != result) {
            for (XdmItem item : result) {
                values.add(item.getStringValue());
            }
        }
        if (LOGR.isLoggable(Level.FINE)) {
            LOGR.log(Level.FINE, "[{0}] Evaluating xpath {1}\n {2}",
                    new Object[] { this.getClass().getName(), xpath, values });
        }
        return values;
    }

    /**
     * Deletes all saved data files.
     * 
     * @return {@code true} if all data files were deleted; {@code false}
     *         otherwise (see warnings in log file for details).
     */
    public boolean deleteData() {
        boolean allDeleted = true;
        for (QName typeName : featureInfo.keySet()) {
            File file = featureInfo.get(typeName).getSampleData();
            if ((file != null) && file.exists()) {
                if (!file.delete()) {
                    allDeleted = false;
                    LOGR.log(Level.WARNING, "Failed to delete sample data file at " + file);
                }
            }
        }
        return allDeleted;
    }

    /**
     * Attempts to acquire instances of all feature types supported by the WFS
     * using all supported GetFeature message bindings (request encodings). The
     * feature representations are saved in a temporary file. If no data exist
     * for a given feature type, {@link FeatureTypeInfo#isInstantiated()}
     * returns {@code false}.
     */
    public void acquireFeatureData() {
        WFSClient wfsClient = new WFSClient(this.serviceDescription);
        Set<ProtocolBinding> getFeatureBindings = ServiceMetadataUtils.getOperationBindings(serviceDescription,
                WFS2.GET_FEATURE);
        if(getFeatureBindings.isEmpty())
            throw new IllegalArgumentException( "No bindings available for GetFeature request." );
        for (Map.Entry<QName, FeatureTypeInfo> entry : featureInfo.entrySet()) {
            QName typeName = entry.getKey();
            FeatureTypeInfo featureTypeInfo = entry.getValue();
            acquireFeatureData( wfsClient, getFeatureBindings, typeName, featureTypeInfo );
        }
        LOGR.log(Level.INFO, featureInfo.toString());
    }

    /**
     * Returns a Map containing information about the feature types supported by
     * the WFS.
     * 
     * @return A Map where the keys are QName objects representing the names of
     *         feature types advertised in the capabilities document.
     */
    public Map<QName, FeatureTypeInfo> getFeatureTypeInfo() {
        return featureInfo;
    }

    /**
     * Returns a feature instance from the sample data.
     * 
     * @param id
     *            The feature identifier (@gml:id).
     * @return An Element representing a feature instance, or null if no
     *         matching feature is found.
     */
    public Element getFeatureById(String id) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(NamespaceBindings.withStandardBindings());
        String expr = String.format("//wfs:member/*[@gml:id='%s']", id);
        Element feature = null;
        for (FeatureTypeInfo featureInfo : this.featureInfo.values()) {
            if (!featureInfo.isInstantiated())
                continue;
            File dataFile = featureInfo.getSampleData();
            NodeList result = null;
            try {
                result = (NodeList) xpath.evaluate(expr, new InputSource(new FileInputStream(dataFile)),
                        XPathConstants.NODESET);
            } catch (XPathExpressionException | FileNotFoundException e) {
                LOGR.log(Level.WARNING, String.format("Failed to evaluate XPath %s against data file at %s", expr,
                        dataFile.getAbsolutePath()));
            }
            if (result.getLength() > 0) {
                feature = (Element) result.item(0);
                break;
            }
        }
        return feature;
    }

    /**
     * Returns the identifier (gml:id attribute value) for an existing feature instance.
     * 
     * @param featureType
     *            The qualified name of a the feature type the feature should not be an instance from.
     * @return A feature identifier, or null if one cannot be found.
     */
    public String getFeatureIdNotOfType( QName featureType ) {
        return getFeatureId( featureTypeInfo -> featureTypeInfo.getTypeName().equals( featureType ) );
    }

    /**
     * Returns the identifier (gml:id attribute value) for an existing feature instance.
     *
     * @return A feature identifier, or null if one cannot be found.
     */
    public String getFeatureId() {
        return getFeatureId( featureTypeInfo -> false );
    }

    private String getFeatureId( Function<FeatureTypeInfo, Boolean> skip ) {
        String expr = "(//wfs:member/*/@gml:id)[1]";
        Map<String, String> nsBindings = new HashMap<>();
        nsBindings.put( Namespaces.GML, "gml" );
        nsBindings.put( Namespaces.WFS, "wfs" );
        for ( FeatureTypeInfo featureTypeInfo : featureInfo.values() ) {
            if ( !skip.apply( featureTypeInfo ) && featureTypeInfo.isInstantiated() ) {
                File dataFile = featureTypeInfo.getSampleData();
                try {
                    XdmValue result = XMLUtils.evaluateXPath2( new StreamSource( dataFile ), expr, nsBindings );
                    if ( result.size() > 0 ) {
                        String featureId = result.itemAt( 0 ).getStringValue();
                        if ( featureId != null && !featureId.isEmpty() )
                            return featureId;
                    }
                } catch ( SaxonApiException e ) {
                    LOGR.log( Level.WARNING,
                              String.format( "Failed to evaluate XPath %s against data file at %s", expr,
                                             dataFile.getAbsolutePath() ) );
                }
            }
        }
        return null;
    }
    
    /**
     * Determines the spatial extent of the feature instances in the sample
     * data. If a feature type defines more than one geometry property, the
     * envelope is calculated using the first non-empty property.
     * 
     * @param model
     *            A model representing the supported GML application schema.
     * @param featureType
     *            The name of the feature type.
     * @return An Envelope, or null if one cannot be created or the feature type
     *         has no geometry properties defined.
     */
    public Envelope getSpatialExtent(XSModel model, QName featureType) {
        Envelope envelope = this.spatialExtents.get(featureType);
        if (null != envelope) {
            return envelope;
        }
        List<XSElementDeclaration> geomProps = AppSchemaUtils.getFeaturePropertiesByType(model, featureType,
                model.getTypeDefinition("AbstractGeometryType", Namespaces.GML));
        if (geomProps.isEmpty()) {
            return null;
        }
        Iterator<XSElementDeclaration> itr = geomProps.iterator();
        NamespaceBindings nsBindings = NamespaceBindings.withStandardBindings();
        XPathFactory factory = XPathFactory.newInstance();
        NodeList geomNodes = null;
        File dataFile = this.featureInfo.get(featureType).getSampleData();
        do {
            XSElementDeclaration geomProp = itr.next();
            nsBindings.addNamespaceBinding(geomProp.getNamespace(), "ns1");
            String expr = String.format("//ns1:%s/*[1]", geomProp.getName());
            XPath xpath = factory.newXPath();
            xpath.setNamespaceContext(nsBindings);
            try {
                geomNodes = (NodeList) xpath.evaluate(expr, new InputSource(new FileInputStream(dataFile)),
                        XPathConstants.NODESET);
            } catch (XPathExpressionException | FileNotFoundException e) {
                LOGR.log(Level.WARNING, String.format("Failed to evaluate XPath %s against data file at %s.\n %s", expr,
                        dataFile.getAbsolutePath(), e.getMessage()));
            }
            if (null != geomNodes && geomNodes.getLength() > 0) {
                break;
            }
        } while (itr.hasNext());
        if (null != geomNodes && geomNodes.getLength() > 0) {
            try {
                envelope = Extents.calculateEnvelopeUsingSingleGeometry(geomNodes);
            } catch (JAXBException e) {
                LOGR.log(Level.WARNING,
                        String.format("Failed to create envelope from geometry nodes.", e.getMessage()));
            }
        }
        this.spatialExtents.put(featureType, envelope);
        return envelope;
    }

    /**
     * Determines the temporal extent of all instances of the specified feature
     * property in the sample data. The temporal extent is extend by 1 day and 1 hour in the beginning and the end.
     * 
     * @param model
     *            A model representing the relevant GML application schema.
     * @param featureType
     *            The name of the feature type.
     * @param tmPropDecl
     *            A declaration of a temporal property.
     * @return A Period, or null if the property does not occur or has no
     *         values.
     */
    public Period getTemporalExtentOfProperty(XSModel model, QName featureType, XSElementDeclaration tmPropDecl) {
        FeatureProperty tmProp;
        try {
            tmProp = new FeatureProperty( featureType, tmPropDecl );
        } catch ( Exception e ) {
            throw new IllegalArgumentException( "Property " + tmPropDecl + " is not suitable as temporal property.", e );
        }
        Period period = this.temporalPropertyExtents.get(tmProp);
        if (null != period) {
            return period;
        }
        File dataFile = this.featureInfo.get(featureType).getSampleData();
        Document data;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            data = factory.newDocumentBuilder().parse(dataFile);
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new RuntimeException(
                    String.format("Failed to parse data file at %s.\n %s", dataFile.getAbsolutePath(), e.getMessage()));
        }
        TreeSet<TemporalGeometricPrimitive> tmSet = new TreeSet<>(new TemporalComparator());
        NodeList propNodes = data.getElementsByTagNameNS(tmPropDecl.getNamespace(), tmPropDecl.getName());
        for (int i = 0; i < propNodes.getLength(); i++) {
            TemporalGeometricPrimitive tVal;
            XSTypeDefinition propType = tmPropDecl.getTypeDefinition();
            Element propElem = (Element) propNodes.item(i);
            try {
                if ( propType.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE
                     || ( (XSComplexTypeDefinition) propType ).getContentType() == XSComplexTypeDefinition.CONTENTTYPE_SIMPLE ) {
                    tVal = TemporalQuery.parseTemporalValue( propElem.getTextContent(), propType );
                } else {
                    Element propValue = (Element) propElem.getElementsByTagName("*").item(0);
                    tVal = GmlUtils.gmlToTemporalGeometricPrimitive(propValue);
                }
                tmSet.add(tVal);
            } catch (RuntimeException re) {
                LOGR.log(Level.WARNING, re.getMessage());
                continue;
            }
        }
        if ( period != null ) {
            TemporalUtils.add( period.getEnding(), 2, ChronoUnit.DAYS );
            TemporalUtils.add( period.getBeginning(), -2, ChronoUnit.DAYS );
        }
        period = TemporalUtils.temporalExtent(tmSet);
        this.temporalPropertyExtents.put(tmProp, period);
        return period;
    }

    /**
     * Determines a property which is nillable and contains nilled properties for the specified feature type in the
     * sample data.
     *
     * @param model
     *            A model representing the relevant GML application schema, never <code>null</code>.
     * @param featureType
     *            The name of the feature type, never <code>null</code>.
     * @return A list of property names which are nillable and contains nilled instances, empty if such a property does
     *         not occur or has no values.
     */
    public List<QName> getNillableProperties( XSModel model, QName featureType ) {
        if ( nillableProperties.containsKey( featureType ) )
            return nillableProperties.get( featureType );
        List<QName> nillableProperties = new ArrayList<>();
        LOGR.fine( "Checking feature type for nillable properties: " + featureType );
        List<XSElementDeclaration> nillableProps = AppSchemaUtils.getNillableProperties( model, featureType );
        LOGR.fine( nillableProps.toString() );
        FeatureTypeInfo typeInfo = getFeatureTypeInfo().get( featureType );

        if ( typeInfo.isInstantiated() ) {
            for ( XSElementDeclaration elementDeclaration : nillableProps ) {
                QName propName = new QName( elementDeclaration.getNamespace(), elementDeclaration.getName() );
                // ignore nillable gml:boundedBy property
                if ( !BOUNDED_BY.equals( propName ) && nillablePropertyContainsNilledProperties( typeInfo, propName ) ) {
                    nillableProperties.add( propName );
                }

            }
        }
        LOGR.fine( "Nillable properties:\n" + nillableProps );
        this.nillableProperties.put( featureType, nillableProperties );
        return nillableProperties;
    }

    private boolean nillablePropertyContainsNilledProperties( FeatureTypeInfo typeInfo, QName propertyName ) {
        LOGR.fine( "Checking property " + propertyName + " for nilled properties." );
        File dataFile = typeInfo.getSampleData();
        Document data;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware( true );
            data = factory.newDocumentBuilder().parse( dataFile );
        } catch ( SAXException | IOException | ParserConfigurationException e ) {
            throw new RuntimeException( String.format( "Failed to parse data file at %s.\n %s",
                                                       dataFile.getAbsolutePath(), e.getMessage() ) );
        }
        NodeList propNodes = data.getElementsByTagNameNS( propertyName.getNamespaceURI(), propertyName.getLocalPart() );
        for ( int i = 0; i < propNodes.getLength(); i++ ) {
            Element propElem = (Element) propNodes.item( i );
            String nilValue = propElem.getAttributeNS( Namespaces.XSI, "nil" );
            if ( "true".equals( nilValue ) )
                return true;
        }
        LOGR.fine( "Property " + propertyName + " does not have nilled properties." );
        return false;
    }
    
    /**
     * Randomly selects a feature instance from the sample data obtained from
     * the IUT.
     * 
     * @return An Element node representing a feature instance.
     */
    public Element randomlySelectFeatureInstance() {
        Element feature = null;
        for (Entry<QName, FeatureTypeInfo> entry : this.featureInfo.entrySet()) {
            if (!entry.getValue().isInstantiated())
                continue;
            Set<String> idSet = selectRandomFeatureIdentifiers(entry.getKey(), 1);
            feature = getFeatureById(idSet.iterator().next());
        }
        return feature;
    }

    /**
     * Sort alphabetically and selects first feature type name for which
     * instances are available in the SUT.
     *
     * @return A QName object denoting the name of a feature type, or
     *         {@code null} if no data exist in the SUT.
     */
    public QName selectFeatureType() {
        List<FeatureTypeInfo> availableTypes = new ArrayList<FeatureTypeInfo>();
        List<String> featureName =  new ArrayList<String>();
        for (FeatureTypeInfo typeInfo : this.featureInfo.values()) {
            if (typeInfo.isInstantiated()) {
                availableTypes.add(typeInfo);
                featureName.add(typeInfo.getTypeName().getLocalPart());
            }
        }
        if (availableTypes.isEmpty()) {
            return null;
        }
        Collections.sort(featureName);
        Optional<FeatureTypeInfo> availableType = availableTypes.stream()
                .filter(x -> x.getTypeName().getLocalPart().equalsIgnoreCase(featureName.get(0))).findFirst();
        return availableType.get().getTypeName();
    }

    /**
     * Evaluates the given XPath expression against all sample data sets. The
     * first non-empty result is returned.
     * 
     * @param expr
     *            An XPath 2.0 expression.
     * @param nsBindings
     *            A collection of namespace bindings required to evaluate the
     *            XPath expression, where each entry maps a namespace URI (key)
     *            to a prefix (value).
     * @return An XdmValue object containing a sequence of zero or more matching
     *         items.
     */
    public XdmValue evaluateXPathAgainstSampleData(String expr, Map<String, String> nsBindings) {
        XdmValue results = null;
        for (Entry<QName, FeatureTypeInfo> entry : this.featureInfo.entrySet()) {
            if (!entry.getValue().isInstantiated())
                continue;
            File dataFile = entry.getValue().getSampleData();
            try {
                results = XMLUtils.evaluateXPath2(new StreamSource(dataFile), expr, nsBindings);
                if (results.size() > 0) {
                    break;
                }
            } catch (SaxonApiException e) {
                LOGR.log(Level.WARNING, e.getMessage());
            }
        }
        return results;
    }

    private void acquireFeatureData( WFSClient wfsClient, Set<ProtocolBinding> getFeatureBindings, QName typeName,
                                     FeatureTypeInfo featureTypeInfo ) {
        for (ProtocolBinding binding : getFeatureBindings) {
            try {
                Document rspEntity = wfsClient.getFeatureByType( typeName, maxFeatures, binding);
                NodeList features = rspEntity.getElementsByTagNameNS( typeName.getNamespaceURI(),
                                                                      typeName.getLocalPart());
                boolean hasFeatures = features.getLength() > 0;
                if (hasFeatures) {
                    saveFeatureDataFile( featureTypeInfo, typeName, rspEntity );
                    return;
                }
            } catch (RuntimeException re) {
                StringBuilder err = new StringBuilder();
                err.append(String.format("Failed to read XML response entity using %s method for feature type %s.",
                                         binding, typeName));
                err.append(" \n").append(re.getMessage());
                LOGR.log( Level.WARNING, err.toString(), re);
            }
        }
    }

    private void saveFeatureDataFile( FeatureTypeInfo featureTypeInfo, QName typeName, Document rspEntity ) {
        try {
            File file = File.createTempFile( typeName.getLocalPart() + "-", ".xml");
            FileOutputStream fos = new FileOutputStream( file);
            XMLUtils.writeNode( rspEntity, fos);
            LOGR.log( Level.CONFIG,
                      this.getClass().getName() + " - wrote feature data to " + file.getAbsolutePath());
            featureTypeInfo.setSampleData(file);
            fos.close();
            featureTypeInfo.setInstantiated(true);
        } catch (Exception e) {
            LOGR.log(Level.WARNING, "Failed to save feature data.", e);
        }
    }

}
