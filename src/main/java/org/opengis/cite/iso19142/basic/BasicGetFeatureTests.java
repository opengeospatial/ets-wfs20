package org.opengis.cite.iso19142.basic;

import java.net.URI;
import java.util.Iterator;
import java.util.logging.Level;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.FeatureTypeInfo;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.iso19142.util.ValidationUtils;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the response to a GetFeature request that returns a selection of
 * features matching specified criteria. The request must include one or more
 * stored query (wfs:StoredQuery) or ad hoc query (wfs:Query) expressions. An ad
 * hoc query element may contain projection clauses (wfs:PropertyName), a
 * selection clause (fes:Filter), or a sorting clause (fes:SortBy).
 * 
 * A successful response entity must include a schema reference (using the
 * xsi:schemaLocation attribute) that is sufficient to validate the response.
 * 
 * @see "ISO 19142:2010, cl. 11: GetFeature operation"
 * @see "ISO 19142:2010, cl. 7.8:  Use of the schemaLocation attribute"
 * @see "ISO 19143:2010, Geographic information -- Filter encoding"
 */
public class BasicGetFeatureTests extends BaseFixture {

	private static final QName FEATURE_COLL = new QName(Namespaces.WFS,
			WFS2.FEATURE_COLLECTION);
	Validator hintsValidator;

