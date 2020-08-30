package mc.dragons.tools.moderation;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.storage.impl.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class EscalateCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		Player player = null; 
		User user = null;
		
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			//if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.MOD, true)) return true;
			if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.HELPER, true)) return true;
		}
		
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Specify a player and reason! /escalate <player> <reason>");
			return true;
		}
		
		Player target = Bukkit.getPlayerExact(args[0]);
		if(target == null) {
			sender.sendMessage(ChatColor.RED + "That player is not online!");
			return true;
		}
		
		String reason = StringUtil.concatArgs(args, 1);
		
		// TODO network-wide or something???
		for(User test : UserLoader.allUsers()) {
			if(PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.MODERATION, false)) {
				test.getPlayer().sendMessage(ChatColor.GOLD + user.getName() + ChatColor.YELLOW + " escalated an issue with "
					+ ChatColor.GOLD + target.getName() + ChatColor.YELLOW + " (" + reason + ")");
			}
		}
		
		sender.sendMessage(ChatColor.GREEN + "Escalated issue successfully.");
		
		return true;
	}

}
