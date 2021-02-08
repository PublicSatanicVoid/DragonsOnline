package mc.dragons.core.gameobject.user;

import org.bukkit.ChatColor;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.channel_handlers.AlvadorChannelHandler;
import mc.dragons.core.gameobject.user.channel_handlers.HelpChannelHandler;
import mc.dragons.core.gameobject.user.channel_handlers.LocalChannelHandler;
import mc.dragons.core.gameobject.user.channel_handlers.StaffChannelHandler;
import mc.dragons.core.gameobject.user.channel_handlers.TradeChannelHandler;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Named channels of text-based communication in the game.
 * 
 * @author Adam
 *
 */
public enum ChatChannel {
	ALVADOR("A", "Global chat for all of Alvador", new AlvadorChannelHandler()), 
	LOCAL("L", "Local chat for your current floor", new LocalChannelHandler()),
	GUILD("G", "Channel for your guild only", null), 
	PARTY("P", "Channel for your party only", null), 
	TRADE("T", "Global chat for trade discussion", new TradeChannelHandler()),
	HELP("H", "Global chat to ask for help", new HelpChannelHandler()), STAFF("S", "Staff-only channel", new StaffChannelHandler());

	private String abbreviation;
	private String description;
	private ChannelHandler handler;

	ChatChannel(String abbreviation, String description, ChannelHandler handler) {
		this.abbreviation = abbreviation;
		this.description = description;
		this.handler = handler;
	}

	public String getAbbreviation() {
		return this.abbreviation;
	}

	public String getDescription() {
		return this.description;
	}

	public TextComponent getPrefix() {
		return format(ChatColor.GRAY + "[" + getAbbreviation() + "]");
	}

	public ChannelHandler getHandler() {
		return this.handler;
	}

	public void setHandler(ChannelHandler handler) {
		this.handler = handler;
	}

	public TextComponent format(String str) {
		long listening = UserLoader.allUsers().stream().filter(u -> u.getActiveChatChannels().contains(this)).count();
		TextComponent component = new TextComponent(str);
		component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
				(new ComponentBuilder(ChatColor.YELLOW + "Channel: " + ChatColor.RESET + toString() + "\n")).append(ChatColor.YELLOW + "Listening: " + ChatColor.RESET + listening + "\n")
						.append(ChatColor.ITALIC + getDescription() + "\n").append(ChatColor.GRAY + "Do " + ChatColor.RESET + "/channel " + ChatColor.GRAY + "to manage channels").create()));
		return component;
	}

	public TextComponent format() {
		return format(toString());
	}

	public boolean canHear(User to, User from) {
		if (to.hasActiveDialogue() || !to.hasJoined())
			return false;
		if (this.handler == null) {
			Dragons.getInstance().getLogger().warning("Channel " + toString() + " does not have an associated handler! This channel will be unusable until a handler is registrered to it.");
			from.getPlayer().sendMessage(ChatColor.RED + "This channel (" + toString() + ") is not set up correctly. Please report this if the error persists.");
			return false;
		}
		return this.handler.canHear(to, from);
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
