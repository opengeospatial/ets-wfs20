package org.opengis.cite.iso19142.basic.filter.temporal;

import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathExpressionException;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSTypeDefinition;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.basic.filter.QueryFilterFixture;
import org.opengis.cite.iso19142.util.AppSchemaUtils;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Tests the response to a GetFeature request that includes the temporal
 * predicate <em>During</em>. The relation can be expressed as follows when
 * comparing a temporal instant to a temporal period:
 * 
 * <pre>
 * self.position &gt; other.begin.position AND self.position &lt; other.end.position
 * </pre>
 * 
 * <p>
 * If both operands are periods then the following must hold:
 * </p>
 * 
 * <pre>
 * self.begin.position &gt; other.begin.position AND self.end.position &lt; other.end.position
 * </pre>
 * 
 * <p>
 * The following figure illustrates the relationship. A solid line denotes a
 * temporal property; a dashed line denotes a literal time value that specifies
 * the temporal extent of interest.
 * </p>
 *
 * <img src="doc-files/during.png" alt="During relationship">
 *
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li>ISO 19108, 5.2.3.5: TM_RelativePosition</li>
 * </ul>
 */
public class DuringTests extends QueryFilterFixture {

	public final static String IMPL_TEMPORAL_FILTER = "ImplementsTemporalFilter";
	private XSTypeDefinition gmlTimeBaseType;

	/**
	 * Checks the value of the filter constraint {@value #IMPL_TEMPORAL_FILTER}
	 * in the capabilities document. All tests are skipped if this is not
	 * "TRUE".
	 * 
	 * @param testContext
	 *            Information about the test run environment.
	 */
	@BeforeTest
	public void implementsTemporalFilter(ITestContext testContext) {
		this.wfsMetadata = (Document) testContext.getSuite().getAttribute(
				SuiteAttribute.TEST_SUBJECT.getName());
		String xpath = String.format(
				"//fes:Constraint[@name='%s' and (ows:DefaultValue = 'TRUE')]",
				IMPL_TEMPORAL_FILTER);
		NodeList result;
		try {
			result = XMLUtils.evaluateXPath(this.wfsMetadata, xpath, null);
		} catch (XPathExpressionException e) {
			throw new AssertionError(e.getMessage());
		}
		if (result.getLength() == 0) {
			throw new SkipException(ErrorMessage.format(
					ErrorMessageKeys.NOT_IMPLEMENTED, IMPL_TEMPORAL_FILTER));
		}
	}

	/**
	 * Creates an XSTypeDefinition object representing the
	 * gml:AbstractTimeGeometricPrimitiveType definition. This is the base type
	 * for <code>TimeInstantType</code> and <code>TimePeriodType</code>.
	 */
	@BeforeClass
	public void createTemporalPrimitiveBaseType() {
		this.gmlTimeBaseType = this.model.getTypeDefinition(
				"AbstractTimeGeometricPrimitiveType", Namespaces.GML);
	}

	public void during() {
		QName featureType = null;
		List<XSElementDeclaration> timeProps = AppSchemaUtils
				.getFeaturePropertiesByType(this.model, featureType,
						this.gmlTimeBaseType);
		// iterate over AppSchemaUtils.getSimpleTemporalDataTypes(model)
		if (timeProps.isEmpty()) {
			throw new SkipException("Feature type has no temporal properties: "
					+ featureType);
		}
	}
}
