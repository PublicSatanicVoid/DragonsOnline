package mc.dragons.tools.dev;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;

public class PingCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args.length == 0 && !(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Console must specify a player! /ping <player>");
			return true;
		}
		
		Player target = null;
		if(args.length == 0) {
			target = (Player) sender;
		}
		else {
			target = Bukkit.getPlayerExact(args[0]);
		}
		
		if(target == null) {
			sender.sendMessage(ChatColor.RED + "Invalid player! /ping <player>");
		}
		
		int ping = Dragons.getInstance().getBridge().getPing(target);
		
		sender.sendMessage(ChatColor.GREEN + "Ping of player " + target.getName() + " is " + ping + "ms");
		
		return true;
	}

}
