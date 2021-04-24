package mc.dragons.dev.build;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.dev.DragonsDev;

public class BackupCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.BUILDER)) return true;
		
		JavaPlugin.getPlugin(DragonsDev.class).backupFloors();
		sender.sendMessage(ChatColor.GREEN + "Backed up all floors successfully.");
		
		return true;
	}

}