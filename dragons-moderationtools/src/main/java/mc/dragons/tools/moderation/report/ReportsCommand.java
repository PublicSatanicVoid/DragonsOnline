package mc.dragons.tools.moderation.report;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.storage.mongo.pagination.PaginatedResult;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.report.ReportLoader.Report;
import mc.dragons.tools.moderation.report.ReportLoader.ReportStatus;
import mc.dragons.tools.moderation.report.ReportLoader.ReportType;

public class ReportsCommand extends DragonsCommandExecutor {
	
	private ReportLoader reportLoader = instance.getLightweightLoaderRegistry().getLoader(ReportLoader.class);

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/reports <all|all-open|escalation|chat|internal|regular|by <player>|on <player>> [-page <#>]");
			return true;
		}
		
		int pageFlagIndex = StringUtil.getFlagIndex(args, "-page", 0);
		Integer page = 1;
		if(pageFlagIndex != -1) {
			page = parseIntType(sender, args[++pageFlagIndex]);
			if(page == null) return true;
		}
		
		PaginatedResult<Report> results = null;
		if(args[0].equalsIgnoreCase("all-open")) {
			results = reportLoader.getReportsByStatus(ReportStatus.OPEN, page);
		}
		else if(args[0].equalsIgnoreCase("all")) {
			results = reportLoader.getAllReports(page);
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
			User filter = lookupUser(sender, args[1]);
			if(filter == null) return true;
			results = reportLoader.getReportsByFiler(filter, page);
		}
		else if(args[0].equalsIgnoreCase("on")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "/reports on <player>");
				return true;
			}
			User filter = lookupUser(sender, args[1]);
			if(filter == null) return true;
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
