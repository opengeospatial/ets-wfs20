package org.opengis.cite.iso19142;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmValue;

import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.opengis.cite.iso19142.util.NamespaceBindings;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.iso19142.util.ValidationUtils;
import org.opengis.cite.iso19142.util.WFSClient;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.opengis.cite.validation.SchematronValidator;
import org.opengis.cite.validation.ValidationErrorHandler;
import org.testng.Assert;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Provides a set of custom assertion methods.
 */
public class ETSAssert {

    private final static Logger LOGR = Logger.getLogger(ETSAssert.class.getName());

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
        Assert.assertEquals(node.getLocalName(), qName.getLocalPart(), ErrorMessage.get(ErrorMessageKeys.LOCAL_NAME));
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
     * <li>xsi:
     * {@value javax.xml.XMLConstants#W3C_XML_SCHEMA_INSTANCE_NS_URI}</li>
     * </ul>
     * 
     * The method arguments will be logged at level FINE or lower.
     * 
     * @param expr
     *            A valid XPath 1.0 expression.
     * @param context
     *            The context node.
     * @param nsBindings
     *            A collection of namespace bindings for the XPath expression,
     *            where each entry maps a namespace URI (key) to a prefix
     *            (value). It may be {@code null}.
     */
    public static void assertXPath(String expr, Node context, Map<String, String> nsBindings) {
        if (null == context) {
            throw new NullPointerException("Context node is null.");
        }
        LOGR.log(Level.FINE, "Evaluating \"{0}\" against context node:\n{1}",
                new Object[] { expr, XMLUtils.writeNodeToString(context) });
        NamespaceBindings bindings = NamespaceBindings.withStandardBindings();
        bindings.addAllBindings(nsBindings);
        XPathFactory factory = null;
        try {
            factory = XPathFactory.newInstance(XPathConstants.DOM_OBJECT_MODEL);
        } catch (XPathFactoryConfigurationException e) {
            // An implementation for the W3C DOM is always available
        }
        XPath xpath = factory.newXPath();
        LOGR.log(Level.FINER, "Using XPath implementation: " + xpath.getClass().getName());
        xpath.setNamespaceContext(bindings);
        Boolean result;
        try {
            result = (Boolean) xpath.evaluate(expr, context, XPathConstants.BOOLEAN);
        } catch (XPathExpressionException xpe) {
            String msg = ErrorMessage.format(ErrorMessageKeys.XPATH_ERROR, expr);
            LOGR.log(Level.WARNING, msg, xpe);
            throw new AssertionError(msg);
        }
        LOGR.log(Level.FINE, "XPath result: " + result);
        Assert.assertTrue(result, ErrorMessage.format(ErrorMessageKeys.XPATH_RESULT, context.getNodeName(), expr));
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
    public static void assertXPath2(String expr, Source source, Map<String, String> namespaceBindings) {
        if (TestSuiteLogger.isLoggable(Level.FINE)) {
            TestSuiteLogger.log(Level.FINE, "Asserting XPath expression {0} against {1} ({2})",
                    new Object[] { expr, source.getClass().getName(), source.getSystemId() });
        }
        XdmValue result = null;
        try {
            result = XMLUtils.evaluateXPath2(source, expr, namespaceBindings);
        } catch (SaxonApiException e) {
            throw new AssertionError(ErrorMessage.format(ErrorMessageKeys.XPATH_ERROR, expr + e.getMessage()));
        }
        Assert.assertTrue(result.size() > 0,
                ErrorMessage.format(ErrorMessageKeys.XPATH_RESULT, source.getSystemId(), expr));
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
            throw new AssertionError(ErrorMessage.format(ErrorMessageKeys.XML_ERROR, e.getMessage()));
        }
        Assert.assertFalse(errHandler.errorsDetected(), ErrorMessage.format(ErrorMessageKeys.NOT_SCHEMA_VALID,
                errHandler.getErrorCount(), errHandler.toString()));
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
            validator = new SchematronValidator(new StreamSource(schemaRef.toString()), "#ALL");
        } catch (Exception e) {
            StringBuilder msg = new StringBuilder("Failed to process Schematron schema at ");
            msg.append(schemaRef).append('\n');
            msg.append(e.getMessage());
            throw new AssertionError(msg);
        }
        DOMResult result = validator.validate(xmlSource);
        Assert.assertFalse(validator.ruleViolationsDetected(), ErrorMessage.format(ErrorMessageKeys.NOT_SCHEMA_VALID,
                validator.getRuleViolationCount(), XMLUtils.writeNodeToString(result.getNode())));
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
    public static void assertFeatureAvailability(String id, boolean isAvailable, WFSClient wfsClient) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        Document rsp = wfsClient.invokeStoredQuery(WFS2.QRY_GET_FEATURE_BY_ID, params);
        String xpath = String.format("/*[@gml:id = '%s']", id);
        NodeList result;
        try {
            result = XMLUtils.evaluateXPath(rsp, xpath, null);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        if (isAvailable && result.getLength() == 0) {
            throw new AssertionError(
                    ErrorMessage.format(ErrorMessageKeys.FEATURE_AVAILABILITY, result.getLength(), id));
        }
        if (!isAvailable && result.getLength() > 0) {
            throw new AssertionError(
                    ErrorMessage.format(ErrorMessageKeys.FEATURE_AVAILABILITY, result.getLength(), id));
        }
    }

    /**
     * Asserts that one or more simple properties of a feature have the expected
     * values. If a property occurs more than once, only the first occurrence is
     * checked.
     * 
     * @param feature
     *            An Element node representing a GML feature.
     * @param expectedValues
     *            A collection of feature properties containing {prop: value}
     *            entries, where the <code>prop</code> key is an element
     *            declaration (XSElementDeclaration) denoting a property.
     * @param nsBindings
     *            A collection of namespace bindings for the supplied
     *            properties, where each entry is a {namespace-name: prefix}
     *            pair. A binding is not required for standard GML properties.
     */
    public static void assertSimpleProperties(Element feature, Map<XSElementDeclaration, Object> expectedValues,
            Map<String, String> nsBindings) {
        for (Map.Entry<XSElementDeclaration, Object> property : expectedValues.entrySet()) {
            XSElementDeclaration propDecl = property.getKey();
            XSSimpleTypeDefinition propType;
            if (propDecl.getTypeDefinition().getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
                propType = (XSSimpleTypeDefinition) propDecl.getTypeDefinition();
            } else {
                XSComplexTypeDefinition complexType = (XSComplexTypeDefinition) propDecl.getTypeDefinition();
                propType = complexType.getSimpleType();
            }
            String expectedValue = XMLUtils.expandReferencesInText(property.getValue().toString());
            String expr = null;
            String nsPrefix = (propDecl.getNamespace().equals(Namespaces.GML)) ? "gml"
                    : nsBindings.get(propDecl.getNamespace());
            if (propType.getNumeric()) {
                expr = String.format("number(%s:%s[1]) eq %s", nsPrefix, propDecl.getName(), expectedValue);
            } else { // string comparison
                expr = String.format("%s:%s[1] eq \"%s\"", nsPrefix, propDecl.getName(), expectedValue);
            }
            assertXPath2(expr, new DOMSource(feature), nsBindings);
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
    public static void assertDescendantElementCount(Document xmlEntity, QName elementName, int expectedCount) {
        NodeList features = xmlEntity.getElementsByTagNameNS(elementName.getNamespaceURI(), elementName.getLocalPart());
        Assert.assertEquals(features.getLength(), expectedCount,
                String.format("Unexpected number of %s descendant elements.", elementName));
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
        Assert.assertTrue(Arrays.binarySearch(expectedCodes, actualCode) >= 0, String
                .format("Expected status code(s) %s but received %d.", Arrays.toString(expectedCodes), actualCode));

    }

    /**
     * Asserts that the given DOM document contains a description of a "Simple
     * WFS" implementation.
     * 
     * @param doc
     *            A Document node representing a WFS capabilities document
     *            (wfs:WFS_Capabilities}.
     */
    public static void assertSimpleWFSCapabilities(Document doc) {
        SchematronValidator validator = ValidationUtils.buildSchematronValidator("wfs-capabilities-2.0.sch",
                "SimpleWFSPhase");
        DOMResult result = validator.validate(new DOMSource(doc, doc.getDocumentURI()));
        Assert.assertFalse(validator.ruleViolationsDetected(), ErrorMessage.format(ErrorMessageKeys.NOT_SCHEMA_VALID,
                validator.getRuleViolationCount(), XMLUtils.writeNodeToString(result.getNode())));
    }

    /**
     * Asserts that the given GetFeature response entity contains the expected
     * number of feature instances having the specified type name.
     * 
     * @param featureCollection
     *            A Document representing a GetFeature response
     *            (wfs:FeatureCollection).
     * @param featureType
     *            The qualified name of a feature type; this may be null, in
     *            which case the actual type is ignored.
     * @param expectedCount
     *            The expected number of feature instances.
     */
    public static void assertFeatureCount(Document featureCollection, QName featureType, int expectedCount) {
        NodeList features;
        if (null != featureType) {
            features = featureCollection.getElementsByTagNameNS(featureType.getNamespaceURI(),
                    featureType.getLocalPart());
            Assert.assertEquals(features.getLength(), expectedCount,
                    String.format("Unexpected number of %s feature instances in response.", featureType));
        } else {
            features = featureCollection.getElementsByTagNameNS(WFS2.NS_URI, "member");
            Assert.assertEquals(features.getLength(), expectedCount,
                    "Unexpected number of feature members in response.");
        }
    }

    /**
     * Asserts that the given response message contains an OGC exception report.
     * The message body must contain an XML document that has a document element
     * with the following properties:
     *
     * <ul>
     * <li>[local name] = "ExceptionReport"</li>
     * <li>[namespace name] = "http://www.opengis.net/ows/1.1"</li>
     * </ul>
     *
     * @param rspEntity
     *            A Document node representing an HTTP response entity.
     * @param exceptionCode
     *            The expected OGC exception code.
     * @param locator
     *            A case-insensitive string value expected to occur in the
     *            locator attribute (e.g. a parameter name); the attribute value
     *            will be ignored if the argument is null or empty.
     */
    public static void assertExceptionReport(Document rspEntity, String exceptionCode, String locator) {
        String expr = String.format("//ows11:Exception[@exceptionCode = '%s']", exceptionCode);
        NodeList nodeList = null;
        try {
            nodeList = XMLUtils.evaluateXPath(rspEntity, expr, Collections.singletonMap(Namespaces.OWS, "ows11"));
        } catch (XPathExpressionException xpe) {
            // won't happen
        }
        Assert.assertTrue(nodeList.getLength() > 0, "Exception not found in response: " + expr);
        if (null != locator && !locator.isEmpty()) {
            Element exception = (Element) nodeList.item(0);
            String locatorValue = exception.getAttribute("locator").toLowerCase();
            Assert.assertTrue(locatorValue.contains(locator.toLowerCase()),
                    String.format("Expected locator attribute to contain '%s']", locator));
        }
    }

    /**
     * Asserts that the specified spatial reference occurs in the given XML
     * entity. In general the reference is conveyed by the srsName attribute
     * that may appear on any geometry element or gml:Envelope. All occurrences
     * must match.
     * 
     * @param entity
     *            A Document representing an XML entity such as a GML document
     *            or a WFS GetFeature response.
     * @param crsId
     *            A CRS identifier (an absolute URI value).
     */
    public static void assertSpatialReference(Document entity, String crsId) {
        NodeList srsNameNodes = null;
        try {
            srsNameNodes = XMLUtils.evaluateXPath(entity, "//*/@srsName", null);
        } catch (XPathExpressionException e) {
            throw new AssertionError(e.getMessage());
        }
        for (int i = 0; i < srsNameNodes.getLength(); i++) {
            Attr attr = (Attr) srsNameNodes.item(i);
            Assert.assertEquals(attr.getValue(), crsId, String.format("Unexpected @srsName value on element %s",
                    new QName(attr.getNamespaceURI(), attr.getLocalName())));
        }
    }

    /**
     * Asserts that the given response entity contains at least one feature
     * instance of the specified type.
     * 
     * @param entity
     *            A Document representing a GetFeature response entity
     *            (wfs:FeatureCollection).
     * @param featureType
     *            A QName that identifies the expected feature type; if null,
     *            any type is acceptable.
     */
    public static void assertResultSetNotEmpty(Document entity, QName featureType) {
        NodeList features;
        if (null != featureType) {
            features = entity.getElementsByTagNameNS(featureType.getNamespaceURI(), featureType.getLocalPart());
        } else {
            features = entity.getElementsByTagNameNS(WFS2.NS_URI, "member");
        }
        Assert.assertTrue(features.getLength() > 0,
                String.format("Expected one or more feature instances in response (type: %s).",
                        (null != featureType) ? featureType : "any"));
    }
}
