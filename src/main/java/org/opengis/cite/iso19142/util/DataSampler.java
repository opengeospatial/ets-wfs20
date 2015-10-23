package org.opengis.cite.iso19142.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.dom.ElementOverNodeInfo;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;

import org.opengis.cite.iso19142.FeatureTypeInfo;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Obtains samples of the feature data available from the WFS under test.
 * Instances of all feature types advertised in the service description are
 * requested, but data need not exist for every type.
 */
public class DataSampler {

	private static final Logger LOGR = Logger.getLogger(DataSampler.class
			.getPackage().getName());
	private int maxFeatures = 25;
	private Document serviceDescription;
	private Map<QName, FeatureTypeInfo> featureInfo;

	/**
	 * Constructs a new DataSampler for a particular WFS implementation.
	 * 
	 * @param wfsCapabilities
	 *            A DOM Document representing the service metadata
	 *            (/wfs:WFS_Capabilities).
	 */
	public DataSampler(Document wfsCapabilities) {
		if (null == wfsCapabilities
				|| !wfsCapabilities.getDocumentElement().getLocalName()
						.equals(WFS2.WFS_CAPABILITIES)) {
			throw new IllegalArgumentException(
					"Did not supply a WFS capabilities document");
		}
		this.serviceDescription = wfsCapabilities;
		// NOTE: Also set in test suite context by SuiteFixtureListener
		this.featureInfo = ServiceMetadataUtils
				.extractFeatureInfo(wfsCapabilities);
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
	public Set<String> selectRandomFeatureIdentifiers(QName featureType,
			int numId) {
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
			result = XMLUtils.evaluateXPath2(new StreamSource(dataFile), xpath,
					nsBindings);
		} catch (SaxonApiException e) {
			LOGR.log(
					Level.WARNING,
					String.format(
							"Failed to extract feature identifiers from data file at %s",
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
	public List<String> getSimplePropertyValues(QName featureType,
			QName propName, String featureId) {
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
			result = XMLUtils.evaluateXPath2(new StreamSource(dataFile),
					xpath.toString(), nsBindings);
		} catch (SaxonApiException e) {
			LOGR.log(
					Level.WARNING,
					String.format(
							"Failed to evaluate XPath expression %s against data at %s\n%s\n",
							xpath, dataFile.getAbsolutePath(), nsBindings)
							+ e.getMessage());
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
					LOGR.log(Level.WARNING,
							"Failed to delete sample data file at " + file);
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
		Set<ProtocolBinding> getFeatureBindings = ServiceMetadataUtils
				.getOperationBindings(serviceDescription, WFS2.GET_FEATURE);
		for (Map.Entry<QName, FeatureTypeInfo> entry : featureInfo.entrySet()) {
			QName typeName = entry.getKey();
			for (ProtocolBinding binding : getFeatureBindings) {
				try {
					Document rspEntity = wfsClient.getFeatureByType(typeName,
							maxFeatures, binding);
					NodeList features = rspEntity
							.getElementsByTagNameNS(typeName.getNamespaceURI(),
									typeName.getLocalPart());
					boolean hasFeatures = features.getLength() > 0;
					entry.getValue().setInstantiated(hasFeatures);
					if (hasFeatures) {
						try {
							File file = File.createTempFile(
									typeName.getLocalPart() + "-", ".xml");
							FileOutputStream fos = new FileOutputStream(file);
							XMLUtils.writeNode(rspEntity, fos);
							LOGR.log(Level.FINE,
									this.getClass().getName()
											+ " - wrote response entity to "
											+ file.getAbsolutePath());
							entry.getValue().setSampleData(file);
							fos.close();
						} catch (IOException iox) {
							LOGR.log(Level.WARNING,
									"Failed to save feature data.", iox);
						}
						break;
					}
				} catch (RuntimeException re) {
					LOGR.log(
							Level.WARNING,
							String.format(
									"Failed to parse response entity using %s binding for feature type %s",
									binding, typeName), re);
					continue;
				}
			}
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
	 * @return An Element representing a feature instance, or {@code null} if no
	 *         matching feature is found.
	 */
	public Element getFeatureById(String id) {
		Element feature = null;
		for (FeatureTypeInfo featureInfo : this.featureInfo.values()) {
			if (!featureInfo.isInstantiated())
				continue;
			File dataFile = featureInfo.getSampleData();
			String xpath = "//wfs:member/*[@gml:id='" + id + "']";
			Map<String, String> nsBindings = new HashMap<String, String>();
			nsBindings.put(Namespaces.GML, "gml");
			nsBindings.put(Namespaces.WFS, "wfs");
			XdmValue result = null;
			try {
				result = XMLUtils.evaluateXPath2(new StreamSource(dataFile),
						xpath, nsBindings);
			} catch (SaxonApiException e) {
				LOGR.log(Level.WARNING, String.format(
						"Failed to evaluate XPath %s against data file at %s",
						xpath, dataFile.getAbsolutePath()));
			}
			if ((null != result) && result.size() > 0) {
				XdmNode node = (XdmNode) result.itemAt(0);
				feature = (Element) ElementOverNodeInfo.wrap(node
						.getUnderlyingNode());
				break;
			}
		}
		return feature;
	}
}
