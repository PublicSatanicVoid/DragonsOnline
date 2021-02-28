package mc.dragons.dev;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.PermissionUtil;

public class BackupCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player && !PermissionUtil.verifyActivePermissionLevel(UserLoader.fromPlayer((Player) sender), PermissionLevel.BUILDER, true)) return true;
		
		JavaPlugin.getPlugin(DragonsDevPlugin.class).backupFloors();
		sender.sendMessage(ChatColor.GREEN + "Backed up all floors successfully.");
		
		return true;
	}

}
