package mc.dragons.social;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.chat.ChatChannel;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;
import net.md_5.bungee.api.chat.TextComponent;

public class ChannelCommand extends DragonsCommandExecutor {
	private boolean handleAlias(CommandSender sender, String label) {
		String command = "";
		switch(label) {
		case "csa":
			command = "c s a";
			break;
		case "csl":
			command = "c s l";
			break;
		case "csg":
			command = "c s g";
			break;
		case "csp":
			command = "c s p";
			break;
		case "cst":
			command = "c s t";
			break;
		case "csh":
			command = "c s h";
			break;
		case "css":
			command = "c s s";
			break;
		case "cla":
			command = "c l a";
			break;
		case "cll":
			command = "c l l";
			break;
		case "clg":
			command = "c l g";
			break;
		case "clp":
			command = "c l p";
			break;
		case "clt":
			command = "c l t";
			break;
		case "clh":
			command = "c l h";
			break;
		case "cls":
			command = "c l s";
			break;
		default:
			break;
		}
		if(!command.isEmpty()) {
			player(sender).performCommand(command);
			return true;
		}
		return false;
	}
	
	private String validChannels(boolean staff) {
		return Arrays.stream(ChatChannel.values())
			.filter(ch -> ch != ChatChannel.STAFF || staff)
			.map(ch -> ch.toString().toLowerCase())
			.reduce((a, b) -> a + "|" + b)
			.get();
	}
	
