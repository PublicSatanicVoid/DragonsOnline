package mc.dragons.core.storage.impl.loader;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import mc.dragons.core.storage.Counter;
import mc.dragons.core.storage.impl.MongoConfig;
import org.bson.Document;

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
