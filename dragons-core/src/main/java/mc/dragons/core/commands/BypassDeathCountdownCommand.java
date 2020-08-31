package mc.dragons.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;

public class BypassDeathCountdownCommand implements CommandExecutor {
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if (!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.TESTER, true))
				return true;
		} else {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command.");
			return true;
		}
		if (!user.hasDeathCountdown()) {
			sender.sendMessage(ChatColor.RED + "You're not currently on a death countdown!");
			return true;
		}
		user.setDeathCountdown(0);
		sender.sendMessage(ChatColor.GREEN + "Respawning now...");
		return true;
	}
}
