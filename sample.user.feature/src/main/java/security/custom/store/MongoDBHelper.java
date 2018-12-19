/*
 * Copyright 2018 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package security.custom.store;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * Helper class to connect to the mongoDB database. This implementation can
 * either pull the database config from a mongoDB.props file or from the
 * server.xml.
 * </p>
 * This example does not have any performance tuning or fail over options
 * configured.
 * </p>
 * It can connect with a username and password enabled, but any other authentication
 * option will need to be added.
 */
public class MongoDBHelper implements ManagedService {

	// To enable trace for this class, enable trace in the server.xml and add this package name to the trace specification: security.custom.store.*=all
	static final Logger LOGGER = Logger.getLogger(MongoDBHelper.class.getName());

	private static MongoDBHelper instance;

	// Attribute keys for loading mongoDB config from server.xml
	// <customStoreMongoDBConfig databaseName="oauthSample" user="user1"
	// password="passwordOfPower" hostname="localhost" port="27017"/>
	public final static String CONFIG_PID = "customStoreMongoDBConfig";
	public final static String DB_KEY = "databaseName";
	public final static String USER_KEY = "user";
	public final static String PASSWORD_KEY = "password";
	public final static String HOST_KEY = "hostname";
	public final static String PORT_KEY = "port";

	// The mongoDB config from a props file. This file originates in SupportFiles/mongoDB.props
	// and is copied over to the server config folder during the gradlew build.
	public final static String MONGO_PROPS_FILE = "mongoDB.props";

	// Default config for mongoDB
	private String dbName = "oauthSample";
	private String dbHost = "localhost";
	private String dbUser = null;
	private String dbPwd = null;
	private int dbPort = 27017;

	boolean loadedProps = false;

	private MongoClient mongoClient = null;
	private MongoDatabase db = null;

	public static MongoDBHelper getInstance() {
		if (instance == null) {
			instance = new MongoDBHelper();
		}
		return instance;
	}

	public MongoDBHelper() {
		LOGGER.log(Level.INFO, "Initialize MongoDBHelper");
	}

	/**
	 * Simple helper method to get a connection to mongoDB.
	 * </p>
	 * It is not a general recommendation for all CustomStore implementations.
	 * </p>
	 * The mongoDB properties are used in this order of precedence:
	 * <ol>
	 * <li>Configuration set in the server.xml using customStoreMongoDBConfig</li>
	 * <li>Configuration set in the mongoDB.props file located in the server config directory (wlp/usr/servers/serverName)</li>
	 * <li>Default values for the database name, host and port (see defaults set in this file).</li>
	 * </ol>
	 *
	 * @return A connected MongoDatabase reference
	 */
	public synchronized MongoDatabase getDB() {
		if (db == null) {

			if (!loadedProps) { // if we didn't load props from the server.xml config, try the default props
								// file.
				LOGGER.log(Level.INFO,
						"Did not load properties from config, trying the " + MONGO_PROPS_FILE + " file.");
				getDatabaseConfig();
			}

			LOGGER.log(Level.INFO, "Connecting to the " + dbName + " database at " + dbHost + ":" + dbPort);

			if (loadedProps) {
				MongoClientSettings settings = null;
				if (dbUser != null && dbPwd != null) {
					LOGGER.log(Level.FINEST, "Logging in with user " + dbUser);
					MongoCredential credential = MongoCredential.createCredential(dbUser, dbName, dbPwd.toCharArray());
					settings = MongoClientSettings.builder().credential(credential).applyToClusterSettings(
							builder -> builder.hosts(Arrays.asList(new ServerAddress(dbHost, dbPort)))).build();
				} else {
					settings = MongoClientSettings.builder().applyToClusterSettings(
							builder -> builder.hosts(Arrays.asList(new ServerAddress(dbHost, dbPort)))).build();
				}
				mongoClient = MongoClients.create(settings);
			} else {
				LOGGER.log(Level.INFO,
						"Customized properties not provided, connecting to database with defaults. localhost:27017");
				mongoClient = MongoClients.create();
			}
			db = mongoClient.getDatabase(dbName);
			LOGGER.log(Level.INFO, "Connected to the database " + dbName);
		}

		return db;

	}

