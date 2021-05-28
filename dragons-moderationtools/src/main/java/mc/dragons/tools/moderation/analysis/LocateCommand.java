package mc.dragons.tools.moderation.analysis;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class LocateCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.HELPER)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/locate <player>");
			return true;
		}
		
		long start = System.currentTimeMillis();
		User target = lookupUser(sender, args[0]);
		if(target == null) return true;
		long end = System.currentTimeMillis();
		
		if(target.getServerName() == null) {
			sender.sendMessage(ChatColor.RED + "That player is not currently connected to any server!");
		}
		else {
			sender.sendMessage(ChatColor.GREEN + target.getName() + " is connected to " + target.getServerName() + " (took " + (end - start) + "ms)");
		}
		
		return true;
	}
}
