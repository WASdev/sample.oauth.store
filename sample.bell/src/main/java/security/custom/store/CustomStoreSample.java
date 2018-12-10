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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.bson.Document;

import com.ibm.websphere.security.oauth20.store.OAuthClient;
import com.ibm.websphere.security.oauth20.store.OAuthConsent;
import com.ibm.websphere.security.oauth20.store.OAuthStore;
import com.ibm.websphere.security.oauth20.store.OAuthStoreException;
import com.ibm.websphere.security.oauth20.store.OAuthToken;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

/**
 * The main purpose of this sample is to demonstrate the use of a CustomStore
 * for an OAuth Provider. It is provided as-is.
 * 
 * It is currently a lazy application user feature. The mongoDB database will
 * not be accessed until a call is made to the customStore. 
 * 
 * It uses a MongoDB back end.
 **/
public class CustomStoreSample implements OAuthStore {

	public final static String MONGO_PROPS_FILE = "mongoDB.props";
	private String dbName = "oauthSample";
	private String dbHost = "localhost";
	private String dbUser = null;
	private String dbPwd = null;
	private int dbPort = 27017;
	boolean loadedPropsFile = false;

	private MongoDatabase db = null;
	private MongoCollection<Document> clientCollection = null;
	private MongoCollection<Document> tokenCollection = null;
	private MongoCollection<Document> consentCollection = null;

	// Collection types in the database
	static String OAUTHCLIENT = "OauthClient";
	static String OAUTHTOKEN = "OauthToken";
	static String OAUTHCONSENT = "OauthConsent";

	// Keys in the database
	final static String LOOKUPKEY = "LOOKUPKEY";
	final static String UNIQUEID = "UNIQUEID";
	final static String COMPONENTID = "COMPONENTID";
	final static String TYPE = "TYPE";
	final static String SUBTYPE = "SUBTYPE";
	final static String CREATEDAT = "CREATEDAT";
	final static String LIFETIME = "LIFETIME";
	final static String EXPIRES = "EXPIRES"; // long
	final static String TOKENSTRING = "TOKENSTRING";
	final static String CLIENTID = "CLIENTID";
	final static String USERNAME = "USERNAME";
	final static String SCOPE = "SCOPE";
	final static String REDIRECTURI = "REDIRECTURI";
	final static String STATEID = "STATEID";
	final static String EXTENDEDFIELDS = "EXTENDEDFIELDS";
	final static String PROPS = "PROPS";
	final static String RESOURCE = "RESOURCE";
	final static String PROVIDERID = "PROVIDERID";
	final static String CLIENTSECRET = "CLIENTSECRET";
	final static String DISPLAYNAME = "DISPLAYNAME";
	final static String ENABLED = "ENABLED";
	final static String METADATA = "METADATA";

	public CustomStoreSample() {
		System.out.println("CustomStoreSample init");
	}

	/**
	 * Simple helper method to get a connection to mongoDB.
	 * 
	 * It is not a general recommendation for all CustomStore implementations.
	 *
	 * @return
	 */
	private synchronized MongoDatabase getDB() {
		if (db == null) {

			getDatabaseConfig();

			MongoClient mongoClient = null;

			System.out
			.println("CustomStoreSample connecting to the " + dbName + " database at " + dbHost + ":" + dbPort);

			if (loadedPropsFile) {
				MongoClientSettings settings = null;
				if (dbUser != null && dbPwd != null) {
					MongoCredential credential = MongoCredential.createCredential(dbUser, dbName, dbPwd.toCharArray());
					// Add any additional appropriate connection settings
					settings = MongoClientSettings.builder().credential(credential).applyToClusterSettings(
							builder -> builder.hosts(Arrays.asList(new ServerAddress(dbHost, dbPort)))).build();
				} else {
					// Add any additional appropriate connection settings
					settings = MongoClientSettings.builder().applyToClusterSettings(
							builder -> builder.hosts(Arrays.asList(new ServerAddress(dbHost, dbPort)))).build();
				}
				mongoClient = MongoClients.create(settings);
			} else {
				mongoClient = MongoClients.create();
			}
			db = mongoClient.getDatabase(dbName);
			System.out.println("CustomStoreSample connected to the database " + dbName);

		}

		return db;

	}

