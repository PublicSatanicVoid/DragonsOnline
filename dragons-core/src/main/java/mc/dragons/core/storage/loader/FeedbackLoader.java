package mc.dragons.core.storage.loader;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import mc.dragons.core.storage.mongo.MongoConfig;

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
			return id;
		}

		public String getFrom() {
			return from;
		}

		public String getFeedback() {
			return feedback;
		}
	}

	public FeedbackLoader(MongoConfig config) {
		super(config, "feedback", "feedback");
	}

	public List<FeedbackEntry> getUnreadFeedback() {
		List<FeedbackEntry> result = new ArrayList<>();
		FindIterable<Document> dbResults = collection.find(new Document("read", Boolean.valueOf(false)));
		for (Document d : dbResults) {
			result.add(new FeedbackEntry(d.getInteger("_id"), d.getString("from"), d.getString("feedback")));
		}
		return result;
	}

	public void deleteFeedback(int id) {
		collection.deleteOne(new Document("_id", id));
	}

	public void markRead(int id, boolean read) {
		collection.updateOne(new Document("_id", id), new Document("$set", new Document("read", read)));
	}

	public void addFeedback(String from, String feedback) {
		collection.insertOne(new Document("_id", reserveNextId()).append("from", from).append("feedback", feedback).append("read", false));
	}
}
