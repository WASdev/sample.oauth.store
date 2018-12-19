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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

/**
 * Functional verification testing for the OAuthStore implementations. To run
 * this test a Liberty server must be running with OIDC and the custom
 * OAuthStore configured. It also requires a MongoDB instance. If one has not
 * been defined, the test will start up a MongoDB instance.
 * 
 * <p/>
 * Note: that this tests only a subset of the methods in the OAuthStore
 * implementations.
 * <p/>
 * Note: doesn't support starting an embedded MongoDB instance with
 * authentication enabled.
 */
public class OAuthStoreSampleFvtTest {

	/**
	 * A handle to the MongoDB executable.
	 */
	private static MongodExecutable mongodExecutable = null;
	
	private static MongoClient mongoClient = null;

	/**
	 * The MongoDB collection name for storing OAuthClients (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_OAUTH_CLIENT_TABLE = "OauthClient";

	/**
	 * The MongoDB collection name for storing OAuthTokens (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_OAUTH_TOKEN_TABLE = "OauthToken";

	/**
	 * The MongoDB collection name for storing OAuthConsents (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_OAUTH_CONSENT_TABLE = "OauthConsent";

	/**
	 * The key used for storing client ID in MongoDB collections (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_KEY_CLIENT_ID = "CLIENTID";

	/**
	 * The key used for storing provider ID in MongoDB collections (as defined in
	 * the CustomStoreSample).
	 */
	private final static String MONGO_KEY_PROVIDER_ID = "PROVIDERID";

	/**
	 * The key used for storing client secret in MongoDB collections (as defined in
	 * the CustomStoreSample).
	 */
	private final static String MONGO_KEY_CLIENT_SECRET = "CLIENTSECRET";

	/**
	 * The key used for storing redirect URI in MongoDB collections (as defined in
	 * the CustomStoreSample).
	 */
	private final static String MONGO_KEY_REDIRECT_URI = "REDIRECTURI";

	/**
	 * The key used for storing display name in MongoDB collections (as defined in
	 * the CustomStoreSample).
	 */
	private final static String MONGO_KEY_DISPLAY_NAME = "DISPLAYNAME";

	/**
	 * The key used for storing metadata in MongoDB collections (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_KEY_METADATA = "METADATA";

	/**
	 * The key used for storing enabled in MongoDB collections (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_KEY_ENABLED = "ENABLED";

	/**
	 * The key used for storing props in MongoDB collections (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_KEY_PROPS = "PROPS";

	/**
	 * The key used for storing resource in MongoDB collections (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_KEY_RESOURCE = "RESOURCE";

	/**
	 * The key used for storing expires in MongoDB collections (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_KEY_EXPIRES = "EXPIRES";

	/**
	 * The key used for storing username in MongoDB collections (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_KEY_USERNAME = "USERNAME";

	/**
	 * The key used for storing scope in MongoDB collections (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_KEY_SCOPE = "SCOPE";

	/**
	 * The key used for storing lookup key in MongoDB collections (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_KEY_LOOKUP_KEY = "LOOKUPKEY";

	/**
	 * The key used for storing unique ID in MongoDB collections (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_KEY_UNIQUE_ID = "UNIQUEID";

	/**
	 * The key used for storing type in MongoDB collections (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_KEY_TYPE = "TYPE";

	/**
	 * The key used for storing sub-type in MongoDB collections (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_KEY_SUBTYPE = "SUBTYPE";

	/**
	 * The key used for storing create date in MongoDB collections (as defined in
	 * the CustomStoreSample).
	 */
	private final static String MONGO_KEY_CREATEDAT = "CREATEDAT";

	/**
	 * The key used for storing lifetime in MongoDB collections (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_KEY_LIFETIME = "LIFETIME";

	/**
	 * The key used for storing token string in MongoDB collections (as defined in
	 * the CustomStoreSample).
	 */
	private final static String MONGO_KEY_TOKENSTRING = "TOKENSTRING";

	/**
	 * The key used for storing state ID in MongoDB collections (as defined in the
	 * CustomStoreSample).
	 */
	private final static String MONGO_KEY_STATEID = "STATEID";

	/**
	 * The MongoDB properties file.
	 */
	private final static String MONGO_PROPS_FILE = "../SupportFiles/mongoDB.props";

