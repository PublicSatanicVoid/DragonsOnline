package mc.dragons.anticheat.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.anticheat.DragonsAntiCheatPlugin;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;

public class AntiCheatCommand implements CommandExecutor {

	private DragonsAntiCheatPlugin plugin;
	
	public AntiCheatCommand(DragonsAntiCheatPlugin instance) {
		plugin = instance;
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		Player player = null;
		User user = null;
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true)) return true;
		}
	
		
		if(label.equalsIgnoreCase("acdebug")) {
			plugin.setDebug(!plugin.isDebug());
			if(plugin.isDebug()) {
				sender.sendMessage(ChatColor.GREEN + "Now debugging anticheat");
			}
			else {
				sender.sendMessage(ChatColor.GREEN + "No longer debugging anticheat");
			}
			return true;
		}
		
		
		sender.sendMessage("Coming Soon...");
		
		return true;
	}

}
