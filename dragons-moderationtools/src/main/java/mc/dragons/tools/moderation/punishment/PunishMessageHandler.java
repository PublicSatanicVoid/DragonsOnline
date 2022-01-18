package mc.dragons.tools.moderation.punishment;

import java.util.UUID;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.networking.MessageHandler;
import mc.dragons.tools.moderation.WrappedUser;

public class PunishMessageHandler extends MessageHandler {
	public PunishMessageHandler() {
		super(Dragons.getInstance(), "punishment");
	}

	public void forwardPunishment(User user, int id, PunishmentType type, String reason, long duration) {
		send(new Document("uuid", user.getUUID().toString()).append("action", "punish").append("punishmentId", id)
				.append("punishmentType", type.toString()).append("reason", reason).append("duration", duration), user.getServerName());
	}
	
	public void forwardUnpunishment(User user, int id) {
		send(new Document("uuid", user.getUUID().toString()).append("action", "unpunish").append("punishmentId", id), user.getServerName());
	}
	
	@Override
	public void receive(String serverFrom, Document data) {
		UUID uuid = UUID.fromString(data.getString("uuid"));
		String action = data.getString("action");
		
		User user = UserLoader.fromPlayer(Bukkit.getPlayer(uuid));
		if(user == null) {
			Dragons.getInstance().getLogger().warning("Could not apply punishment from server " + serverFrom + " on " + uuid + ": target user is null");
			return;
		}
		
		user.safeResyncData();
		WrappedUser wrapped = WrappedUser.of(user);
		
		new BukkitRunnable() {
			@Override public void run() {
				if(action.equals("punish")) {
					int id = data.getInteger("punishmentId");
					PunishmentType type = PunishmentType.valueOf(data.getString("punishmentType"));
					String reason = data.getString("reason");
					wrapped.applyPunishmentLocally(id, type, reason);
				}
				else if(action.equals("unpunish")) {
					int id = data.getInteger("punishmentId");
					wrapped.applyUnpunishmentLocally(id);
				}
			}
		}.runTask(Dragons.getInstance());
	}
}
