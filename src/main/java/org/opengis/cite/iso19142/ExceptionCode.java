package org.opengis.cite.iso19142;

/**
 * An enumerated type defining OGC exception codes that appear in an exception report.
 * Each error code has an associated HTTP status code.
 *
 * @see "OGC 09-025r2, Table D.2"
 */
public enum ExceptionCode {

	/** OGC 06-121r3, Table 25. */
	INVALID_PARAM_VALUE("InvalidParameterValue", 400);

	private final String ogcCode;

	private final int httpStatus;

	private ExceptionCode(String ogcCode, int httpStatus) {
		this.ogcCode = ogcCode;
		this.httpStatus = httpStatus;
	}

	public String ogcCode() {
		return ogcCode;
	}

	public int statusCode() {
		return httpStatus;
	}

}
