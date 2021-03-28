package mc.dragons.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class RespawnCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.TESTER)) return true;
		
		User user = user(sender);
		if (!user.hasDeathCountdown()) {
			sender.sendMessage(ChatColor.RED + "You're not currently on a death countdown!");
			return true;
		}
		user.setDeathCountdown(0);
		user.getPlayer().resetTitle();
		sender.sendMessage(ChatColor.GREEN + "Respawning now...");
		return true;
	}
}
