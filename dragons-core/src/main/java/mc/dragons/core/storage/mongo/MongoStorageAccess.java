package mc.dragons.core.storage.mongo;

import static mc.dragons.core.util.BukkitUtil.rollingAsync;

import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.mongodb.client.MongoCollection;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.storage.Identifier;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.util.PermissionUtil;

/**
 * Persistent unit of data storage backed by a MongoDB instance.
 * 
 * @author Adam
 *
 */
public class MongoStorageAccess implements StorageAccess {
	private Identifier identifier;
	private Document document;
	private MongoCollection<Document> collection;

	public MongoStorageAccess(Identifier identifier, Document document, MongoCollection<Document> collection) {
		this.identifier = identifier;
		this.document = document.append("type", identifier.getType().toString()).append("_id", identifier.getUUID());
		this.collection = collection;
	}

	@Override
	public void set(String key, Object value) {
		if (key.equals("type") || key.equals("_id")) {
			throw new IllegalArgumentException("Cannot modify type or UUID of storage access once instantiated");
		}
		document.append(key, value);
		update(new Document(key, value));
	}

	@Override
	public void update(Document document) {
		this.document.putAll(document);
		tryAsyncMongo(() -> collection.updateOne(identifier.getDocument(), new Document("$set", document)), () -> "update " + document.toJson());
	}
	
	@Override
	public void delete(String key) {
		this.document.remove(key);
		tryAsyncMongo(() -> collection.updateOne(identifier.getDocument(), new Document("$unset", new Document(key, null))), () -> "delete key " + key);
	}

	@Override
	public Object get(String key) {
		return document.get(key);
	}
	
	@Override
	public <T> T get(String key, Class<? extends T> clazz) {
		return document.get(key, clazz);
	}
	
	@Override
	public <T> T pull(String key, Class<? extends T> clazz) {
		Document remote = collection.find(identifier.getDocument()).first();
		document.append(key, remote.get(key, clazz));
		return remote.get(key, clazz);
	}

	@Override
	public Set<Entry<String, Object>> getAll() {
		return document.entrySet();
	}

	@Override
	public Document getDocument() {
		return document;
	}

	@Override
	public Identifier getIdentifier() {
		return identifier;
	}
	
	private void tryAsyncMongo(Runnable runnable, Supplier<String> actionMessage) {
		try {
			rollingAsync(runnable);
		} catch(Throwable throwable) {
			String errorMsg = "An exception occurred while updating MongoDB in storage access " + getIdentifier() + " while trying to " + actionMessage.get() + ": " + throwable.getMessage();
			Dragons.getInstance().getLogger().severe(errorMsg);
			throwable.printStackTrace();
			String alertMsg = ChatColor.DARK_RED + "Database Error: " + ChatColor.RED + errorMsg + " (a stack trace has been dumped to the console.)";
			Bukkit.getOnlinePlayers().stream()
				.map(p -> UserLoader.fromPlayer(p))
				.filter(u -> PermissionUtil.verifyActiveProfileFlag(u, SystemProfileFlag.DEVELOPMENT, false))
				.map(u -> u.getPlayer())
				.forEach(p -> p.sendMessage(alertMsg));
		}
	}
}
