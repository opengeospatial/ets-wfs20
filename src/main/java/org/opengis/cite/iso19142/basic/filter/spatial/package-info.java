/**
 * This package includes tests covering the use of spatial operators in query 
 * expressions. The BBOX predicate must be implemented at the "Basic WFS" conformance 
 * level. Support for at least one other spatial operator is required to meet the 
 * requirements of the <strong>Spatial Filter</strong> conformance class.
 * 
 * <ul>
 * <li>BBOX (required for "Basic WFS" conformance)</li>
 * <li>Equals</li>
 * <li>Disjoint</li>
 * <li>Intersects</li>
 * <li>Touches</li>
 * <li>Crosses</li>
 * <li>Within</li>
 * <li>Contains</li>
 * <li>Overlaps</li>
 * <li>Beyond</li>
 * <li>DWithin</li>
 * </ul>
 *
 * <p style="margin-bottom: 0.5em"><strong>Sources</strong></p>
 * <ul>
 * <li>OGC 09-026r2, Table 1: FE conformance classes</li>
 * <li>OGC 09-026r2, 7.8: Spatial operators</li>
 * <li>OGC 06-103r4, 6.1.15.3: Named spatial relationship predicates based on the DE-9IM</li>
 * </ul>
 */
package org.opengis.cite.iso19142.basic.filter.spatial;