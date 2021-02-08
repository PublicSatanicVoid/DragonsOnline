package mc.dragons.core.storage.loader;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import mc.dragons.core.storage.Counter;
import mc.dragons.core.storage.mongo.MongoConfig;

/**
 * Loads objects from MongoDB which are not classified
 * as game objects. Changelogs, warps, etc. are all
 * lightweight objects, and theoretically have dedicated
 * collections within the database.
 * 
 * Lightweight objects may have a unique integral ID
 * associated with them, provided through a Counter.
 * However, individual loaders may or may not choose to
 * use these.
 * 
 * @author Adam
 *
 * @param <E> The object type to be loaded.
 */
public abstract class AbstractLightweightLoader<E> {
	protected String counterName;
	protected String collectionName;
	protected MongoDatabase database;
	protected MongoCollection<Document> collection;
	protected Counter counter;

	protected AbstractLightweightLoader(MongoConfig config, String counterName, String collectionName) {
		this.counterName = counterName;
		this.collectionName = collectionName;
		this.database = config.getDatabase();
		this.collection = database.getCollection(collectionName);
		this.counter = config.getCounter();
	}

	protected int reserveNextId() {
		return this.counter.reserveNextId(this.counterName);
	}

	public int getCurrentMaxId() {
		return this.counter.getCurrentId(this.counterName);
	}
}