	/**
	 * Creates a special XML Schema validator that uses schema location hints
	 * specified in an XML instance document. Beware that this can introduce a
	 * vulnerability to denial-of-service attacks, even though local copies of
	 * standard schemas will be used if possible.
	 */
	@BeforeClass
	public void buildValidator() {
		SchemaFactory factory = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			Schema schema = factory.newSchema();
			this.hintsValidator = schema.newValidator();
			LSResourceResolver resolver = ValidationUtils
					.createSchemaResolver(Namespaces.XSD);
			this.hintsValidator.setResourceResolver(resolver);
		} catch (SAXException e) {
			// very unlikely to occur with no schema to process
			TestSuiteLogger
					.log(Level.WARNING,
							"Failed to build XML Schema Validator that heeds location hints.",
							e);
		}
	}

	/**
	 * Builds a DOM Document node representing the entity body for a GetFeature
	 * request. A minimal XML representation is read from the classpath
	 * ("util/GetFeature-Minimal.xml").
	 */
	@BeforeMethod
	public void buildRequestEntity() {
		this.reqEntity = WFSMessage.createRequestEntity("GetFeature-Minimal",
				this.wfsVersion);
		this.rspEntity = null;
	}

	/**
	 * Resets the validator to its original configuration.
	 */
	@AfterMethod
	public void resetValidator() {
		this.hintsValidator.reset();
	}

	/**
	 * [{@code Test}] Submits a minimal GetFeature request (without a filter
	 * predicate) for feature types listed in the WFS the capabilities document.
	 * The test is run for all supported protocol bindings and feature types.
	 * The response entity (wfs:FeatureCollection) must be schema-valid and
	 * contain only instances of the requested type as members.
	 * 
	 * @param binding
	 *            A supported message binding.
	 * @param featureType
	 *            A QName representing the qualified name of some feature type.
	 */
	@Test(description = "See ISO 19142: 11.2.2, 11.2.3", dataProvider = "all-protocols-featureTypes")
	public void getFeaturesByType(ProtocolBinding binding, QName featureType) {
		WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
		URI endpoint = ServiceMetadataUtils.getOperationEndpoint(
				this.wfsMetadata, WFS2.GET_FEATURE, binding);
		ClientResponse rsp = wfsClient.submitRequest(new DOMSource(reqEntity),
				binding, endpoint);
		this.rspEntity = extractBodyAsDocument(rsp);
		Assert.assertEquals(rsp.getStatus(),
				ClientResponse.Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		ETSAssert.assertQualifiedName(rspEntity.getDocumentElement(),
				FEATURE_COLL);
		ETSAssert.assertSchemaValid(this.hintsValidator, new DOMSource(
				rspEntity.getDocumentElement(), rspEntity.getDocumentURI()));
	}

	/**
	 * [{@code Test}] Submits a request for geometry representations in some
	 * other (non-default) CRS that is supported by the IUT, as indicated by the
	 * value of the {@literal wfs:Query/@srsName} attribute or srsName query
	 * parameter. The test is skipped if no alternatives are offered for any
	 * available feature type.
	 * 
	 * @see "OGC 09-025r2, 7.9.2.4.4: srsName parameter"
	 */
	@Test(description = "See OGC 09-025r2: 7.9.2.4.4")
	public void getFeatureInOtherCRS() {
		String otherCRSId = null;
		Iterator<QName> itr = this.featureTypes.iterator();
		QName featureType;
		do {
			featureType = itr.next();
			FeatureTypeInfo typeInfo = this.featureInfo.get(featureType);
			if (typeInfo.isInstantiated()
					&& typeInfo.getSupportedCRSIdentifiers().size() > 1) {
				// skip first (default) CRS
				otherCRSId = typeInfo.getSupportedCRSIdentifiers().get(1);
				break;
			}
		} while (itr.hasNext());
		if (null == otherCRSId) {
			throw new SkipException(
					"No alternative (non-default) CRS supported for any feature type with data.");
		}
		WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
		Element qry = (Element) this.reqEntity.getElementsByTagNameNS(
				WFS2.NS_URI, WFS2.QUERY_ELEM).item(0);
		qry.setAttribute(WFS2.SRSNAME_PARAM, otherCRSId);
		ClientResponse rsp = wfsClient.submitRequest(this.reqEntity,
				ProtocolBinding.ANY);
		Assert.assertEquals(rsp.getStatus(),
				ClientResponse.Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		this.rspEntity = extractBodyAsDocument(rsp);
		ETSAssert.assertSpatialReference(this.rspEntity, otherCRSId);
	}

	/**
	 * [{@code Test}] Submits a request for geometry representations in a
	 * non-existent CRS, as indicated by the value of the
	 * {@literal wfs:Query/@srsName} attribute or srsName parameter. An
	 * exception is expected in response, with status code 400 and OGC exception
	 * code "InvalidParameterValue".
	 * 
	 * @see "OGC 09-025r2, A.2.22.1.4: srsName parameter"
	 */
	@Test(description = "See OGC 09-025r2: A.2.22.1.4")
	public void getFeatureInUnsupportedCRS() {
		String crsId = (this.wfsVersion.equals(WFS2.V2_0_0)) ? "urn:ogc:def:crs:EPSG::32690"
				: "http://www.opengis.net/def/crs/EPSG/0/32690";
		QName featureType = getFeatureTypeWithInstanceData();
		WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
		Element qry = (Element) this.reqEntity.getElementsByTagNameNS(
				WFS2.NS_URI, WFS2.QUERY_ELEM).item(0);
		qry.setAttribute(WFS2.SRSNAME_PARAM, crsId);
		ClientResponse rsp = wfsClient.submitRequest(this.reqEntity,
				ProtocolBinding.ANY);
		Assert.assertEquals(rsp.getStatus(),
				ClientResponse.Status.BAD_REQUEST.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		this.rspEntity = extractBodyAsDocument(rsp);
		ETSAssert.assertExceptionReport(this.rspEntity,
				"InvalidParameterValue", "SRSNAME");
	}

	/**
	 * Sets the availability status of instances of a given feature type
	 * according to the content of a GetFeature response entity. Availability is
	 * set to {@code true} if the response entity contains at least one
	 * instance.
	 * 
	 * @param featureInfo
	 *            A FeatureTypeInfo object that provides information about a
	 *            feature type.
	 * @param entity
	 *            A GetFeature response entity (wfs:FeatureCollection).
	 */
	void setFeatureAvailability(FeatureTypeInfo featureInfo, Document entity) {
		QName typeName = featureInfo.getTypeName();
		NodeList features = entity.getElementsByTagNameNS(
				typeName.getNamespaceURI(), typeName.getLocalPart());
		featureInfo.setInstantiated(features.getLength() > 0);
	}

	/**
	 * Gets the name of a feature type for which instances exist.
	 * 
	 * @return The qualified name of a feature type, or <code>null</code> if no
	 *         data are available.
	 */
	QName getFeatureTypeWithInstanceData() {
		Iterator<QName> itr = this.featureTypes.iterator();
		QName featureType = null;
		do {
			featureType = itr.next();
			FeatureTypeInfo typeInfo = this.featureInfo.get(featureType);
			if (typeInfo.isInstantiated()) {
				break;
			}
		} while (itr.hasNext());
		return featureType;
	}
}
