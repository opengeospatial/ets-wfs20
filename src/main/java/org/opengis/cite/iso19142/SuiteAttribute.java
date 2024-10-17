package org.opengis.cite.iso19142;

import java.util.List;
import java.util.Map;

import javax.xml.validation.Schema;

import org.opengis.cite.iso19142.util.DataSampler;
import org.w3c.dom.Document;

/**
 * An enumerated type defining ISuite attributes that may be set to constitute a shared
 * test fixture.
 */
@SuppressWarnings("rawtypes")
public enum SuiteAttribute {

	/**
	 * A DOM Document describing the WFS under test. This is typically a WFS capabilities
	 * document.
	 */
	TEST_SUBJECT("testSubject", Document.class),
	/**
	 * An immutable Schema object representing the complete WFS 2.0 schema (wfs.xsd).
	 */
	WFS_SCHEMA("wfsSchema", Schema.class),
	/**
	 * A {@literal Map<QName, {@link FeatureTypeInfo}>} containing one or more entries
	 * providing information about managed feature types.
	 */
	FEATURE_INFO("featureInfo", Map.class),
	/**
	 * A DataSampler object that obtains sample data from the WFS under test.
	 */
	SAMPLER("sampler", DataSampler.class),
	/**
	 * A {@literal List<String>} of test suite preconditions that were not satisfied.
	 */
	FAILED_PRECONDITIONS("failedPreconditions", List.class),
	/**
	 * The highest specification version supported by the IUT.
	 */
	WFS_VERSION("wfsVersion", String.class);

	private final Class attrType;

	private final String attrName;

	private SuiteAttribute(String attrName, Class attrType) {
		this.attrName = attrName;
		this.attrType = attrType;
	}

	public Class getType() {
		return attrType;
	}

	public String getName() {
		return attrName;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(attrName);
		sb.append('(').append(attrType.getName()).append(')');
		return sb.toString();
	}

}
