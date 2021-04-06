package mc.dragons.core.gameobject.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.networking.MessageHandler;
import mc.dragons.core.util.PermissionUtil;

public class ConnectionMessageHandler extends MessageHandler {

	private UserLoader userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	private MongoCollection<Document> manifest = Dragons.getInstance().getMongoConfig().getDatabase().getCollection("manifest");
	
	public ConnectionMessageHandler() {
		super(Dragons.getInstance(), "conn");
		manifest.updateOne(new Document("server", Dragons.getInstance().getServerName()), new Document("$set", new Document("online", new ArrayList<>())));
	}

	public void clearServerEntries() {
		manifest.deleteOne(new Document("server", Dragons.getInstance().getServerName()));
	}
	
	public void logConnect(User user) {
		sendAll(new Document("user", user.getUUID()).append("vanished", user.isVanished()).append("action", "connect"));
		String server = Dragons.getInstance().getServerName();
		manifest.updateOne(new Document("server", server), new Document("$set", new Document("server", server)).append("$push", new Document("online", user.getUUID())), new UpdateOptions().upsert(true));
	}
	
	public void logDisconnect(User user) {
		sendAll(new Document("user", user.getUUID()).append("vanished", user.isVanished()).append("action", "disconnect"));
		manifest.updateOne(new Document("server", Dragons.getInstance().getServerName()), new Document("$pull", new Document("online", user.getUUID())));
	}
	
	public Map<String, List<UUID>> getManifest() {
		Map<String, List<UUID>> manifest = new HashMap<>();
		this.manifest.find().iterator().forEachRemaining(d -> {
			manifest.put(d.getString("server"), d.getList("online", UUID.class));
		});
		return manifest;
	}
	
	@Override
	public void receive(String serverFrom, Document data) {
		User user = userLoader.loadObject(data.get("user", UUID.class));
		boolean vanished = data.getBoolean("vanished");
		String action = data.getString("action");
		
		String message;
		if(action.equals("connect")) {
			message = ChatColor.GREEN + user.getName() + " is online" + (vanished ? " (vanished)" : "") + " on " + serverFrom;
		}
		else {
			message = ChatColor.GRAY + user.getName() + " left on " + serverFrom + "!";
		}
		
		if(serverFrom.equals(Dragons.getInstance().getServerName())) return;
		Bukkit.getOnlinePlayers().stream().map(p -> UserLoader.fromPlayer(p))
			.filter(u -> vanished && PermissionUtil.verifyActivePermissionLevel(u, PermissionLevel.MODERATOR, false) || !vanished)
			.forEach(u -> {
				u.getPlayer().sendMessage(message);
			});
	}
	
	
}
