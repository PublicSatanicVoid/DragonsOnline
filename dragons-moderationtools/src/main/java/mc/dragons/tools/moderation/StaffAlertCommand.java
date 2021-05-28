package mc.dragons.tools.moderation;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;

public class StaffAlertCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.MODERATOR)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/staffalert <alert message>");
			return true;
		}
		
		dragons.getStaffAlertHandler().sendGenericMessage(PermissionLevel.HELPER, "[" + sender.getName() + "] " + StringUtil.concatArgs(args, 0));
		
		return true;
	}

}