	private MongoCollection<Document> getClientCollection() {
		if (clientCollection == null) {
			clientCollection = getDB().getCollection(OAUTHCLIENT);
		}
		return clientCollection;
	}

	private MongoCollection<Document> getTokenCollection() {
		if (tokenCollection == null) {
			tokenCollection = getDB().getCollection(OAUTHTOKEN);
		}
		return tokenCollection;
	}

	private MongoCollection<Document> getConsentCollection() {
		if (consentCollection == null) {
			consentCollection = getDB().getCollection(OAUTHCONSENT);
		}
		return consentCollection;
	}

	/**
	 * This helper method uses a properties file to get the database connection
	 * parameters. It was done this way to support local testing of both Bell
	 * and User Feature configurations.
	 * 
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
			BufferedReader br = new BufferedReader(
					new InputStreamReader(new FileInputStream(f), Charset.forName("UTF8")));
			try {
				String line;
				while ((line = br.readLine()) != null) {
					if (line.startsWith("#") || line.trim().equals("")) { 
						continue;
					}
					String[] prop = line.split(":");
					if (prop.length != 2) {
						System.out.println("CustomStoreSample Exception key:value syntax of properties in " + MONGO_PROPS_FILE
								+ ", line is: " + line);
					} else {
						if (prop[0].equals("DBNAME")) {
							dbName = prop[1];
						} else if (prop[0].equals("HOST")) {
							dbHost = prop[1];
						} else if (prop[0].equals("PWD")) {
							dbPwd = prop[1];
						} else if (prop[0].equals("PORT")) {
							dbPort = Integer.parseInt(prop[1]);
						} else if (prop[0].equals("USER")) {
							dbUser = prop[1];
						} else {
							System.out.println("CustomStoreSample Unexpected property in " + MONGO_PROPS_FILE + ": " + prop[0]);
						}
					}
				}
				loadedPropsFile = true;

			} finally {
				br.close();
			}
		} catch (IOException e) {
			System.out.println("Database config could not be retrieved from " + MONGO_PROPS_FILE +". Using defaults.");
		}
	}

	@Override
	public void create(OAuthClient oauthClient) throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getClientCollection();
			col.insertOne(createClientDBObjectHelper(oauthClient));
		} catch (Exception e) {
			throw new OAuthStoreException("Failed to process create on OAuthClient " + oauthClient.getClientId(), e);
		}
	}

	private Document createClientDBObjectHelper(OAuthClient oauthClient) {
		Document d = new Document(CLIENTID, oauthClient.getClientId());

		d.append(PROVIDERID, oauthClient.getProviderId());
		d.append(CLIENTSECRET, /* PasswordUtil.passwordEncode( */oauthClient.getClientSecret()/* ) */);
		d.append(DISPLAYNAME, oauthClient.getDisplayName());
		d.append(ENABLED, oauthClient.isEnabled());
		d.append(METADATA, oauthClient.getClientMetadata());
		return d;
	}

	@Override
	public void create(OAuthToken oauthToken) throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getTokenCollection();
			col.insertOne(createTokenDBObjectHelper(oauthToken));
		} catch (Exception e) {
			throw new OAuthStoreException("Failed to process create on OAuthToken " + oauthToken.getClientId(), e);
		}
	}

	private Document createTokenDBObjectHelper(OAuthToken oauthToken) {
		Document d = new Document(LOOKUPKEY, oauthToken.getLookupKey());
		d.append(UNIQUEID, oauthToken.getUniqueId());
		d.append(PROVIDERID, oauthToken.getProviderId());
		d.append(TYPE, oauthToken.getType());
		d.append(SUBTYPE, oauthToken.getSubType());
		d.append(CREATEDAT, oauthToken.getCreatedAt());
		d.append(LIFETIME, oauthToken.getLifetimeInSeconds());
		d.append(EXPIRES, oauthToken.getExpires());
		d.append(TOKENSTRING, /* PasswordUtil.passwordEncode( */oauthToken.getTokenString()/* ) */);
		d.append(CLIENTID, oauthToken.getClientId());
		d.append(USERNAME, oauthToken.getUsername());
		d.append(SCOPE, oauthToken.getScope());
		d.append(REDIRECTURI, oauthToken.getRedirectUri());
		d.append(STATEID, oauthToken.getStateId());
		d.append(PROPS, oauthToken.getTokenProperties());
		return d;
	}

	@Override
	public void create(OAuthConsent oauthConsent) throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getConsentCollection();
			col.insertOne(createConsentDBObjectHelper(oauthConsent));
		} catch (Exception e) {
			throw new OAuthStoreException("Failed to process create on OAuthConsent " + oauthConsent.getClientId(), e);
		}
	}

	private Document createConsentDBObjectHelper(OAuthConsent oauthConsent) {
		Document d = new Document(CLIENTID, oauthConsent.getClientId());
		d.append(USERNAME, oauthConsent.getUser());
		d.append(SCOPE, oauthConsent.getScope());
		d.append(RESOURCE, oauthConsent.getResource());
		d.append(PROVIDERID, oauthConsent.getProviderId());
		d.append(EXPIRES, oauthConsent.getExpires());
		d.append(PROPS, oauthConsent.getConsentProperties());
		return d;
	}

	@Override
	public OAuthClient readClient(String providerId, String clientId) throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getClientCollection();
			Document d = new Document(CLIENTID, clientId);
			d.append(PROVIDERID, providerId);
			FindIterable<Document> findResult = col.find(d).limit(1);
			Document dbo = findResult.first();
			if (dbo == null) {
				System.out.println("CustomStoreSample readClient Did not find clientId " + clientId + " under " + providerId);
				return null;
			}
			
			System.out.println("CustomStoreSample readClient Found clientId " + clientId + " under " + providerId + " _id " + dbo.get("_id"));
			return createOAuthClientHelper(dbo);

		} catch (Exception e) {
			throw new OAuthStoreException("Failed to readClient " + clientId + " under " + providerId, e);
		}
	}

	private OAuthClient createOAuthClientHelper(Document dbo) {
		return new OAuthClient((String) dbo.get(PROVIDERID), (String) dbo.get(CLIENTID), (String) dbo.get(CLIENTSECRET),
				(String) dbo.get(DISPLAYNAME), (boolean) dbo.get(ENABLED), (String) dbo.get(METADATA));
	}

	@Override
	public Collection<OAuthClient> readAllClients(String providerId, String attribute) throws OAuthStoreException {
		Collection<OAuthClient> results = null;

		try {
			MongoCollection<Document> col = getClientCollection();

			FindIterable<Document> findResult = null;
			if (attribute == null || attribute.isEmpty()) {
				findResult = col.find(new Document(PROVIDERID, providerId));
			} else {
				System.out.println("CustomStoreSample Attribute on readAllClients not implemented");
				// TODO Need to create query to check for all clients that
				// contain 'attribute' in metadata.
			}

			if (findResult != null) {
				MongoCursor<Document> mc = findResult.iterator();
				if (mc.hasNext()) {
					results = new HashSet<OAuthClient>();

					while (mc.hasNext()) {
						Document dbo = mc.next();
						results.add(createOAuthClientHelper(dbo));
					}
				}
			}

		} catch (Exception e) {
			throw new OAuthStoreException("Failed on readAllClients found under " + providerId, e);
		}
		return results;
	}

	@Override
	public OAuthToken readToken(String providerId, String lookupKey) throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getTokenCollection();
			FindIterable<Document> findResult = col.find(createTokenKeyHelper(providerId, lookupKey)).limit(1);
			Document dbo = findResult.first();
			if (dbo == null) {
				System.out.println("CustomStoreSample readToken Did not find lookupKey " + lookupKey);
				return null;
			}
			System.out.println("CustomStoreSample readToken Found lookupKey " + lookupKey + " under " + providerId + " _id " + dbo.get("_id"));
			return createOAuthTokenHelper(dbo);
		} catch (Exception e) {
			throw new OAuthStoreException("Failed to readToken " + lookupKey, e);
		}
	}

	private OAuthToken createOAuthTokenHelper(Document dbo) {
		return new OAuthToken((String) dbo.get(LOOKUPKEY), (String) dbo.get(UNIQUEID), (String) dbo.get(PROVIDERID),
				(String) dbo.get(TYPE), (String) dbo.get(SUBTYPE), (long) dbo.get(CREATEDAT), (int) dbo.get(LIFETIME),
				(long) dbo.get(EXPIRES), (String) dbo.get(TOKENSTRING), (String) dbo.get(CLIENTID),
				(String) dbo.get(USERNAME), (String) dbo.get(SCOPE), (String) dbo.get(REDIRECTURI),
				(String) dbo.get(STATEID), (String) dbo.get(PROPS));
	}

	@Override
	public Collection<OAuthToken> readAllTokens(String providerId, String username) throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getTokenCollection();
			Document d = new Document(USERNAME, username);
			d.append(PROVIDERID, providerId);
			FindIterable<Document> findResult = col.find(d);
			Collection<OAuthToken> collection = null;

			MongoCursor<Document> result = findResult.iterator();
			while (result.hasNext()) {
				Document dbo = result.next();
				if (collection == null) {
					collection = new ArrayList<OAuthToken>();
				}
				collection.add(createOAuthTokenHelper(dbo));
			}
			return collection;
		} catch (Exception e) {
			throw new OAuthStoreException("Failed to readAllTokens for " + username + " under " + providerId, e);
		}
	}

	@Override
	public int countTokens(String providerId, String username, String clientId) throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getTokenCollection();
			Document d = new Document(USERNAME, username);
			d.append(PROVIDERID, providerId);
			d.append(CLIENTID, clientId);
			return (int) col.countDocuments(d); // mongoDB returns as a long
		} catch (Exception e) {
			throw new OAuthStoreException("Failed on countTokens for " + username, e);
		}
	}

	@Override
	public OAuthConsent readConsent(String providerId, String username, String clientId, String resource)
			throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getConsentCollection();
			FindIterable<Document> findResult = col.find(createConsentKeyHelper(providerId, username, clientId, resource)).limit(1);
			Document dbo = findResult.first();
			if (dbo == null) {
				System.out.println("CustomStoreSample readConsent Did not find username " + username);
				return null;
			}
			System.out.println("CustomStoreSample readConsent Found clientId " + clientId + " under " + providerId + " _id " + dbo.get("_id"));
			return new OAuthConsent(clientId, (String) dbo.get(USERNAME), (String) dbo.get(SCOPE), resource, providerId,
					(long) dbo.get(EXPIRES), (String) dbo.get(PROPS));
		} catch (Exception e) {
			throw new OAuthStoreException("Failed on readConsent for " + username, e);
		}
	}

	@Override
	public void update(OAuthClient oauthClient) throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getClientCollection();
			col.updateOne(createClientKeyHelper(oauthClient), createClientDBObjectHelper(oauthClient), null);
		} catch (Exception e) {
			throw new OAuthStoreException("Failed on update for OAuthClient for " + oauthClient.getClientId(), e);
		}
	}

	@Override
	public void update(OAuthToken oauthToken) throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getTokenCollection();
			col.updateOne(createTokenKeyHelper(oauthToken), createTokenDBObjectHelper(oauthToken), null);
		} catch (Exception e) {
			throw new OAuthStoreException("Failed on update for OAuthToken for " + oauthToken.getClientId(), e);
		}
	}

	@Override
	public void update(OAuthConsent oauthConsent) throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getConsentCollection();
			col.updateOne(createConsentKeyHelper(oauthConsent), createConsentDBObjectHelper(oauthConsent), null);
		} catch (Exception e) {
			throw new OAuthStoreException("Failed on update for OAuthConsent for " + oauthConsent.getClientId(), e);
		}
	}

	@Override
	public void deleteClient(String providerId, String clientId) throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getClientCollection();
			col.deleteOne(createClientKeyHelper(providerId, clientId));
			System.out.println("CustomStoreSample deleteClient requested on clientId " + clientId + " under " + providerId);
		} catch (Exception e) {
			throw new OAuthStoreException("Failed on delete for OAuthClient for " + clientId, e);
		}
	}

	@Override
	public void deleteToken(String providerId, String lookupKey) throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getTokenCollection();
			col.deleteOne(createTokenKeyHelper(providerId, lookupKey));
		} catch (Exception e) {
			throw new OAuthStoreException("Failed on delete for OAuthToken for " + lookupKey, e);
		}
	}

	@Override
	public void deleteTokens(String providerId, long timestamp) throws OAuthStoreException {
		try {
			System.out.println("CustomStoreSample deleteTokens request for " + providerId + " expiring before " + timestamp);
			MongoCollection<Document> col = getTokenCollection();
			System.out.println("CustomStoreSample deleteTokens before " + col.countDocuments());
			Document query = new Document();
			query.put(EXPIRES, new Document("$lt", timestamp));
			query.put(PROVIDERID, providerId);
			col.deleteMany(query);
			System.out.println("CustomStoreSample deleteTokens after " + col.countDocuments());
		} catch (Exception e) {
			throw new OAuthStoreException("Failed on deleteTokens for time after " + timestamp, e);
		}
	}

	@Override
	public void deleteConsent(String providerId, String username, String clientId, String resource)
			throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getConsentCollection();
			Document db = new Document(CLIENTID, clientId);
			db.put(USERNAME, username);
			db.put(PROVIDERID, providerId);
			db.put(RESOURCE, resource);
			col.deleteOne(db);

		} catch (Exception e) {
			throw new OAuthStoreException("Failed on delete for Consent for " + username, e);
		}

	}

	@Override
	public void deleteConsents(String providerId, long timestamp) throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getConsentCollection();
			Document query = new Document();
			query.put(EXPIRES, new Document("$lt", timestamp));
			query.put(PROVIDERID, providerId);
			col.deleteMany(query);
		} catch (Exception e) {
			throw new OAuthStoreException("Failed on deleteConsents for time after " + timestamp, e);
		}
	}

	private Document createClientKeyHelper(OAuthClient oauthClient) {
		return createClientKeyHelper(oauthClient.getProviderId(), oauthClient.getClientId());
	}

	private Document createClientKeyHelper(String providerId, String clientId) {
		Document d = new Document(CLIENTID, clientId);
		d.append(PROVIDERID, providerId);
		return d;
	}

	private Document createTokenKeyHelper(OAuthToken oauthToken) {
		return createTokenKeyHelper(oauthToken.getProviderId(), oauthToken.getLookupKey());
	}

	private Document createTokenKeyHelper(String providerId, String lookupKey) {
		Document d = new Document(LOOKUPKEY, lookupKey);
		d.append(PROVIDERID, providerId);
		return d;
	}

	private Document createConsentKeyHelper(OAuthConsent oauthConsent) {
		return createConsentKeyHelper(oauthConsent.getClientId(), oauthConsent.getUser(), oauthConsent.getResource(),
				oauthConsent.getProviderId());
	}

	private Document createConsentKeyHelper(String providerId, String username, String clientId, String resource) {
		Document d = new Document(CLIENTID, clientId);
		d.append(USERNAME, username);
		d.append(RESOURCE, resource);
		d.append(PROVIDERID, providerId);
		return d;
	}

}
