package mc.dragons.tools.moderation.hold;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.networking.MessageHandler;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldType;

public class HoldMessageHandler extends MessageHandler {
	public HoldMessageHandler(Dragons instance) {
		super(instance, "hold");
	}

	public void sendHold(List<User> on, HoldType type, String reason) {
		Set<String> servers = on.stream().map(u -> u.getServer()).filter(Objects::nonNull).collect(Collectors.toSet());
		List<UUID> uuids = on.stream().map(u -> u.getUUID()).collect(Collectors.toList());
		for(String server : servers) {
			send(new Document("action", "hold").append("reason", reason).append("on", uuids).append("type", type.toString()), server);
		}
	}
	
	public void sendReleaseHold(List<User> on) {
		Set<String> servers = on.stream().map(u -> u.getServer()).filter(Objects::nonNull).collect(Collectors.toSet());
		List<UUID> uuids = on.stream().map(u -> u.getUUID()).collect(Collectors.toList());
		for(String server : servers) {
			send(new Document("action", "release").append("on", uuids), server);
		}
	}
	
	private void localApplyHold(UUID on, HoldType type) {
		Player player = Bukkit.getPlayer(on);
		if(player == null) return;
		if(type == HoldType.SUSPEND) { 
			player.kickPlayer(ChatColor.RED + "Your account has been flagged for suspicious activity and is suspended pending review.\n"
				+ "Your suspension will be resolved in at most 48 hours.");
		}
	}
	
	@Override
	public void receive(String serverFrom, Document data) {
		switch(data.getString("action")) {
		case "hold":
			HoldType type = HoldType.valueOf(data.getString("type"));
			for(UUID uuid : data.getList("on", UUID.class)) {
				localApplyHold(uuid, type);
			}
			break;
		case "release":
			// Nothing to do currently
			break;
		default:
			break;
		}
	}

}
