package mc.dragons.tools.content;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;

public class SpeedCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command!");
			return true;
		}
		
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		
		if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.BUILDER, true)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /speed <walk|fly> <value> OR /speed walk default");
			sender.sendMessage(ChatColor.RED + "Your current walk speed is " + player.getWalkSpeed());
			sender.sendMessage(ChatColor.RED + "Your current fly speed is " + player.getFlySpeed());
			return true;
		}
		

		if(args.length == 1) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /speed <walk|fly> <value>");
			return true;
		}
		
		
		if(args[0].equalsIgnoreCase("walk")) {
			if(args[1].equalsIgnoreCase("default")) {
				user.removeWalkSpeedOverride();
				sender.sendMessage(ChatColor.GREEN + "Removed walk speed override.");
				return true;
			}
			float speed = Float.valueOf(args[1]);
			user.overrideWalkSpeed(speed);
			sender.sendMessage(ChatColor.GREEN + "Set your walk speed to " + speed);
			return true;
		}
		
		if(args[0].equalsIgnoreCase("fly")) {
			float speed = Float.valueOf(args[1]);
			player.setFlySpeed(speed);
			sender.sendMessage(ChatColor.GREEN + "Set your fly speed to " + speed);
			return true;
		}
		
		
		return true;
	}

}
