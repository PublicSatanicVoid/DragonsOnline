package mc.dragons.core.storage.impl.loader;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;

import mc.dragons.core.storage.impl.MongoConfig;

public class FeedbackLoader extends AbstractLightweightLoader<FeedbackLoader.FeedbackEntry> {
	public static class FeedbackEntry {
		private int id;

		private String from;

		private String feedback;

		public FeedbackEntry(int id, String from, String feedback) {
			this.id = id;
			this.from = from;
			this.feedback = feedback;
		}

		public int getId() {
			return this.id;
		}

		public String getFrom() {
			return this.from;
		}

		public String getFeedback() {
			return this.feedback;
		}
	}

	public FeedbackLoader(MongoConfig config) {
		super(config, "feedback", "feedback");
	}

	public List<FeedbackEntry> getUnreadFeedback() {
		List<FeedbackEntry> result = new ArrayList<>();
		FindIterable<Document> dbResults = this.collection.find((Bson) new Document("read", Boolean.valueOf(false)));
		for (Document d : dbResults)
			result.add(new FeedbackEntry(d.getInteger("_id").intValue(), d.getString("from"), d.getString("feedback")));
		return result;
	}

	public void deleteFeedback(int id) {
		this.collection.deleteOne((Bson) new Document("_id", Integer.valueOf(id)));
	}

	public void markRead(int id, boolean read) {
		this.collection.updateOne((Bson) new Document("_id", Integer.valueOf(id)), (Bson) new Document("$set", new Document("read", Boolean.valueOf(read))));
	}

	public void addFeedback(String from, String feedback) {
		this.collection.insertOne((new Document("_id", Integer.valueOf(reserveNextId()))).append("from", from).append("feedback", feedback).append("read", Boolean.valueOf(false)));
	}
}