	/**
	 * The property used to store the database name for MongoDB. This is retrieved
	 * from the mongoDB.props file.
	 */
	private final static String MONGO_PROP_DBNAME = "DBNAME";

	/**
	 * The property used to store the database host for MongoDB. This is retrieved
	 * from the mongoDB.props file.
	 */
	private final static String MONGO_PROP_HOST = "HOST";

	/**
	 * The property used to store the database port for MongoDB. This is retrieved
	 * from the mongoDB.props file.
	 */
	private final static String MONGO_PROP_PORT = "PORT";

	/**
	 * The property used to store the administrative user for MongoDB. This is
	 * retrieved from the mongoDB.props file.
	 */
	private final static String MONGO_PROP_USER = "USER";

	/**
	 * The property used to store the administrative password for MongoDB. This is
	 * retrieved from the mongoDB.props file.
	 */
	private final static String MONGO_PROP_PWD = "PWD";

	/**
	 * The property used to store whether to start the embedded MongoDB in the test.
	 * This is retrieved from the mongoDB.props file.
	 */
	private final static String MONGO_PROP_START_MONGODB = "START_MONGODB";

	/**
	 * MongoDB collection containing clients.
	 */
	private static MongoCollection<Document> clientCollection;

	/**
	 * MongoDB collection containing tokens.
	 */
	private static MongoCollection<Document> tokenCollection;

	/**
	 * MongoDB collection containing consents.
	 */
	private static MongoCollection<Document> consentCollection;

	/**
	 * A test user used for registering clients, etc. This user is defined in the
	 * server.xml.
	 */
	private static final String TEST_USER_ID = "testuser";

	/**
	 * The password for {@link #TEST_USER_ID}. This password is defined in the
	 * server.xml.
	 */
	private static final String TEST_USER_PW = "password";

	/**
	 * Base64 encoded credentials for {@link #TEST_USER_ID}, suitable for use in an
	 * HTTP Authentication header for use in basic authentication.
	 */
	private static final String CREDENTIALS_BASE64 = Base64.getEncoder()
			.encodeToString((TEST_USER_ID + ":" + TEST_USER_PW).getBytes());

	/**
	 * The OAuth provider configuration ID. This is defined in the 'oauthProvider'
	 * element of the sever.xml.
	 */
	private static final String OAUTH_PROVIDER_ID = "OauthConfigSample";

	/**
	 * The OAuth provider configuration ID. This is defined in the
	 * 'openidConnectProvider' element of the sever.xml.
	 */
	private static final String OIDC_PROVIDER_ID = "OidcConfigSample";

	/**
	 * The host name for the Liberty server that is the OIDC provider.
	 */
	private static final String OP_HOST = "localhost";

	/**
	 * The port for the Liberty server that is the OIDC provider.
	 */
	private static final String OP_PORT = "8443";

	/**
	 * The host name for the Liberty server that is the resource provider.
	 */
	private static final String RP_HOST = "localhost";

	/**
	 * The host name for the Liberty server that is the resource provider.
	 */
	private static final String RP_PORT = "8443";

	/**
	 * The client ID for the client that will be used for testing.
	 */
	private static final String CLIENT_ID = "client01";

	/**
	 * The client name for the client that will be used for testing.
	 */
	private static final String CLIENT_NAME = "client01 name";

	/**
	 * The client secret that will be used for testing.
	 */
	private static final String CLIENT_SECRET = "secret01";

	/**
	 * The XOR'd {@link #CLIENT_SECRET} that will be used for testing.
	 */
	private static final String CLIENT_SECRET_XOR = "{xor}LDo8LTorb24=";

	/**
	 * The OIDC registration URI.
	 */
	private static final String REGISTRATION_URI = "https://" + OP_HOST + ":" + OP_PORT + "/oidc/endpoint/"
			+ OIDC_PROVIDER_ID + "/registration";

	/**
	 * The OIDC redirect URI for {@link #CLIENT_ID}.
	 */
	private static final String REDIRECT_URI = "https://" + RP_HOST + ":" + RP_PORT + "/oidcclient/redirect/"
			+ CLIENT_ID;

	/**
	 * The OIDC authorize URI.
	 */
	private static final String AUTHORIZE_ENDPOINT = "https://" + OP_HOST + ":" + OP_PORT + "/oidc/endpoint/"
			+ OIDC_PROVIDER_ID + "/authorize";

