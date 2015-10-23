package org.opengis.cite.iso19142;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;

import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.occamlab.te.spi.executors.TestRunExecutor;
import com.occamlab.te.spi.executors.testng.TestNGExecutor;
import com.occamlab.te.spi.jaxrs.TestSuiteController;

/**
 * Main test run controller oversees execution of TestNG test suites.
 */
public class TestNGController implements TestSuiteController {

	private TestRunExecutor executor;
	private Properties etsProperties = new Properties();

	/**
	 * A convenience method to facilitate test development.
	 * 
	 * @param args
	 *            Test run arguments (optional). The first argument must refer
	 *            to an XML properties file containing the expected set of test
	 *            run arguments. If no argument is supplied, the file located at
	 *            ${user.home}/test-run-props.xml will be used.
	 * @throws Exception
	 *             If the test run cannot be executed (usually due to
	 *             unsatisfied pre-conditions).
	 */
	public static void main(String[] args) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		File xmlArgs = null;
		if (args.length > 0) {
			xmlArgs = (args[0].startsWith("file:")) ? new File(
					URI.create(args[0])) : new File(args[0]);
		} else {
			String homeDir = System.getProperty("user.home");
			xmlArgs = new File(homeDir, "test-run-props.xml");
		}
		if (!xmlArgs.exists()) {
			throw new IllegalArgumentException(
					"Test run arguments not found at " + xmlArgs);
		}
		Document testRunArgs = db.parse(xmlArgs);
		TestNGController controller = new TestNGController();
		Source testResults = controller.doTestRun(testRunArgs);
		System.out.println("Test results: " + testResults.getSystemId());
	}

	/**
	 * Default constructor uses the location given by the "user.home" system
	 * property as the root output directory.
	 */
	public TestNGController() {
		this(new File(System.getProperty("user.home")).toURI().toString());
	}

	/**
	 * Construct a controller that writes results to the given output directory.
	 * 
	 * @param outputDirUri
	 *            A file URI that specifies the location of the directory in
	 *            which test results will be written. It will be created if it
	 *            does not exist.
	 */
	public TestNGController(String outputDirUri) {
		InputStream is = getClass().getResourceAsStream("ets.properties");
		try {
			this.etsProperties.load(is);
		} catch (IOException ex) {
			TestSuiteLogger.log(Level.WARNING,
					"Unable to load ets.properties. " + ex.getMessage());
		}
		URL tngSuite = TestNGController.class.getResource("testng.xml");
		File resultsDir = new File(URI.create(outputDirUri));
		TestSuiteLogger.log(Level.CONFIG, "Using TestNG config: " + tngSuite);
		TestSuiteLogger.log(Level.INFO,
				"Using outputDirPath: " + resultsDir.getAbsolutePath());
		// NOTE: setting third argument to 'true' enables the default listeners
		this.executor = new TestNGExecutor(tngSuite.toString(),
				resultsDir.getAbsolutePath(), false);
	}

	@Override
	public String getCode() {
		return etsProperties.getProperty("ets-code");
	}

	@Override
	public String getVersion() {
		return etsProperties.getProperty("ets-version");
	}

	@Override
	public String getTitle() {
		return etsProperties.getProperty("ets-title");
	}

	@Override
	public Source doTestRun(Document testRunArgs) throws Exception {
		validateTestRunArgs(testRunArgs);
		return executor.execute(testRunArgs);
	}

	/**
	 * Validates the given set of test run arguments. The test run is aborted if
	 * any checks fail. At least one of the test run arguments (
	 * {@link org.opengis.cite.iso19142.TestRunArg#WFS}, {link
	 * org.opengis.cite.iso19142.TestRunArg#IUT}) must be supplied.
	 * 
	 * @param testRunArgs
	 *            A DOM Document containing a set of XML properties (key-value
	 *            pairs).
	 * @throws IllegalArgumentException
	 *             If any arguments are missing or invalid for some reason.
	 */
	void validateTestRunArgs(Document testRunArgs) {
		if (null == testRunArgs
				|| !testRunArgs.getDocumentElement().getNodeName()
						.equals("properties")) {
			throw new IllegalArgumentException(
					"Input is not an XML properties document.");
		}
		NodeList entries = testRunArgs.getDocumentElement()
				.getElementsByTagName("entry");
		if (entries.getLength() == 0) {
			throw new IllegalArgumentException("No test run arguments found.");
		}
		Map<String, String> args = new HashMap<String, String>();
		for (int i = 0; i < entries.getLength(); i++) {
			Element entry = (Element) entries.item(i);
			args.put(entry.getAttribute("key"), entry.getTextContent().trim());
		}
		if (!(args.containsKey(TestRunArg.IUT.toString()) || args
				.containsKey(TestRunArg.WFS.toString()))) {
			throw new IllegalArgumentException(String.format(
					"Missing argument: '%s' or '%s' must be present.",
					TestRunArg.IUT, TestRunArg.WFS));
		}
	}
}
