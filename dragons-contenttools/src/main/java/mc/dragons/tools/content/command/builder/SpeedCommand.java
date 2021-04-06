package mc.dragons.tools.content.command.builder;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class SpeedCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		if(!requirePermission(sender, PermissionLevel.BUILDER)) return true;
		
		User user = user(sender);
		Player player = player(sender);
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/speed <walk|fly> <value> OR /speed walk default");
			sender.sendMessage(ChatColor.YELLOW + "Your current walk speed is " + player.getWalkSpeed());
			sender.sendMessage(ChatColor.YELLOW + "Your current fly speed is " + player.getFlySpeed());
			return true;
		}
		

		boolean shortcut = !label.equalsIgnoreCase("speed");
		
		if(!shortcut && args.length == 1 || shortcut && args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /speed <walk|fly> <value>");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("walk") || label.equalsIgnoreCase("walkspeed") || label.equalsIgnoreCase("ws")) {
			if((shortcut ? args[0] : args[1]).equalsIgnoreCase("default")) {
				user.removeWalkSpeedOverride();
				sender.sendMessage(ChatColor.GREEN + "Removed walk speed override.");
				return true;
			}
			Float speed = parseFloatType(sender, shortcut ? args[0] : args[1]);
			if(speed == null) return true;
			user.overrideWalkSpeed(speed);
			sender.sendMessage(ChatColor.GREEN + "Set your walk speed to " + speed);
			return true;
		}
		
		if(args[0].equalsIgnoreCase("fly") || label.equalsIgnoreCase("flyspeed") || label.equalsIgnoreCase("fs")) {
			Float speed = parseFloatType(sender, shortcut ? args[0] : args[1]);
			if(speed == null) return true;
			player.setFlySpeed(speed);
			sender.sendMessage(ChatColor.GREEN + "Set your fly speed to " + speed);
			return true;
		}
		
		
		return true;
	}
}
