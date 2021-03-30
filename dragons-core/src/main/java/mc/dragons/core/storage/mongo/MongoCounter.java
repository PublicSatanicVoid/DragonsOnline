package mc.dragons.core.storage.mongo;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import mc.dragons.core.storage.Counter;

/**
 * Provides sequential IDs to various data components.
 * 
 * <p>Each counter is associated with a name and its current
 * highest sequential number. This can be incremented safely
 * (with reasonable guarantees of avoiding double-counting)
 * through this class.
 * 
 * <p>Note that if an object with a sequential ID provided by
 * this class is deleted, this will not affect the numbering
 * going forward; e.g. if object #3, the highest ID for counter
 * "foo", is deleted, the next ID for counter "foo" will be 4
 * and not 3. This class has no awareness of what sequential
 * IDs are used for downstream.
 * 
 * @author Adam
 *
 */
public class MongoCounter implements Counter {
	private MongoDatabase database;
	private MongoCollection<Document> counters;

	public MongoCounter(MongoConfig config) {
		database = config.getDatabase();
		counters = database.getCollection(MongoConfig.COUNTER_COLLECTION);
	}
	
	@Override
	public int getCurrentId(String counter) {
		FindIterable<Document> iter = counters.find(new Document("_id", counter));
		Document result = iter.first();
		if (result == null) {
			counters.insertOne(new Document("_id", counter).append("seq", 0));
			return 0;
		}
		return result.getInteger("seq", 0);
	}

	@Override
	public int reserveNextId(String counter) {
		int id = getCurrentId(counter) + 1;
		counters.updateOne(new Document("_id", counter), new Document("$set", new Document("seq", id)));
		return id;
	}
}
