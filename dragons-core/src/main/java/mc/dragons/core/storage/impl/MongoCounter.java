package mc.dragons.core.storage.impl;

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
		FindIterable<Document> iter = this.counters.find(new Document("_id", counter));
		Document result = iter.first();
		if (result == null) {
			this.counters.insertOne((new Document("_id", counter)).append("seq", Integer.valueOf(0)));
			return 0;
		}
		return result.getInteger("seq", 0);
	}

	@Override
	public int reserveNextId(String counter) {
		int id = getCurrentId(counter) + 1;
		this.counters.updateOne(new Document("_id", counter), new Document("$set", new Document("seq", Integer.valueOf(id))));
		return id;
	}
}
