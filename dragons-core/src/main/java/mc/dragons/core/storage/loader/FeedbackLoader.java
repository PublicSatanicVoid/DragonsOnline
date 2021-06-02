package mc.dragons.core.storage.loader;

import java.util.ArrayList;

import org.bson.Document;

import com.google.common.collect.Iterables;
import com.mongodb.client.FindIterable;

import mc.dragons.core.storage.mongo.MongoConfig;
import mc.dragons.core.storage.mongo.pagination.PaginatedResult;
import mc.dragons.core.storage.mongo.pagination.PaginationUtil;

public class FeedbackLoader extends AbstractLightweightLoader<FeedbackLoader.FeedbackEntry> {
	public static final int PAGE_SIZE = 10;
	
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

	public PaginatedResult<FeedbackEntry> getUnreadFeedback(int page) {
		FindIterable<Document> results = collection.find(new Document("read", false));
		int total = Iterables.size(results);
		return new PaginatedResult<FeedbackEntry>(PaginationUtil.sortAndPaginate(results, page, PAGE_SIZE)
				.map(d -> new FeedbackEntry(d.getInteger("_id"), d.getString("from"), d.getString("feedback")))
				.into(new ArrayList<>()), total, page, PAGE_SIZE);
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
