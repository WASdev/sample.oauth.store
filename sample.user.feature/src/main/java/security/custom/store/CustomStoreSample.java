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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;

import com.ibm.websphere.security.oauth20.store.OAuthClient;
import com.ibm.websphere.security.oauth20.store.OAuthConsent;
import com.ibm.websphere.security.oauth20.store.OAuthStore;
import com.ibm.websphere.security.oauth20.store.OAuthStoreException;
import com.ibm.websphere.security.oauth20.store.OAuthToken;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

/**
 * The main purpose of this sample is to demonstrate the use of a CustomStore
 * for an OAuth Provider. It is provided as-is.
 * </p>
 * It is a lazy application user feature. The mongoDB database will
 * not be accessed until a call is made to the customStore.
 * </p>
 * It uses a MongoDB back end.
 * </p>
 * Some additional items (not a comprehensive list) to consider for a production ready CustomStore:
 * <ul>
 * <li>Database tuning (connection timeouts, etc)</li>
 * <li>Database fail over</li>
 * <li>Additional logging and tracing</li>
 * <li>Adding a custom primary key (see the _id field for MongoDB)</li>
 * <li>Appropriate security for your mongoDB implementation</li>
 * </ul>
 **/
public class CustomStoreSample implements OAuthStore {

	// To enable trace for this class, enable trace in the server.xml and add this package name to the trace specification: security.custom.store.*=all
	static final Logger LOGGER = Logger.getLogger(CustomStoreSample.class.getName());

	private MongoCollection<Document> clientCollection = null;
	private MongoCollection<Document> tokenCollection = null;
	private MongoCollection<Document> consentCollection = null;

	// Collection names in the database.
	private final static String OAUTHCLIENT = "OauthClient";
	private final static String OAUTHTOKEN = "OauthToken";
	private final static String OAUTHCONSENT = "OauthConsent";

	// Keys in the database
	private final static String LOOKUPKEY = "LOOKUPKEY";
	private final static String UNIQUEID = "UNIQUEID";
	private final static String TYPE = "TYPE";
	private final static String SUBTYPE = "SUBTYPE";
	private final static String CREATEDAT = "CREATEDAT";
	private final static String LIFETIME = "LIFETIME";
	private final static String EXPIRES = "EXPIRES"; // long
	private final static String TOKENSTRING = "TOKENSTRING";
	private final static String CLIENTID = "CLIENTID";
	private final static String USERNAME = "USERNAME";
	private final static String SCOPE = "SCOPE";
	private final static String REDIRECTURI = "REDIRECTURI";
	private final static String STATEID = "STATEID";
	private final static String PROPS = "PROPS";
	private final static String RESOURCE = "RESOURCE";
	private final static String PROVIDERID = "PROVIDERID";
	private final static String CLIENTSECRET = "CLIENTSECRET";
	private final static String DISPLAYNAME = "DISPLAYNAME";
	private final static String ENABLED = "ENABLED";
	private final static String METADATA = "METADATA";

	public CustomStoreSample() {
		LOGGER.log(Level.INFO, "CustomStoreSample User Feature initialized.");
	}

	/**
	 * Helper method to lazy initialize the collection for the OAuthClient collection
	 * @return A MongoCollection for the OAuthClient collection.
	 */
	private MongoCollection<Document> getClientCollection() {
		if (clientCollection == null) {
			clientCollection = MongoDBHelper.getInstance().getDB().getCollection(OAUTHCLIENT);
		}
		return clientCollection;
	}

	/**
	 * Helper method to lazy initialize the collection for the OAuthToken collection
	 * @return  A MongoCollection for the OAuthToken collection.
	 */
	private MongoCollection<Document> getTokenCollection() {
		if (tokenCollection == null) {
			tokenCollection = MongoDBHelper.getInstance().getDB().getCollection(OAUTHTOKEN);
		}
		return tokenCollection;
	}

	/**
	 * Helper method to lazy initialize the collection for the OAuthConsent collection
	 * @return  A MongoCollection for the OAuthConsent collection.
	 */
	private MongoCollection<Document> getConsentCollection() {
		if (consentCollection == null) {
			consentCollection = MongoDBHelper.getInstance().getDB().getCollection(OAUTHCONSENT);
		}
		return consentCollection;
	}

	
	@Override
	public void create(OAuthClient oauthClient) throws OAuthStoreException {
		try {
			MongoCollection<Document> col = getClientCollection();
			col.insertOne(createClientDBObjectHelper(oauthClient));
		} catch (Exception e) {
			throw new OAuthStoreException("Failed to process create on OAuthClient " + oauthClient.getClientId(), e);
		}
		LOGGER.log(Level.INFO, "Created OAuthClient: " + toString(oauthClient));
	}

