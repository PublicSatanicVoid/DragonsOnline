package mc.dragons.social;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;

public class ShoutCommand extends DragonsCommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.MODERATOR)) return true;
		
		if (args.length == 0) {
			if (!label.equalsIgnoreCase("shout"))
				sender.sendMessage(ChatColor.RED + "Alias for /shout.");
			sender.sendMessage(ChatColor.RED + "/shout <message>");
			return true;
		}
		
		String message = StringUtil.concatArgs(args, 0);
		Bukkit.broadcastMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + sender.getName() + " " + ChatColor.AQUA + message);
		return true;
	}
}
