package mc.dragons.tools.dev;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.PermissionUtil;

public class PluginManagementCommands implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player) sender;
			User user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true)) return true;
		}
		
		if(label.equalsIgnoreCase("ilikevanilla")) {
			Bukkit.getPluginManager().disablePlugins();
			sender.sendMessage(ChatColor.GREEN + "Disabled all loaded plugins");
			return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + label + " <plugin>");
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
			return true;
		}
		
		if(label.equalsIgnoreCase("disableplugin")) {
			Bukkit.getPluginManager().disablePlugin(plugin);
			sender.sendMessage(ChatColor.GREEN + "Disabled plugin " + plugin.getName());
			return true;
		}
		
		
		return true;
	}

}