	/**
	 * Helper method to create the mongoDB Document from an OAuthClient object
	 * @param oauthClient
	 * @return Document representing the provided OAuthClient
	 */
	private Document createClientDBObjectHelper(OAuthClient oauthClient) {
		Document d = new Document(CLIENTID, oauthClient.getClientId());

		d.append(PROVIDERID, oauthClient.getProviderId());
		d.append(CLIENTSECRET, oauthClient.getClientSecret());
		d.append(DISPLAYNAME, oauthClient.getDisplayName());
		d.append(ENABLED, true); /* oauthClient.isEnabled() - Currently can't set on registration end-point */
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
		LOGGER.log(Level.INFO, "Created OAuthToken: " + toString(oauthToken));
	}

	/**
	 * Helper method to create the mongoDB Document from an OAuthToken object
	 * @param oauthToken
	 * @return Document representing the provided OAuthToken
	 */
	private Document createTokenDBObjectHelper(OAuthToken oauthToken) {
		Document d = new Document(LOOKUPKEY, oauthToken.getLookupKey());
		d.append(UNIQUEID, oauthToken.getUniqueId());
		d.append(PROVIDERID, oauthToken.getProviderId());
		d.append(TYPE, oauthToken.getType());
		d.append(SUBTYPE, oauthToken.getSubType());
		d.append(CREATEDAT, oauthToken.getCreatedAt());
		d.append(LIFETIME, oauthToken.getLifetimeInSeconds());
		d.append(EXPIRES, oauthToken.getExpires());
		d.append(TOKENSTRING, oauthToken.getTokenString());
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
		LOGGER.log(Level.INFO, "Created OAuthConsent: " + toString(oauthConsent));
	}

