package mc.dragons.tools.content.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;

public class PlaceholderCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.GM)) return true;
		User user = user(sender);	
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/placeholder [-append] <Data...>");
			sender.sendMessage(ChatColor.RED + "Curent placeholder: " + user.getLocalData().get("placeholder", "(None)"));
			sender.sendMessage(ChatColor.RED + "Use %PH% in dialogue, etc. to substitute placeholder.");
		}
		
		else if(args[0].equalsIgnoreCase("-append")) {
			user.getLocalData().append("placeholder", user.getLocalData().get("placeholder", "") + " " + StringUtil.concatArgs(args, 1));
			sender.sendMessage(ChatColor.GREEN + "Appended to placeholder successfully.");
		}
		
		else {
			user.getLocalData().append("placeholder", StringUtil.concatArgs(args, 0));
			sender.sendMessage(ChatColor.GREEN + "Set placeholder successfully.");
		}
		
		return true;
	}

}
