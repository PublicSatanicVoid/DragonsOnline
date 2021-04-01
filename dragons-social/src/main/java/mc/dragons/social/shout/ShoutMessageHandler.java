package mc.dragons.social.shout;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import mc.dragons.core.Dragons;
import mc.dragons.core.networking.MessageHandler;

public class ShoutMessageHandler extends MessageHandler {
	public ShoutMessageHandler() {
		super(Dragons.getInstance(), "globalMessage");
	}

	public void send(String sender, String message) {
		sendAll(new Document("sender", sender).append("message", message));
	}
	
	@Override
	public void receive(String serverFrom, Document data) {
		Bukkit.broadcastMessage(ChatColor.GRAY + "[" + serverFrom + "] "
			+ ChatColor.DARK_AQUA + "" + ChatColor.BOLD + data.getString("sender") + " "
			+ ChatColor.AQUA + data.getString("message"));
	}
}