	/**
	 * Helper method to create the mongoDB Document from an OAuthConsent object
	 * @param oauthConsent
	 * @return Document representing the provided OAuthConsent
	 */
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
				LOGGER.log(Level.FINEST, "readClient Did not find clientId " + clientId + " under " + providerId);
				return null;
			}
			
			LOGGER.log(Level.FINEST, "Found clientId " + clientId + " under " + providerId + " _id " + dbo.get("_id"));
			return createOAuthClientHelper(dbo);

		} catch (Exception e) {
			throw new OAuthStoreException("Failed to readClient " + clientId + " under " + providerId, e);
		}
	}

	/**
	 * Helper method to create an OAuthClient from a database Document object
	 * @param dbo
	 * @return OAuthClient created from the provided Document
	 */
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
				LOGGER.log(Level.WARNING, "Attribute on readAllClients not implemented");
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
				LOGGER.log(Level.FINEST, "readToken Did not find lookupKey " + lookupKey);
				return null;
			}
			LOGGER.log(Level.FINEST, "readToken Found lookupKey " + lookupKey + " under " + providerId + " _id " + dbo.get("_id"));
			return createOAuthTokenHelper(dbo);
		} catch (Exception e) {
			throw new OAuthStoreException("Failed to readToken " + lookupKey, e);
		}
	}

	/**
	 * Helper method to create an OAuthToken from a database Document object	
	 * @param dbo
	 * @return OAuthToken created from the provided Document
	 */
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
				LOGGER.log(Level.FINEST, "readConsent Did not find username " + username);
				return null;
			}
			LOGGER.log(Level.FINEST, "readConsent Found clientId " + clientId + " under " + providerId + " _id " + dbo.get("_id"));
			return createOAuthConsentHelper(dbo);
		} catch (Exception e) {
			throw new OAuthStoreException("Failed on readConsent for " + username, e);
		}
	}
	
	/**
	 * Helper method to create an OAuthConsent from a database Document object	
	 * @param dbo
	 * @return OAuthConsent created from the provided Document
	 */
	private OAuthConsent createOAuthConsentHelper(Document dbo) {
		return new OAuthConsent((String) dbo.get(CLIENTID), (String) dbo.get(USERNAME), (String) dbo.get(SCOPE), (String) dbo.get(RESOURCE), (String) dbo.get(PROVIDERID),
				(long) dbo.get(EXPIRES), (String) dbo.get(PROPS));
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
		long deleted = 0;
		try {
			MongoCollection<Document> col = getClientCollection();
			deleted = col.deleteOne(createClientKeyHelper(providerId, clientId)).getDeletedCount();
		} catch (Exception e) {
			throw new OAuthStoreException("Failed on delete for OAuthClient for " + clientId, e);
		}
		if (deleted > 0) {
			LOGGER.log(Level.INFO, "Deleted OAuthClient: providerId=" + providerId + ", clientId=" + clientId);
		}
	}

	@Override
	public void deleteToken(String providerId, String lookupKey) throws OAuthStoreException {
		long deleted = 0;
		try {
			MongoCollection<Document> col = getTokenCollection();
			deleted = col.deleteOne(createTokenKeyHelper(providerId, lookupKey)).getDeletedCount();
		} catch (Exception e) {
			throw new OAuthStoreException("Failed on delete for OAuthToken for " + lookupKey, e);
		}
		if (deleted > 0) {
			LOGGER.log(Level.INFO, "Deleted OAuthToken: providerId=" + providerId + ", lookupKey=" + lookupKey);
		}
	}

	@Override
	public void deleteTokens(String providerId, long timestamp) throws OAuthStoreException {
		long deleted = 0;
		try {
			MongoCollection<Document> col = getTokenCollection();
			LOGGER.log(Level.FINEST, "deleteTokens before count " + col.countDocuments());
			Document query = new Document();
			query.put(EXPIRES, new Document("$lt", timestamp));
			query.put(PROVIDERID, providerId);
			deleted = col.deleteMany(query).getDeletedCount();
			LOGGER.log(Level.FINEST, "deleteTokens after count " + col.countDocuments());
		} catch (Exception e) {
			throw new OAuthStoreException("Failed on deleteTokens for time after " + timestamp, e);
		}
		if (deleted > 0) {
			LOGGER.log(Level.INFO, "Deleted OAuthToken(s): providerId=" + providerId + ", timeStamp=" + timestamp
					+ ", count=" + deleted);
		}
	}

	@Override
	public void deleteConsent(String providerId, String username, String clientId, String resource)
			throws OAuthStoreException {
		long deleted = 0;
		try {
			MongoCollection<Document> col = getConsentCollection();
			Document db = new Document(CLIENTID, clientId);
			db.put(USERNAME, username);
			db.put(PROVIDERID, providerId);
			db.put(RESOURCE, resource);
			deleted = col.deleteOne(db).getDeletedCount();

		} catch (Exception e) {
			throw new OAuthStoreException("Failed on delete for Consent for " + username, e);
		}
		if (deleted > 0) {
			LOGGER.log(Level.INFO, "Deleted OAuthConsent: providerId=" + providerId + ", username=" + username
					+ ", clientId=" + clientId + ", resource=" + resource);
		}
	}

	@Override
	public void deleteConsents(String providerId, long timestamp) throws OAuthStoreException {
		long deleted = 0;
		try {
			MongoCollection<Document> col = getConsentCollection();
			Document query = new Document();
			query.put(EXPIRES, new Document("$lt", timestamp));
			query.put(PROVIDERID, providerId);
			deleted = col.deleteMany(query).getDeletedCount();
		} catch (Exception e) {
			throw new OAuthStoreException("Failed on deleteConsents for time after " + timestamp, e);
		}
		if (deleted > 0) {
			LOGGER.log(Level.INFO, "Deleted OAuthConsent(s): providerId=" + providerId + ", timeStamp=" + timestamp
					+ ", count=" + deleted);
		}
	}

	/**
	 * Helper method to create a filter Document to look up an OAuthClient.
	 * @param oauthClient
	 * @return A filter Document created with the providerId and clientId from the provided OAuthClient
	 */
	private Document createClientKeyHelper(OAuthClient oauthClient) {
		return createClientKeyHelper(oauthClient.getProviderId(), oauthClient.getClientId());
	}

	/**
	 * Helper method to create a filter Document to look up an OAuthClient.
	 * @param providerId
	 * @param clientId
	 * @return A filter Document created with the provided fields.
	 */
	private Document createClientKeyHelper(String providerId, String clientId) {
		Document d = new Document(CLIENTID, clientId);
		d.append(PROVIDERID, providerId);
		return d;
	}

	/**
	 * Helper method to create a filter Document to look up an OAuthToken.
	 * @param oauthToken
	 * @return A filter Document created with the providerId and lookupKey from the provided OAuthToken
	 */
	private Document createTokenKeyHelper(OAuthToken oauthToken) {
		return createTokenKeyHelper(oauthToken.getProviderId(), oauthToken.getLookupKey());
	}

	/**
	 * Helper method to create a filter Document to look up an OAuthToken.
	 * @param oauthToken
	 * @return A filter Document created with the provided fields
	 */
	private Document createTokenKeyHelper(String providerId, String lookupKey) {
		Document d = new Document(LOOKUPKEY, lookupKey);
		d.append(PROVIDERID, providerId);
		return d;
	}

	/**
	 * Helper method to create a filter Document to look up an OAuthConsent.
	 * @param oauthConsent
	 * @return A filter Document created with the clientId, user, resource and providerId fields from the provided OAuthConsent
	 */
	private Document createConsentKeyHelper(OAuthConsent oauthConsent) {
		return createConsentKeyHelper(oauthConsent.getClientId(), oauthConsent.getUser(), oauthConsent.getResource(),
				oauthConsent.getProviderId());
	}


	/**
	 * Helper method to create a filter Document to look up an OAuthConsent.
	 * @param providerId 
	 * @param username
	 * @param clientId
	 * @param resource
	 * @return A filter Document created with the provided fields.
	 */
	private Document createConsentKeyHelper(String providerId, String username, String clientId, String resource) {
		Document d = new Document(CLIENTID, clientId);
		d.append(USERNAME, username);
		d.append(RESOURCE, resource);
		d.append(PROVIDERID, providerId);
		return d;
	}

	/**
	 * Get a string representation of an OAuthClient.
	 * 
	 * <p/>
	 * WARNING! This method is for demonstrative purposes only and care should be
	 * taken to not print out confidential information.
	 * 
	 * @param client The OAuthClient to get a string representation of.
	 * @return The toString.
	 */
	private static String toString(OAuthClient client) {
		return "{" + client.toString() + ": " + "clientId=" + client.getClientId() + ", providerID="
				+ client.getProviderId() + ", displayName=" + client.getDisplayName() + ", clientSecret="
				+ client.getClientSecret() + ", clientMetadata=" + client.getClientMetadata() + ", enabled="
				+ client.isEnabled() + "}";
	}

	/**
	 * Get a string representation of an OAuthConsent.
	 * 
	 * <p/>
	 * WARNING! This method is for demonstrative purposes only and care should be
	 * taken to not print out confidential information.
	 * 
	 * @param client The OAuthClient to get a string representation of.
	 * @return The toString.
	 */
	private static String toString(OAuthConsent consent) {
		return "{" + consent.toString() + ": " + "clientId=" + consent.getClientId() + ", providerID="
				+ consent.getProviderId() + ", user=" + consent.getUser() + ", resource=" + consent.getResource()
				+ ", scope=" + consent.getScope() + ", expires=" + consent.getExpires() + ", consentProperties="
				+ consent.getConsentProperties() + "}";
	}

	/**
	 * Get a string representation of an OAuthToken.
	 * 
	 * <p/>
	 * WARNING! This method is for demonstrative purposes only and care should be
	 * taken to not print out confidential information.
	 * 
	 * @param client The OAuthClient to get a string representation of.
	 * @return The toString.
	 */
	private static String toString(OAuthToken token) {
		return "{" + token.toString() + ": " + "clientId=" + token.getClientId() + ", providerID="
				+ token.getProviderId() + ", lookupKey=" + token.getLookupKey() + ", username=" + token.getUsername()
				+ ", createdAt=" + token.getCreatedAt() + ", expires=" + token.getExpires() + ", lifetimeInSeconds="
				+ token.getLifetimeInSeconds() + ", redirectUri=" + token.getRedirectUri() + ", scope="
				+ token.getScope() + ", stateId=" + token.getStateId() + ", subType=" + token.getSubType()
				+ ", tokenProperties=" + token.getTokenProperties() + ", type=" + token.getType() + ", uniqueId="
				+ token.getUniqueId() + "}";
	}
}
