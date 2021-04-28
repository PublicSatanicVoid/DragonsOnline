package mc.dragons.core.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import mc.dragons.core.DragonsJavaPlugin;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class DragonsCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return true;
		
		List<DragonsJavaPlugin> plugins = DragonsJavaPlugin.getDragonsPlugins();
		sender.sendMessage(ChatColor.DARK_GREEN + "DragonsOnline status data:");
		sender.sendMessage(ChatColor.GRAY + "Server Version: " + ChatColor.GREEN + Bukkit.getServer().getVersion());
		sender.sendMessage(ChatColor.GRAY + "Installed Dragons plugins: " + ChatColor.GREEN + plugins.size());
		for(Plugin plugin : plugins) {
			PluginDescriptionFile desc = plugin.getDescription();
			sender.sendMessage(ChatColor.GRAY + "- " + (plugin.isEnabled() ? ChatColor.GREEN : ChatColor.RED) + plugin.getName() + ChatColor.GRAY + " - v" + desc.getVersion() 
				+ " - " + ChatColor.ITALIC + desc.getMain());
			if(desc.getDescription() != null) {
				sender.sendMessage(ChatColor.GRAY + "   " + desc.getDescription());
			}
		}
		
		return true;
	}

}
