package org.opengis.cite.iso19142.simple;

import com.sun.jersey.api.client.ClientResponse;
import java.net.URI;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.validation.ValidationErrorHandler;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tests the service response to a DescribeStoredQueries request. This operation
 * provides a detailed description of each stored query that a server offers.
 * 
 * @see "ISO 19142:2010, cl. 14.2: Defining stored queries"
 * @see "ISO 19142:2010, cl. 14.4: DescribeStoredQueries operations [sic]"
 */
public class DescribeStoredQueriesTests extends BaseFixture {

    private Schema wfsSchema;

    /**
     * Retrieves the (pre-compiled) WFS schema from the suite fixture and builds
     * a DOM Document node representing the request entity.
     * 
     * @param testContext
     *            The test (group) context.
     */
    @BeforeClass
    public void setupClassFixture(ITestContext testContext) {
        this.wfsSchema = (Schema) testContext.getSuite().getAttribute(
                SuiteAttribute.WFS_SCHEMA.getName());
        Assert.assertNotNull(this.wfsSchema,
                "WFS schema not found in suite fixture.");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            this.reqEntity = builder.parse(getClass().getResourceAsStream(
                    "DescribeStoredQueries.xml"));
        } catch (Exception e) {
            TestSuiteLogger.log(Level.WARNING,
                    "Failed to parse request entity from classpath", e);
        }
    }

    @BeforeMethod
    public void clearQueryIdentifiers() {
        removeAllQueryIdentifiers(this.reqEntity);
    }

    /**
     * If no stored query identifiers are supplied in the request then all
     * stored queries offered by a server shall be described (one or more).
     * 
     * @param binding
     *            The ProtocolBinding to use.
     * 
     * @see "ISO 19142:2010, cl. 14.4.2: XML encoding"
     * @see "ISO 19142:2010, Table 21: Keywords for DescribeStoredQueries KVP-encoding"
     */
    @Test(description = "See ISO 19142: 14.4.2, Table 21", dataProvider = "protocol-binding")
    public void describeAllStoredQueries(ProtocolBinding binding) {
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(
                this.wfsMetadata, WFS2.DESC_STORED_QUERIES, binding);
        ClientResponse rsp = wfsClient.submitRequest(new DOMSource(reqEntity),
                binding, endpoint);
        Assert.assertTrue(rsp.hasEntity(),
                ErrorMessage.get(ErrorMessageKeys.MISSING_XML_ENTITY));
        this.rspEntity = extractBodyAsDocument(rsp, binding);
        Validator validator = this.wfsSchema.newValidator();
        ValidationErrorHandler errHandler = new ValidationErrorHandler();
        validator.setErrorHandler(errHandler);
        try {
            validator.validate(new DOMSource(this.rspEntity, this.rspEntity
                    .getDocumentURI()));
        } catch (Exception ex) {
            // unlikely with DOM object and ErrorHandler set
            TestSuiteLogger.log(Level.WARNING,
                    "Failed to validate WFS capabilities document", ex);
        }
        Assert.assertFalse(errHandler.errorsDetected(), ErrorMessage.format(
                ErrorMessageKeys.NOT_SCHEMA_VALID, errHandler.getErrorCount(),
                errHandler.toString()));
        String xpath = "count(//wfs:StoredQueryDescription) > 0";
        ETSAssert.assertXPath(xpath, this.rspEntity.getDocumentElement(), null);
    }

    /**
     * A conforming service must implement at least the GetFeatureById stored
     * query, which has the identifier
     * {@value org.opengis.cite.iso19142.WFS2#QRY_GET_FEATURE_BY_ID}.
     * 
     * @param binding
     *            The ProtocolBinding to use.
     * 
     * @see "ISO 19142:2010, cl. 7.9.3.6: GetFeatureById stored query"
     */
    @Test(description = "See ISO 19142: 7.9.3.6", dataProvider = "protocol-binding")
    public void describeStoredQuery_GetFeatureById(ProtocolBinding binding) {
        addQueryIdentifier(this.reqEntity, WFS2.QRY_GET_FEATURE_BY_ID);
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(
                this.wfsMetadata, WFS2.DESC_STORED_QUERIES, binding);
        ClientResponse rsp = wfsClient.submitRequest(new DOMSource(reqEntity),
                binding, endpoint);
        Assert.assertTrue(rsp.hasEntity(),
                ErrorMessage.get(ErrorMessageKeys.MISSING_XML_ENTITY));
        this.rspEntity = extractBodyAsDocument(rsp, binding);
        String xpath = String.format("//wfs:StoredQueryDescription[@id='%s']",
                WFS2.QRY_GET_FEATURE_BY_ID);
        ETSAssert.assertXPath(xpath, this.rspEntity.getDocumentElement(), null);
    }

    /**
     * Removes all wfs:StoredQueryId elements from the request entity.
     * 
     * @param reqEntity
     *            A Document with wfs:DescribeStoredQueries as the document
     *            element.
     */
    void removeAllQueryIdentifiers(Document reqEntity) {
        Element docElem = reqEntity.getDocumentElement();
        NodeList children = docElem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            docElem.removeChild(children.item(i));
        }
    }

    /**
     * Adds a wfs:StoredQueryId element with the given text value.
     * 
     * @param request
     *            A Document with wfs:DescribeStoredQueries as the document
     *            element.
     * @param queryId
     *            A URI value that identifies a stored query.
     */
    void addQueryIdentifier(Document request, String queryId) {
        Element docElem = reqEntity.getDocumentElement();
        Element storedQueryId = request.createElementNS(Namespaces.WFS,
                WFS2.STORED_QRY_ID_ELEM);
        storedQueryId.setTextContent(queryId);
        docElem.appendChild(storedQueryId);
    }
}
