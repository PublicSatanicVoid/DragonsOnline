package mc.dragons.core.commands;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.gameobject.user.Rank;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;

public class AutoRankCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return true;
		
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "/autorank <player> <rank>");
			return true;
		}
		
		User user = userLoader.loadObject(args[0]);
		if(user != null) {
			sender.sendMessage(ChatColor.RED + "That user already exists! Use /rank <player> <rank> instead");
			return true;
		}
		
		Rank rank = StringUtil.parseEnum(sender, Rank.class, args[1]);
		
		if(VAR.get("autorank") == null) {
			VAR.set("autorank", new Document());
		}
		
		UserLoader.uuidFromUsername(args[0], uuid -> {
			if(uuid == null) {
				sender.sendMessage(ChatColor.RED + "Invalid username! No Minecraft user by that name exists.");
				return;
			}
			VAR.set("autorank", VAR.getDocument("autorank").append(uuid.toString(), rank.toString()));
			sender.sendMessage(ChatColor.GREEN + "Updated auto rank successfully. When " + args[0] + " joins, they will automatically receive the " + rank + " rank.");
		});
		
		return true;
	}

}
