package mc.dragons.tools.moderation.abilities;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class UnVanishCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.HELPER)) return true;
		user(sender).setVanished(false);
		sender.sendMessage(ChatColor.GREEN + "You are no longer vanished.");
		return true;
	}
}
