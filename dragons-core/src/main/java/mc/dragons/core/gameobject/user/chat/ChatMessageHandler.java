package mc.dragons.core.gameobject.user.chat;

import java.util.UUID;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.networking.MessageHandler;
import mc.dragons.core.util.StringUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

/**
 * Handles cross-server player chat.
 * 
 * @author Adam
 *
 */
public class ChatMessageHandler extends MessageHandler {
	private UserLoader userLoader = GameObjectType.USER.getLoader();
	private ChatMessageRegistry registry = Dragons.getInstance().getChatMessageRegistry();
	private DragonsLogger LOGGER = Dragons.getInstance().getLogger();
	
	public ChatMessageHandler(Dragons instance) {
		super(instance, "chat");
	}

	private void doLocalSend(User user, ChatChannel channel, String message) {
		LOGGER.trace("Chat message from " + user.getName());
		LOGGER.verbose("-Creating message text component");
		String messageSenderInfo = "";
		if(user.isVerified()) {
			messageSenderInfo = ChatColor.GREEN + "✓ " + ChatColor.GRAY;
		}
		if (user.getRank().hasChatPrefix()) {
			messageSenderInfo += user.getRank().getChatPrefix() + " ";
		}
		MessageData messageData = new MessageData(user, message);
		messageSenderInfo += user.getRank().getNameColor() + user.getName();
		TextComponent messageInfoComponent = new TextComponent(messageSenderInfo);
		Location loc = user.getSavedLocation();
		if(user.getPlayer() != null) {
			loc = user.getPlayer().getLocation();
		}
		messageInfoComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
				new Text(ChatColor.YELLOW + "" + ChatColor.BOLD + user.getName() + "\n"),
				new Text(ChatColor.GRAY + "Server: " + ChatColor.RESET + user.getServerName() + "\n"),
				new Text(ChatColor.GRAY + "Rank: " + ChatColor.RESET + user.getRank().getNameColor() + user.getRank().getRankName() + "\n"),
				new Text(ChatColor.GRAY + "Level: " + user.getLevelColor() + user.getLevel() + "\n"),
				new Text(ChatColor.GRAY + "XP: " + ChatColor.RESET + user.getXP() + "\n"),
				new Text(ChatColor.GRAY + "Gold: " + ChatColor.RESET + user.getGold() + "\n"),
				new Text(ChatColor.GRAY + "Location: " + ChatColor.RESET + StringUtil.locToString(loc) + ChatColor.DARK_GRAY + ChatColor.ITALIC + " (when message sent)\n"),
				new Text(ChatColor.GRAY + "Floor: " + ChatColor.RESET + FloorLoader.fromWorld(loc.getWorld()).getDisplayName() + ChatColor.DARK_GRAY + ChatColor.ITALIC
						+ " (when message sent)\n"),
				new Text(ChatColor.GRAY + "First Joined: " + ChatColor.RESET + user.getFirstJoined().toString())));
		messageInfoComponent.addExtra(ChatColor.GRAY + " » ");
		TextComponent messageComponent = new TextComponent(user.getRank().getChatColor() + message);
		messageComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
				new Text(ChatColor.YELLOW + "Click to report this message")));
		messageComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chatreport " + messageData.getId() + " --id"));
		registry.register(messageData);
		for (User test: UserLoader.allUsers()) {
			LOGGER.verbose("-Checking if " + user.getName() + " can receive");
			if(test.getPlayer() == null) continue;
			if(test.getBlockedUsers().contains(user)) continue;
			// For now, we allow X's blocked users to see X's PUBLIC messages, but not the other way around
			if (!channel.canHear(test, user) && !test.hasChatSpy()) continue;
			LOGGER.verbose("  -Yes!");
			test.getSeenMessages().add(messageData);
			test.sendMessage(channel, user, new BaseComponent[] { messageInfoComponent, messageComponent });
		}
		LOGGER.info("[" + user.getServerName() + "/" + channel.getAbbreviation() + "/" + loc.getWorld().getName() + "] [" + user.getName() + "] " + message);
	}
	
	/**
	 * Send a message from the specified player on the specified channel.
	 * 
	 * @param user
	 * @param channel
	 * @param message
	 */
	public void send(User user, ChatChannel channel, String message) {
		if(channel.isNetworked()) {
			LOGGER.trace("-Channel " + channel + " is networked, sending all");
			sendAll(new Document("sender", user.getUUID().toString()).append("channel", channel.toString()).append("message", message));
		}
		else {
			LOGGER.trace("-Channel " + channel + " is local, sending locally");
			doLocalSend(user, channel, message);
		}
	}
	
	@Override
	public void receive(String serverFrom, Document data) {
		User sender = userLoader.loadObject(UUID.fromString(data.getString("sender")));
		ChatChannel channel = ChatChannel.valueOf(data.getString("channel"));
		String message = data.getString("message");
	
		doLocalSend(sender, channel, message);
		
	}
	
}
