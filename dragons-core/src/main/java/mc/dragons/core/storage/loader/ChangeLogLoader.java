package mc.dragons.core.storage.loader;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import mc.dragons.core.storage.mongo.MongoConfig;

public class ChangeLogLoader extends AbstractLightweightLoader<ChangeLogLoader.ChangeLogEntry> {
	public static class ChangeLogEntry {
		private int id;
		private String date;
		private String by;
		private String title;
		private List<String> changelog;

		public ChangeLogEntry(int id, String date, String by, String title, List<String> changelog) {
			this.date = date;
			this.by = by;
			this.title = title;
			this.changelog = changelog;
		}

		public int getId() {
			return id;
		}

		public String getDate() {
			return date;
		}

		public String getBy() {
			return by;
		}

		public String getTitle() {
			return title;
		}

		public List<String> getChangeLog() {
			return changelog;
		}
	}

	public ChangeLogLoader(MongoConfig config) {
		super(config, "changelogs", "changelogs");
	}

	public List<ChangeLogEntry> getUnreadChangelogs(int lastReadChangelog) {
		List<ChangeLogEntry> result = new ArrayList<>();
		FindIterable<Document> dbResults = collection.find(new Document("_id", new Document("$gt", Integer.valueOf(lastReadChangelog))));
		for (Document d : dbResults) {
			result.add(new ChangeLogEntry(d.getInteger("_id").intValue(), d.getString("date"), d.getString("by"), d.getString("title"), d.getList("changelog", String.class)));
		}
		return result;
	}

	public void deleteChangeLog(int id) {
		collection.deleteOne(new Document("_id", Integer.valueOf(id)));
	}

	public void addChangeLog(String by, String title, List<String> changelog) {
		String date = Date.from(Instant.now()).toString();
		collection.insertOne(new Document("_id", Integer.valueOf(reserveNextId())).append("date", date).append("by", by).append("title", title).append("changelog", changelog));
	}
}
