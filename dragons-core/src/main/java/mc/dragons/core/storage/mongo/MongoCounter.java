package mc.dragons.core.storage.mongo;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import mc.dragons.core.storage.Counter;

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
