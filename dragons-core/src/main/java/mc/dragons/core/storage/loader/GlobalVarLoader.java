package mc.dragons.core.storage.loader;

import org.bson.Document;

import mc.dragons.core.gameobject.user.Rank;
import mc.dragons.core.storage.mongo.MongoConfig;

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
		accessionToken = config.getAccessionToken();
		setup();
	}
	
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
	
	public <T> T get(String key, Class<T> clazz) {
		return collection.find(cachedLookup).first().get(key, clazz);
	}
	
	public void set(String key, Object value) {
		collection.updateOne(cachedLookup, new Document("$set", new Document(key, value)));
	}
	
	public void delete(String key) {
		collection.updateOne(cachedLookup, new Document("$unset", new Document(key, null)));
	}
	
	public void changeAccessionToken(String token) {
		accessionToken = token;
		setup();
	}
	
	public String getAccessionToken() {
		return accessionToken;
	}
	
	public Document getDocument() {
		return collection.find(cachedLookup).first();
	}

}
