package mc.dragons.tools.content;

import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class ReloreCommand implements CommandExecutor {

	//private UserLoader userLoader;
	
	public ReloreCommand() {
		//userLoader = (UserLoader) GameObjectType.USER.<User>getLoader();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command.");
			return true;
		}
		
		
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		
		boolean bypassed = PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, true); //false);
		boolean valid = false; //user.getSkillLevel(SkillType.BLACKSMITHING) >= 40 && user.getGold() >= 100.0 || bypassed;
		
		if(!valid && !bypassed) {
			//sender.sendMessage(ChatColor.RED + "Requires Blacksmithing Lv. 40+ and 100 Gold.");
			return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a new name for the item! /relore <ItemLoreLine1|ItemLoreLine2|ItemLoreLine3...>");
			sender.sendMessage(ChatColor.RED + "e.g. /relore The re-lored sword of awesomeness!|This sword is extra fancy.|Another line");
			return true;
		}
		
		String[] reloreTo = ChatColor.translateAlternateColorCodes('&', StringUtil.concatArgs(args, 0)).split(Pattern.quote("|"));

		Item heldItem = ItemLoader.fromBukkit(user.getPlayer().getInventory().getItemInMainHand());
		
		if(heldItem == null) {
			sender.sendMessage(ChatColor.RED + "You must hold the item you want to relore!");
			return true;
		}

		heldItem.setCustom(true);
		user.getPlayer().getInventory().setItemInMainHand(heldItem.relore(reloreTo));
		sender.sendMessage(ChatColor.GREEN + "Relored your item successfully.");
		
		if(!bypassed) {
			user.takeGold(100.0);
		}
		
		return true;
		
	}

}
