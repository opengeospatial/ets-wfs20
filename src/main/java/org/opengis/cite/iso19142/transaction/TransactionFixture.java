package org.opengis.cite.iso19142.transaction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSModel;
import org.opengis.cite.iso19142.BaseFixture;
import org.opengis.cite.iso19142.FeatureTypeInfo;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.SuiteAttribute;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.DataSampler;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.WFSRequest;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.w3c.dom.Document;

/**
 * Provides configuration methods that facilitate testing of transaction
 * capabilities.
 */
public class TransactionFixture extends BaseFixture {

    protected DataSampler dataSampler;
    /**
     * An XSModel object representing the application schema supported by the
     * SUT.
     */
    protected XSModel model;

    public TransactionFixture() {
        super();
    }

    /**
     * Obtains a DataSampler object from the test run context (the value of the
     * {@link SuiteAttribute#SAMPLER SuiteAttribute.SAMPLER attribute}). A
     * schema model (XSModel) is also obtained from the test context by
     * accessing the {@link org.opengis.cite.iso19136.SuiteAttribute#XSMODEL
     * xsmodel} attribute.
     * 
     * @param testContext
     *            The test run context.
     */
    @BeforeClass(alwaysRun = true)
    public void initTransactionFixture(ITestContext testContext) {
        ISuite suite = testContext.getSuite();
        this.dataSampler = (DataSampler) suite
                .getAttribute(SuiteAttribute.SAMPLER.getName());
        this.model = (XSModel) suite
                .getAttribute(org.opengis.cite.iso19136.SuiteAttribute.XSMODEL
                        .getName());
    }

    /**
     * Builds a DOM Document representing a Transaction request entity.
     */
    @BeforeMethod
    public void buildTransactionRequest() {
        this.reqEntity = WFSRequest.createRequestEntity(WFS2.TRANSACTION);
    }

    /**
     * A DataProvider that supplies a collection of parameter tuples where each
     * tuple has two elements:
     * <ol>
     * <li>ProtocolBinding - a supported transaction request binding</li>
     * <li>QName - the name of a feature type for which data are available</li>
     * </ol>
     * 
     * @param testContext
     *            The ITestContext object for the test run.
     * @return Iterator<Object[]> An iterator over a collection of parameter
     *         tuples.
     */
    @DataProvider(name = "binding+availFeatureType")
    public Iterator<Object[]> trxTestParameters(ITestContext testContext) {
        ISuite suite = testContext.getSuite();
        Document wfsMetadata = (Document) suite
                .getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        if (null == wfsMetadata) {
            throw new NullPointerException(
                    "Service description not found in ITestContext");
        }
        Set<ProtocolBinding> trxBindings = ServiceMetadataUtils
                .getOperationBindings(wfsMetadata, WFS2.TRANSACTION);
        DataSampler sampler = (DataSampler) suite
                .getAttribute(SuiteAttribute.SAMPLER.getName());
        Map<QName, FeatureTypeInfo> featureInfo = sampler.getFeatureTypeInfo();
        List<Object[]> paramList = new ArrayList<Object[]>();
        for (ProtocolBinding binding : trxBindings) {
            for (FeatureTypeInfo typeInfo : featureInfo.values()) {
                if (typeInfo.isInstantiated()) {
                    Object[] tuple = { binding, typeInfo.getTypeName() };
                    paramList.add(tuple);
                }
            }
        }
        return paramList.iterator();
    }
}
