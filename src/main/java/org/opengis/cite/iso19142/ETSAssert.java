package org.opengis.cite.iso19142;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmValue;

import org.opengis.cite.iso19142.util.XMLUtils;
import org.opengis.cite.iso19142.util.NamespaceBindings;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.iso19142.util.WFSClient;
import org.opengis.cite.validation.SchematronValidator;
import org.opengis.cite.validation.ValidationErrorHandler;
import org.testng.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Provides a set of custom assertion methods.
 */
public class ETSAssert {

    private ETSAssert() {
    }

    /**
     * Asserts that the qualified name of a DOM Node matches the expected value.
     * 
     * @param node
     *            The Node to check.
     * @param qName
     *            A QName object containing a namespace name (URI) and a local
     *            part.
     */
    public static void assertQualifiedName(Node node, QName qName) {
        Assert.assertEquals(node.getLocalName(), qName.getLocalPart(),
                ErrorMessage.get(ErrorMessageKeys.LOCAL_NAME));
        Assert.assertEquals(node.getNamespaceURI(), qName.getNamespaceURI(),
                ErrorMessage.get(ErrorMessageKeys.NAMESPACE_NAME));
    }

    /**
     * Asserts that an XPath 1.0 expression holds true for the given evaluation
     * context. The following standard namespace bindings do not need to be
     * explicitly declared:
     * 
     * <ul>
     * <li>wfs: {@value org.opengis.cite.iso19142.Namespaces#WFS}</li>
     * <li>fes: {@value org.opengis.cite.iso19142.Namespaces#FES}</li>
     * <li>ows: {@value org.opengis.cite.iso19142.Namespaces#OWS}</li>
     * <li>xlink: {@value org.opengis.cite.iso19142.Namespaces#XLINK}</li>
     * <li>gml: {@value org.opengis.cite.iso19142.Namespaces#GML}</li>
     * <li>soap: {@value org.opengis.cite.iso19142.Namespaces#SOAP_ENV}</li>
     * <li>xsi: {@value javax.xml.XMLConstants#W3C_XML_SCHEMA_INSTANCE_NS_URI}</li>
     * </ul>
     * 
     * @param expr
     *            A valid XPath 1.0 expression.
     * @param context
     *            The context node.
     * @param namespaceBindings
     *            A collection of namespace bindings for the XPath expression,
     *            where each entry maps a namespace URI (key) to a prefix
     *            (value). It may be {@code null}.
     */
    public static void assertXPath(String expr, Node context,
            Map<String, String> namespaceBindings) {
        if (null == context) {
            throw new NullPointerException("Context node is null.");
        }
        NamespaceBindings bindings = NamespaceBindings.withStandardBindings();
        bindings.addAllBindings(namespaceBindings);
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(bindings);
        Boolean result;
        try {
            result = (Boolean) xpath.evaluate(expr, context,
                    XPathConstants.BOOLEAN);
        } catch (XPathExpressionException xpe) {
            String msg = ErrorMessage
                    .format(ErrorMessageKeys.XPATH_ERROR, expr);
            TestSuiteLogger.log(Level.WARNING, msg, xpe);
            throw new AssertionError(msg);
        }
        Assert.assertTrue(
                result,
                ErrorMessage.format(ErrorMessageKeys.XPATH_RESULT,
                        context.getNodeName(), expr));
    }

    /**
     * Asserts that an XPath 2.0 expression evaluates to {@code true} for the
     * given XML source. That is, the result set is not empty.
     * 
     * @param expr
     *            An XPath 2.0 expression.
     * @param source
     *            A Source object representing an XML resource.
     * @param namespaceBindings
     *            A collection of namespace bindings for the XPath expression,
     *            where each entry maps a namespace URI (key) to a prefix
     *            (value). It may be {@code null}.
     */
    public static void assertXPath2(String expr, Source source,
            Map<String, String> namespaceBindings) {
        if (TestSuiteLogger.isLoggable(Level.FINE)) {
            TestSuiteLogger.log(
                    Level.FINE,
                    "Asserting XPath expression {0} against {1} ({2})",
                    new Object[] { expr, source.getClass().getName(),
                            source.getSystemId() });
        }
        XdmValue result = null;
        try {
            result = XMLUtils.evaluateXPath2(source, expr, namespaceBindings);
        } catch (SaxonApiException e) {
            throw new AssertionError(ErrorMessage.format(
                    ErrorMessageKeys.XPATH_ERROR, expr + e.getMessage()));
        }
        Assert.assertTrue(
                result.size() > 0,
                ErrorMessage.format(ErrorMessageKeys.XPATH_RESULT,
                        source.getSystemId(), expr));
    }

    /**
     * Asserts that an XML resource is schema-valid.
     * 
     * @param validator
     *            The Validator to use.
     * @param source
     *            The XML Source to be validated.
     */
    public static void assertSchemaValid(Validator validator, Source source) {
        ValidationErrorHandler errHandler = new ValidationErrorHandler();
        validator.setErrorHandler(errHandler);
        try {
            validator.validate(source);
        } catch (Exception e) {
            throw new AssertionError(ErrorMessage.format(
                    ErrorMessageKeys.XML_ERROR, e.getMessage()));
        }
        Assert.assertFalse(errHandler.errorsDetected(), ErrorMessage.format(
                ErrorMessageKeys.NOT_SCHEMA_VALID, errHandler.getErrorCount(),
                errHandler.toString()));
    }

