package mc.dragons.core.storage.impl;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import mc.dragons.core.storage.Counter;
import org.bson.Document;
import org.bson.conversions.Bson;

public class MongoCounter implements Counter {
	private MongoDatabase database = MongoConfig.getDatabase();

	private MongoCollection<Document> counters = this.database.getCollection("counters");

	public int getCurrentId(String counter) {
		FindIterable<Document> iter = this.counters.find((Bson) new Document("_id", counter));
		Document result = (Document) iter.first();
		if (result == null) {
			this.counters.insertOne((new Document("_id", counter)).append("seq", Integer.valueOf(0)));
			return 0;
		}
		return result.getInteger("seq", 0);
	}

	public int reserveNextId(String counter) {
		int id = getCurrentId(counter) + 1;
		this.counters.updateOne((Bson) new Document("_id", counter), (Bson) new Document("$set", new Document("seq", Integer.valueOf(id))));
		return id;
	}
}
