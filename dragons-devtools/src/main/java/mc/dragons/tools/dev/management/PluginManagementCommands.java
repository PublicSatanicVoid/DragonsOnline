package mc.dragons.tools.dev.management;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;

public class PluginManagementCommands extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.DEVELOPMENT)) return true;
		
		if(label.equalsIgnoreCase("ilikevanilla")) {
			Bukkit.getPluginManager().disablePlugins();
			sender.sendMessage(ChatColor.GREEN + "Disabled all loaded plugins");
			return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/" + label + " <plugin>");
			return true;
		}

		Plugin plugin = Bukkit.getPluginManager().getPlugin(args[0]);
		if(plugin == null) {
			sender.sendMessage(ChatColor.RED + "Invalid plugin!");
			return true;
		}
		
		if(label.equalsIgnoreCase("enableplugin")) {
			Bukkit.getPluginManager().enablePlugin(plugin);
			sender.sendMessage(ChatColor.GREEN + "Enabled plugin " + plugin.getName());
		}
		
		else if(label.equalsIgnoreCase("disableplugin")) {
			Bukkit.getPluginManager().disablePlugin(plugin);
			sender.sendMessage(ChatColor.GREEN + "Disabled plugin " + plugin.getName());
		}
		
		
		return true;
	}

}
