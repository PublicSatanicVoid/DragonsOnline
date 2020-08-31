package mc.dragons.tools.moderation;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;

public class GodModeCommand implements CommandExecutor {

	//private UserLoader userLoader;
	
	public GodModeCommand() {
		//userLoader = (UserLoader) GameObjectType.USER.<User>getLoader();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.MODERATOR, true)) return true;
		}
		else {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command.");
			return true;
		}
		
		user.setGodMode(!user.isGodMode());
		sender.sendMessage(ChatColor.GREEN + "God mode " + (user.isGodMode() ? "enabled" : "disabled"));
		
		return true;
		
	}

}
