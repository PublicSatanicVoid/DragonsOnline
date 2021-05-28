package mc.dragons.core.networking;

import static mc.dragons.core.util.BukkitUtil.sync;

import java.util.UUID;

import org.bson.Document;
import org.bukkit.Bukkit;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;

/**
 * Handles remote administration and telemetry.
 * 
 * @author Adam
 *
 */
public class RemoteAdminMessageHandler extends MessageHandler {
	private boolean ignoresRemoteRestarts = false;
	
	public RemoteAdminMessageHandler() {
		super(Dragons.getInstance(), "remoteAdmin");
	}

	public void setIgnoresRemoteRestarts(boolean ignoresRemoteRestarts) {
		this.ignoresRemoteRestarts = ignoresRemoteRestarts;
	}
	
	public boolean ignoresRemoteRestarts() {
		return ignoresRemoteRestarts;
	}
	
	/**
	 * Sends the specified server a message to restart immediately.
	 * 
	 * @implNote The receiving server can choose to ignore this.
	 * 
	 * @param server
	 */
	public void sendRemoteRestart(String server) {
		send(new Document("action", "restart"), server);
	}
	
	public void sendRemoteSudo(String server, UUID uuid, String command) {
		send(new Document("action", "sudo").append("uuid", uuid).append("command", command), server);
	}
	
	@Override
	public void receive(String serverFrom, Document data) {
		String action = data.getString("action");
		if(action.equals("restart")) {
			Dragons.getInstance().getLogger().info("RECEIVED REMOTE RESTART COMMAND FROM " + serverFrom);
			if(ignoresRemoteRestarts) return;
			sync(() -> Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "spigot:restart"));
		}
		else if(action.equals("sudo")) {
			User user = GameObjectType.USER.<User, UserLoader>getLoader().loadObject(data.get("uuid", UUID.class));
			Dragons.getInstance().getLogger().debug("Running sudo on user " + user + " (" + user.getCommandSender() + ")");
			sync(() -> Bukkit.dispatchCommand(user.getCommandSender(), data.getString("command")));
		}
	}
	
	
}
