package mc.dragons.tools.moderation.report;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.storage.mongo.pagination.PaginatedResult;
import mc.dragons.core.util.StringUtil;
import mc.dragons.core.util.TableGenerator;
import mc.dragons.core.util.TableGenerator.Alignment;
import mc.dragons.tools.moderation.report.ReportLoader.Report;
import mc.dragons.tools.moderation.report.ReportLoader.ReportStatus;
import mc.dragons.tools.moderation.report.ReportLoader.ReportType;

public class ReportsCommand extends DragonsCommandExecutor {
	private ReportLoader reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);

	private static String COL_ID = ChatColor.DARK_AQUA + "ID";
	private static String COL_TARGET = ChatColor.DARK_AQUA + "User";
	private static String COL_PREVIEW = ChatColor.DARK_AQUA + "Preview";
	private static String COL_TYPE = ChatColor.DARK_AQUA + "Type";
	private static String COL_STATUS = ChatColor.DARK_AQUA + "Status";
	private static String COL_FILER = ChatColor.DARK_AQUA + "Filer";
	
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
			page = parseInt(sender, args[++pageFlagIndex]);
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
		
		TableGenerator tg = new TableGenerator(Alignment.LEFT, Alignment.LEFT, Alignment.LEFT, Alignment.LEFT, Alignment.LEFT, Alignment.LEFT);
		tg.addRow(COL_ID, COL_TARGET, COL_PREVIEW, COL_TYPE, COL_STATUS, COL_FILER);
		String idPrefix = ChatColor.AQUA + "";
		String dataPrefix = ChatColor.GRAY + "";
		
		for(Report report : results.getPage()) {
			String targets = report.getTargets().get(0).getName();
			if(report.getTargets().size() > 1) {
				targets += " +" + (report.getTargets().size() - 1);
			}
			tg.addRowEx("/viewreport " + report.getId(), ChatColor.GRAY + "Click to view report #" + report.getId(), 
					idPrefix + "#" + report.getId(), 
					dataPrefix + targets, 
					dataPrefix + StringUtil.truncateWithEllipsis(report.getPreview(), 30),
					dataPrefix + report.getType(), 
					dataPrefix + report.getStatus(), 
					dataPrefix + report.getFiledBy().getName());
		}

		sender.sendMessage(ChatColor.GREEN + "Page " + page + " of " + results.getPages() + " (" + results.getTotal() + " results)");
		tg.display(sender);
		
		return true;
	}
}
