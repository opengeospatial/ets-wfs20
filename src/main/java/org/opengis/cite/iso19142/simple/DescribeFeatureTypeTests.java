package org.opengis.cite.iso19142.simple;

import java.net.URI;
import java.util.logging.Level;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMSource;

import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.iso19142.util.ValidationUtils;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.opengis.cite.validation.SchematronValidator;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the service response to a DescribeFeatureType request. No particular
 * HTTP method binding is mandated. A conforming service must be able to provide
 * a GML application schema, although alternative schema languages are
 * permitted.
 * 
 * @see "ISO 19142:2010, cl. 9: DescribeFeatureType operation"
 */
public class DescribeFeatureTypeTests extends BaseFixture {

    /**
     * Builds a DOM Document node representing the request entity
     * (/wfs:DescribeFeatureType).
     */
    @BeforeClass
    public void buildRequestEntity() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            this.reqEntity = builder.parse(getClass().getResourceAsStream("DescribeFeatureType.xml"));
        } catch (Exception e) {
            TestSuiteLogger.log(Level.WARNING, "Failed to parse request entity from classpath", e);
        }
    }

    @BeforeMethod
    public void clearTypeNames() {
        removeAllTypeNames(this.reqEntity);
    }

    /**
     * Removes all child wfs:TypeName elements from the request entity.
     * 
     * @param reqEntity
     *            The request entity (/wfs:DescribeFeatureType).
     */
    void removeAllTypeNames(Document reqEntity) {
        Element docElem = reqEntity.getDocumentElement();
        NodeList children = docElem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            docElem.removeChild(children.item(i));
        }
    }

    /**
     * Adds a wfs:TypeName child element to a wfs:DescribeFeatureType entity. A
     * suitable namespace binding will be added to the document element if
     * necessary.
     * 
     * @param request
     *            The request entity (wfs:DescribeFeatureType).
     * @param qName
     *            The qualified name of the feature type.
     */
    void addFeatureType(Document request, QName qName) {
        Element docElem = request.getDocumentElement();
        Element typeName = request.createElementNS(Namespaces.WFS, WFS2.TYPENAME_ELEM);
        String nsPrefix = docElem.lookupPrefix(qName.getNamespaceURI());
        if (null == nsPrefix) {
            nsPrefix = "ns" + Integer.toString((int) (Math.random() * 100));
        }
        typeName.setTextContent(nsPrefix + ":" + qName.getLocalPart());
        typeName.setPrefix("wfs");
        docElem.appendChild(typeName);
        docElem.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + nsPrefix, qName.getNamespaceURI());
    }

    /**
     * If the typeNames parameter is omitted, the complete application schema(s)
     * supported by the server shall be returned in response. By default, it
     * must be a GML application schema (XML Schema).
     * 
     * @param binding
     *            The ProtocolBinding to use.
     * 
     * @see "ISO 19142:2010, cl. 9.2.4.1: typeNames parameter"
     */
    @Test(description = "See ISO 19142: 9.2.4.1", dataProvider = "protocol-binding")
    public void describeAllFeatureTypes(ProtocolBinding binding) {
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.DESCRIBE_FEATURE_TYPE, binding);
        ClientResponse rsp = wfsClient.submitRequest(new DOMSource(reqEntity), binding, endpoint);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Assert.assertTrue(rsp.hasEntity(), ErrorMessage.get(ErrorMessageKeys.MISSING_XML_ENTITY));
        Element docElem = this.rspEntity.getDocumentElement();
        Assert.assertEquals(docElem.getLocalName(), "schema", "Document element has unexpected [local name].");
        // TODO: compile schema
    }

    /**
     * If the typeNames parameter specifies an unknown feature type, the
     * resulting exception report must indicate an "InvalidParameterValue"
     * error. The set of permissible values for the typeNames parameter is the
     * set of feature type names listed in the capabilities document.
     * 
     * @param binding
     *            The ProtocolBinding to use.
     * 
     * @see "ISO 19142:2010, cl. 8.3.4: FeatureTypeList section"
     * @see "ISO 19142:2010, cl. 9.2.4.1: typeNames parameter"
     * @see "OGC 06-121r3, cl. 8.3: exceptionCode parameter values"
     */
    @Test(description = "See ISO 19142: 8.3.4, 9.2.4.1", dataProvider = "protocol-binding")
    public void describeUnknownFeatureType(ProtocolBinding binding) {
        addFeatureType(this.reqEntity, new QName("http://example.org", "Unknown1.Type"));
        URI endpoint = ServiceMetadataUtils.getOperationEndpoint(this.wfsMetadata, WFS2.DESCRIBE_FEATURE_TYPE, binding);
        ClientResponse rsp = wfsClient.submitRequest(new DOMSource(this.reqEntity), binding, endpoint);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Assert.assertTrue(rsp.hasEntity(), ErrorMessage.get(ErrorMessageKeys.MISSING_XML_ENTITY));
        SchematronValidator validator = ValidationUtils.buildSchematronValidator("ExceptionReport.sch",
                "InvalidParameterValuePhase");
        Result result = validator.validate(new DOMSource(this.rspEntity), false);
        Assert.assertFalse(validator.ruleViolationsDetected(), ErrorMessage.format(ErrorMessageKeys.NOT_SCHEMA_VALID,
                validator.getRuleViolationCount(), XMLUtils.resultToString(result)));
    }
}
