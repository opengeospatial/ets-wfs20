package org.opengis.cite.iso19142.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.junit.Test;
import org.opengis.cite.validation.SchematronValidator;

/**
 * Verifies the behavior of the ValidationUtils class.
 */
public class VerifyValidationUtils {

    public VerifyValidationUtils() {
    }

    @Test
    public void testBuildSchematronValidator() {
        String schemaRef = "http://schemas.opengis.net/gml/3.2.1/SchematronConstraints.xml";
        String phase = "";
        SchematronValidator result = ValidationUtils.buildSchematronValidator(
                schemaRef, phase);
        assertNotNull(result);
    }

    @Test
    public void extractRelativeSchemaReference() throws FileNotFoundException,
            XMLStreamException {
        File xmlFile = new File("src/test/resources/Alpha-1.xml");
        URI xsdRef = ValidationUtils.extractSchemaReference(new StreamSource(
                xmlFile), null);
        assertTrue("Expected schema reference */xsd/alpha.xsd", xsdRef
                .toString().endsWith("/xsd/alpha.xsd"));
    }

    @Test
    public void compileWFSSchema() {
        Schema schema = ValidationUtils.createWFSSchema();
        assertNotNull(schema);
    }
}
