package mc.dragons.tools.dev.monitor;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class LogLevelCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /loglevel <OFF|SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST|ALL> [Plugin]");
			sender.sendMessage(ChatColor.RED + "Global log level is currently " + Dragons.getInstance().getServerOptions().getLogLevel());
			return true;
		}

		Level level = this.lookup(sender, () -> Level.parse(args[0].toUpperCase()), ChatColor.RED + "Invalid log level! /loglevel <OFF|SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST|ALL>");
		if(level == null) return true;
		
		if(args.length == 1) {
			Dragons.getInstance().getServerOptions().setLogLevel(level);
			sender.sendMessage(ChatColor.GREEN + "Set log level to " + level.toString());
		}
		else {
			Plugin plugin = Bukkit.getPluginManager().getPlugin(args[1]);
			if(plugin == null) {
				sender.sendMessage(ChatColor.RED + "Invalid plugin name! /debug level <OFF|SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST|ALL> [Plugin]");
				return true;
			}
			plugin.getLogger().setLevel(level);
			sender.sendMessage(ChatColor.GREEN + "Set log level of plugin " + plugin.getName() + " to " + level.toString());
		}
		
		return true;
	}
}
