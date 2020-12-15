package mc.dragons.tools.dev;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;

public class TerminateCommands implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player) sender;
			User user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true)) return true;
		}
		
		if(label.equalsIgnoreCase("killtask")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/killtask <TaskID>");
				return true;
			}
			int id = Integer.valueOf(args[0]);
			Bukkit.getScheduler().cancelTask(id);
			sender.sendMessage(ChatColor.GREEN + "Killed task #" + id);
		}
		
		if(label.equalsIgnoreCase("killtasks")) {
			Bukkit.getScheduler().cancelAllTasks();
			sender.sendMessage(ChatColor.GREEN + "Killed all tasks.");
		}
		
		if(label.equalsIgnoreCase("killtasksfor")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/killtasksfor <Plugin>");
				return true;
			}
			Plugin plugin = Bukkit.getPluginManager().getPlugin(args[0]);
			Bukkit.getScheduler().cancelTasks(plugin);
		}
		
		if(label.equalsIgnoreCase("crashserver")) {
			System.exit(-1);
		}
		
		
		return true;
	}

}
