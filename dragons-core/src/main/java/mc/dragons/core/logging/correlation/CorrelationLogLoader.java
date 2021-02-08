package mc.dragons.core.logging.correlation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bukkit.Bukkit;

import com.mongodb.client.FindIterable;

import mc.dragons.core.Dragons;
import mc.dragons.core.logging.correlation.CorrelationLogLoader.CorrelationLogEntry;
import mc.dragons.core.storage.loader.AbstractLightweightLoader;
import mc.dragons.core.storage.mongo.MongoConfig;
import mc.dragons.core.util.StringUtil;

/**
 * Correlation ID-based logging provider to enable trackable messages.
 * Read more about the idea of correlation-based logging:
 * https://www.oreilly.com/library/view/building-microservices-with/9781785887833/1bebcf55-05bb-44a1-a4e5-f9733b8edfe3.xhtml
 * 
 * Correlation IDs will be displayed to users in relevant error messages,
 * enabling developers or support staff to find all information related to
 * that error.
 * 
 * @author Adam
 *
 */
public class CorrelationLogLoader extends AbstractLightweightLoader<CorrelationLogEntry> {
	public static class CorrelationLogEntry {
		private UUID correlationID;
		private String timestamp;
		private Level level;
		private String message;
		private String instance;
		
		private CorrelationLogEntry(UUID correlationID, Level level, String timestamp, String message, String instance) {
			this.correlationID = correlationID;
			this.timestamp = timestamp;
			this.level = level;
			this.message = message;
			this.instance = instance;
		}
		
		public UUID getCorrelationID() {
			return correlationID;
		}
		
		public String getTimestamp() {
			return timestamp;
		}
		
		public Level getLevel() {
			return level;
		}
		
		public String getMessage() {
			return message;
		}
		
		public String getInstance() {
			return instance;
		}
		
		public static CorrelationLogEntry fromDocument(Document document) {
			return new CorrelationLogEntry(UUID.fromString(document.getString("correlationID")), Level.parse(document.getString("level")), 
					document.getString("timestamp"), document.getString("message"), document.getString("instance"));
		}
	}
	
	public CorrelationLogLoader(MongoConfig config) {
		super(config, "#unused#", "correlation");
	}

	public UUID registerNewCorrelationID() {
		UUID correlationID = UUID.randomUUID();
		collection.insertOne(new Document("correlationID", correlationID.toString()).append("level", Level.INFO.getName())
				.append("timestamp", StringUtil.dateFormatNow()).append("message", "Correlation ID registered")
				.append("instance", Dragons.getInstance().getServerName()));
		return correlationID;
	}
	
	public void log(UUID correlationID, Level level, String message) {
		if(correlationID == null) return;
		Bukkit.getLogger().log(level, "[CID " + correlationID.toString().substring(0, 5) + "...] " + message);
		collection.insertOne(new Document("correlationID", correlationID.toString()).append("level", level.getName())
				.append("timestamp", StringUtil.dateFormatNow()).append("message", message)
				.append("instance", Dragons.getInstance().getServerName()));
	}
	
	public List<CorrelationLogEntry> getAllByCorrelationID(String startsWith) {
		List<CorrelationLogEntry> result = new ArrayList<>();
		FindIterable<Document> mongoResult = collection.find(new Document("correlationID", new Document("$regex", Pattern.quote(startsWith) + ".*")));
		for(Document entry : mongoResult) {
			result.add(CorrelationLogEntry.fromDocument(entry));
		}
		return result;
	}
	
	public List<CorrelationLogEntry> getAllByCorrelationID(UUID correlationID) {
		return getAllByCorrelationID(correlationID.toString());
	}
}
