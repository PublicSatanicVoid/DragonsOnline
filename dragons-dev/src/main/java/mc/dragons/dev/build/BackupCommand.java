package mc.dragons.dev.build;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.dev.DragonsDev;

public class BackupCommand extends DragonsCommandExecutor {
	private DragonsDev plugin;
	
	public BackupCommand(DragonsDev instance) {
		plugin = instance;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.BUILDER)) return true;
		
		plugin.backupFloors();
		sender.sendMessage(ChatColor.GREEN + "Backed up all floors successfully.");
		
		return true;
	}

}
