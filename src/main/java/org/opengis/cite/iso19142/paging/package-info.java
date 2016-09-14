/**
 * This package contains tests to verify that the IUT satisfies the requirements
 * of the <strong>Response paging</strong> conformance class. If the WFS service
 * constraint <em>ImplementsResultPaging</em> is set to "TRUE" in the
 * capabilities document, a client may choose to page through the items in a
 * result set.
 * 
 * <p>
 * A server implementation that caches query results shall advertise for how
 * long items can stay in the query cache before they are invalidated; the
 * <em>ResponseCacheTimeout</em> constraint in the capabilities document is used
 * for this purpose (duration in seconds). A service may also support
 * transactional consistency for paging using the
 * <em>PagingIsTransactionSafe</em> constraint. If enabled, a result set is not
 * affected by any transactions that were completed after the original query was
 * submitted.
 * </p>
 *
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li>OGC 09-025, cl. 7.7.4.4: Response paging</li>
 * <li>OGC 09-025, cl. A.1.10: Response Paging</li>
 * <li>OGC 09-025, cl. A.2.20: Response paging</li>
 * </ul>
 */
package org.opengis.cite.iso19142.paging;