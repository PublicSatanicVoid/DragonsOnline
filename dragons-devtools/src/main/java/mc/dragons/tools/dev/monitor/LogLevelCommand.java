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
import mc.dragons.core.logging.LogLevel;
import mc.dragons.core.util.StringUtil;

public class LogLevelCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /loglevel <" + StringUtil.parseList(LogLevel.getApprovedLevels(), "|") + "> [Plugin]");
			sender.sendMessage(ChatColor.RED + "Global log level is currently " + Dragons.getInstance().getServerOptions().getLogLevel());
			return true;
		}

		Level level = this.lookup(sender, () -> Level.parse(args[0].toUpperCase()), ChatColor.RED + "Invalid log level! /loglevel <" + StringUtil.parseList(LogLevel.getApprovedLevels(), "|") + ">");
		if(level == null) return true;
		
		if(args.length == 1) {
			Dragons.getInstance().getServerOptions().setLogLevel(level);
			sender.sendMessage(ChatColor.GREEN + "Set log level to " + level.toString());
		}
		else {
			Plugin plugin = Bukkit.getPluginManager().getPlugin(args[1]);
			if(plugin == null) {
				sender.sendMessage(ChatColor.RED + "Invalid plugin name! /debug level <"  + StringUtil.parseList(LogLevel.getApprovedLevels(), "|") + ">" + " [Plugin]");
				return true;
			}
			plugin.getLogger().setLevel(level);
			sender.sendMessage(ChatColor.GREEN + "Set log level of plugin " + plugin.getName() + " to " + level.toString());
		}
		
		return true;
	}
}
