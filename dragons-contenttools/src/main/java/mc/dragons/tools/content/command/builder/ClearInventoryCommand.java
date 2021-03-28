package mc.dragons.tools.content.command.builder;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class ClearInventoryCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		if(!requirePermission(sender, PermissionLevel.TESTER)) return true;

		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "Warning! " + ChatColor.YELLOW + "This will clear all items from your inventory. This cannot be undone.");
			sender.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GOLD + "/clear confirm" + ChatColor.YELLOW + " to proceed.");
			return true;
		}
		else if(args[0].equalsIgnoreCase("confirm")) {
			user(sender).clearInventory();
			sender.sendMessage(ChatColor.GREEN + "Cleared your inventory.");
			return true;
		}
		
		
		return true;
	}
}
