package org.opengis.cite.iso19142;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.geotoolkit.factory.Hints;
import org.geotoolkit.referencing.factory.epsg.EpsgInstaller;
import org.opengis.cite.iso19142.util.TestSuiteLogger;
import org.opengis.util.FactoryException;
import org.testng.IExecutionListener;

/**
 * A listener that is invoked before and after a test run. It is often used to
 * configure a shared fixture that endures for the duration of the entire test
 * run.
 * 
 * <p>
 * A shared fixture should be used with caution in order to avoid undesirable
 * test interactions. In general, it should be populated with "read-only"
 * objects that are not modified during the test run.
 * </p>
 * 
 * @see com.occamlab.te.spi.executors.FixtureManager FixtureManager
 * 
 */
public class TestRunListener implements IExecutionListener {

	/**
	 * Notifies the listener that a test run is about to start; it looks for a
	 * JNDI DataSource named "jdbc/EPSG" that provides access to a database
	 * containing the official EPSG geodetic parameters. If one is found, it is
	 * set as a {@link org.geotoolkit.factory.Hints#EPSG_DATA_SOURCE hint} when
	 * initializing the EPSG factory. An embedded database will be created if
	 * necessary.
	 */
	@Override
	public void onExecutionStart() {
		DataSource epsgDataSource = null;
		try {
			Context initContext = new InitialContext();
			Context envContext = (Context) initContext.lookup("java:/comp/env");
			epsgDataSource = (DataSource) envContext.lookup("jdbc/EPSG");
		} catch (NamingException nx) {
			TestSuiteLogger
					.log(Level.CONFIG,
							"DataSource 'jdbc/EPSG' was not found. An embedded database will be created if necessary.");
		}
		if (null != epsgDataSource) {
			checkEpsgDataSource(epsgDataSource);
			Hints.putSystemDefault(Hints.EPSG_DATA_SOURCE, epsgDataSource);
		}
	}

	@Override
	public void onExecutionFinish() {
	}

	/**
	 * Checks that the database represented by the given DataSource is
	 * accessible and contains the EPSG geodetic parameter data. If the EPSG
	 * schema is not found, it will be created and the data loaded.
	 * 
	 * @param dataSource
	 *            A JDBC DataSource.
	 */
	void checkEpsgDataSource(DataSource dataSource) {
		if (null == dataSource) {
			throw new IllegalArgumentException("DataSource is null.");
		}
		try (Connection dbConn = dataSource.getConnection()) {
			boolean epsgSchemaExists = false;
			ResultSet schemas = dbConn.getMetaData().getSchemas();
			while (schemas.next()) {
				// first column is schema name
				if (schemas.getString(1).equalsIgnoreCase("EPSG")) {
					epsgSchemaExists = true;
					break;
				}
			}
			if (!epsgSchemaExists) {
				TestSuiteLogger
						.log(Level.WARNING,
								"EPSG schema not found in DataSource--it will be created.");
				EpsgInstaller installer = new EpsgInstaller();
				installer.setDatabase(dbConn);
				EpsgInstaller.Result result = installer.call();
				TestSuiteLogger.log(Level.INFO, result.toString());
			}
		} catch (SQLException | FactoryException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
}
