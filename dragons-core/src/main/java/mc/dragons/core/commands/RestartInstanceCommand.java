package mc.dragons.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class RestartInstanceCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/restartinstance <server>");
			return true;
		}
		
		instance.getRemoteAdminHandler().sendRemoteRestart(args[0]);
		sender.sendMessage(ChatColor.GREEN + "Remote restart sent successfully.");
		
		return true;
	}

}
