/**
 * This package includes tests for join queries. Servers that implement join 
 * queries shall implement an inner join; that is, only features that satisfy 
 * the join condition(s) shall be included in the result set.
 * 
 * <p>Three conformance classes are distinguished; the corresponding service 
 * constraints are shown in parentheses:</p>
 * <ul>
 * <li>Standard joins (ImplementsStandardJoins)</li>
 * <li>Spatial joins (ImplementsSpatialJoins)</li>
 * <li>Temporal joins (ImplementsTemporalJoins)</li>
 * </ul>
 *
 * <p style="margin-bottom: 0.5em"><strong>Sources</strong></p>
 * <ul>
 * <li>OGC 09-025r2, 7.9.2.5.3: Join processing</li>
 * <li>OGC 09-025r2, Table 13: Service constraints</li>
 * </ul>
 */
package org.opengis.cite.iso19142.joins;