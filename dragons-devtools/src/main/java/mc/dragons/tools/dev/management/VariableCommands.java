package mc.dragons.tools.dev.management;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;

public class VariableCommands extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return true;
		
		if(label.equalsIgnoreCase("getglobalaccessiontoken")) {
			sender.sendMessage(ChatColor.GREEN + "Accession Token: " + ChatColor.GRAY + VAR.getAccessionToken());
		}
		else if(label.equalsIgnoreCase("setglobalaccessiontoken")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/setglobalaccessiontoken <new token>");
				return true;
			}
			VAR.changeAccessionToken(StringUtil.concatArgs(args, 0));
			sender.sendMessage(ChatColor.GREEN + "Updated accession token successfully.");
		}
		else if(label.equalsIgnoreCase("getglobalvariables")) {
			sender.sendMessage(ChatColor.GREEN + "Displaying raw global variable document.");
			sender.sendMessage(ChatColor.GRAY + VAR.getFullDocument().toJson());
		}
		
		return true;
	}

}
