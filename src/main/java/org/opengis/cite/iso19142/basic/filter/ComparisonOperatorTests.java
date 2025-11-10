package org.opengis.cite.iso19142.basic.filter;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSTypeDefinition;
import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.FES2;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.AppSchemaUtils;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.bind.DatatypeConverter;

/**
 * Tests the response to a GetFeature request that includes a filter predicate containing
 * one of the following comparison operators:
 * <ul>
 * <li>PropertyIsLessThan</li>
 * <li>PropertyIsGreaterThan</li>
 * <li>PropertyIsLessThanOrEqualTo</li>
 * <li>PropertyIsGreaterThanOrEqualTo</li>
 * </ul>
 * <p>
 * These operators compare the value of a simple property against some specified value;
 * they can be applied to numeric, temporal, and text data types (although lexicographic
 * order may depend on the collation rules). If a property has multiple values, the
 * {@code matchAction} parameter affects the scope of the comparison ("All", "Any",
 * "One").
 * </p>
 *
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li>ISO 19142:2010, cl. A.1.2: Basic WFS</li>
 * <li>ISO 19143:2010, cl. 7.7: Comparison operators</li>
 * <li>ISO 19143:2010, cl. A.5: Test cases for minimum standard filter</li>
 * </ul>
 */
public class ComparisonOperatorTests extends QueryFilterFixture {

	private static String MATCH_ALL = "All";

	private static String MATCH_ANY = "Any";

