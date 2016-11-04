package org.opengis.cite.iso19142;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.opengis.cite.iso19142.util.DataSampler;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.WFSClient;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.jersey.api.client.ClientResponse;

/**
 * A supporting base class that provides common configuration methods and data
 * providers. The configuration methods are invoked before any that may be
 * defined in a subclass.
 */
public class BaseFixture {

    /** Root ETS package. */
    protected static final String ETS_PKG = "/org/opengis/cite/iso19142";
    /** Maximum length of response (string) added as result attribute. */
    private static final int MAX_RSP_ATTR_LENGTH = 4096;
    private static final String REQ_ATTR = "request";
    private static final String RSP_ATTR = "response";
    /** A DOM document containing service metadata (OGC capabilities). */
    protected Document wfsMetadata;
    /**
     * A client component for interacting with a WFS.
     */
    protected WFSClient wfsClient;
    protected Set<ProtocolBinding> supportedBindings;
    protected List<QName> featureTypes;
    protected Map<QName, FeatureTypeInfo> featureInfo;
    /** A DOM parser. */
    protected DocumentBuilder docBuilder;
    protected static final String TNS_PREFIX = "tns";
    /** A Document representing the content of the request message. */
    protected Document reqEntity;
    /** A Document representing the content of the response message. */
    protected Document rspEntity;
    /** Highest version supported by the IUT. */
    protected String wfsVersion;

    public void setWfsClient(WFSClient wfsClient) {
        this.wfsClient = wfsClient;
    }

    /**
     * Sets up the base fixture. The service metadata document is obtained from
     * the ISuite context. The suite attribute
     * {@link SuiteAttribute#TEST_SUBJECT testSubject} should yield a DOM
     * Document node having {http://www.opengis.net/wfs/2.0}WFS_Capabilities as
     * the document element.
     * 
     * The set of implemented protocol bindings is determined from the service
     * metadata by checking the values of the following service constraints:
     * 
     * <ul>
     * <li>KVPEncoding</li>
     * <li>XMLEncoding</li>
     * <li>SOAPEncoding</li>
     * </ul>
     * 
     * @param testContext
     *            The test (set) context.
     */
    @BeforeClass(alwaysRun = true)
    @SuppressWarnings("unchecked")
    public void initBaseFixture(ITestContext testContext) {
        if (null != this.featureTypes && !this.featureTypes.isEmpty()) {
            return;
        }
        this.wfsVersion = (String) testContext.getSuite().getAttribute(SuiteAttribute.WFS_VERSION.getName());
        this.wfsMetadata = (Document) testContext.getSuite().getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        this.wfsClient = new WFSClient(this.wfsMetadata);
        Set<ProtocolBinding> protoBindings = ServiceMetadataUtils.getGlobalBindings(wfsMetadata);
        if (protoBindings.isEmpty()) {
            TestSuiteLogger.log(Level.WARNING, "No protocol bindings found in capabilities document.");
        }
        this.supportedBindings = protoBindings;
        this.featureTypes = ServiceMetadataUtils.getFeatureTypes(wfsMetadata);
        this.featureInfo = (Map<QName, FeatureTypeInfo>) testContext.getSuite()
                .getAttribute(SuiteAttribute.FEATURE_INFO.getName());
    }

