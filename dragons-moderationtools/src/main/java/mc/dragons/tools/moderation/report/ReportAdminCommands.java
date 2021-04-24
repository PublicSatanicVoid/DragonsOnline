package mc.dragons.tools.moderation.report;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;

public class ReportAdminCommands extends DragonsCommandExecutor {

	private ReportLoader reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return true;
		
		if(label.equalsIgnoreCase("toggleselfreport")) {
			user(sender).getLocalData().append("canSelfReport", !user(sender).getLocalData().getBoolean("canSelfReport", false));
			sender.sendMessage(ChatColor.GREEN + "Toggled self-reporting ability.");
		}
		
		else if(label.equalsIgnoreCase("deletereport")) {
			if(!requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/deletereport <ID>");
				return true;
			}
			Integer id = parseInt(sender, args[0]);
			if(id == null) return true;
			boolean success = reportLoader.deleteReport(id);
			if(success) {
				sender.sendMessage(ChatColor.GREEN + "Deleted report #" + id + " successfully.");
			}
			else {
				sender.sendMessage(ChatColor.RED + "Could not delete report #" + id);
			}
		}
		
		return true;
	}

}
