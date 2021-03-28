package mc.dragons.tools.content.command.statistics;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;

public class RenameCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.GM) || !requirePlayer(sender)) return true;
		
		User user = user(sender);
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a new name for the item! /rename <NewItemName>");
			return true;
		}
		
		String renameTo = ChatColor.YELLOW + ChatColor.translateAlternateColorCodes('&', StringUtil.concatArgs(args, 0));
		Item heldItem = ItemLoader.fromBukkit(user.getPlayer().getInventory().getItemInMainHand());
		
		if(heldItem == null) {
			sender.sendMessage(ChatColor.RED + "You must hold the item you want to rename!");
			return true;
		}

		heldItem.setCustom(true);
		user.getPlayer().getInventory().setItemInMainHand(heldItem.rename(renameTo));
		sender.sendMessage(ChatColor.GREEN + "Renamed your held item to " + renameTo);
		
		return true;
	}
}
