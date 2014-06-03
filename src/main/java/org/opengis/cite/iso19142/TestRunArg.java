package org.opengis.cite.iso19142;

/**
 * An enumerated type defining all recognized test run arguments.
 */
public enum TestRunArg {

    /** A string that identifies an available feature instance. */
    FID,
    /**
     * An absolute URI that refers to a representation of the test subject or
     * metadata about it.
     */
    IUT,
    /**
     * An absolute URI referring to metadata about the WFS implementation under
     * test. This is expected to be a WFS capabilities document where the
     * document element is {@code http://www.opengis.net/wfs/2.0}
     * WFS_Capabilities}.
     */
    WFS,
    /**
     * An implementation conformance statement: a comma-separated list
     * indicating which conformance classes are supported.
     */
    ICS;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
