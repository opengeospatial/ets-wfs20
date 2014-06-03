package org.opengis.cite.iso19142;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.geotoolkit.geometry.ImmutableEnvelope;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.geometry.DirectPosition;
import org.opengis.util.FactoryException;

public class VerifyFeatureTypeInfo {

    public VerifyFeatureTypeInfo() {
    }

    @BeforeClass
    public static void setUpClass() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.newDocumentBuilder();
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void getDefaultGeoExtent_epsg4326() throws FactoryException {
        FeatureTypeInfo iut = new FeatureTypeInfo();
        iut.setDefaultCRS("urn:ogc:def:crs:EPSG::4326");
        ImmutableEnvelope envelope = iut.getGeoExtent();
        Assert.assertNotNull("Default extent is null.", envelope);
        String lowerCoordAsWKT = envelope.getLowerCorner().toString();
        String lowerCoord = lowerCoordAsWKT.substring(
                lowerCoordAsWKT.indexOf('(') + 1, lowerCoordAsWKT.indexOf(')'));
        Assert.assertEquals("Unexpected coordinates of lower corner.",
                "-90 -180", lowerCoord);
    }

    @Test
    public void getDefaultGeoExtent_epsg26910() throws FactoryException {
        FeatureTypeInfo iut = new FeatureTypeInfo();
        // NAD83 / UTM zone 10N
        iut.setDefaultCRS("urn:ogc:def:crs:EPSG::26910");
        ImmutableEnvelope envelope = iut.getGeoExtent();
        Assert.assertNotNull("Default extent is null.", envelope);
        DirectPosition pos = envelope.getLowerCorner();
        Assert.assertTrue("Expected easting of lower corner > 200000 ",
                pos.getOrdinate(0) > 200000);
    }
}
