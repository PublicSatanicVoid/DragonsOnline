package mc.dragons.core.networking;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.bson.Document;
import org.bukkit.Bukkit;

import mc.dragons.core.Dragons;
import mc.dragons.core.util.BukkitUtil;

public class InternalMessageHandler extends MessageHandler {
	private Map<UUID, Consumer<Optional<Boolean>>> callbacks;
	
	public InternalMessageHandler(Dragons plugin) {
		super(plugin, "internal");
		callbacks = new HashMap<>();
	}
	
	public void sendCheckUserPing(String server, UUID uuid, Consumer<Optional<Boolean>> callback) {
		callbacks.put(uuid, callback);
		send(new Document("action", "checkUserPing").append("uuid", uuid.toString()), server);
		BukkitUtil.sync(() -> {
			if(callbacks.containsKey(uuid) && callbacks.get(uuid).equals(callback)) {
				callback.accept(Optional.empty());
			}
		}, 20 * 3);
	}
	
	public void sendCheckUserPong(String server, UUID uuid) {
		boolean online = Bukkit.getOnlinePlayers().stream().filter(p -> p.getUniqueId().equals(uuid)).count() > 0;
		send(new Document("action", "checkUserPong").append("uuid", uuid.toString()).append("online", online), server);
	}
	
	public void receive(String serverFrom, Document data) {
		String action = data.getString("action");
		if(action.equals("checkUserPing")) {
			sendCheckUserPong(serverFrom, UUID.fromString(data.getString("uuid")));
		}
		else if(action.equals("checkUserPong")) {
			UUID uuid = UUID.fromString(data.getString("uuid"));
			boolean online = data.getBoolean("online");
			callbacks.getOrDefault(uuid, unused -> {}).accept(Optional.of(online));
			callbacks.remove(uuid);
		}
	}
}
