package org.opengis.cite.iso19142.basic;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Validator;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.WFS2;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * Verifies the behavior of the BasicGetFeatureTests class.
 */
public class VerifyBasicGetFeatureTests {

    private static ITestContext testContext;
    private static ISuite suite;

    public VerifyBasicGetFeatureTests() {
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

    @Test
    public void buildAndResetValidator() {
        BasicGetFeatureTests iut = new BasicGetFeatureTests();
        iut.buildValidator();
        Validator validator = iut.hintsValidator;
        LSResourceResolver resolver = validator.getResourceResolver();
        assertNotNull("Resolver is null.", resolver);
        iut.resetValidator();
        LSInput resource = resolver.resolveResource(Namespaces.XSD.toString(),
                Namespaces.WFS, null, WFS2.SCHEMA_URI, null);
        assertNotNull("WFS2 schema resource is null.", resource);
    }
}
