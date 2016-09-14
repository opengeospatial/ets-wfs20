/**
 * This package contains tests to verify that the IUT satisfies the requirements
 * of the <strong>Feature versions</strong> conformance class. If the WFS
 * service constraint <em>ImplementsFeatureVersioning</em> is set to "TRUE" in
 * the capabilities document, a client may choose to navigate versions of
 * feature instances.
 * 
 * <p>
 * A server implementation that supports versioning shall maintain version
 * information about each feature instance, but exactly how this is accomplished
 * is not specified. The ResourceId operator may be used to query feature
 * versions; the filter constraint <em>ImplementsVersionNav</em> must be set to
 * "TRUE" in the capabilities document.
 * </p>
 * 
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li>OGC 09-025, cl. 7.2.3: Version identification</li>
 * <li>OGC 09-025, cl. A.1.14: Feature versions</li>
 * <li>OGC 09-025, cl. A.2.11: Versioning</li>
 * <li>OGC 09-026, cl. 7.11: Object identifiers</li>
 * </ul>
 */
package org.opengis.cite.iso19142.versioning;