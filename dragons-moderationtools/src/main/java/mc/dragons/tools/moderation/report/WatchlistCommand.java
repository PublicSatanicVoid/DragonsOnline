package mc.dragons.tools.moderation.report;

import java.util.ArrayList;
import java.util.List;

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

public class WatchlistCommand extends DragonsCommandExecutor {
	private ReportLoader reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/watchlist list [page#] [--offline]");
			sender.sendMessage(ChatColor.RED + "/watchlist add <player> [reason]");
			sender.sendMessage(ChatColor.RED + "/watchlist remove <player>");
			return true;
		}
		
		else if(args[0].equalsIgnoreCase("list")) {
			List<ReportStatus> filter = new ArrayList<>();
			filter.add(ReportStatus.OPEN);
			if(StringUtil.getFlagIndex(args, "--offline", 0) != -1) {
				filter.add(ReportStatus.SUSPENDED);
			}
			Integer page = 1;
			if(args.length > 1) {
				page = parseInt(sender, args[1]);
				if(page == null) return true;
			}
			PaginatedResult<Report> reports = reportLoader.getReportsByTypeAndStatus(ReportType.WATCHLIST, filter, page);
			sender.sendMessage(ChatColor.DARK_GREEN + "" + reports.getTotal() + " results returned (Page " + reports.getPageIndex() + " of " + reports.getPages() + ")");
			for(Report report : reports.getPage()) {
				User target = report.getTargets().get(0); // Well-defined because watchlist reports only ever have one target
				target.safeResyncData();
				boolean online = target.getServer() != null;
				String username = target.getName();
				sender.spigot().sendMessage(
						StringUtil.clickableHoverableText(ChatColor.RED + "[-] ", "/watchlist remove " + report.getTargets().get(0).getName(), "Click to remove " + username + " from the watchlist"),
						StringUtil.clickableHoverableText(ChatColor.YELLOW + "[i] ", "/viewreport " + report.getId(), "Click to view " + username + "'s watchlist info"),
						StringUtil.plainText(ChatColor.GRAY + "#" + report.getId() + ChatColor.RESET + " " + username + ChatColor.GRAY + " (" + (online ? "ONLINE" : "OFFLINE") + ")"));
			}
		}
		
		else if(args[0].equalsIgnoreCase("add")) {
			if(args.length <= 1) {
				sender.sendMessage(ChatColor.RED + "/watchlist add <player> [reason]");
				return true;
			}
			User target = lookupUser(sender, args[1]);
			if(target == null) return true;
			reportLoader.fileWatchlistReport(target, user(sender), StringUtil.concatArgs(args, 2));
			sender.sendMessage(ChatColor.GREEN + "Added " + target.getName() + " to the watchlist.");
		}
		
		else if(args[0].equalsIgnoreCase("remove")) {
			if(args.length <= 1) {
				sender.sendMessage(ChatColor.RED + "/watchlist remove <player>");
				return true;
			}
			User target = lookupUser(sender, args[1]);
			if(target == null) return true;
			PaginatedResult<Report> reports = reportLoader.getReportsByTypeStatusAndTarget(ReportType.WATCHLIST, ReportStatus.OPEN, target, 1);
			if(reports.getTotal() == 0) {
				sender.sendMessage(ChatColor.RED + "That player is not on the watchlist!");
			}
			else {
				reports.getPage().get(0).setStatus(ReportStatus.NO_ACTION);
				sender.sendMessage(ChatColor.GREEN + "Removed " + target.getName() + " from the watchlist.");
			}
		}
		
		else {
			sender.sendMessage(ChatColor.RED + "/watchlist");
		}
		
		
		return true;
	}
	
}
