package mc.dragons.tools.moderation.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;

public class ModActionUtil {
	public static boolean canPunish(CommandSender sender, User target) {
		if(sender instanceof ConsoleCommandSender) return true;
		User issuer = UserLoader.fromPlayer((Player) sender);
		if(issuer == null || target == null) return false;
		return issuer.getRank().ordinal() > target.getRank().ordinal();
	}
	
	public static boolean checkCanPunish(CommandSender sender, User target) {
		if(canPunish(sender, target)) return true;
		sender.sendMessage(ChatColor.RED + "You cannot punish " + target.getName() + " (" + target.getRank() + "). Use /report instead!");
		return false;
	}
}