	/**
	 * This helper method uses a properties file to get the database connection
	 * parameters.
	 * </p>
	 * It is not a general recommendation for all CustomStore implementations.
	 * 
	 */
	private void getDatabaseConfig() {
		File f = new File(MONGO_PROPS_FILE);
		if (!f.exists()) {
			throw new IllegalStateException("CustomStoreSample Database config file " + MONGO_PROPS_FILE
					+ " was not found. This may be normal during server startup");
		}
		try {
			Properties mongoProps = new Properties();
			mongoProps.load(new FileReader(f));

			dbName = mongoProps.getProperty("DBNAME", dbName);
			dbHost = mongoProps.getProperty("HOST", dbHost);
			dbPort = Integer.valueOf(mongoProps.getProperty("PORT", String.valueOf(dbPort)));
			dbUser = mongoProps.getProperty("USER", dbUser);
			dbPwd = mongoProps.getProperty("PWD", dbPwd);
			loadedProps = true;
			
		} catch (IOException e) {
			LOGGER.log(Level.WARNING,
					"Database config could not be retrieved from " + MONGO_PROPS_FILE + ". Using defaults.");
		}
	}

	/**
	 * Close the mongoDB client connection. This can be called by the bundle Activator.
	 */
	public synchronized void stopDB() {
		if (mongoClient != null) {
			mongoClient.close();
			LOGGER.log(Level.INFO, "Disconnected database connection to " + dbName);
		}
	}

	/**
	 * Helper method that defines the mongoDB configuration attribute, which can be
	 * set in the server.xml.
	 * @return A Dictionary containing the mongoDB configuration {@link CONFIG_PID}
	 */
	public Dictionary<String, String> getDefaults() {
		Dictionary<String, String> defaults = new Hashtable<String, String>();
		defaults.put(org.osgi.framework.Constants.SERVICE_PID, CONFIG_PID);
		return defaults;
	}

	/**
	 * This ManagedService method receives configuration from the server.xml for the mongoDB database.
	 * </p>
	 * Sample config for the server.xml:
	 * 
	 * &lt;customStoreMongoDBConfig databaseName="oauthSample" hostname="localhost" user="user1" password=
	 * "passwordOfPower" port="27017"/&gt;
	 */
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		if (properties != null) {
			loadedProps = true;
			LOGGER.log(Level.FINEST, "Received properties map, processing.");
			String db = (String) properties.get(DB_KEY);
			if (db != null) {
				dbName = db;
				LOGGER.log(Level.FINEST, "Found database name from the server config: " + dbName);
			}

			String host = (String) properties.get(HOST_KEY);
			if (host != null) {
				dbHost = host;
				LOGGER.log(Level.FINEST, "Found host name from the server config: " + dbHost);
			}

			String user = (String) properties.get(USER_KEY);
			if (user != null) {
				dbUser = user;
				LOGGER.log(Level.FINEST, "Found user name from the server config: " + dbUser);
			}

			String password = (String) properties.get(PASSWORD_KEY);
			if (password != null) {
				dbPwd = password;
				LOGGER.log(Level.FINEST, "Found password from the server config (not printing to log)");
			}

			String port = (String) properties.get(PORT_KEY);
			if (port != null) {
				try {
					dbPort = Integer.valueOf(port);
					LOGGER.log(Level.FINEST, "Found port from the server config: " + port);
				} catch (NumberFormatException ne) {
					LOGGER.log(Level.WARNING,
							"Port provided, " + port + ", was not an integer. Using default port " + dbPort);
				}
			}
		} else {
			LOGGER.log(Level.FINEST, "Received ManagedService updated() call, but the properties map was null.");
		}
	}

}
