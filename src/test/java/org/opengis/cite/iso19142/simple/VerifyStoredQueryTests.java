package org.opengis.cite.iso19142.simple;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.testng.ISuite;
import org.testng.ITestContext;

/**
 * Verifies the behavior of the StoredQueryTests class.
 */
public class VerifyStoredQueryTests {

	private static ITestContext testContext;

	private static ISuite suite;

	public VerifyStoredQueryTests() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
		testContext = mock(ITestContext.class);
		suite = mock(ISuite.class);
		when(testContext.getSuite()).thenReturn(suite);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.newDocumentBuilder();
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

}
