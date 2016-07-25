package org.opengis.cite.iso19142;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
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
	public void getDefaultExtent_epsg4326() throws FactoryException {
		FeatureTypeInfo iut = new FeatureTypeInfo();
		iut.addCRSIdentifiers("urn:ogc:def:crs:EPSG::4326");
		Envelope envelope = iut.getSpatialExtent();
		Assert.assertNotNull("Default extent is null.", envelope);
		String lowerCoordAsWKT = envelope.getLowerCorner().toString();
		String lowerCoord = lowerCoordAsWKT.substring(
				lowerCoordAsWKT.indexOf('(') + 1, lowerCoordAsWKT.indexOf(')'));
		Assert.assertEquals("Unexpected coordinates of lower corner.",
				"-90 -180", lowerCoord);
	}

	@Test
	public void getDefaultExtent_epsg26910() throws FactoryException {
		FeatureTypeInfo iut = new FeatureTypeInfo();
		// NAD83 / UTM zone 10N
		iut.addCRSIdentifiers("http://www.opengis.net/def/crs/EPSG/0/26910");
		Envelope envelope = iut.getSpatialExtent();
		Assert.assertNotNull("Default extent is null.", envelope);
		DirectPosition pos = envelope.getLowerCorner();
		Assert.assertTrue("Expected easting of lower corner > 200000 ",
				pos.getOrdinate(0) > 200000);
	}
}
