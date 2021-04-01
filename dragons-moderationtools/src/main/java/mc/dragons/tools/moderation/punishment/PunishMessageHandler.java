package mc.dragons.tools.moderation.punishment;

import java.util.UUID;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.punishment.PunishmentType;
import mc.dragons.core.networking.MessageHandler;

public class PunishMessageHandler extends MessageHandler {
	public PunishMessageHandler() {
		super(Dragons.getInstance(), "punishment");
	}

	public void forwardPunishment(User user, PunishmentType type, String reason, long duration) {
		send(new Document("uuid", user.getUUID().toString()).append("action", "punish").append("punishmentType", type.toString()).append("reason", reason).append("duration", duration), user.getServer());
	}
	
	public void forwardUnpunishment(User user, PunishmentType type) {
		send(new Document("uuid", user.getUUID().toString()).append("action", "unpunish").append("punishmentType", type.toString()), user.getServer());
	}
	
	@Override
	public void receive(String serverFrom, Document data) {
		UUID uuid = UUID.fromString(data.getString("uuid"));
		String action = data.getString("action");
		PunishmentType type = PunishmentType.valueOf(data.getString("punishmentType"));
		
		User user = UserLoader.fromPlayer(Bukkit.getPlayer(uuid));
		if(user == null) {
			Dragons.getInstance().getLogger().warning("Could not apply punishment from server " + serverFrom + " on " + uuid + ": user is null");
			return;
		}
		
		new BukkitRunnable() {
			@Override public void run() {
				if(action.equals("punish")) {
					String reason = data.getString("reason");
					long duration = data.getLong("duration");
					user.punish(type, reason, duration);
				}
				else if(action.equals("unpunish")) {
					user.unpunish(type);
				}
			}
		}.runTask(Dragons.getInstance());
	}
}
