package org.opengis.cite.iso19142;

import java.net.URI;

/**
 * XML namespace names.
 *
 * @see <a href="http://www.w3.org/TR/xml-names/">Namespaces in XML 1.0</a>
 */
public class Namespaces {

	private Namespaces() {
	}

	/** Legacy SOAP 1.1 message envelopes. */
	public static final String SOAP11 = "http://schemas.xmlsoap.org/soap/envelope/";

	/** SOAP 1.2 message envelopes. */
	public static final String SOAP_ENV = "http://www.w3.org/2003/05/soap-envelope";

	/** W3C XLink */
	public static final String XLINK = "http://www.w3.org/1999/xlink";

	/** XML Schema instance namespace */
	public static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";

	/** OGC 06-121r3 (OWS 1.1) */
	public static final String OWS = "http://www.opengis.net/ows/1.1";

	/** ISO 19136:2007 (GML 3.2) */
	public static final String GML = "http://www.opengis.net/gml/3.2";

	/** ISO 19142:2010 (WFS 2.0) */
	public static final String WFS = "http://www.opengis.net/wfs/2.0";

	/** ISO 19143:2010 (FES 2.0) */
	public static final String FES = "http://www.opengis.net/fes/2.0";

	/** W3C XML Schema namespace */
	public static final URI XSD = URI.create("http://www.w3.org/2001/XMLSchema");

	/** Schematron (ISO 19757-3) namespace */
	public static final URI SCH = URI.create("http://purl.oclc.org/dsdl/schematron");

}
