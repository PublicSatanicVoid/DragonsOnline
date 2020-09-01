package mc.dragons.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.user.ChatChannel;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class ChannelCommand implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command!");
			return true;
		}
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		boolean staff = PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.BUILDER, false);
		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "/channel <speak|listen> <alvador|local|guild|party|trade|help" + (staff ? "|staff" : "") + ">");
			sender.sendMessage(ChatColor.RED + "Curently speaking on " + user.getSpeakingChannel());
			sender.sendMessage(ChatColor.RED + "Currently listening to " + StringUtil.parseList(user.getActiveChatChannels()));
			return true;
		}
		ChatChannel channel = ChatChannel.parse(args[1].toUpperCase());
		if (channel == null) {
			sender.sendMessage(ChatColor.RED + "Invalid channel name! Valid channels are " + StringUtil.parseList((Object[]) ChatChannel.values()));
			return true;
		}
		if (channel == ChatChannel.STAFF && !staff) {
			sender.sendMessage(ChatColor.RED + "That is a staff-only channel!");
			return true;
		}
		if (args[0].equalsIgnoreCase("speak") || args[0].equalsIgnoreCase("s")) {
			user.setSpeakingChannel(channel);
			sender.sendMessage(ChatColor.GREEN + "Now speaking on " + channel);
			if (!user.getActiveChatChannels().contains(channel)) {
				user.addActiveChatChannel(channel);
				sender.sendMessage(ChatColor.GREEN + "You are now listening to this channel as well.");
			}
			return true;
		}
		if (args[0].equalsIgnoreCase("listen") || args[0].equalsIgnoreCase("l")) {
			if (user.getActiveChatChannels().contains(channel)) {
				user.removeActiveChatChannel(channel);
				sender.sendMessage(ChatColor.GREEN + "No longer listening to " + channel);
				return true;
			}
			user.addActiveChatChannel(channel);
			sender.sendMessage(ChatColor.GREEN + "Now listening to " + channel);
			return true;
		}
		sender.sendMessage(ChatColor.RED + "Invalid arguments!");
		return true;
	}
}
