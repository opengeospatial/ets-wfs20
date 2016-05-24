/**
 * This package includes tests covering the use of temporal operators in query 
 * expressions. There are 14 temporal operators defined in the OGC Filter Encoding 
 * specification:
 * 
 * <ul>
 * <li>During (required for "Minimum Temporal Filter" conformance)</li>
 * <li>After</li>
 * <li>Before</li>
 * <li>Begins</li>
 * <li>BegunBy</li>
 * <li>TContains</li>
 * <li>TEquals</li>
 * <li>TOverlaps</li>
 * <li>OverlappedBy</li>
 * <li>Meets</li>
 * <li>MetBy</li>
 * <li>Ends</li>
 * <li>EndedBy</li>
 * <li>AnyInteracts</li>
 * </ul>
 *
 * <p>The conformance class "Minimum Temporal Filter" only requires support for 
 * the <strong>During</strong> operator. At least one other operator must be implemented 
 * to satisfy the requirements for "Temporal Filter" conformance.</p>
 * 
 * <p style="margin-bottom: 0.5em"><strong>Sources</strong></p>
 * <ul>
 * <li>OGC 09-026r2, Table 1: FE conformance classes</li>
 * <li>OGC 09-026r2, 7.9: Temporal operators</li>
 * <li>ISO 19108, Geographic information -- Temporal schema</li>
 * </ul>
 */
package org.opengis.cite.iso19142.basic.filter.temporal;