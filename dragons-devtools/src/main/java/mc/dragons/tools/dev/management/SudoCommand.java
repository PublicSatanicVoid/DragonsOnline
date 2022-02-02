package mc.dragons.tools.dev.management;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;

public class SudoCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.SYSOP)) return true;
		
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "/sudo <player> <command>");
			return true;
		}
		User target = lookupUser(sender, args[0]);
		if(target == null) return true;
		String cmd = StringUtil.concatArgs(args, 1);
		dragons.getRemoteAdminHandler().sendRemoteSudo(target.getServerName(), target.getUUID(), cmd);
		sender.sendMessage("Dispatched sudo command to " + target.getServerName());
		
		return true;
	}

}
