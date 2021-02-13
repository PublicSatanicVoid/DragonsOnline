package mc.dragons.core.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;

public class HealCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.MODERATOR, true)) {
				return true;
			}
		}
		
		if(args.length == 0) {
			if(player == null) {
				sender.sendMessage(ChatColor.RED + "Specify a player to heal. /heal <player>");
				return true;
			}
			player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
			sender.sendMessage(ChatColor.GREEN + "Healed yourself successfully.");
			return true;
		}
		
		Player target = Bukkit.getPlayer(args[0]);
		if(target == null) {
			sender.sendMessage(ChatColor.RED + "That player is not online! /heal <player>");
			return true;
		}
		
		target.setHealth(target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		target.sendMessage(ChatColor.GREEN + "You have been healed.");
		if(target != sender) {
			sender.sendMessage(ChatColor.GREEN + "Healed " + target.getName() + " successfully.");
		}
		
		return true;
	}
	
}
