/**
 * This package contains tests that assess behavior of the SUT with respect to 
 * the "Locking WFS" conformance level. It builds upon the "Transaction WFS" 
 * conformance level and adds support for locking operations that allow 
 * exclusive access to feature instances for the purpose of modifying or 
 * deleting them.
 * 
 * At least one of the following operations must be implemented:
 * <ul>
 * <li>GetFeatureWithLock</li>
 * <li>LockFeature</li>
 * </ul>
 *
 * <h6 style="margin-bottom: 0.5em">Sources</h6>
 * <ul>
 * <li>ISO ISO 19142:2010, cl. 2, Table 1</li>
 * <li>ISO ISO 19142:2010, cl. A.1.4: Locking WFS</li>
 * </ul>
 */
package org.opengis.cite.iso19142.locking;