	private ChatChannel parse(CommandSender sender, boolean staff, String param) {
		for(ChatChannel channel : ChatChannel.values()) {
			if(channel.toString().equalsIgnoreCase(param) || channel.getAbbreviation().equalsIgnoreCase(param)) {
				if(channel == ChatChannel.STAFF && !staff) {
					sender.sendMessage(ChatColor.RED + "That is a staff-only channel!");
					return null;
				}
				return channel;
			}
		}
		sender.sendMessage(ChatColor.RED + "Invalid chat channel! Channels are " + validChannels(staff));
		return null;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		User user = user(sender);
		boolean staff = hasPermission(sender, PermissionLevel.BUILDER);
		
		if(handleAlias(sender, label)) return true;
		
		if (args.length == 0) {
			TextComponent header = StringUtil.plainText(ChatColor.YELLOW + "" + ChatColor.STRIKETHROUGH + "===" + ChatColor.YELLOW + " Chat Channels " + ChatColor.YELLOW + "" + ChatColor.STRIKETHROUGH + "===");
			TextComponent help = StringUtil.hoverableText(ChatColor.GRAY + " [?]",
				ChatColor.YELLOW + "" + ChatColor.BOLD + "Chat Channels",
				ChatColor.GREEN + "Channels are different streams of chat you can speak or listen on.",
				ChatColor.WHITE + "You can listen to as many channels as you like,",
				ChatColor.WHITE + "but can only speak on one.",
				"",
				ChatColor.WHITE + "Click " + ChatColor.GRAY + "[S]" + ChatColor.RESET + " to speak on a channel.",
				ChatColor.WHITE + "Click " + ChatColor.GRAY + "[L]" + ChatColor.RESET + " to listen on a channel.",
				ChatColor.RESET + "Do " + ChatColor.GRAY + "/channel <channel>" + ChatColor.RESET + " to manually toggle a channel.");
			sender.spigot().sendMessage(header, help);
			for(ChatChannel channel : ChatChannel.values()) {
				if(channel == ChatChannel.STAFF && !staff) continue;
				boolean speaking = user.getSpeakingChannel() == channel;
				boolean listening = user.getActiveChatChannels().contains(channel);
				TextComponent speak = speaking ? 
						StringUtil.clickableHoverableText(ChatColor.DARK_GREEN + "[S] ", "/channel listen " + channel, 
							ChatColor.YELLOW + "» Click to stop listening on " + channel,
							ChatColor.GRAY + "   " + channel.getDescription(),
							"",
							ChatColor.GREEN + "You are currently speaking and listening on this channel") :
						StringUtil.clickableHoverableText(ChatColor.GRAY + "[S] ", "/channel speak " + channel, 
							ChatColor.YELLOW + "» Click to speak on channel " + channel, 
							ChatColor.GRAY + "   " + channel.getDescription());
				TextComponent listen = listening ?
						StringUtil.clickableHoverableText(ChatColor.DARK_GREEN + "[L] ", "/channel listen " + channel, 
							ChatColor.YELLOW + "» Click to stop listening on " + channel,
							ChatColor.GRAY + "   " + channel.getDescription(),
							"",
							ChatColor.GREEN + "You are currently " + (speaking ? "speaking and " : "") + "listening on this channel"):
						StringUtil.clickableHoverableText(ChatColor.GRAY + "[L] ", "/channel listen " + channel, 
							ChatColor.YELLOW + "» Click to listen on channel " + channel,
							ChatColor.GRAY + "   " + channel.getDescription());
				TextComponent channelName = StringUtil.hoverableText((listening ? ChatColor.GREEN : ChatColor.RESET) + "" + channel, ChatColor.GRAY + channel.getDescription());
				sender.spigot().sendMessage(speak, listen, channelName);
			}
//			sender.sendMessage(ChatColor.RED + "/channel <speak|listen> <" + validChannels(staff) + ">");
//			sender.sendMessage(ChatColor.RED + "Curently speaking on " + user.getSpeakingChannel());
//			sender.sendMessage(ChatColor.RED + "Currently listening to " + StringUtil.parseList(user.getActiveChatChannels()));
			return true;
		}
		
		if (args.length == 1) {
			ChatChannel channel = parse(sender, staff, args[0]);
			if(channel == null) return true;
			
			if(user.getActiveChatChannels().contains(channel)) {
				user.removeActiveChatChannel(channel);
				sender.sendMessage(ChatColor.GREEN + "You are no longer " + (user.getSpeakingChannel() == channel ? "speaking or " : "") + "listening on " + channel);
			}
			else {
				user.setSpeakingChannel(channel);
				user.addActiveChatChannel(channel);
				sender.sendMessage(ChatColor.GREEN + "You are now speaking and listening on " + channel);
				if(user.getActiveChatChannels().size() > 1) {
					List<ChatChannel> others = new ArrayList<>(user.getActiveChatChannels());
					others.remove(channel);
					sender.sendMessage(ChatColor.GRAY + "You are also listening on " + StringUtil.parseList(others.stream().map(c -> c.toString()).collect(Collectors.toList())));
				}
			}
			return true;
		}
		
		ChatChannel channel = parse(sender, staff, args[1]);
		
		if(channel == null) return true;
		if (args[0].equalsIgnoreCase("speak") || args[0].equalsIgnoreCase("s")) {
			if(user.getSpeakingChannel() == channel) {
				sender.sendMessage(ChatColor.RED + "You are already speaking on " + channel);
			}
			user.setSpeakingChannel(channel);
			sender.sendMessage(ChatColor.GREEN + "Now speaking and listening on " + channel);
			if (!user.getActiveChatChannels().contains(channel)) {
				user.addActiveChatChannel(channel);
//				sender.sendMessage(ChatColor.GREEN + "You are now listening to this channel as well.");
			}
		}
		else if (args[0].equalsIgnoreCase("listen") || args[0].equalsIgnoreCase("l")) {
			if (user.getActiveChatChannels().contains(channel)) {
				user.removeActiveChatChannel(channel);
				sender.sendMessage(ChatColor.GREEN + "You are no longer listening on " + channel);
				return true;
			}
			user.addActiveChatChannel(channel);
			sender.sendMessage(ChatColor.GREEN + "You are now listening on " + channel);
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid arguments! /channel");
		}
		
		return true;
	}
}
