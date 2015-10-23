package org.opengis.cite.iso19142;

import java.net.URI;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.namespace.QName;
import javax.xml.validation.Schema;

import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.cite.iso19142.util.URIUtils;
import org.opengis.cite.iso19142.util.ValidationUtils;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.w3c.dom.Document;

/**
 * A listener that performs various tasks before and after a test suite is run,
 * usually concerned with maintaining a shared test suite fixture. Since this
 * listener is loaded using the ServiceLoader mechanism, its methods will be
 * called before those of other suite listeners listed in the test suite
 * definition and before any annotated configuration methods.
 * 
 * Attributes set on an ISuite instance are not inherited by constituent test
 * group contexts (ITestContext). However, suite attributes are still accessible
 * from lower contexts.
 * 
 * @see org.testng.ISuite ISuite interface
 */
public class SuiteFixtureListener implements ISuiteListener {

	@Override
	public void onStart(ISuite suite) {
		Schema wfsSchema = ValidationUtils.createWFSSchema();
		if (null != wfsSchema) {
			suite.setAttribute(SuiteAttribute.WFS_SCHEMA.getName(), wfsSchema);
		}
		processWfsParameter(suite);
		setAppSchemaParameter(suite);
		StringBuilder str = new StringBuilder("Initial test run parameters:\n");
		str.append(suite.getXmlSuite().getAllParameters().toString());
		TestSuiteLogger.log(Level.CONFIG, str.toString());
	}

	@Override
	public void onFinish(ISuite suite) {
	}

	/**
	 * Processes the "wfs" test suite parameter that specifies a URI reference
	 * for the service description (capabilities document). The URI is
	 * dereferenced and the entity is parsed; the resulting Document object is
	 * set as the value of the {@link SuiteAttribute#TEST_SUBJECT testSubject}
	 * suite attribute.
	 * 
	 * The {@link SuiteAttribute#FEATURE_INFO featureInfo} suite attribute is
	 * also set; its value is a {@literal Map<QName, FeatureTypeInfo>} object
	 * that provides summary information about available feature types, mostly
	 * gleaned from the service description.
	 * 
	 * @param suite
	 *            An ISuite object representing a TestNG test suite.
	 */
	void processWfsParameter(ISuite suite) {
		Map<String, String> params = suite.getXmlSuite().getParameters();
		// metadata may have been submitted as entity body in POST request
		String iutRef = params.get(TestRunArg.IUT.toString());
		String wfsRef = (null != iutRef) ? iutRef : params.get(TestRunArg.WFS
				.toString());
		if ((null == wfsRef) || wfsRef.isEmpty()) {
			throw new IllegalArgumentException("Required parameter not found");
		}
		URI wfsURI = URI.create(wfsRef);
		Document doc = null;
		try {
			doc = URIUtils.resolveURIAsDocument(wfsURI);
			if (!doc.getDocumentElement().getLocalName()
					.equals(WFS2.WFS_CAPABILITIES)) {
				throw new RuntimeException(
						"Did not receive WFS capabilities document: "
								+ doc.getDocumentElement().getNodeName());
			}
		} catch (Exception ex) {
			// push exception up through TestNG ISuiteListener interface
			throw new RuntimeException("Failed to parse resource located at "
					+ wfsURI, ex);
		}
		if (null != doc) {
			suite.setAttribute(SuiteAttribute.TEST_SUBJECT.getName(), doc);
			Map<QName, FeatureTypeInfo> featureInfo = ServiceMetadataUtils
					.extractFeatureInfo(doc);
			suite.setAttribute(SuiteAttribute.FEATURE_INFO.getName(),
					featureInfo);
			if (TestSuiteLogger.isLoggable(Level.FINER)) {
				// log service description
				StringBuilder logMsg = new StringBuilder(
						"Parsed resource from ");
				logMsg.append(wfsURI).append("\n");
				logMsg.append(XMLUtils.writeNodeToString(doc));
				TestSuiteLogger.log(Level.FINER, logMsg.toString());
			}
		}
	}

	/**
	 * Sets the value of the "xsd" suite parameter, the value of which is the
	 * request URI used to retrieve the GML application schema(s) supported by
	 * the WFS under test. The URI corresponds to a DescribeFeatureType request;
	 * its value is derived from information in the service metadata document
	 * (GET method).
	 * 
	 * @param suite
	 *            An ISuite object representing a TestNG test suite. The value
	 *            of the attribute {@link SuiteAttribute#TEST_SUBJECT} should be
	 *            a Document node representing service metadata.
	 */
	void setAppSchemaParameter(ISuite suite) {
		if (null == suite.getAttribute(SuiteAttribute.TEST_SUBJECT.getName())) {
			return;
		}
		Document wfsMetadata = (Document) suite
				.getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
		URI endpoint = ServiceMetadataUtils.getOperationEndpoint(wfsMetadata,
				WFS2.DESCRIBE_FEATURE_TYPE, ProtocolBinding.GET);
		if (endpoint.isAbsolute()) {
			StringBuilder reqURI = new StringBuilder(endpoint.toString());
			reqURI.append("?service=WFS&version=2.0.0&request=DescribeFeatureType");
			Map<String, String> params = suite.getXmlSuite().getParameters();
			params.put(org.opengis.cite.iso19136.TestRunArg.XSD.toString(),
					reqURI.toString());
		}
	}
}
