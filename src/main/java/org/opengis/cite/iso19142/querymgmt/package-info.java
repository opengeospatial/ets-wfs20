/**
 * This package contains tests to verify that the IUT satisfies the requirements
 * of the <strong>Manage stored queries</strong> conformance class. If the WFS
 * service constraint <em>ManageStoredQueries</em> is set to "TRUE" in the
 * capabilities document, it is possible for clients to add and remove stored
 * queries.
 * 
 * <p>
 * All WFS implementations must handle the <code>ListStoredQueries</code> and
 * <code>DescribeStoredQueries</code> requests. This conformance class
 * introduces two requests for managing stored query definitions:
 * </p>
 * <ul>
 * <li><code>CreateStoredQuery</code></li>
 * <li><code>DropStoredQuery</code></li>
 * </ul>
 * 
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li>OGC 09-025, cl. 14.5: CreateStoredQuery operation</li>
 * <li>OGC 09-025, cl. 14.6: DropStoredQuery operations</li>
 * <li>OGC 09-025, cl. A.1.15: Manage stored queries</li>
 * </ul>
 */
package org.opengis.cite.iso19142.querymgmt;