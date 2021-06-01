package mc.dragons.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class HelpCommand extends DragonsCommandExecutor {
	private static final String INDENT = ChatColor.GRAY + "   " + ChatColor.ITALIC;

	private void help(CommandSender sender, String command, String usage) {
		sender.sendMessage(ChatColor.YELLOW + command);
		sender.sendMessage(INDENT + usage);
		sender.sendMessage("");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		sender.sendMessage(ChatColor.GOLD + "Displaying help for DragonsOnline.");
		help(sender, "/channel", "Manage the channels you speak and listen on.");
		help(sender, "/duel <player>", "Send a duel request.");
		help(sender, "/feedback <your feedback here>", "Send us feedback about the game.");
		help(sender, "/friend <player>", "Send a friend request.");
		help(sender, "/guild", "Join, leave, and manage guilds.");
		help(sender, "/msg <player> <message>", "Send a private message.");
		help(sender, "/myquests", "View your active and completed quests.");
		help(sender, "/reply <message>", "Reply to the last player who messaged you.");
		help(sender, "/report <player> [reason]", "Report a rule-breaker.");
		help(sender, "/togglemsg", "Toggle messaging between public and friends-only.");
		if(hasPermission(sender, PermissionLevel.TESTER)) {
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.ITALIC + "As a staff member, you have access to additional commands. Please refer to the staff documentation.");
		}
		return true;
	}
}