    /**
     * Asserts that an XML resource satisfies all applicable constraints
     * specified in a Schematron (ISO 19757-3) schema. The "xslt2" query
     * language binding is supported. All patterns are checked.
     * 
     * @param schemaRef
     *            A URL that denotes the location of a Schematron schema.
     * @param xmlSource
     *            The XML Source to be validated.
     */
    public static void assertSchematronValid(URL schemaRef, Source xmlSource) {
        SchematronValidator validator;
        try {
            validator = new SchematronValidator(new StreamSource(
                    schemaRef.toString()), "#ALL");
        } catch (Exception e) {
            StringBuilder msg = new StringBuilder(
                    "Failed to process Schematron schema at ");
            msg.append(schemaRef).append('\n');
            msg.append(e.getMessage());
            throw new AssertionError(msg);
        }
        DOMResult result = validator.validate(xmlSource);
        Assert.assertFalse(validator.ruleViolationsDetected(), ErrorMessage
                .format(ErrorMessageKeys.NOT_SCHEMA_VALID,
                        validator.getRuleViolationCount(),
                        XMLUtils.writeNodeToString(result.getNode())));
    }

    /**
     * Asserts the availability of a feature having the specified resource
     * identifier. The following XPath expression is checked against the
     * GetFeature response entity:
     * 
     * {@code //wfs:member/*[@gml:id = '$id']}
     * 
     * @param id
     *            The feature identifier (assigned by the system).
     * @param isAvailable
     *            A boolean value asserting that the feature either is (
     *            {@code true}) or is not ({@code false}) available.
     * @param wfsClient
     *            A WFSClient component that interacts with the SUT.
     */
    public static void assertFeatureAvailability(String id,
            boolean isAvailable, WFSClient wfsClient) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        Document rsp = wfsClient.invokeStoredQuery(WFS2.QRY_GET_FEATURE_BY_ID,
                params);
        String xpath = String.format("/*[@gml:id = '%s']", id);
        NodeList result;
        try {
            result = XMLUtils.evaluateXPath(rsp, xpath, null);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        if (isAvailable && result.getLength() == 0) {
            throw new AssertionError(ErrorMessage.format(
                    ErrorMessageKeys.FEATURE_AVAILABILITY, result.getLength(),
                    id));
        }
        if (!isAvailable && result.getLength() > 0) {
            throw new AssertionError(ErrorMessage.format(
                    ErrorMessageKeys.FEATURE_AVAILABILITY, result.getLength(),
                    id));
        }
    }

    /**
     * Asserts that selected properties of a feature have specified values. A
     * {@code GetFeatureById} stored query is submitted, and the resulting
     * feature instance is examined for the expected property values.
     * 
     * @param gmlId
     *            A GML object identifier (gml:id attribute value).
     * @param properties
     *            A collection of feature properties containing {name: value}
     *            entries, where the name is an XPath expression.
     * @param nsBindings
     *            A collection of namespace bindings for the supplied
     *            properties, where each entry is a {namespace-URI: prefix}
     *            pair.
     * @param wfsClient
     *            A WFSClient component that interacts with the SUT.
     */
    public static void assertFeatureProperties(String gmlId,
            Map<String, Object> properties, Map<String, String> nsBindings,
            WFSClient wfsClient) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", gmlId);
        Document rsp = wfsClient.invokeStoredQuery(WFS2.QRY_GET_FEATURE_BY_ID,
                params);
        String xpath = String.format("//*[@gml:id = '%s']", gmlId);
        Node feature;
        try {
            feature = (Element) XMLUtils.evaluateXPath(rsp, xpath, nsBindings)
                    .item(0);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        Assert.assertNotNull(feature, ErrorMessage.format(
                ErrorMessageKeys.XPATH_RESULT, rsp.getDocumentElement()
                        .getNodeName(), xpath));
        for (Map.Entry<String, Object> property : properties.entrySet()) {
            String expr = null;
            if (Node.class.isInstance(property.getValue())) {
                // complex property value
                // TODO: should be more thorough here comparing XML nodes
                Node value = (Node) property.getValue();
                expr = String.format("%s/%s", property.getKey(),
                        value.getNodeName());
            } else {
                expr = String.format("%s = '%s'", property.getKey(), property
                        .getValue().toString());
            }
            assertXPath(expr, feature, nsBindings);
        }
    }

    /**
     * Asserts that the given XML entity contains the expected number of
     * descendant elements having the specified name.
     * 
     * @param xmlEntity
     *            A Document representing an XML entity.
     * @param elementName
     *            The qualified name of the element.
     * @param expectedCount
     *            The expected number of occurrences.
     */
    public static void assertDescendantElementCount(Document xmlEntity,
            QName elementName, int expectedCount) {
        NodeList features = xmlEntity.getElementsByTagNameNS(
                elementName.getNamespaceURI(), elementName.getLocalPart());
        Assert.assertEquals(features.getLength(), expectedCount, String.format(
                "Unexpected number of %s descendant elements.", elementName));
    }

    /**
     * Asserts that the actual HTTP status code matches one of the expected
     * status codes.
     * 
     * @param actualCode
     *            The actual status code.
     * @param expectedCodes
     *            An int[] array containing the expected status codes.
     */
    public static void assertStatusCode(int actualCode, int[] expectedCodes) {
        Arrays.sort(expectedCodes); // precondition for binary search
        Assert.assertTrue(Arrays.binarySearch(expectedCodes, actualCode) >= 0,
                String.format("Expected status code(s) %s but received %d.",
                        Arrays.toString(expectedCodes), actualCode));

    }
}
