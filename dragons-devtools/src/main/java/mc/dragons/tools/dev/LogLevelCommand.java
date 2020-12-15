package mc.dragons.tools.dev;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;

public class LogLevelCommand implements CommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		Player player = null; 
		User user = null;
		
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true)) return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /loglevel <OFF|SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST|ALL> [Plugin]");
			sender.sendMessage(ChatColor.RED + "Global log level is currently " + Dragons.getInstance().getServerOptions().getLogLevel());
			return true;
		}

		Level level = null;
		try {
			level = Level.parse(args[0].toUpperCase());
		}
		catch(Exception e) {
			sender.sendMessage(ChatColor.RED + "Invalid log level! /debug level <OFF|SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST|ALL> [Plugin]");
			return true;
		}
		
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
