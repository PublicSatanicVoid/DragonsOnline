package mc.dragons.core.gameobject.user;

import java.util.UUID;
import java.util.logging.Logger;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.user.chat.ChatChannel;
import mc.dragons.core.gameobject.user.chat.ChatMessageRegistry;
import mc.dragons.core.gameobject.user.chat.MessageData;
import mc.dragons.core.networking.MessageHandler;
import mc.dragons.core.util.StringUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class ChatMessageHandler extends MessageHandler {
	private UserLoader userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	private ChatMessageRegistry registry = Dragons.getInstance().getChatMessageRegistry();
	private Logger LOGGER = Dragons.getInstance().getLogger();
	
	public ChatMessageHandler() {
		super(Dragons.getInstance(), "chat");
	}

	private void doLocalSend(User user, ChatChannel channel, String message) {
		LOGGER.finer("Chat message from " + user.getName());
		LOGGER.finer("-Creating message text component");
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
				new ComponentBuilder(ChatColor.YELLOW + "" + ChatColor.BOLD + user.getName() + "\n")
					.append(ChatColor.GRAY + "Server: " + ChatColor.RESET + user.getServer() + "\n")
					.append(ChatColor.GRAY + "Rank: " + ChatColor.RESET + user.getRank().getNameColor() + user.getRank().getRankName() + "\n")
					.append(ChatColor.GRAY + "Level: " + user.getLevelColor() + user.getLevel() + "\n")
					.append(ChatColor.GRAY + "XP: " + ChatColor.RESET + user.getXP() + "\n")
					.append(ChatColor.GRAY + "Gold: " + ChatColor.RESET + user.getGold() + "\n")
					.append(ChatColor.GRAY + "Location: " + ChatColor.RESET + StringUtil.locToString(loc) + ChatColor.DARK_GRAY + ChatColor.ITALIC + " (when message sent)\n")
					.append(ChatColor.GRAY + "Floor: " + ChatColor.RESET + FloorLoader.fromWorld(loc.getWorld()).getDisplayName() + ChatColor.DARK_GRAY + ChatColor.ITALIC
							+ " (when message sent)\n")
					.append(ChatColor.GRAY + "First Joined: " + ChatColor.RESET + user.getFirstJoined().toString()).create()));
		messageInfoComponent.addExtra(ChatColor.GRAY + " » ");
		TextComponent messageComponent = new TextComponent(user.getRank().getChatColor() + message);
		messageComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
				new ComponentBuilder(ChatColor.YELLOW + "Click to report this message").create()));
		messageComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chatreport " + messageData.getId()));
		registry.register(messageData);
		for (User test: UserLoader.allUsers()) {
			LOGGER.finer("-Checking if " + user.getName() + " can receive");
			if(test.getPlayer() == null) continue;
			if (!channel.canHear(test, user) && !test.hasChatSpy()) continue;
			LOGGER.finer("  -Yes!");
			test.sendMessage(channel, user, new BaseComponent[] { messageInfoComponent, messageComponent });
		}
		LOGGER.info("[" + user.getServer() + "/" + channel.getAbbreviation() + "/" + loc.getWorld().getName() + "] [" + user.getName() + "] " + message);
	}
	
	public void send(User user, ChatChannel channel, String message) {
		if(channel.isNetworked()) {
			LOGGER.finer("-Channel " + channel + " is networked, sending all");
			sendAll(new Document("sender", user.getUUID().toString()).append("channel", channel.toString()).append("message", message));
		}
		else {
			LOGGER.finer("-Channel " + channel + " is local, sending locally");
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