	/**
	 * A GSON instance.
	 */
	private static final Gson gson = new Gson();

	/**
	 * A Type of Map<String, Object>.
	 */
	private static final Type map_of_string_object_type = new TypeToken<Map<String, Object>>() {
	}.getType();

	static {
		/*
		 * Enable HTTP client logging. Will log to stderr.
		 */
		try {
			StringBuffer sb = new StringBuffer();
			sb.append(".level = INFO").append("\n");
			sb.append("handlers=java.util.logging.ConsoleHandler").append("\n");
			sb.append("java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter").append("\n");
			sb.append("java.util.logging.ConsoleHandler.level = ALL").append("\n");
			sb.append("org.apache.http.headers.level = FINEST").append("\n");
			sb.append("org.apache.http.wire.level = ALL").append("\n");
			LogManager.getLogManager()
					.readConfiguration(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			/*
			 * Should not occur. Logging for HttpClient will be non-functional.
			 */
		}
	}

	@AfterAll
	public static void afterAll() {
		if (mongoClient != null) {
			System.out.println("Closed mongoClient");
			mongoClient.close();
		}
		if (mongodExecutable != null) {
			mongodExecutable.stop();
			System.out.println("Stopped mongoDB server.");
		}
	}

	/**
	 * Assert the client data stored in MongoDB.
	 */
	private static void assertClientDataInMongo() {

		Document clientDoc = getClientDocumentFromMongo();
		assertNotNull(clientDoc, "Did not find client document in MongoDB.");

		/*
		 * Get the actual values from the client document.
		 */
		Object actualClientId = clientDoc.get(MONGO_KEY_CLIENT_ID);
		Object actualProviderId = clientDoc.get(MONGO_KEY_PROVIDER_ID);
		Object actualDisplayName = clientDoc.get(MONGO_KEY_DISPLAY_NAME);
		Object actualClientSecret = clientDoc.get(MONGO_KEY_CLIENT_SECRET);
		Object actualMetadata = clientDoc.get(MONGO_KEY_METADATA);
		Object actualEnabled = clientDoc.get(MONGO_KEY_ENABLED);

		/*
		 * Validate the actual values with the expected values.
		 */
		assertEquals(CLIENT_ID, actualClientId, "MongoDB client document did not contain expected client ID.");
		assertEquals(OAUTH_PROVIDER_ID, actualProviderId,
				"MongoDB client document did not contain expected provider ID.");
		assertEquals(CLIENT_NAME, actualDisplayName, "MongoDB client document did not contain expected display name.");
		assertEquals(CLIENT_SECRET_XOR, actualClientSecret,
				"MongoDB client document did not contain expected client secret.");
		assertEquals(true, actualEnabled, "MongoDB client document did not contain expected enabled state.");

		/*
		 * There is more than just the redirect URI in the metadata, but that is the
		 * only part we set, so just check for it.
		 */
		assertTrue(actualMetadata.toString().contains("\"redirect_uris\":[\"" + REDIRECT_URI + "\"]"),
				"MongoDB client document did not contain expected redirecty URI in client metadata.");
	}

	/**
	 * Assert that there is no client data stored in MongoDB.
	 */
	private static void assertClientDataNotInMongo() {
		assertNull(getClientDocumentFromMongo(), "Found unexpected client document in MongoDB.");
	}

	/**
	 * Assert the consent data stored in MongoDB.
	 */
	private static void assertConsentDataInMongo() {

		Document consentDoc = getConsentDocumentFromMongo();
		assertNotNull(consentDoc, "Did not find consent document in MongoDB.");

		/*
		 * Get the actual values from the consent document.
		 */
		Object actualClientId = consentDoc.get(MONGO_KEY_CLIENT_ID);
		Object actualProviderId = consentDoc.get(MONGO_KEY_PROVIDER_ID);
		Object actualUsername = consentDoc.get(MONGO_KEY_USERNAME);
		Object actualScope = consentDoc.get(MONGO_KEY_SCOPE);
		Object actualResource = consentDoc.get(MONGO_KEY_RESOURCE);
		Object actualExpires = consentDoc.get(MONGO_KEY_EXPIRES);
		Object actualProps = consentDoc.get(MONGO_KEY_PROPS);

		/*
		 * Validate the actual values with the expected values.
		 */
		assertEquals(CLIENT_ID, actualClientId, "MongoDB consent document did not contain expected client ID.");
		assertEquals(OAUTH_PROVIDER_ID, actualProviderId,
				"MongoDB consent document did not contain expected provider ID.");
		assertEquals(TEST_USER_ID, actualUsername, "MongoDB consent document did not contain expected username.");
		assertEquals("openid", actualScope, "MongoDB consent document did not contain expected scope.");
		assertEquals(null, actualResource, "MongoDB consent document did not contain expected resource.");
		assertNotNull(actualExpires, "MongoDB consent document did not contain expected expires.");
		assertEquals("{\"\":\"\"}", actualProps, "MongoDB consent document did not contain expected props.");
	}

	/**
	 * Assert that there is no consent data stored in MongoDB.
	 */
	private static void assertConsentDataNotInMongo() {
		assertNull(getConsentDocumentFromMongo(), "Found unexpected consent document in MongoDB.");
	}

	/**
	 * Assert the token data stored in MongoDB.
	 */
	private static void assertTokenDataInMongo() {

		Document tokenDoc = getTokenDocumentFromMongo();
		assertNotNull(tokenDoc, "Did not find token document in MongoDB.");

		/*
		 * Get the actual values from the token document.
		 */
		Object actualLookupKey = tokenDoc.get(MONGO_KEY_LOOKUP_KEY);
		Object actualUniqueId = tokenDoc.get(MONGO_KEY_UNIQUE_ID);
		Object actualProviderId = tokenDoc.get(MONGO_KEY_PROVIDER_ID);
		Object actualType = tokenDoc.get(MONGO_KEY_TYPE);
		Object actualSubType = tokenDoc.get(MONGO_KEY_SUBTYPE);
		Object actualCreateDate = tokenDoc.get(MONGO_KEY_CREATEDAT);
		Object actualLifetime = tokenDoc.get(MONGO_KEY_LIFETIME);
		Object actualExpires = tokenDoc.get(MONGO_KEY_EXPIRES);
		Object actualTokenString = tokenDoc.get(MONGO_KEY_TOKENSTRING);
		Object actualClientId = tokenDoc.get(MONGO_KEY_CLIENT_ID);
		Object actualUsername = tokenDoc.get(MONGO_KEY_USERNAME);
		Object actualScope = tokenDoc.get(MONGO_KEY_SCOPE);
		Object actualRedirectUri = tokenDoc.get(MONGO_KEY_REDIRECT_URI);
		Object actualStateId = tokenDoc.get(MONGO_KEY_STATEID);
		Object actualProps = tokenDoc.get(MONGO_KEY_PROPS);

		/*
		 * Validate the actual values with the expected values.
		 */
		assertNotNull(actualLookupKey, "MongoDB token document did not contain expected lookup key.");
		assertNotNull(actualUniqueId, "MongoDB token document did not contain expected unique ID.");
		assertEquals(OAUTH_PROVIDER_ID, actualProviderId,
				"MongoDB token document did not contain expected provider ID.");
		assertEquals("authorization_grant", actualType, "MongoDB token document did not contain expected type.");
		assertEquals("authorization_code", actualSubType, "MongoDB token document did not contain expected sub type.");
		assertNotNull(actualCreateDate, "MongoDB token document did not contain expected create date.");
		assertEquals(5, actualLifetime, "MongoDB token document did not contain expected lifetime.");
		assertNotNull(actualExpires, "MongoDB token document did not contain expected expires.");
		assertNotNull(actualTokenString, "MongoDB token document did not contain expected token string.");
		assertEquals(CLIENT_ID, actualClientId, "MongoDB token document did not contain expected client ID.");
		assertEquals(TEST_USER_ID, actualUsername, "MongoDB token document did not contain expected username.");
		assertEquals("openid", actualScope, "MongoDB token document did not contain expected scope.");
		assertEquals(REDIRECT_URI, actualRedirectUri,
				"MongoDB token document did not contain expected redirect endpoint.");
		assertNotNull(actualStateId, "MongoDB token document did not contain expected state ID.");
		assertEquals("{\"grant_type\":\"authorization_code\"}", actualProps,
				"MongoDB token document did not contain expected props.");
	}

	/**
	 * Assert that there is no token data stored in MongoDB.
	 */
	private static void assertTokenDataNotInMongo() {
		assertNull(getTokenDocumentFromMongo(), "Found unexpected token document in MongoDB.");
	}

	@BeforeAll
	public static void beforeAll() throws Exception {

		/*
		 * Get the MongoDB connection properties. These are read in from the
		 * mongoDB.props file.
		 */
		String mongodbName = "oauthSample";
		String mongodbHost = "localHost";
		int mongodbPort = 27017;
		String mongodbUser = null;
		String mongodbPassword = null;
		boolean mongodbStart = true;
		try {
			Properties props = new Properties();
			props.load(new FileReader(MONGO_PROPS_FILE));
			mongodbName = props.getProperty(MONGO_PROP_DBNAME, "oauthSample");
			mongodbHost = props.getProperty(MONGO_PROP_HOST, "localhost");
			mongodbPort = Integer.valueOf(props.getProperty(MONGO_PROP_PORT, "27017"));
			mongodbUser = props.getProperty(MONGO_PROP_USER);
			mongodbPassword = props.getProperty(MONGO_PROP_PWD);
			mongodbStart = Boolean.valueOf(props.getProperty(MONGO_PROP_START_MONGODB, "true"));

			/*
			 * We don't currently start MongoDb with authentication enabled. Fail if the
			 * MongoDB user and password has been specified and the test is to start the
			 * MongoDB instance.
			 */
			assertFalse(mongodbStart && (mongodbUser != null || mongodbPassword != null),
					"The embedded MongoDB instance started by this test does not support authentication. "
							+ "Either disable authentication by not specifying the user and password or use "
							+ "an external MongoDB instance that has authentication enabled.");
		} catch (FileNotFoundException e) {
			/* Use defaults. */
		}

		System.out.println("=================================================================");
		System.out.println("DBNAME: " + mongodbName);
		System.out.println("HOST: " + mongodbHost);
		System.out.println("PORT: " + mongodbPort);
		System.out.println("USER: " + mongodbUser);
		System.out.println("PWD: " + mongodbPassword);
		System.out.println("START_MONGODB: " + mongodbStart);
		System.out.println("=================================================================");

		/*
		 * Startup a MondoDB instance.
		 */
		if (mongodbStart) {
			System.out.println("Starting a local mongoDB.");
			MongodStarter starter = MongodStarter.getDefaultInstance();
			MongodConfigBuilder builder = new MongodConfigBuilder().version(Version.V3_6_5)
					.net(new Net(mongodbHost, mongodbPort, Network.localhostIsIPv6()));
			mongodExecutable = starter.prepare(builder.build());
			mongodExecutable.start();
		} else {
			System.out.println("Will connect to an existing mongoDB at " + mongodbHost +":" + mongodbPort);
		}

		/*
		 * Open up a connection to the MongoDB and get a reference to each of the
		 * collections. We will use these to verify the data is being written to MongoDB
		 * through the OAuthStore implementation.
		 */
		MongoDatabase mongoDb = getMongoDatabase(mongodbName, mongodbHost, mongodbPort, mongodbUser, mongodbPassword);
		clientCollection = mongoDb.getCollection(MONGO_OAUTH_CLIENT_TABLE);
		consentCollection = mongoDb.getCollection(MONGO_OAUTH_CONSENT_TABLE);
		tokenCollection = mongoDb.getCollection(MONGO_OAUTH_TOKEN_TABLE);
	}

	/**
	 * Retrieve the MongoDB Document that contains the client data.
	 * 
	 * @return The document containing the client data or null if it does not exist.
	 */
	private static Document getClientDocumentFromMongo() {
		/*
		 * Get the client document from MongoDB.
		 */
		Document queryDoc = new Document(MONGO_KEY_CLIENT_ID, CLIENT_ID);
		queryDoc.append(MONGO_KEY_PROVIDER_ID, OAUTH_PROVIDER_ID);
		return clientCollection.find(queryDoc).limit(1).first();
	}

	/**
	 * Retrieve the MongoDB Document that contains the consent data.
	 * 
	 * @return The document containing the consent data or null if it does not
	 *         exist.
	 */
	private static Document getConsentDocumentFromMongo() {
		/*
		 * Get the consent document from MongoDB.
		 */
		Document queryDoc = new Document(MONGO_KEY_CLIENT_ID, CLIENT_ID);
		queryDoc.append(MONGO_KEY_USERNAME, TEST_USER_ID);
		queryDoc.append(MONGO_KEY_RESOURCE, null);
		queryDoc.append(MONGO_KEY_PROVIDER_ID, OAUTH_PROVIDER_ID);
		return consentCollection.find(queryDoc).limit(1).first();
	}

	/**
	 * Get a connection to the MongoDB database. This connection can be used to
	 * verify that the OAuthStore implementation is writing the relevant data to
	 * MongoDB.
	 * 
	 * @param mongodbName     The database name.
	 * @param mongodbHost     The host.
	 * @param mongodbPort     The port.
	 * @param mongodbUser     The administrative user (can be null).
	 * @param mongodbPassword The administrative user (can be null).
	 * @return The database connection.
	 */
	private static MongoDatabase getMongoDatabase(String mongodbName, String mongodbHost, int mongodbPort,
			String mongodbUser, String mongodbPassword) {

		MongoClientSettings settings = null;
		if (mongodbUser != null && mongodbPassword != null) {
			MongoCredential credential = MongoCredential.createCredential(mongodbUser, mongodbName,
					mongodbPassword.toCharArray());
			// Add any additional appropriate connection settings
			settings = MongoClientSettings.builder().credential(credential)
					.applyToClusterSettings(
							builder -> builder.hosts(Arrays.asList(new ServerAddress(mongodbHost, mongodbPort))))
					.build();
		} else {
			// Add any additional appropriate connection settings
			settings = MongoClientSettings.builder()
					.applyToClusterSettings(
							builder -> builder.hosts(Arrays.asList(new ServerAddress(mongodbHost, mongodbPort))))
					.build();
		}

		mongoClient = MongoClients.create(settings);
		return mongoClient.getDatabase(mongodbName);
	}

	/**
	 * Retrieve the MongoDB Document that contains the token data.
	 * 
	 * @return The document containing the token data or null if it does not exist.
	 */
	private static Document getTokenDocumentFromMongo() {
		/*
		 * Get the token document from MongoDB.
		 */
		Document queryDoc = new Document(MONGO_KEY_CLIENT_ID, CLIENT_ID);
		queryDoc.append(MONGO_KEY_USERNAME, TEST_USER_ID);
		queryDoc.append(MONGO_KEY_RESOURCE, null);
		queryDoc.append(MONGO_KEY_PROVIDER_ID, OAUTH_PROVIDER_ID);
		return tokenCollection.find(queryDoc).limit(1).first();
	}

	/**
	 * Create an {@link CloseableHttpClient} that trusts all HTTPS connections.
	 * 
	 * @return An HTTP client that trusts all HTTPS connections.
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 */
	private static CloseableHttpClient getTrustAllHttpClient()
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		final SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, (x509CertChain, authType) -> true)
				.build();
		return HttpClientBuilder.create().setSSLContext(sslContext)
				.setConnectionManager(new PoolingHttpClientConnectionManager(RegistryBuilder
						.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.INSTANCE)
						.register("https", new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
						.build()))
				.build();
	}

	/**
	 * This test runs some HTTP requests against the OIDC provider to trigger calls
	 * to the custom OAuthStore implementation. This test is not exhaustive and does
	 * not exercise all methods within the interface. For example the update methods
	 * and some of the delete methods are not called.
	 * 
	 * @throws Exception If the test failed for some unforeseen reason.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testCustomOauthStore() throws Exception {

		/*
		 * There should no data for the client, consent, or token.
		 */
		assertClientDataNotInMongo();
		assertConsentDataNotInMongo();
		assertTokenDataNotInMongo();

		/*
		 * Register the client.
		 */
		Executor executor = Executor.newInstance(getTrustAllHttpClient());
		executor.execute(Request.Post(REGISTRATION_URI).addHeader("Content-Type", "application/json")
				.addHeader("Authorization", "Basic " + CREDENTIALS_BASE64)
				.bodyString("{\"client_id\":\"" + CLIENT_ID + "\",\"client_secret\":\"" + CLIENT_SECRET
						+ "\",\"client_name\":\"" + CLIENT_NAME + "\",\"redirect_uris\":[\"" + REDIRECT_URI
						+ "\"],\"scope\":\"ALL_SCOPES\"}", ContentType.DEFAULT_TEXT))
				.handleResponse(new AssertResponseHandler(201));

		/*
		 * Registration should have added a client entry.
		 */
		assertClientDataInMongo();
		assertConsentDataNotInMongo();
		assertTokenDataNotInMongo();

		/*
		 * Send an request to the authorize endpoint. We will receive the consent form
		 * in response.
		 */
		Collection<BasicNameValuePair> nvps = new HashSet<BasicNameValuePair>();
		nvps.add(new BasicNameValuePair("auto", "true"));
		nvps.add(new BasicNameValuePair("response_type", "code"));
		nvps.add(new BasicNameValuePair("user_name", CLIENT_NAME));
		nvps.add(new BasicNameValuePair("client_id", CLIENT_ID));
		nvps.add(new BasicNameValuePair("redirect_uri", REDIRECT_URI));
		nvps.add(new BasicNameValuePair("authorize_endpoint", AUTHORIZE_ENDPOINT));
		nvps.add(new BasicNameValuePair("state", "abcdefg"));
		nvps.add(new BasicNameValuePair("scope", "openid"));
		nvps.add(new BasicNameValuePair("autoauthz", "true"));
		String responseContent = executor.execute(Request.Post(AUTHORIZE_ENDPOINT).bodyForm(nvps))
				.handleResponse(new AssertResponseHandler(200));

		/*
		 * Populate the consent form and submit the response.
		 */
		Pattern p = Pattern.compile(".+oauthFormData=(\\{.*\\}).+", Pattern.DOTALL);
		Matcher m = p.matcher(responseContent);
		assertTrue(m.matches(), "Did not find 'oauthFormData' in consent form.");
		Map<String, Object> oauthFormData = gson.fromJson(m.group(1), map_of_string_object_type);
		nvps = new HashSet<BasicNameValuePair>();
		nvps.add(new BasicNameValuePair("consentNonce", (String) oauthFormData.get("consentNonce")));
		nvps.add(new BasicNameValuePair("client_id", CLIENT_ID));
		nvps.add(new BasicNameValuePair("response_type", "code"));
		nvps.add(new BasicNameValuePair("state", "abcdefg"));
		nvps.add(new BasicNameValuePair("scope", "openid"));
		nvps.add(new BasicNameValuePair("prompt", "none")); // 'none' == allow, remember my decision
		nvps.add(new BasicNameValuePair("redirect_uri", REDIRECT_URI));
		Map<String, Object> extendedProperties = (Map<String, Object>) oauthFormData.get("extendedProperties");
		nvps.add(new BasicNameValuePair("nonce", (String) extendedProperties.get("nonce")));
