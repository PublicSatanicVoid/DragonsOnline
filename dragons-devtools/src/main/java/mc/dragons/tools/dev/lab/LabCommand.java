package mc.dragons.tools.dev.lab;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

/**
 * Designed for rapidly iterating on ideas / making a minimum working example.
 * 
 * @author Adam
 *
 */
public class LabCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return true;
		sender.sendMessage("Until next time...");
		return true;
	}
}
