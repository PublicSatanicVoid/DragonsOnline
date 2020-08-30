package mc.dragons.tools.content;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.PermissionUtil;

public class ClearInventoryCommand implements CommandExecutor {
	//private UserLoader userLoader;
	
	public ClearInventoryCommand() {
		//userLoader = (UserLoader) GameObjectType.USER.<User>getLoader();
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command!");
			return true;
		}
		
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		
		if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.TESTER, true)) return true;
		
		if(args.length == 0) {
			player.sendMessage(ChatColor.GOLD + "Warning!" + ChatColor.YELLOW + " This will clear all items from your inventory. This cannot be undone.");
			player.sendMessage(ChatColor.YELLOW + "Type" + ChatColor.GOLD + " /clear confirm" + ChatColor.YELLOW + " to proceed.");
			return true;
		}
		else {
			if(args[0].equalsIgnoreCase("confirm")) {
				user.clearInventory();
				player.sendMessage(ChatColor.GREEN + "Cleared your inventory.");
				return true;
			}
		}
		
		return true;
	}
}
