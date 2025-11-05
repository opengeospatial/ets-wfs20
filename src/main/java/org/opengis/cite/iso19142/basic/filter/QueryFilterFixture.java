package org.opengis.cite.iso19142.basic.filter;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSTypeDefinition;
import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.util.AppSchemaUtils;
import org.opengis.cite.iso19142.util.DataSampler;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.testng.Assert;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import jakarta.xml.bind.DatatypeConverter;

/**
 * Provides configuration methods that facilitate the testing of query filters by
 * inspecting the application schema and sample data in order to deduce appropriate
 * property names and values to include in filter predicates.
 */
public class QueryFilterFixture extends BaseFixture {

	/** Acquires and saves sample data. */
	protected DataSampler dataSampler;

	protected Map<QName, List<XSElementDeclaration>> temporalProperties;

	/**
	 * An XSModel object representing the application schema supported by the SUT.
	 */
	protected XSModel model;

	protected final String GET_FEATURE_MINIMAL = "GetFeature-Minimal";

	public QueryFilterFixture() {
		super();
	}

	/**
	 * Obtains a DataSampler object from the test suite context (the value of the
	 * {@link SuiteAttribute#SAMPLER SuiteAttribute.SAMPLER attribute}), or adds one if
	 * it's not found there.
	 *
	 * A schema model (XSModel) is also obtained from the test suite context by accessing
	 * the {@link org.opengis.cite.iso19136.SuiteAttribute#XSMODEL xsmodel} attribute.
	 * @param testContext The test (set) context.
	 */
	@BeforeClass()
	public void initQueryFilterFixture(ITestContext testContext) {
		ISuite suite = testContext.getSuite();
		this.dataSampler = (DataSampler) suite.getAttribute(SuiteAttribute.SAMPLER.getName());
		this.model = (XSModel) suite.getAttribute(org.opengis.cite.iso19136.SuiteAttribute.XSMODEL.getName());
		this.temporalProperties = new HashMap<>();
	}

	/**
	 * Builds a DOM Document node representing the entity body for a GetFeature request. A
	 * minimal XML representation is read from the classpath
	 * ("util/GetFeature-Minimal.xml").
	 */
	@BeforeMethod
	public void buildRequestEntity() {
		this.reqEntity = WFSMessage.createRequestEntity(GET_FEATURE_MINIMAL, this.wfsVersion);
	}

	/**
	 * Discard previous response entity.
	 */
	@BeforeMethod
	public void discardResponseEntity() {
		this.rspEntity = null;
	}

	/**
	 * Finds the temporal properties defined for the specified feature type.
	 * @param featureType The qualified name of a feature type (which corresponds to an
	 * element declared in an application schema).
	 * @return A sequence of element declarations representing simple and complex temporal
	 * properties; the list may be empty.
	 */
	protected List<XSElementDeclaration> findTemporalProperties(QName featureType) {
		List<XSElementDeclaration> tmProps = this.temporalProperties.get(featureType);
		if (tmProps != null) {
			return tmProps;
		}
		tmProps = AppSchemaUtils.getTemporalFeatureProperties(getModel(), featureType);
		this.temporalProperties.put(featureType, tmProps);
		TestSuiteLogger.log(Level.FINE,
				String.format("Temporal properties for feature type %s: %s", featureType, tmProps));
		return tmProps;
	}

	public XSModel getModel() {
		if (model == null) {
			Assert.fail(
					"Test cannot be executed as no schema can be found; Please check if DescribeFeatureType returns a valid schema.");
		}
		return model;
	}

	/**
	 * Inspects sample data retrieved from the SUT and determines the range of simple
	 * property values for the specified feature type. The method finds the first feature
	 * property that (a) conforms to one of the given type definitions, and (b) has at
	 * least one value in the data sample.
	 * @param model An XSModel object representing an application schema.
	 * @param featureType The qualified name of some feature type.
	 * @param dataTypes A Set of simple data types that possess an interval or ratio scale
	 * of measurement (e.g. numeric or temporal data types).
	 * @return A Map containing a single entry where the key is an element declaration and
	 * the value is a {@code String[]} array containing two String objects representing
	 * the minimum and maximum values of the property.
	 */
	Map<XSElementDeclaration, String[]> findFeaturePropertyValue(XSModel model, QName featureType,
			Set<XSTypeDefinition> dataTypes) {
		List<XSElementDeclaration> featureProps = null;
		// look for properties of the given data types
		for (XSTypeDefinition dataType : dataTypes) {
			featureProps = AppSchemaUtils.getFeaturePropertiesByType(model, featureType, dataType);
			if (!featureProps.isEmpty()) {
				break;
			}
		}
		ListIterator<XSElementDeclaration> listItr = featureProps.listIterator(featureProps.size());
		XSElementDeclaration prop = null;
		String[] valueRange = null;
		// start with application-specific properties at end of list
		while (listItr.hasPrevious()) {
			prop = listItr.previous();
			QName propName = new QName(prop.getNamespace(), prop.getName());
			List<String> valueList = this.dataSampler.getSimplePropertyValues(featureType, propName, null);
			if (!valueList.isEmpty()) {
				String[] values = new String[valueList.size()];
				for (int i = 0; i < values.length; i++) {
					values[i] = valueList.get(i);
				}
				// use actual datatype to produce valid string representation
				QName datatype = AppSchemaUtils.getBuiltInDatatype(prop);
				valueRange = calculateRange(values, datatype);
				TestSuiteLogger.log(Level.FINE, String.format("Found property values of %s, %s \n %s", featureType,
						propName, Arrays.toString(valueRange)));
				break;
			}
		}
		Map<XSElementDeclaration, String[]> map = new HashMap<XSElementDeclaration, String[]>();
		if (null != valueRange) {
			map.put(prop, valueRange);
		}
		return map;
	}

