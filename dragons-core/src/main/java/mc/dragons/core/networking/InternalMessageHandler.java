package mc.dragons.core.networking;

import static mc.dragons.core.util.BukkitUtil.sync;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.util.StringUtil;

public class InternalMessageHandler extends MessageHandler {
	private Map<UUID, Consumer<Optional<Boolean>>> callbacks;
	
	public InternalMessageHandler(Dragons plugin) {
		super(plugin, "internal");
		callbacks = new HashMap<>();
	}
	
	public void sendRawMsg(String server, UUID toUUID, String msg) {
		send(new Document("action", "sendRawMsg").append("uuid", toUUID.toString()).append("msg", msg), server);
	}
	
	public void broadcastRawMsg(String msg) {
		sendAll(new Document("action", "broadcastRawMsg").append("msg", msg));
	}
	
	public void broadcastRawMsgHoverable(String msg, String hover) {
		sendAll(new Document("action", "broadcastRawMsgHoverable").append("msg", msg).append("hover", hover));
	}
	
	public void broadcastConsoleCmd(String cmd) {
		sendAll(new Document("action", "ccmd").append("cmd", cmd));
	}
	
	public void sendCheckUserPing(String server, UUID uuid, Consumer<Optional<Boolean>> callback) {
		callbacks.put(uuid, callback);
		send(new Document("action", "checkUserPing").append("uuid", uuid.toString()), server);
		sync(() -> {
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
		else if(action.equals("sendRawMsg")) {
			UUID uuid = UUID.fromString(data.getString("uuid"));
			Player player = Bukkit.getPlayer(uuid);
			if(player == null) return;
			player.sendMessage(data.getString("msg"));
		}
		else if(action.equals("broadcastRawMsg")) {
			Bukkit.broadcastMessage(data.getString("msg"));
		}
		else if(action.equals("broadcastRawMsgHoverable")) {
			Bukkit.spigot().broadcast(StringUtil.hoverableText(data.getString("msg"), data.getString("hover")));
		}
		else if(action.equals("ccmd")) {
			sync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), data.getString("cmd")));
		}
	}
}
