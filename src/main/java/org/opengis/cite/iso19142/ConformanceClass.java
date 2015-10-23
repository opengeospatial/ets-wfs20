package org.opengis.cite.iso19142;

/**
 * A WFS 2.0 conformance class. Four fundamental conformance levels are defined:
 * <ol>
 * <li>Simple WFS</li>
 * <li>Basic WFS</li>
 * <li>Transactional WFS</li>
 * <li>Locking WFS</li>
 * </ol>
 * 
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li>ISO 19142:2010, Geographic information -- Web Feature Service: Table 1</li>
 * <li>ISO 19142:2010, Geographic information -- Web Feature Service: Table 13</li>
 * </ul>
 */
public enum ConformanceClass {

	/** Simple WFS (A.1.1) */
	SIMPLE_WFS("ImplementsSimpleWFS"),
	/** Basic WFS (A.1.2) */
	BASIC_WFS("ImplementsBasicWFS"),
	/** Transactional WFS (A.1.3) */
	TRANSACTIONAL_WFS("ImplementsTransactionalWFS"),
	/** Locking WFS (A.1.4) */
	LOCKING_WFS("ImplementsLockingWFS"),
	/** KVP requests (A.1.5) */
	HTTP_GET("KVPEncoding"),
	/** XML requests (A.1.6) */
	HTTP_POST("XMLEncoding"),
	/** SOAP requests (A.1.7) */
	SOAP("SOAPEncoding");

	private final String constraintName;

	private ConformanceClass(String constraintName) {
		this.constraintName = constraintName;
	}

	public String getConstraintName() {
		return constraintName;
	}
}
