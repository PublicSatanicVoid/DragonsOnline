package mc.dragons.tools.moderation;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.PermissionUtil;

public class VanishCommands implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command!");
			return true;
		}
		
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		
		if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.MODERATOR, true)) return true;
		
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
