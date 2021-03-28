package mc.dragons.social;

import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.chat.ChatChannel;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;

public class ChannelCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		User user = user(sender);
		boolean staff = hasPermission(sender, PermissionLevel.BUILDER);
		
		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "/channel <speak|listen>"
					+ " <" + Arrays.stream(ChatChannel.values())
								.filter(ch -> ch != ChatChannel.STAFF || staff)
								.map(ch -> ch.toString().toLowerCase())
								.reduce((a, b) -> a + "|" + b)
								.get() + ">");
			sender.sendMessage(ChatColor.RED + "Curently speaking on " + user.getSpeakingChannel());
			sender.sendMessage(ChatColor.RED + "Currently listening to " + StringUtil.parseList(user.getActiveChatChannels()));
			return true;
		}
		
		ChatChannel channel = StringUtil.parseEnum(sender, ChatChannel.class, args[1]);
		if(channel == null) return true;
		
		if (channel == ChatChannel.STAFF && !staff) {
			sender.sendMessage(ChatColor.RED + "That is a staff-only channel!");
		}
		else if (args[0].equalsIgnoreCase("speak") || args[0].equalsIgnoreCase("s")) {
			user.setSpeakingChannel(channel);
			sender.sendMessage(ChatColor.GREEN + "Now speaking on " + channel);
			if (!user.getActiveChatChannels().contains(channel)) {
				user.addActiveChatChannel(channel);
				sender.sendMessage(ChatColor.GREEN + "You are now listening to this channel as well.");
			}
		}
		else if (args[0].equalsIgnoreCase("listen") || args[0].equalsIgnoreCase("l")) {
			if (user.getActiveChatChannels().contains(channel)) {
				user.removeActiveChatChannel(channel);
				sender.sendMessage(ChatColor.GREEN + "No longer listening to " + channel);
				return true;
			}
			user.addActiveChatChannel(channel);
			sender.sendMessage(ChatColor.GREEN + "Now listening to " + channel);
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid arguments! /channel");
		}
		
		return true;
	}
}
