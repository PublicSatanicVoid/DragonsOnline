package mc.dragons.tools.content;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.storage.Identifier;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.storage.loader.AbstractLightweightLoader;
import mc.dragons.core.storage.mongo.MongoConfig;
import mc.dragons.tools.content.AuditLogLoader.AuditLogEntry;

public class AuditLogLoader extends AbstractLightweightLoader<AuditLogEntry> {
	public static String PUSH_MEMO = "Pushed to production staging";
	
	private UserLoader userLoader = GameObjectType.USER.getLoader();
	
	private Document blankDelta() {
		return StorageUtil.getDelta(new Document(), new Document());
	}
	
	public class AuditLogEntry {
		private Document data;
		
		public AuditLogEntry(Document data) {
			this.data = data;
		}
		
		public Identifier getObjectIdentifier() {
			return new Identifier(GameObjectType.valueOf(data.getString("type")), data.get("uuid", UUID.class));
		}
		
		public int getId() {
			return data.getInteger("_id");
		}
		
		public Document getDelta() {
			return data.get("delta", blankDelta());
		}
		
		public User getBy() {
			return userLoader.loadObject(data.get("by", UUID.class));
		}
		
		public String getLine() {
			return data.getString("line");
		}
		
		public Date getDate() {
			return new Date(data.getLong("on") * 1000);
		}
	}
	
	public AuditLogLoader(MongoConfig config) {
		super(config, "auditLog", "auditLog");
	}
	
	public AuditLogEntry getEntry(Identifier identifier, int id) {
		FindIterable<Document> result = collection.find(new Document("type", identifier.getType().toString()).append("uuid", identifier.getUUID()).append("_id", id));
		if(result.first() == null) return null;
		return new AuditLogEntry(result.first());
	}
	
	public List<AuditLogEntry> getEntries(Identifier identifier) {
		return collection.find(new Document("type", identifier.getType().toString()).append("uuid", identifier.getUUID())).map(d -> new AuditLogEntry(d)).into(new ArrayList<>());
	}
	
	public AuditLogEntry getLastPush(Identifier identifier) {
		FindIterable<Document> result = collection.find(new Document("type", identifier.getType().toString()).append("uuid", identifier.getUUID()).append("line", PUSH_MEMO)).sort(new Document("_id", -1));
		if(result.first() == null) return null;
		return new AuditLogEntry(result.first());
	}
	
	public int saveEntry(Identifier identifier, User by, Document base, Document current, String line) {
		int id = reserveNextId();
		Document baseSafe = Document.parse(base.toJson());
		Document currentSafe = Document.parse(current.toJson());
		Document data = new Document("_id", id).append("type", identifier.getType().toString()).append("uuid", identifier.getUUID()).append("delta", StorageUtil.getDelta(currentSafe, baseSafe))
			.append("by", by == null ? null : by.getUUID()).append("line", line).append("on", Instant.now().getEpochSecond());
		collection.insertOne(data);
		return id;
	}
	
	public int saveEntry(GameObject obj, User by, Document base, String line) {
		return saveEntry(obj.getIdentifier(), by, base, obj.getData(), line);
	}
	
	public int saveEntry(GameObject obj, User by, String line) {
		return saveEntry(obj, by, obj.getData(), line);
	}
	
	public int savePush(GameObject obj, User by) {
		return saveEntry(obj, by, PUSH_MEMO);
	}
	
}
