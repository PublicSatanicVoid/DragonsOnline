package mc.dragons.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class HelpCommand extends DragonsCommandExecutor {
	public static final String INDENT = ChatColor.GRAY + "   " + ChatColor.ITALIC;

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		sender.sendMessage(ChatColor.GOLD + "Displaying help for DragonsOnline.");
		sender.sendMessage(ChatColor.YELLOW + "/channel <speak|listen> <alvador|local|guild|party|trade|help>");
		sender.sendMessage(INDENT + "Set the channels you speak and listen to.");
		sender.sendMessage("");
		sender.sendMessage(ChatColor.YELLOW + "/feedback <your feedback here>");
		sender.sendMessage(INDENT + "Send us feedback about the game.");
		sender.sendMessage("");
		sender.sendMessage(ChatColor.YELLOW + "/guild");
		sender.sendMessage(INDENT + "Join, leave, and manage guilds.");
		sender.sendMessage("");
		sender.sendMessage(ChatColor.YELLOW + "/msg <player> <message>");
		sender.sendMessage(INDENT + "Send a private message.");
		sender.sendMessage("");
		sender.sendMessage(ChatColor.YELLOW + "/myquests");
		sender.sendMessage(INDENT + "View your quests.");
		sender.sendMessage("");
		sender.sendMessage(ChatColor.YELLOW + "/reply <message>");
		sender.sendMessage(INDENT + "Reply to the last player who messaged you.");
		sender.sendMessage("");
		sender.sendMessage(ChatColor.YELLOW + "/report <player> [reason]");
		sender.sendMessage(INDENT + "Report a rule-breaker.");
		sender.sendMessage("");
		if(hasPermission(sender, PermissionLevel.TESTER)) {
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.ITALIC + "As a staff member, you have access to additional commands. Please refer to the staff documentation.");
		}
		return true;
	}
}
