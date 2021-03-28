package mc.dragons.tools.moderation.abilities;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class GodModeCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.MODERATOR)) return true;
		
		User user = user(sender);
		user.setGodMode(!user.isGodMode());
		sender.sendMessage(ChatColor.GREEN + "God mode " + (user.isGodMode() ? "enabled" : "disabled"));
		
		return true;
		
	}

}
