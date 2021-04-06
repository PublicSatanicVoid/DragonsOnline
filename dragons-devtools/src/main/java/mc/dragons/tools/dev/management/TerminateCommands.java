package mc.dragons.tools.dev.management;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;

public class TerminateCommands extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.DEVELOPMENT)) return true;
		
		if(label.equalsIgnoreCase("killtask")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/killtask <TaskID>");
				return true;
			}
			Integer id = parseIntType(sender, args[0]);
			if(id == null) return true;
			Bukkit.getScheduler().cancelTask(id);
			sender.sendMessage(ChatColor.GREEN + "Killed task #" + id);
		}
		
		else if(label.equalsIgnoreCase("killtasks")) {
			Bukkit.getScheduler().cancelAllTasks();
			sender.sendMessage(ChatColor.GREEN + "Killed all tasks.");
		}
		
		else if(label.equalsIgnoreCase("killtasksfor")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/killtasksfor <Plugin>");
				return true;
			}
			Plugin plugin = Bukkit.getPluginManager().getPlugin(args[0]);
			Bukkit.getScheduler().cancelTasks(plugin);
		}
		
		// Appropriate when the server needs to be shut down immediately without saving.
		else if(label.equalsIgnoreCase("crashserver")) {
			System.exit(-1);
		}
		
		// Appropriate when the server needs to be shut down gracefully but is in an error state.
		// If normal mechanisms fail to stop the server, the runtime will be forcibly shut down.
		// DATA MAY BE SAVED IF INVOKED. IF SOMETHING IS CORRUPTED, THIS SHOULD NOT BE USED.
		else if(label.equalsIgnoreCase("panic")) {
			Bukkit.getLogger().setLevel(Level.SEVERE);
			Bukkit.getLogger().severe(" ");
			Bukkit.getLogger().severe("=== SERVER PANIC  ===");
			Bukkit.getLogger().severe("=== Something went horribly wrong! ===");
			Bukkit.getLogger().severe("=== Dump follows: ===");
			Bukkit.getLogger().setLevel(Level.INFO);
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "getprocessid");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "serverperformance");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "worldperformance");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lag");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "generatedump");
			Bukkit.getLogger().setLevel(Level.SEVERE);
			Bukkit.getLogger().severe("=== SHUTTING DOWN ===");
			Bukkit.getScheduler().cancelAllTasks();
			Bukkit.getPluginManager().disablePlugins();
			Bukkit.getLogger().severe(" ");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop");
			System.exit(-1);
		}
		
		return true;
	}

}
