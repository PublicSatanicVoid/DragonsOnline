package mc.dragons.tools.moderation.report;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.report.ReportLoader.ReportStatus;
import mc.dragons.tools.moderation.report.ReportLoader.ReportType;

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
		
		else if(label.equalsIgnoreCase("bulkdeletereports")) {
			if(!requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/bulkdeletereports on <Player>");
				sender.sendMessage(ChatColor.RED + "/bulkdeletereports by <Player>");
				sender.sendMessage(ChatColor.RED + "/bulkdeletereports type <Type>");
				sender.sendMessage(ChatColor.RED + "/bulkdeletereports status <Status>");
			}
			else if(args[0].equalsIgnoreCase("on")) {
				User target = lookupUser(sender, args[1]);
				if(target == null) return true;
				long n = reportLoader.deleteReports("target", target.getUUID().toString(), true);
				sender.sendMessage(ChatColor.GREEN + "Deleted " + n + " reports against " + args[1]);
			}
			else if(args[0].equalsIgnoreCase("by")) {
				User target = lookupUser(sender, args[1]);
				if(target == null) return true;
				long n = reportLoader.deleteReports("filedBy", target.getUUID().toString(), true);
				sender.sendMessage(ChatColor.GREEN + "Deleted " + n + " reports by " + args[1]);
			}
			else if(args[0].equalsIgnoreCase("type")) {
				ReportType type = StringUtil.parseEnum(sender, ReportType.class, args[1]);
				if(type == null) return true;
				long n = reportLoader.deleteReports("type", type.toString(), false);
				sender.sendMessage(ChatColor.GREEN + "Deleted " + n + " reports of type " + type);
			}
			else if(args[0].equalsIgnoreCase("status")) {
				ReportStatus status = StringUtil.parseEnum(sender, ReportStatus.class, args[1]);
				if(status == null) return true;
				long n = reportLoader.deleteReports("status", status.toString(), false);
				sender.sendMessage(ChatColor.GREEN + "Deleted " + n + " reports of status " + status);
			}
			else {
				sender.sendMessage(ChatColor.RED + "Invalid parameters! /bulkdeletereports");
			}
		}
		
		else if(label.equalsIgnoreCase("skipallreports")) {
			if(!requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
			long n = reportLoader.skipReports(new Document("status", ReportStatus.OPEN.toString()));
			sender.sendMessage(ChatColor.GREEN + "Skipped " + n + " open reports. Moderation queue is cleared");
			dragons.getStaffAlertHandler().sendGenericMessage(PermissionLevel.HELPER, 
				ChatColor.RED + "The moderation queue was cleared by an administrator. All outstanding reports are marked as skipped.");
		}
		
		return true;
	}

}
