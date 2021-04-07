package mc.dragons.tools.moderation;

import org.bson.Document;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import net.md_5.bungee.api.ChatColor;

public class ModNotificationsCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
		User user = user(sender);
		Document settings = user.getData().get("modnotifs", new Document());
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/mn reports" + ChatColor.GRAY + " toggles real-time report notifications " + (settings.getBoolean("reports", true) ? "[ON]" : "[OFF]"));
			sender.sendMessage(ChatColor.YELLOW + "/mn susjoin" + ChatColor.GRAY + " toggles suspicious join notifications " + (settings.getBoolean("susjoin", true) ? "[ON]" : "[OFF]"));
			return true;
		}
		
		
		if(args[0].equalsIgnoreCase("reports")) {
			settings.append("reports", !settings.getBoolean("reports", true));
			sender.sendMessage(ChatColor.GREEN + "Toggled report notifications " + (settings.getBoolean("reports") ? "on" : "off"));
		}
		
		else if(args[0].equalsIgnoreCase("susjoin")) {
			settings.append("susjoin", !settings.getBoolean("susjoin", true));
			sender.sendMessage(ChatColor.GREEN + "Toggled suspicious join notifications " + (settings.getBoolean("susjoin") ? "on" : "off"));
		}
		
		else {
			sender.sendMessage(ChatColor.RED + "Invalid setting! /mn");
		}
		
		user.getStorageAccess().set("modnotifs", settings);
		
		return true;
	}

}
