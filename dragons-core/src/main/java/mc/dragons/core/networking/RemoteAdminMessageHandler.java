package mc.dragons.core.networking;

import org.bson.Document;
import org.bukkit.Bukkit;

import mc.dragons.core.Dragons;

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
	
	public void sendRemoteRestart(String server) {
		send(new Document("action", "restart"), server);
	}
	
	@Override
	public void receive(String serverFrom, Document data) {
		String action = data.getString("action");
		if(action.equals("restart")) {
			Dragons.getInstance().getLogger().info("RECEIVED REMOTE RESTART COMMAND FROM " + serverFrom);
			if(ignoresRemoteRestarts) return;
			Bukkit.getScheduler().runTaskLater(Dragons.getInstance(), () -> {
				Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "spigot:restart");
			}, 1L);
		}
	}
	
	
}
