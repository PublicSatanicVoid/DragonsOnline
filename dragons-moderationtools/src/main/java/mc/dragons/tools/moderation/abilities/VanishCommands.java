package mc.dragons.tools.moderation.abilities;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class VanishCommands extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.HELPER)) return true;
		User user = user(sender);
		
		if(label.equalsIgnoreCase("vanish") || label.equalsIgnoreCase("v")) {
			user.setVanished(true);
			sender.sendMessage(ChatColor.GREEN + "You are now vanished.");
		}
		else {
			user.setVanished(false);
			sender.sendMessage(ChatColor.GREEN + "You are no longer vanished.");
		}
		
		return true;
	}

}
