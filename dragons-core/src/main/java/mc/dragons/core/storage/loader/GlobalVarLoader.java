package mc.dragons.core.storage.loader;

import org.bson.Document;

import mc.dragons.core.gameobject.user.Rank;
import mc.dragons.core.storage.mongo.MongoConfig;

/**
 * Load global variables from MongoDB.
 * 
 * <p>Global variables are (possibly nested) key-value pairs
 * which are accessible from multiple servers.
 * 
 * <p>Servers can use the same global variable storage by
 * specifying the same <i>accession token</i> in their
 * configuration files. This can also be temporarily changed 
 * at runtime.
 * 
 * @author Adam
 *
 */
public class GlobalVarLoader extends AbstractLightweightLoader<Rank> {
	
	private String accessionToken;
	private Document cachedLookup;
	
	private void setup() {
		cachedLookup = new Document("_id", accessionToken);
		if(collection.find(cachedLookup).first() == null) {
			collection.insertOne(cachedLookup);
		}
	}
	
	public GlobalVarLoader(MongoConfig config) {
		super(config, "#unused#", "globalvars");
		accessionToken = config.getDefaultAccessionToken();
		setup();
	}
	
	/* Shortcut methods modeled off of Document */
	
	public Object get(String key) {
		return get(key, Object.class);
	}
	
	public String getString(String key) {
		return get(key, String.class);
	}
	
	public int getInteger(String key) {
		return get(key, Integer.class);
	}
	
	public double getDouble(String key) {
		return get(key, Double.class);
	}
	
	public boolean getBoolean(String key) {
		return get(key, Boolean.class);
	}
	
	public Document getDocument(String key) {
		return get(key, Document.class);
	}
	
	
	/* CRUD */
	
	public <T> T get(String key, Class<T> clazz) {
		return collection.find(cachedLookup).first().get(key, clazz);
	}
	
	public void set(String key, Object value) {
		collection.updateOne(cachedLookup, new Document("$set", new Document(key, value)));
	}
	
	public void delete(String key) {
		collection.updateOne(cachedLookup, new Document("$unset", new Document(key, null)));
	}
	
	public Document getFullDocument() {
		return collection.find(cachedLookup).first();
	}

	
	
	/* Management */
	
	public void changeAccessionToken(String token) {
		accessionToken = token;
		setup();
	}
	
	public String getAccessionToken() {
		return accessionToken;
	}
}