	/**
	 * [{@code Test}] Submits a GetFeature request containing a {@code PropertyIsLessThan}
	 * predicate that applies to some simple (numeric or temporal) feature property. The
	 * response entity must include only feature instances that satisfy the predicate; if
	 * multiple values exist, at least one must match (matchAction="Any").
	 * @param binding The ProtocolBinding to use for this request.
	 * @param featureType A QName representing the qualified name of some feature type.
	 */
	@Test(description = "See ISO 19143: 7.7", dataProvider = "protocol-featureType")
	public void propertyIsLessThan_matchAny(ProtocolBinding binding, QName featureType) {
		Set<XSTypeDefinition> dataTypes = getNumericDataTypes(getModel());
		dataTypes.addAll(AppSchemaUtils.getSimpleTemporalDataTypes(getModel()));
		Map<XSElementDeclaration, String[]> propRangeMap = findFeaturePropertyValue(getModel(), featureType, dataTypes);
		if (propRangeMap.isEmpty()) {
			throw new SkipException("No numeric or temporal property values found for " + featureType);
		}
		Entry<XSElementDeclaration, String[]> propRange = propRangeMap.entrySet().iterator().next();
		String propValue = propRange.getValue()[1]; // use max value
		XSElementDeclaration propDecl = propRange.getKey();
		QName propName = new QName(propDecl.getNamespace(), propDecl.getName());
		WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
		addComparisonPredicate(this.reqEntity, FES2.LESS_THAN, propName, propValue, true, MATCH_ANY);
		Response rsp = wfsClient.submitRequest(reqEntity, binding);
		this.rspEntity = extractBodyAsDocument(rsp);
		Assert.assertEquals(rsp.getStatus(), Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		NodeList features = this.rspEntity.getElementsByTagNameNS(featureType.getNamespaceURI(),
				featureType.getLocalPart());
		// Add constructor functions for property type (XML Schema datatype)
		QName dataType = AppSchemaUtils.getBuiltInDatatype(propDecl);
		String propTypeName = dataType.getLocalPart();
		String xpath = String.format("xs:%s(ns1:%s) lt xs:%s('%s')", propTypeName, propName.getLocalPart(),
				propTypeName, propValue);
		Map<String, String> nsBindings = new HashMap<String, String>();
		nsBindings.put(propName.getNamespaceURI(), "ns1");
		for (int i = 0; i < features.getLength(); i++) {
			ETSAssert.assertXPath2(xpath, new DOMSource(features.item(i)), nsBindings);
		}
	}

	/**
	 * [{@code Test}] Submits a GetFeature request containing a {@code PropertyIsLessThan}
	 * predicate that applies to some simple (numeric or temporal) feature property. The
	 * response entity must include only feature instances that satisfy the predicate; if
	 * multiple values exist, all of them must match (matchAction="All").
	 * @param binding The ProtocolBinding to use for this request.
	 * @param featureType A QName representing the qualified name of some feature type.
	 */
	@Test(description = "See ISO 19143: 7.7.3.3", dataProvider = "protocol-featureType")
	public void propertyIsLessThan_matchAll(ProtocolBinding binding, QName featureType) {
		Set<XSTypeDefinition> dataTypes = getNumericDataTypes(getModel());
		dataTypes.addAll(AppSchemaUtils.getSimpleTemporalDataTypes(getModel()));
		Map<XSElementDeclaration, String[]> propRangeMap = findFeaturePropertyValue(getModel(), featureType, dataTypes);
		if (propRangeMap.isEmpty()) {
			throw new SkipException("No numeric or temporal property values found for " + featureType);
		}
		Entry<XSElementDeclaration, String[]> propRange = propRangeMap.entrySet().iterator().next();
		String propValue = propRange.getValue()[1]; // use max value
		XSElementDeclaration propDecl = propRange.getKey();
		QName propName = new QName(propDecl.getNamespace(), propDecl.getName());
		WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
		addComparisonPredicate(this.reqEntity, FES2.LESS_THAN, propName, propValue, true, MATCH_ALL);
		Response rsp = wfsClient.submitRequest(reqEntity, binding);
		this.rspEntity = extractBodyAsDocument(rsp);
		Assert.assertEquals(rsp.getStatus(), Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		NodeList features = this.rspEntity.getElementsByTagNameNS(featureType.getNamespaceURI(),
				featureType.getLocalPart());
		// Add constructor functions for property type (XML Schema datatype)
		QName dataType = AppSchemaUtils.getBuiltInDatatype(propDecl);
		String propTypeName = dataType.getLocalPart();
		String xpath = String.format("xs:%s(ns1:%s) lt xs:%s('%s')", propTypeName, propName.getLocalPart(),
				propTypeName, propValue);
		Map<String, String> nsBindings = new HashMap<String, String>();
		nsBindings.put(propName.getNamespaceURI(), "ns1");
		for (int i = 0; i < features.getLength(); i++) {
			ETSAssert.assertXPath2(xpath, new DOMSource(features.item(i)), nsBindings);
		}
	}

	/**
	 * [{@code Test}] Submits a GetFeature request containing a
	 * {@code PropertyIsGreaterThan} predicate that applies to some simple (numeric or
	 * temporal) feature property. The response entity must include only feature instances
	 * that satisfy the predicate; if multiple values exist, at least one must match
	 * (matchAction="Any").
	 * @param binding The ProtocolBinding to use for this request.
	 * @param featureType A QName representing the qualified name of some feature type.
	 */
	@Test(description = "See ISO 19143: 7.7.3.1", dataProvider = "protocol-featureType")
	public void propertyIsGreaterThan_matchAny(ProtocolBinding binding, QName featureType) {
		Set<XSTypeDefinition> dataTypes = getNumericDataTypes(getModel());
		dataTypes.addAll(AppSchemaUtils.getSimpleTemporalDataTypes(getModel()));
		Map<XSElementDeclaration, String[]> propRangeMap = findFeaturePropertyValue(getModel(), featureType, dataTypes);
		if (propRangeMap.isEmpty()) {
			throw new SkipException("No numeric or temporal property values found for " + featureType);
		}
		Entry<XSElementDeclaration, String[]> propRange = propRangeMap.entrySet().iterator().next();
		String propValue = propRange.getValue()[0]; // use min value
		XSElementDeclaration propDecl = propRange.getKey();
		QName propName = new QName(propDecl.getNamespace(), propDecl.getName());
		WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
		addComparisonPredicate(this.reqEntity, FES2.GREATER_THAN, propName, propValue, true, MATCH_ANY);
		Response rsp = wfsClient.submitRequest(reqEntity, binding);
		this.rspEntity = extractBodyAsDocument(rsp);
		Assert.assertEquals(rsp.getStatus(), Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		NodeList features = this.rspEntity.getElementsByTagNameNS(featureType.getNamespaceURI(),
				featureType.getLocalPart());
		// Add constructor functions for property type (XML Schema datatype)
		QName dataType = AppSchemaUtils.getBuiltInDatatype(propDecl);
		String propTypeName = dataType.getLocalPart();
		String xpath = String.format("xs:%s(ns1:%s) gt xs:%s('%s')", propTypeName, propName.getLocalPart(),
				propTypeName, propValue);
		Map<String, String> nsBindings = new HashMap<String, String>();
		nsBindings.put(propName.getNamespaceURI(), "ns1");
		for (int i = 0; i < features.getLength(); i++) {
			ETSAssert.assertXPath2(xpath, new DOMSource(features.item(i)), nsBindings);
		}
	}

	/**
	 * [{@code Test}] Submits a GetFeature request containing a
	 * {@code PropertyIsGreaterThanOrEqualTo} predicate that applies to some simple
	 * (numeric or temporal) feature property. The response entity must include only
	 * feature instances that satisfy the predicate; if multiple values exist, at least
	 * one must match (matchAction="Any").
	 * @param binding The ProtocolBinding to use for this request.
	 * @param featureType A QName representing the qualified name of some feature type.
	 */
	@Test(description = "See ISO 19143: 7.7.3.1", dataProvider = "protocol-featureType")
	public void propertyIsGreaterThanEqualTo_matchAny(ProtocolBinding binding, QName featureType) {
		Set<XSTypeDefinition> dataTypes = getNumericDataTypes(getModel());
		dataTypes.addAll(AppSchemaUtils.getSimpleTemporalDataTypes(getModel()));
		Map<XSElementDeclaration, String[]> propRangeMap = findFeaturePropertyValue(getModel(), featureType, dataTypes);
		if (propRangeMap.isEmpty()) {
			throw new SkipException("No numeric or temporal property values found for " + featureType);
		}
		Entry<XSElementDeclaration, String[]> propRange = propRangeMap.entrySet().iterator().next();
		String propValue = propRange.getValue()[0]; // use min value
		XSElementDeclaration propDecl = propRange.getKey();
		QName propName = new QName(propDecl.getNamespace(), propDecl.getName());
		WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
		addComparisonPredicate(this.reqEntity, FES2.GREATER_THAN_OR_EQUAL, propName, propValue, true, MATCH_ANY);
		Response rsp = wfsClient.submitRequest(reqEntity, binding);
		this.rspEntity = extractBodyAsDocument(rsp);
		Assert.assertEquals(rsp.getStatus(), Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		NodeList features = this.rspEntity.getElementsByTagNameNS(featureType.getNamespaceURI(),
				featureType.getLocalPart());
		// Add constructor functions for property type (XML Schema datatype)
		QName dataType = AppSchemaUtils.getBuiltInDatatype(propDecl);
		String propTypeName = dataType.getLocalPart();
		String xpath = String.format("xs:%s(ns1:%s) ge xs:%s('%s')", propTypeName, propName.getLocalPart(),
				propTypeName, propValue);
		Map<String, String> nsBindings = new HashMap<String, String>();
		nsBindings.put(propName.getNamespaceURI(), "ns1");
		for (int i = 0; i < features.getLength(); i++) {
			ETSAssert.assertXPath2(xpath, new DOMSource(features.item(i)), nsBindings);
		}
	}

	/**
	 * [{@code Test}] Submits a GetFeature request containing a
	 * {@code PropertyIsLessThanOrEqualTo} predicate that applies to some simple (numeric
	 * or temporal) feature property. The response entity must include only feature
	 * instances that satisfy the predicate; if multiple values exist, at least one must
	 * match (matchAction="Any").
	 * @param binding The ProtocolBinding to use for this request.
	 * @param featureType A QName representing the qualified name of some feature type.
	 */

	@Test(description = "See ISO 19143: 7.7.3.1", dataProvider = "protocol-featureType")
	public void propertyIsLessThanEqualTo_matchAny(ProtocolBinding binding, QName featureType) {
		Set<XSTypeDefinition> dataTypes = getNumericDataTypes(getModel());
		dataTypes.addAll(AppSchemaUtils.getSimpleTemporalDataTypes(getModel()));
		Map<XSElementDeclaration, String[]> propRangeMap = findFeaturePropertyValue(getModel(), featureType, dataTypes);
		if (propRangeMap.isEmpty()) {
			throw new SkipException("No numeric or temporal property values found for " + featureType);
		}
		Entry<XSElementDeclaration, String[]> propRange = propRangeMap.entrySet().iterator().next();
		String propValue = propRange.getValue()[1]; // use max value
		XSElementDeclaration propDecl = propRange.getKey();
		QName propName = new QName(propDecl.getNamespace(), propDecl.getName());
		WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
		addComparisonPredicate(this.reqEntity, FES2.LESS_THAN_OR_EQUAL, propName, propValue, true, MATCH_ANY);
		Response rsp = wfsClient.submitRequest(reqEntity, binding);
		this.rspEntity = extractBodyAsDocument(rsp);
		Assert.assertEquals(rsp.getStatus(), Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		NodeList features = this.rspEntity.getElementsByTagNameNS(featureType.getNamespaceURI(),
				featureType.getLocalPart());
		// Add constructor functions for property type (XML Schema datatype)
		QName dataType = AppSchemaUtils.getBuiltInDatatype(propDecl);
		String propTypeName = dataType.getLocalPart();
		String xpath = String.format("xs:%s(ns1:%s) le xs:%s('%s')", propTypeName, propName.getLocalPart(),
				propTypeName, propValue);
		Map<String, String> nsBindings = new HashMap<String, String>();
		nsBindings.put(propName.getNamespaceURI(), "ns1");
		for (int i = 0; i < features.getLength(); i++) {
			ETSAssert.assertXPath2(xpath, new DOMSource(features.item(i)), nsBindings);
		}
	}

	/**
	 * [{@code Test}] Submits a GetFeature request containing a comparison filter
	 * predicate that refers to an invalid feature property. An exception report is
	 * expected in response with status code 400 and exception code
	 * {@code InvalidParameterValue}.
	 * @param binding The ProtocolBinding to use for this request.
	 *
	 * @see "ISO 19143:2010, cl. 8.3: Exceptions"
	 */
	@Test(description = "See ISO 19143: 8.3", dataProvider = "protocol-binding")
	public void invalidPropertyReference(ProtocolBinding binding) {
		QName propName = new QName("http://example.org", "undefined", "ex");
		// randomly select a feature type
		Random rnd = new Random();
		int index = rnd.nextInt(this.featureTypes.size());
		WFSMessage.appendSimpleQuery(this.reqEntity, this.featureTypes.get(index));
		addComparisonPredicate(this.reqEntity, FES2.LESS_THAN_OR_EQUAL, propName, "1355941270", true, MATCH_ANY);
		Response rsp = wfsClient.submitRequest(reqEntity, binding);
		this.rspEntity = rsp.readEntity(Document.class);
		Assert.assertEquals(rsp.getStatus(), Status.BAD_REQUEST.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		String xpath = "//ows:Exception[@exceptionCode='InvalidParameterValue']";
		ETSAssert.assertXPath(xpath, this.rspEntity, null);
	}

	/**
	 * [{@code Test}] Submits a GetFeature request containing a comparison filter
	 * predicate that refers to the complex feature property gml:boundedBy (with
	 * fes:Literal/gml:Envelope as the literal operand).
	 *
	 * An exception report is expected in response with exception code
	 * {@code OperationProcessingFailed} and status code 400 or 403.
	 *
	 * <p style="margin-bottom: 0.5em">
	 * <strong>Sources</strong>
	 * </p>
	 * <ul>
	 * <li>ISO 19142:2010, cl. 11.4: Exceptions</li>
	 * <li>ISO 19142:2010, Table 3 - WFS exception codes</li>
	 * </ul>
	 * @param binding The ProtocolBinding to use for this request.
	 */
	@Test(description = "See ISO 19142: 7.5, 11.4", dataProvider = "protocol-binding")
	public void invalidOperand_boundedBy(ProtocolBinding binding) {
		QName propName = new QName(Namespaces.GML, "boundedBy", "gml");
		// randomly select a feature type
		Random rnd = new Random();
		int index = rnd.nextInt(this.featureTypes.size());
		WFSMessage.appendSimpleQuery(this.reqEntity, this.featureTypes.get(index));
		Document gmlEnv = WFSMessage.createGMLEnvelope();
		addComparisonPredicate(this.reqEntity, FES2.LESS_THAN_OR_EQUAL, propName, gmlEnv, true, MATCH_ANY);
		Response rsp = wfsClient.submitRequest(reqEntity, binding);
		this.rspEntity = rsp.readEntity(Document.class);
		ETSAssert.assertStatusCode(rsp.getStatus(), new int[] { 500, 400, 403 });
		String xpath = "//ows:Exception[@exceptionCode='OperationProcessingFailed']";
		ETSAssert.assertXPath(xpath, this.rspEntity, null);
	}

	/**
	 * Adds a comparison predicate to a GetFeature request entity with the given property
	 * name and literal value. The predicate is structured as shown in the listing below.
	 *
	 * <pre>
	 * {@code
	 * <Filter xmlns="http://www.opengis.net/fes/2.0">
	 *   <PropertyIsLessThan matchCase="true" matchAction="Any">
	 *     <Literal>value</Literal>
	 *     <ValueReference>tns:featureProperty</ValueReference>
	 *   </PropertyIsLessThan>
	 * </Filter>
	 * }
	 * </pre>
	 * @param request The request entity (/wfs:GetFeature).
	 * @param operator The name of the comparison operator.
	 * @param propertyName A QName that specifies the feature property to check.
	 * @param literalValue The literal object to compare the property value with; it must
	 * be a String or a DOM Document (in which case the document element is used to
	 * represent a complex literal).
	 * @param matchCase A boolean value indicating whether or not the comparison should be
	 * case-sensitive.
	 * @param matchAction A String specifying how the predicate should be applied to a
	 * multi-valued property; the default value is "Any".
	 */
	void addComparisonPredicate(Document request, String operator, QName propertyName, Object literalValue,
			boolean matchCase, String matchAction) {
		if (!request.getDocumentElement().getLocalName().equals(WFS2.GET_FEATURE)) {
			throw new IllegalArgumentException(
					"Not a GetFeature request: " + request.getDocumentElement().getNodeName());
		}
		if (null == propertyName) {
			throw new IllegalArgumentException("propertyName is required.");
		}
		Element queryElem = (Element) request.getElementsByTagNameNS(Namespaces.WFS, WFS2.QUERY_ELEM).item(0);
		Element filter = request.createElementNS(Namespaces.FES, "Filter");
		queryElem.appendChild(filter);
		Element predicate = request.createElementNS(Namespaces.FES, operator);
		filter.appendChild(predicate);
		String matchActionAttr = (null != matchAction && !matchAction.isEmpty()) ? matchAction : MATCH_ANY;
		predicate.setAttribute("matchCase", Boolean.toString(matchCase));
		predicate.setAttribute("matchAction", matchActionAttr);
		Element literalElem = request.createElementNS(Namespaces.FES, "Literal");
		if (String.class.isInstance(literalValue)) {
			literalElem.setTextContent((String) literalValue);
		}
		else {
			Document literalDoc = (Document) literalValue;
			literalElem.appendChild(request.adoptNode(literalDoc.getDocumentElement()));
		}
		predicate.appendChild(literalElem);
		Element valueRef = request.createElementNS(Namespaces.FES, "ValueReference");
		predicate.appendChild(valueRef);
		String prefix = (propertyName.getPrefix().length() > 0) ? propertyName.getPrefix() : TNS_PREFIX;
		String nsURI = request.lookupNamespaceURI(prefix);
		if (null == nsURI) {
			valueRef.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix, propertyName.getNamespaceURI());
		}
		valueRef.setTextContent(prefix + ":" + propertyName.getLocalPart());
	}

}
