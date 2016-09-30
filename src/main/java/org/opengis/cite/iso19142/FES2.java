package org.opengis.cite.iso19142;

/**
 * Contains various constants pertaining to standard filter expressions
 * specified in ISO 19143:2010.
 * 
 * @see "ISO 19143:2010, Geographic information -- Filter encoding"
 */
public class FES2 {

    private FES2() {
    }

    /** PropertyIsEqualTo operator. */
    public static final String EQUAL = "PropertyIsEqualTo";
    /** PropertyIsNotEqualTo operator. */
    public static final String NOT_EQUAL = "PropertyIsNotEqualTo";
    /** PropertyIsLessThan operator. */
    public static final String LESS_THAN = "PropertyIsLessThan";
    /** PropertyIsGreaterThan operator. */
    public static final String GREATER_THAN = "PropertyIsGreaterThan";
    /** PropertyIsLessThanOrEqualTo operator. */
    public static final String LESS_THAN_OR_EQUAL = "PropertyIsLessThanOrEqualTo";
    /** PropertyIsGreaterThanOrEqualTo operator. */
    public static final String GREATER_THAN_OR_EQUAL = "PropertyIsGreaterThanOrEqualTo";
    /** ResourceId operator. */
    public static final String RESOURCE_ID = "ResourceId";

    /** VersionAction is used to filter the version chain in ResourceId. */
    public enum VersionAction {
        FIRST, LAST, PREVIOUS, NEXT, ALL
    }

}