	/**
	 * Returns a set of primitive numeric data type definitions (xsd:decimal, xsd:double,
	 * xsd:float). Derived data types are also implicitly included (e.g. xsd:integer).
	 * @param model An XSModel object representing an application schema.
	 * @return A Set of simple type definitions corresponding to numeric data types.
	 */
	Set<XSTypeDefinition> getNumericDataTypes(XSModel model) {
		Set<XSTypeDefinition> dataTypes = new HashSet<XSTypeDefinition>();
		dataTypes.add(model.getTypeDefinition("decimal", XMLConstants.W3C_XML_SCHEMA_NS_URI));
		dataTypes.add(model.getTypeDefinition("double", XMLConstants.W3C_XML_SCHEMA_NS_URI));
		dataTypes.add(model.getTypeDefinition("float", XMLConstants.W3C_XML_SCHEMA_NS_URI));
		return dataTypes;
	}

	/**
	 * Calculates the range of the given values and returns the minimum and maximum values
	 * as valid string literals.
	 * @param values An array of strings representing numeric or temporal values.
	 * @param datatype The name of the built-in XML Schema datatype to which the values
	 * must conform.
	 * @return An array containing the (min, max) values.
	 */
	String[] calculateRange(String[] values, QName datatype) {
		sortValues(values);
		Set<String> integerDatatypes = new HashSet<String>();
		// WARNING: some subtypes omitted
		Collections.addAll(integerDatatypes,
				new String[] { "integer", "nonPositiveInteger", "nonNegativeInteger", "long", "int" });
		if (integerDatatypes.contains(datatype.getLocalPart())) {
			for (int i = 0; i < values.length; i++) {
				int intValue = Double.valueOf(values[i]).intValue();
				values[i] = Integer.toString(intValue);
			}
		}
		return new String[] { values[0], values[values.length - 1] };
	}

	/**
	 * Sorts the given array into ascending order, assuming its elements represent either
	 * numeric (Double) or temporal (Calendar) values. Temporal values are expressed in
	 * UTC. The corresponding built-in datatypes from XML Schema are:
	 *
	 * <ul>
	 * <li>xsd:decimal (including xsd:integer and its subtypes)</li>
	 * <li>xsd:double</li>
	 * <li>xsd:float</li>
	 * <li>xsd:dateTime</li>
	 * <li>xsd:date</li>
	 * </ul>
	 * @param values An array containing String representations of numeric or temporal
	 * values.
	 */
	void sortValues(String[] values) {
		if ((null == values) || values.length == 0) {
			return;
		}
		Object[] objValues = null;
		try {
			objValues = new Double[values.length];
			for (int i = 0; i < objValues.length; i++) {
				objValues[i] = Double.valueOf(values[i]);
			}
			Arrays.sort(objValues);
		}
		catch (NumberFormatException nfe) {
			// use (Gregorian)Calendar to sort temporal values
			objValues = new Calendar[values.length];
			for (int i = 0; i < objValues.length; i++) {
				if (values[i].indexOf('T') > 0) {
					objValues[i] = DatatypeConverter.parseDateTime(values[i]);
				}
				else {
					objValues[i] = DatatypeConverter.parseDate(values[i]);
				}
			}
			Arrays.sort(objValues);
		}
		// Earlier we using ISO_OFFSET_DATE_TIME but offset is appended with seconds due
		// to that test is getting failed.
		// So we have added a custom datetime formatter pattern(yyyy-MM-dd'T'HH:mm:ssXXX)
		// which will ignore seconds from offset.
		DateTimeFormatter tmFormatter = (values[0].indexOf('T') > 0)
				? DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX") : DateTimeFormatter.ISO_DATE;
		for (int i = 0; i < values.length; i++) {
			if (GregorianCalendar.class.isInstance(objValues[i])) {
				GregorianCalendar cal = (GregorianCalendar) objValues[i];
				values[i] = tmFormatter.format(cal.toZonedDateTime());
			}
			else {
				values[i] = DatatypeConverter.printDecimal(new BigDecimal(objValues[i].toString()));
			}
		}
	}

}
