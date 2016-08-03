package org.opengis.cite.iso19142;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

public class CommonTestFixture {
    protected static DocumentBuilder docBuilder;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void initCommonTestFixture() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        docBuilder = dbf.newDocumentBuilder();

    }

    protected String writeNodeToString(Node node) {
        DOMImplementationRegistry registry;
        try {
            registry = DOMImplementationRegistry.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
        LSSerializer serializer = impl.createLSSerializer();
        serializer.getDomConfig().setParameter("xml-declaration", false);
        serializer.getDomConfig().setParameter("format-pretty-print", true);
        return serializer.writeToString(node);
    }
}
