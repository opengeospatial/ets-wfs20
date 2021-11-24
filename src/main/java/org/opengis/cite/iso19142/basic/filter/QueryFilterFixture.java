package org.opengis.cite.iso19142.basic.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
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

/**
 * Provides configuration methods that facilitate the testing of query filters
 * by inspecting the application schema and sample data in order to deduce
 * appropriate property names and values to include in filter predicates.
 */
public class QueryFilterFixture extends BaseFixture {

    /** Acquires and saves sample data. */
    protected DataSampler dataSampler;
    protected Map<QName, List<XSElementDeclaration>> temporalProperties;

    /**
     * An XSModel object representing the application schema supported by the
     * SUT.
     */
    protected XSModel model;
    protected final String GET_FEATURE_MINIMAL = "GetFeature-Minimal";

    public QueryFilterFixture() {
        super();
    }

    /**
     * Obtains a DataSampler object from the test suite context (the value of
     * the {@link SuiteAttribute#SAMPLER SuiteAttribute.SAMPLER attribute}), or
     * adds one if it's not found there.
     * 
     * A schema model (XSModel) is also obtained from the test suite context by
     * accessing the {@link org.opengis.cite.iso19136.SuiteAttribute#XSMODEL
     * xsmodel} attribute.
     * 
     * @param testContext
     *            The test (set) context.
     */
    @BeforeClass()
    public void initQueryFilterFixture(ITestContext testContext) {
        ISuite suite = testContext.getSuite();
        this.dataSampler = (DataSampler) suite.getAttribute(SuiteAttribute.SAMPLER.getName());
        this.model = (XSModel) suite.getAttribute(org.opengis.cite.iso19136.SuiteAttribute.XSMODEL.getName());
        this.temporalProperties = new HashMap<>();
    }

    /**
     * Builds a DOM Document node representing the entity body for a GetFeature
     * request. A minimal XML representation is read from the classpath
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
     * 
     * @param featureType
     *            The qualified name of a feature type (which corresponds to an
     *            element declared in an application schema).
     * @return A sequence of element declarations representing simple and
     *         complex temporal properties; the list may be empty.
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
		if(model == null) {
			Assert.fail("Test cannot be executed as no schema can be found; Please check if DescribeFeatureType returns a valid schema.");
		}
		return model;
	}

}
