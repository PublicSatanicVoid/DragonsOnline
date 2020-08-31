package mc.dragons.tools.dev;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class PlaceholderCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command!");
			return true;
		}
		
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		
		if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, true)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/placeholder [-append] <Data...>");
			sender.sendMessage(ChatColor.RED + "Curent placeholder: " + user.getLocalData().get("placeholder", "(None)"));
			sender.sendMessage(ChatColor.RED + "Use %PH% in dialogue, etc. to substitute placeholder.");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("-append")) {
			user.getLocalData().append("placeholder", user.getLocalData().get("placeholder", "") + " " + StringUtil.concatArgs(args, 1));
			sender.sendMessage(ChatColor.GREEN + "Appended to placeholder successfully.");
			return true;
		}
		
		user.getLocalData().append("placeholder", StringUtil.concatArgs(args, 0));
		sender.sendMessage(ChatColor.GREEN + "Set placeholder successfully.");
		
		return true;
	}

}