//		nvps.add(new BasicNameValuePair("acr_values", (String) extendedProperties.get("acr_values")));
		nvps.add(new BasicNameValuePair("response_mode", (String) extendedProperties.get("response_mode")));
		nvps.add(new BasicNameValuePair("action", AUTHORIZE_ENDPOINT));
		nvps.add(new BasicNameValuePair("method", "POST"));
		executor.execute(Request.Post(AUTHORIZE_ENDPOINT).bodyForm(nvps))
				.handleResponse(new AssertResponseHandler(302));

		/*
		 * Submitting the consent form will have created entries for both a token and a
		 * consent.
		 */
		assertClientDataInMongo();
		assertConsentDataInMongo();
		assertTokenDataInMongo();

		/*
		 * Delete the client.
		 */
		executor.execute(Request.Delete(REGISTRATION_URI + "/" + CLIENT_ID).addHeader("Authorization",
				"Basic " + CREDENTIALS_BASE64)).handleResponse(new AssertResponseHandler(204));
		assertClientDataNotInMongo();

		/*
		 * Allow the timer task / reaper to remove expired tokens and consents.
		 */
		Thread.sleep(15000); // 5 seconds for authorization code lifetime, 5 seconds from
								// cleanupExpiredInterval
		assertTokenDataNotInMongo();
		assertConsentDataNotInMongo();
	}

	/**
	 * Simple response handler to return the content and make assertions on the
	 * return code.
	 */
	class AssertResponseHandler implements ResponseHandler<String> {

		final int expectedStatus;
		String content = null;

		AssertResponseHandler(int expectedResponse) {
			this.expectedStatus = expectedResponse;
		}

		@Override
		public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {

			assertEquals(expectedStatus, response.getStatusLine().getStatusCode(), "Unexpected HTTP response status.");

			HttpEntity responseEntity = response.getEntity();
			if (responseEntity != null) {
				content = EntityUtils.toString(responseEntity);
			}

			return content;
		}
	}
}
