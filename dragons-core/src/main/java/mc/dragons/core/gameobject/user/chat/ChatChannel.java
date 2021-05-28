package mc.dragons.core.gameobject.user.chat;

import org.bukkit.ChatColor;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.chat.channel_handlers.AlvadorChannelHandler;
import mc.dragons.core.gameobject.user.chat.channel_handlers.HelpChannelHandler;
import mc.dragons.core.gameobject.user.chat.channel_handlers.LocalChannelHandler;
import mc.dragons.core.gameobject.user.chat.channel_handlers.StaffChannelHandler;
import mc.dragons.core.gameobject.user.chat.channel_handlers.TradeChannelHandler;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

/**
 * Named channels of text-based communication in the game.
 * 
 * <p>Users speak on one channel and listen on one or more.
 * 
 * <p>Some channels are context-sensitive; for example, the
 * LOCAL channel is associated with the floor a player is on.
 * Thus, two players listening only on the LOCAL channel 
 * cannot hear each other if they are on different floors.
 * 
 * @author Adam
 *
 */
public enum ChatChannel {
	ALVADOR("A", "Global chat for all of Alvador", true, new AlvadorChannelHandler()), 
	LOCAL("L", "Local chat for your current floor", true, new LocalChannelHandler()),
	GUILD("G", "Channel for your guild only", true, null), 
	PARTY("P", "Channel for your party only", true, null), 
	TRADE("T", "Global chat for trade discussion", true, new TradeChannelHandler()),
	HELP("H", "Global chat to ask for help", true, new HelpChannelHandler()), 
	STAFF("S", "Staff-only channel", true, new StaffChannelHandler());

	private String abbreviation;
	private String description;
	private boolean networked;
	private ChannelHandler handler;

	ChatChannel(String abbreviation, String description, boolean networked, ChannelHandler handler) {
		this.abbreviation = abbreviation;
		this.description = description;
		this.handler = handler;
		this.networked = networked;
	}

	public String getAbbreviation() {
		return abbreviation;
	}

	public String getDescription() {
		return description;
	}

	public TextComponent getPrefix() {
		return format(ChatColor.GRAY + "[" + getAbbreviation() + "]");
	}

	/**
	 * 
	 * @return Whether chat messages on this channel are synced with other servers.
	 */
	public boolean isNetworked() {
		return networked;
	}
	
	public ChannelHandler getHandler() {
		return handler;
	}

	public void setHandler(ChannelHandler handler) {
		this.handler = handler;
	}

	/**
	 * 
	 * @param str
	 * @return A hoverable overlay showing a snapshot of data about the channel.
	 */
	public TextComponent format(String str) {
		long listening = UserLoader.allUsers().stream().filter(u -> u.getServerName() != null).filter(u -> u.getActiveChatChannels().contains(this)).count();
		TextComponent component = new TextComponent(str);
		component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
				new Text(ChatColor.YELLOW + "Channel: " + ChatColor.RESET + toString() + "\n"),
				new Text(ChatColor.YELLOW + "Listening: " + ChatColor.RESET + listening + "\n"),
				new Text(ChatColor.ITALIC + getDescription() + "\n"),
				new Text(ChatColor.GRAY + "Do " + ChatColor.RESET + "/channel " + ChatColor.GRAY + "to manage channels")));
		return component;
	}

	public TextComponent format() {
		return format(toString());
	}

	public boolean canHear(User to, User from) {
		if (to.hasActiveDialogue() || !to.hasJoined()) {
			return false;
		}
		if(from == null) {
			return true;
		}
		if (handler == null) {
			Dragons.getInstance().getLogger().warning("Channel " + toString() + " does not have an associated handler! This channel will be unusable until a handler is registrered to it.");
			from.getPlayer().sendMessage(ChatColor.RED + "This channel (" + toString() + ") is not set up correctly. Please report this if the error persists.");
			return false;
		}
		return handler.canHear(to, from);
	}

	public static ChatChannel parse(String str) {
		for(ChatChannel ch : values()) {
			if(ch.toString().equalsIgnoreCase(str) || ch.getAbbreviation().equalsIgnoreCase(str)) {
				return ch;
			}
		}
		return null;
	}
}
