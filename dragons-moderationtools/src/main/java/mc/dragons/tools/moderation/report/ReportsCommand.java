package mc.dragons.tools.moderation.report;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.storage.mongo.pagination.PaginatedResult;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.report.ReportLoader.Report;
import mc.dragons.tools.moderation.report.ReportLoader.ReportStatus;
import mc.dragons.tools.moderation.report.ReportLoader.ReportType;

public class ReportsCommand implements CommandExecutor {
	
	private UserLoader userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	private ReportLoader reportLoader;
	
	public ReportsCommand(Dragons instance) {
		reportLoader = instance.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		User user = UserLoader.fromPlayer((Player) sender);
		if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.MODERATION, true)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/reports <all-open|all-closed|escalation|chat|internal|regular|by <player>|on <player>> [-page <#>]");
			return true;
		}
		
		int pageFlagIndex = StringUtil.getFlagIndex(args, "-page", 0);
		int page = pageFlagIndex == -1 ? 1 : Integer.valueOf(args[++pageFlagIndex]);
		
		PaginatedResult<Report> results = null;
		if(args[0].equalsIgnoreCase("all-open")) {
			results = reportLoader.getReportsByStatus(ReportStatus.OPEN, page);
		}
		else if(args[0].equalsIgnoreCase("all-closed")) {
			results = reportLoader.getReportsByStatus(ReportStatus.CLOSED, page);
		}
		else if(args[0].equalsIgnoreCase("escalation")) {
			results = reportLoader.getReportsByType(ReportType.STAFF_ESCALATION, page);
		}
		else if(args[0].equalsIgnoreCase("chat")) {
			results = reportLoader.getReportsByType(ReportType.CHAT, page);
		}
		else if(args[0].equalsIgnoreCase("internal")) {
			results = reportLoader.getReportsByType(ReportType.AUTOMATED, page);
		}
		else if(args[0].equalsIgnoreCase("regular")) {
			results = reportLoader.getReportsByType(ReportType.REGULAR, page);
		}
		else if(args[0].equalsIgnoreCase("by")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "/reports by <player>");
				return true;
			}
			User filter = userLoader.loadObject(args[1]);
			if(filter == null) {
				sender.sendMessage(ChatColor.RED + "Invalid player!");
				return true;
			}
			results = reportLoader.getReportsByFiler(filter, page);
		}
		else if(args[0].equalsIgnoreCase("on")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "/reports on <player>");
				return true;
			}
			User filter = userLoader.loadObject(args[1]);
			if(filter == null) {
				sender.sendMessage(ChatColor.RED + "Invalid player!");
				return true;
			}
			results = reportLoader.getReportsByTarget(filter, page);
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid usage!");
			return true;
		}
		
		if(results.getTotal() == 0) {
			sender.sendMessage(ChatColor.RED + "No results returned for this query!");
			return true;
		}
		
		sender.sendMessage(ChatColor.GREEN + "Page " + page + " of " + results.getPages() + " (" + results.getTotal() + " results)");
		for(Report report : results.getPage()) {
			sender.sendMessage(ChatColor.GRAY + "- #" + report.getId() + ": On " + report.getTarget().getName() + ", By " + report.getFiledBy().getName() + ", " 
					+ report.getType() + "/" + report.getStatus());
		}
		
		return true;
	}
}
