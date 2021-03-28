package mc.dragons.tools.dev.monitor;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;

public class PingCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args.length == 0 && !isPlayer(sender)) {
			sender.sendMessage(ChatColor.RED + "Console must specify a player! /ping <player>");
			return true;
		}
		
		Player target = player(sender);
		if(args.length > 0) {
			target = lookupPlayer(sender, args[0]);
		}
		
		int ping = instance.getBridge().getPing(target);
		sender.sendMessage(ChatColor.GREEN + "Ping of player " + target.getName() + " is " + ping + "ms");
		
		return true;
	}

}
