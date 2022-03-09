package mc.dragons.tools.moderation.abilities;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.tools.moderation.DragonsModerationTools;

public class SlowModeCommand extends DragonsCommandExecutor {

	private DragonsModerationTools plugin;
	
	public SlowModeCommand(DragonsModerationTools plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/slowmode <seconds|0>");
			return true;
		}
		
		Integer secs = parseInt(sender, args[0]);
		if(secs == null) return true;
		
		plugin.setSlowMode(secs);
		
		if(secs > 0) {
			Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "SLOW MODE ENABLED: " + ChatColor.RED + "Unranked players must wait " + secs + "s between messages!");
		}
		else {
			Bukkit.broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "SLOW MODE DISABLED");
		}
		
		return true;
	}

}