    /**
     * Initializes the (namespace-aware) DOM parser.
     */
    @BeforeClass
    public void initParser() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            this.docBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            TestSuiteLogger.log(Level.WARNING, "Failed to create DOM parser", e);
        }
    }

    /**
     * Augments the test result with supplementary attributes in the event that
     * a test method failed. The "request" attribute contains a String
     * representing the request entity (POST method) or query component (GET
     * method). The "response" attribute contains the content of the response
     * entity.
     * 
     * @param result
     *            A description of the test result.
     */
    @AfterMethod
    public void addAttributesOnTestFailure(ITestResult result) {
        if (result.getStatus() != ITestResult.FAILURE) {
            return;
        }
        if (null != this.reqEntity) {
            String request = null;
            Object[] params = result.getParameters();
            if (WFSMessage.containsGetProtocolBinding(params)) {
                request = WFSMessage.transformEntityToKVP(new DOMSource(this.reqEntity));
            } else {
                request = XMLUtils.writeNodeToString(this.reqEntity);
            }
            result.setAttribute(REQ_ATTR, request);
        }
        if (null != this.rspEntity) {
            StringBuilder response = new StringBuilder(XMLUtils.writeNodeToString(this.rspEntity));
            if (response.length() > MAX_RSP_ATTR_LENGTH) {
                response.delete(MAX_RSP_ATTR_LENGTH, response.length());
            }
            result.setAttribute(RSP_ATTR, response.toString());
        }
    }

    /**
     * A DataProvider that supplies the set of application protocol bindings
     * supported by the SUT.
     * 
     * @param testContext
     *            Supplies details about the test run.
     *
     * @return An Object[][] array containing a ProtocolBinding object in each
     *         row (first dimension).
     */
    @DataProvider(name = "protocol-binding")
    public Object[][] getProtocolBindings(ITestContext testContext) {
        if (null == this.supportedBindings) {
            // BeforeClass method may not have been called yet
            initBaseFixture(testContext);
        }
        Object[][] bindings = new Object[this.supportedBindings.size()][];
        Iterator<ProtocolBinding> itr = this.supportedBindings.iterator();
        for (int i = 0; i < bindings.length; i++) {
            bindings[i] = new Object[] { itr.next() };
        }
        return bindings;
    }

    /**
     * A DataProvider that supplies the complete set of feature types recognized
     * by the SUT.
     * 
     * @return An Object[][] array containing a QName object in each row (first
     *         dimension).
     */
    @DataProvider(name = "feature-types")
    public Object[][] getFeatureTypes() {
        Object[][] typeNames = new Object[this.featureTypes.size()][];
        Iterator<QName> itr = this.featureTypes.iterator();
        for (int i = 0; i < typeNames.length; i++) {
            typeNames[i] = new Object[] { itr.next() };
        }
        return typeNames;
    }

    /**
     * A DataProvider that supplies feature type names for which instances
     * exist.
     * 
     * @return An Iterator over an array containing a QName object representing
     *         the qualified name of a feature type.
     */
    @DataProvider(name = "instantiated-feature-types")
    public Iterator<Object[]> getInstantiatedFeatureTypes() {
        List<Object[]> data = new ArrayList<Object[]>();
        for (QName typeName : this.featureTypes) {
            if (this.featureInfo.get(typeName).isInstantiated()) {
                data.add(new Object[] { typeName });
            }
        }
        return data.iterator();
    }

    /**
     * A DataProvider that supplies parameters specifying a supported protocol
     * binding and a feature type. The resulting set is given by the Cartesian
     * product of the sets {bindings} x {featureTypes}; its cardinality is equal
     * to the product of the cardinalities of the two input sets.
     * 
     * @return An {@literal Iterator<Object[]>} over the set of
     *         (ProtocolBinding, QName) pairs.
     */
    @DataProvider(name = "all-protocols-featureTypes")
    public Iterator<Object[]> allProtocolsAndFeatureTypes() {
        List<Object[]> params = new ArrayList<Object[]>();
        for (ProtocolBinding binding : supportedBindings) {
            for (QName typeName : featureTypes) {
                params.add(new Object[] { binding, typeName });
            }
        }
        return params.iterator();
    }

    /**
     * A DataProvider that supplies a collection of parameter tuples (a product
     * set) where each tuple has two elements:
     * <ol>
     * <li>ProtocolBinding - a supported request binding</li>
     * <li>QName - the name of a feature type for which data are available</li>
     * </ol>
     * 
     * @param testContext
     *            The ITestContext object for the test run.
     * @return {@literal Iterator<Object[]>} An iterator over a collection of
     *         parameter tuples (ProtocolBinding, QName).
     */
    @DataProvider(name = "protocol-featureType")
    public Iterator<Object[]> bindingAndAvailFeatureTypeProductSet(ITestContext testContext) {
        ISuite suite = testContext.getSuite();
        Document wfsMetadata = (Document) suite.getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        if (null == wfsMetadata) {
            throw new NullPointerException("Service description not found in ITestContext");
        }
        Set<ProtocolBinding> globalBindings = ServiceMetadataUtils.getGlobalBindings(wfsMetadata);
        DataSampler sampler = (DataSampler) suite.getAttribute(SuiteAttribute.SAMPLER.getName());
        Map<QName, FeatureTypeInfo> featureInfo = sampler.getFeatureTypeInfo();
        List<Object[]> paramList = new ArrayList<Object[]>();
        for (ProtocolBinding binding : globalBindings) {
            for (FeatureTypeInfo typeInfo : featureInfo.values()) {
                if (typeInfo.isInstantiated()) {
                    Object[] tuple = { binding, typeInfo.getTypeName() };
                    paramList.add(tuple);
                }
            }
        }
        return paramList.iterator();
    }

    /**
     * Extracts the body of the response message as a DOM Document node. For a
     * SOAP response this will contain the content of the SOAP body element.
     * 
     * @param rsp
     *            A ClientResponse representing an HTTP response message.
     * @return A Document representing the response entity, or {@code null} if
     *         it could not be parsed.
     */
    protected Document extractBodyAsDocument(ClientResponse rsp) {
        Document entity = rsp.getEntity(Document.class);
        try {
            Element soapBody = (Element) XMLUtils.evaluateXPath(entity, "//soap11:Body/*[1] | //soap:Body/*[1]", null,
                    XPathConstants.NODE);
            if (null != soapBody) {
                entity.replaceChild(soapBody, entity.getDocumentElement());
            }
        } catch (XPathExpressionException xpe) {
            throw new RuntimeException(xpe);
        }
        return entity;
    }
}
