package mc.dragons.social;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.chat.ChatChannel;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gui.GUI;
import mc.dragons.core.gui.GUIElement;
import mc.dragons.core.util.StringUtil;

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
	
	private void openGUI(User user) {
		boolean staff = hasPermission(user, PermissionLevel.BUILDER);
		GUI gui = new GUI(5, ChatColor.WHITE + "Chat Channels");
		gui.add(new GUIElement(4, Material.BOOK, ChatColor.AQUA + "Chat Channels", List.of(
				ChatColor.WHITE + "Channels are different streams of chat",
				ChatColor.WHITE + "that you can speak or listen on.",
				"",
				ChatColor.WHITE + "You can listen to multiple channels,",
				ChatColor.WHITE + "but can only speak on one.",
				"",
				ChatColor.WHITE + "Do " + ChatColor.GRAY + "/channel <channel> " + ChatColor.WHITE + "to manually",
				ChatColor.WHITE + "change channels."), 1, u -> {}));
		// Assume we have 7 or fewer channels, otherwise overflow occurs
		int i = 10;
		for(ChatChannel channel : ChatChannel.values()) {
			if(channel == ChatChannel.STAFF && !staff) continue;
			boolean speaking = user.getSpeakingChannel() == channel;
			boolean listening = user.getActiveChatChannels().contains(channel);
			gui.add(new GUIElement(i, Material.OAK_SIGN, ChatColor.YELLOW + "" + ChatColor.BOLD + channel, List.of(
					ChatColor.GRAY + channel.getDescription(),
					ChatColor.GRAY + "- " + (speaking ? ChatColor.GREEN + "You are speaking on this channel" : ChatColor.RED + "You are not speaking on this channel"),
					ChatColor.GRAY + "- " + (listening ? ChatColor.GREEN + "You are listening to this channel" : ChatColor.RED + "You are not speaking to this channel")), 
					1, u -> {}));
			gui.add(new GUIElement(i + 9, speaking ? Material.EMERALD_BLOCK : Material.STONE, 
					speaking ? ChatColor.GRAY + "You are currently speaking on this channel" : ChatColor.GREEN + "Click to speak on " + channel,
					ChatColor.GRAY + "You can only speak on one channel at a time", u -> {
						u.setSpeakingChannel(channel);
						if(!u.getActiveChatChannels().contains(channel)) {
							u.addActiveChatChannel(channel);
						}
						openGUI(u);
					}));
			gui.add(new GUIElement(i + 9 * 2, listening ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK, 
					listening ? 
						  speaking ? ChatColor.RED + "You must listen to the channel you're speaking on" : ChatColor.RED + "Click to stop listening to " + channel 
						: ChatColor.GREEN + "Click to listen to " + channel,
					ChatColor.GRAY + "You can listen to multiple channels at once", u -> {
						if(speaking && listening) return; 
						if(listening) {
							u.removeActiveChatChannel(channel);
						}
						else {
							u.addActiveChatChannel(channel);
						}
						openGUI(u);
					}));
			i++;
		}
		gui.open(user);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		User user = user(sender);
		boolean staff = hasPermission(sender, PermissionLevel.BUILDER);
		
		if(handleAlias(sender, label)) return true;
		
		if (args.length == 0) {
			openGUI(user);
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
