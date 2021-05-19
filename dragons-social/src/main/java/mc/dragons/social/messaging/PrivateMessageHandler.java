package mc.dragons.social.messaging;

import java.util.UUID;

import org.bson.Document;
import org.bukkit.ChatColor;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.chat.ChatMessageRegistry;
import mc.dragons.core.gameobject.user.chat.MessageData;
import mc.dragons.core.networking.MessageHandler;
import mc.dragons.core.util.StringUtil;
import mc.dragons.social.DragonsSocial;

public class PrivateMessageHandler extends MessageHandler {
	private DragonsSocial plugin;
	private UserLoader userLoader = GameObjectType.USER.getLoader();
	private ChatMessageRegistry registry;
	
	public PrivateMessageHandler(DragonsSocial instance) {
		super(instance.getDragonsInstance(), "directMessage");
		plugin = instance;
		registry = instance.getDragonsInstance().getChatMessageRegistry();
	}
	
	private void informLocalSpies(String from, String to, String message) {
		UserLoader.allUsers().stream()
			.filter(u -> u.getPlayer() != null)
			.filter(u -> u.hasChatSpy())
			.filter(u -> !u.getName().equals(from) && !u.getName().equals(to))
			.forEach(u -> 
				u.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "[" + from + " -> " + to + "] " + ChatColor.GRAY + message));
	}
	
	private void doLocalSend(User from, String to, String message) {
		from.getPlayer().sendMessage(ChatColor.GOLD + "[" + from.getName() + " -> " + to + "] " + ChatColor.GRAY + message);
	}

	private void doLocalReceive(User to, User from, String message) {
		MessageData messageData = new MessageData(from, to, message);
		registry.register(messageData);
		to.getPlayer().spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GOLD + "[" + from.getName() + " -> " + to.getName() + "] " + ChatColor.GRAY + message, 
			"/chatreport " + messageData.getId() + " --id", ChatColor.YELLOW + "Click to report this message"));
		to.setLastReceivedMessageFrom(from.getName());
	}
	
	public void send(User from, User to, String message) {
		if(from.getPlayer() != null) doLocalSend(from, to.getName(), message);
		informLocalSpies(from.getName(), to.getName(), message);
		if(to.getPlayer() == null) send(new Document("from", from.getUUID().toString()).append("to", to.getUUID().toString()).append("message", message), to.getServer());
		else doLocalReceive(to, from, message);
	}
	
	@Override
	public void receive(String serverFrom, Document data) {
		User from = userLoader.loadObject(UUID.fromString(data.getString("from")));
		User to = userLoader.loadObject(UUID.fromString(data.getString("to")));
		String message = data.getString("message");
		
		if(from == null || to == null) {
			plugin.getLogger().warning("Cannot handle private message receipt from " + serverFrom + ": missing data (from=" + from + ", to=" + to + ")");
			return;
		}
		
		if(to.getPlayer() == null) {
			plugin.getLogger().warning("Cannot handle private message receipt from " + serverFrom + ": recipient is not online locally (" + to.getName() + ")");
			return;
		}
		
		doLocalReceive(to, from, message);
		informLocalSpies(to.getName(), from.getName(), message);
	}
}
