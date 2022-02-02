package mc.dragons.tools.moderation.report;

import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.StateLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.storage.mongo.pagination.PaginatedResult;
import mc.dragons.core.util.StringUtil;
import mc.dragons.core.util.TableGenerator;
import mc.dragons.core.util.TableGenerator.Alignment;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.WrappedUser.AppliedPunishmentData;
import mc.dragons.tools.moderation.hold.HoldLoader;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldEntry;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldStatus;
import mc.dragons.tools.moderation.punishment.PunishmentCode;
import mc.dragons.tools.moderation.report.ReportLoader.Report;
import mc.dragons.tools.moderation.report.ReportLoader.ReportStatus;
import mc.dragons.tools.moderation.report.ReportLoader.ReportType;
import net.md_5.bungee.api.chat.TextComponent;

public class ViewReportCommand extends DragonsCommandExecutor {
	private ReportLoader reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	private HoldLoader holdLoader = dragons.getLightweightLoaderRegistry().getLoader(HoldLoader.class);
	private StateLoader stateLoader = dragons.getLightweightLoaderRegistry().getLoader(StateLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.HELPER)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/viewreport <ID>");
			return true;
		}
		
		Integer id = parseInt(sender, args[0]);
		if(id == null) return true;
		
		Report report = reportLoader.getReportById(id);
		if(report == null) {
			sender.sendMessage(ChatColor.RED + "No report with ID " + args[0] + " was found!");
			return true;
		}			
		
		PermissionLevel req = report.getType() == ReportType.CHAT ? PermissionLevel.HELPER : PermissionLevel.MODERATOR;
		if(report.getData().containsKey("permissionReq")) {
			req = PermissionLevel.valueOf(report.getData().getString("permissionReq"));
		}
		boolean canEdit = hasPermission(sender, req) && (hasPermission(sender, PermissionLevel.ADMIN) || !report.getTargets().contains(user(sender)));
		boolean closed = report.getStatus() != ReportStatus.OPEN;
		
		// Helpers can only view open chat reports, or chat reports that they've reviewed
		// Moderators can only view open reports, or reports that they've reviewed
		// Appeals team and admins can view all reports
		boolean canView = hasPermission(sender, SystemProfileFlag.APPEALS_TEAM) || hasPermission(sender, PermissionLevel.ADMIN)
				|| hasPermission(sender, SystemProfileFlag.MODERATION) && (!closed || report.getReviewedBy().equals(user(sender)))
				|| hasPermission(sender, SystemProfileFlag.HELPER)&& report.getType() == ReportType.CHAT && (!closed || report.getReviewedBy().equals(user(sender)));
		
		TextComponent nextReport = StringUtil.clickableHoverableText(ChatColor.GRAY + " [Next Report]", "/modqueue", "Click to handle next report in queue");
		
		if(!canView) {
			sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to view this report! (#" + report.getId() + ")");
			sender.spigot().sendMessage(nextReport);
			return true;
		}
		
		if(args.length == 1) {
			sender.sendMessage(ChatColor.DARK_GREEN + "Report #" + report.getId() + ": " + report.getType() + "/" + report.getStatus());

			TextComponent snapshots = null;
			if(report.getData().containsKey("states") && report.getData().getList("states", String.class).size() > 0) {
				snapshots = StringUtil.clickableHoverableText(ChatColor.AQUA + "[User Snapshots]", "/viewreport " + id + " snapshots", "Click to view snapshots of the reported player(s)");
			}
			
			if(canEdit) {
				if(closed) {
					sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.YELLOW + "[Re-Open] ", "/viewreport " + id + " status OPEN", "Re-open this report for further action or review"),
							snapshots == null ? StringUtil.plainText("") : snapshots);
				}
				else {
					TextComponent confirm = StringUtil.clickableHoverableText(ChatColor.GREEN + "[Confirm] ", "/viewreport " + id + " confirm", "Confirm report and apply punishment(s)", "Closes the report");
					TextComponent insufficient = StringUtil.clickableHoverableText(ChatColor.YELLOW + "[Insufficient] ", "/viewreport " + id + " insufficient", "Insufficient evidence to confirm report", "Closes the report");
					TextComponent watchlist = report.getType() == ReportType.WATCHLIST ? StringUtil.plainText("") : 
						StringUtil.clickableHoverableText(ChatColor.YELLOW + "[Watchlist] ", "/viewreport " + id + " watch", "Insufficient evidence to confirm report, but high suspicion", "Closes the report and places the user on a watchlist for further monitoring");
					TextComponent escalate = StringUtil.clickableHoverableText(ChatColor.GOLD + "[Escalate] ", "/viewreport " + id + " escalate", "Escalate report for review by a senior staff member", "Does not close the report");
					TextComponent duplicate = StringUtil.clickableHoverableText(ChatColor.GRAY + "[Duplicated] ", "/viewreport " + id + " duplicate", "This report is a duplicate of another report", "Closes the report");
					TextComponent cancel = report.getStatus() != ReportStatus.OPEN || !report.getFiledBy().contains(user(sender)) ? StringUtil.plainText("") :
						StringUtil.clickableHoverableText(ChatColor.GRAY + "[Cancel] ", "/viewreport " + id + " cancel", "Cancel this report", "Closes the report");
					TextComponent skip = StringUtil.clickableHoverableText(ChatColor.GRAY + "[Skip] ", "/viewreport " + id + " skip", "Skip this report and go to the next one in the queue", "Does not close the report");
					TextComponent addNote = StringUtil.clickableHoverableText(ChatColor.WHITE + "" + ChatColor.ITALIC + "  [+Add Note]", "/viewreport " + id + " note ", true, "Add a note to this report", "Does not close the report");
					sender.spigot().sendMessage(confirm, insufficient, watchlist, escalate, duplicate, cancel, skip, addNote);
					if(snapshots != null) {
						sender.spigot().sendMessage(snapshots);
					}
				}
			}
			else {
				sender.sendMessage(ChatColor.GRAY + "- You do not have sufficient permission to modify this report -");					
				if(snapshots != null) {
					sender.spigot().sendMessage(snapshots);
				}
			}
			sender.sendMessage(ChatColor.GRAY + "Filed Against: " + ChatColor.RESET + StringUtil.parseList(report.getTargets().stream().map(u -> u.getName()).collect(Collectors.toList())));
			if(report.getFiledBy() != null) {
				sender.sendMessage(ChatColor.GRAY + "Filed By: " + ChatColor.RESET + StringUtil.parseList(report.getFiledBy().stream().map(u -> u.getName()).collect(Collectors.toList())));
			}
			sender.sendMessage(ChatColor.GRAY + "Filing Date: " + ChatColor.RESET + StringUtil.DATE_FORMAT.format(report.getFiledOn()));
			if(!hasPermission(sender, SystemProfileFlag.DEVELOPMENT)) {
				if(report.getData().containsKey("message")) {
					sender.sendMessage(ChatColor.GRAY + "Message: " + ChatColor.RESET + report.getData().getString("message"));
				}
				else if(report.getData().containsKey("reason")) {
					sender.sendMessage(ChatColor.GRAY + "Reason: " + ChatColor.RESET + report.getData().getString("reason"));
				}
			}
			if(report.getReviewedBy() != null) {
				sender.sendMessage(ChatColor.GRAY + "Primary Reviewer: " + ChatColor.RESET + report.getReviewedBy().getName());
			}
			else {
				sender.sendMessage(ChatColor.GRAY + "Unreviewed.");
			}
			if(report.getData().size() > 0 && hasPermission(sender, SystemProfileFlag.DEVELOPMENT)) {
				sender.sendMessage(ChatColor.GRAY + "Internal Data: ");
				for(Entry<String, Object> entry : report.getData().entrySet()) {
					sender.sendMessage(ChatColor.GRAY + "- " + entry.getKey() + ChatColor.GRAY + ": " + ChatColor.RESET + entry.getValue());
				}
			}
			if(report.getNotes().size() > 0) {
				sender.sendMessage(ChatColor.GRAY + "Notes:");
				for(String note : report.getNotes()) {
					sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.RESET + note);
				}
			}
			PaginatedResult<Report> linked = reportLoader.getRecentReportsByTargets(report.getTargets(), report.getFiledOn().toInstant().getEpochSecond(), report.getId(), 1);
			if(linked.getTotal() > 0) {
				sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.YELLOW + "[Possible Duplicates]", "/viewreport " + report.getId() + " linked", "Click to view possible duplicate reports"));
			}
			return true;
		}
		else if(args[1].equalsIgnoreCase("linked")) {
			Integer page = 1;
			if(args.length > 2) {
				page = parseInt(sender, args[2]);
				if(page == null) return true;
			}
			PaginatedResult<Report> linked = reportLoader.getRecentReportsByTargets(report.getTargets(), report.getFiledOn().toInstant().getEpochSecond(), report.getId(), 1);
			TableGenerator tg = new TableGenerator(Alignment.LEFT, Alignment.LEFT, Alignment.LEFT, Alignment.LEFT, Alignment.LEFT, Alignment.LEFT);
			tg.addRow(ChatColor.YELLOW + "ID", ChatColor.YELLOW + "Status", ChatColor.YELLOW + "Targets");
			for(Report r : linked.getPage()) {
				String targets = r.getTargets().get(0).getName();
				if(r.getTargets().size() > 1) {
					targets += " +" + (r.getTargets().size() - 1);
				}
				tg.addRowEx("/viewreport " + r.getId(), "Click to view report #" + r.getId(), ChatColor.GRAY + "" + r.getId(), ChatColor.RESET + r.getStatus().toString(), targets);
			}
			tg.display(sender);
		}
		else if(args[1].equalsIgnoreCase("duplicate")) {
			if(!canEdit) {
				sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to edit this report");
				return true;
			}
			if(closed) {
				sender.sendMessage(ChatColor.RED + "This report is closed!");
				return true;
			}
			report.addNote("Marked as a duplicate by " + sender.getName());
			report.setStatus(ReportStatus.NO_ACTION);
			if(report.getData().containsKey("holdId")) {
				holdLoader.getHoldById(report.getData().getInteger("holdId")).setStatus(HoldStatus.CLOSED_ACTION);
			}
			report.setReviewedBy(user(sender));
			sender.sendMessage(ChatColor.GREEN + "Marked this report as a duplicate.");
			sender.spigot().sendMessage(nextReport);
		}
		else if(args[1].equalsIgnoreCase("cancel")) {
			if(!canEdit) {
				sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to edit this report");
				return true;
			}
			if(closed) {
				sender.sendMessage(ChatColor.RED + "This report is closed!");
				return true;
			}
			report.addNote("Cancelled by " + sender.getName());
			report.setStatus(ReportStatus.NO_ACTION);
			if(report.getData().containsKey("holdId")) {
				holdLoader.getHoldById(report.getData().getInteger("holdId")).setStatus(HoldStatus.CLOSED_ACTION);
			}
			report.setReviewedBy(user(sender));
			sender.sendMessage(ChatColor.GREEN + "Cancelled this report.");
			sender.spigot().sendMessage(nextReport);
		}
		else if(args[1].equalsIgnoreCase("skip")) {
			if(!canEdit) {
				sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to edit this report");
				return true;
			}
			if(closed) {
				sender.sendMessage(ChatColor.RED + "This report is closed!");
				return true;
			}
			report.addSkippedBy(user(sender));
			sender.sendMessage(ChatColor.GREEN + "Skipped this report.");
			Bukkit.dispatchCommand(sender, "modqueue");
		}
		else if(args[1].equalsIgnoreCase("snapshots")) {
			sender.sendMessage(ChatColor.GRAY + "User Snapshots:");
			for(String token : report.getData().getList("states", String.class)) {
				UUID uuid = UUID.fromString(token);
				Document data = stateLoader.getState(uuid);
				if(data == null) {
					sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.RED + "Could not load snapshot " + uuid);
					continue;
				}
				sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + "- " + ChatColor.RESET + userLoader.loadObject(UUID.fromString(data.getString("originalUser"))).getName() 
						+ ChatColor.GRAY + " (" + data.getString("originalTime") + ")",
					"/setstate " + uuid, true, "Click to go to this snapshot"));
			}
		}
		else if(args[1].equalsIgnoreCase("confirm")) {
			if(!canEdit) {
				sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to edit this report");
				return true;
			}
			if(closed && !report.getReviewedBy().equals(user(sender))) {
				sender.sendMessage(ChatColor.RED + "This report is closed!");
				return true;
			}
			if(!closed && report.getData().containsKey("confirmCommand") && !report.getData().getString("confirmCommand").isEmpty()) {
				report.setStatus(ReportStatus.ACTION_TAKEN);
				if(report.getData().containsKey("holdId")) {
					holdLoader.getHoldById(report.getData().getInteger("holdId")).setStatus(HoldStatus.CLOSED_ACTION);
				}
				player(sender).performCommand(report.getData().getString("confirmCommand"));
				sender.sendMessage(ChatColor.GREEN + "Report closed successfully.");
				sender.spigot().sendMessage(nextReport);
			}
			else {
				sender.sendMessage(ChatColor.DARK_GREEN + "Select a punishment code to apply:");
				for(PunishmentCode code : PunishmentCode.values()) {
					if(code.isHidden()) continue;
					sender.spigot().sendMessage(StringUtil.clickableHoverableText(" " + code.getCode() + ChatColor.GRAY + " - " + code.getName(), "/viewreport " + id + " apply " + code.getCode() + " ", true,
						new String[] {
							ChatColor.YELLOW + "" + ChatColor.BOLD + code.getName(),
							ChatColor.GRAY + code.getDescription(),
							"",
							ChatColor.DARK_GRAY + "Level " + code.getStandingLevel() + " - " + code.getType()
						}));
				}
			}
		}
		else if(args[1].equalsIgnoreCase("escalate")) {
			if(!canEdit) {
				sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to edit this report");
				return true;
			}
			if(closed) {
				sender.sendMessage(ChatColor.RED + "This report is closed!");
				return true;
			}
			report.addNote("Escalated for further action (by " + sender.getName() + ")");

			PermissionLevel permissionReq = null;
			for(PermissionLevel level : PermissionLevel.values()) {
				if(level.ordinal() == user(sender).getActivePermissionLevel().ordinal() + 1) {
					permissionReq = level;
					break;
				}
			}
			
			if(permissionReq == null) {
				sender.sendMessage(ChatColor.RED + "You don't have anyone to escalate this report to!");
				return true;
			}
			
			report.getData().append("permissionReq", permissionReq.toString());
			sender.sendMessage(ChatColor.GREEN + "Escalated this report for further action by a senior staff member.");
			sender.spigot().sendMessage(nextReport);
		}
		else if(args[1].equalsIgnoreCase("insufficient")) {
			if(!canEdit) {
				sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to edit this report");
				return true;
			}
			if(closed) {
				sender.sendMessage(ChatColor.RED + "This report is closed!");
				return true;
			}
			report.addNote("Marked as insufficient evidence (by " + sender.getName() + ")");
			report.setStatus(ReportStatus.NO_ACTION);
			if(report.getData().containsKey("holdId")) {
				holdLoader.getHoldById(report.getData().getInteger("holdId")).setStatus(HoldStatus.CLOSED_NOACTION);
			}
			report.setReviewedBy(user(sender));
			sender.sendMessage(ChatColor.GREEN + "Marked report as insufficient evidence.");
			sender.spigot().sendMessage(nextReport);
		}
		else if(args[1].equalsIgnoreCase("watch")) {
			if(!canEdit) {
				sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to edit this report");
				return true;
			}
			if(closed) {
				sender.sendMessage(ChatColor.RED + "This report is closed!");
				return true;
			}
			report.addNote("Marked as insufficient evidence and watchlisted (by " + sender.getName() + ")");
			report.setStatus(ReportStatus.NO_ACTION);
			if(report.getData().containsKey("holdId")) {
				holdLoader.getHoldById(report.getData().getInteger("holdId")).setStatus(HoldStatus.CLOSED_NOACTION);
			}
			report.setReviewedBy(user(sender));
			for(User target : report.getTargets()) {
				reportLoader.fileWatchlistReport(target, user(sender), "Moved to watchlist from report #" + report.getId());
			}
			sender.sendMessage(ChatColor.GREEN + "Marked report as insufficient evidence and placed users on the watchlist.");
			sender.spigot().sendMessage(nextReport);
		}
		else if(args[1].equalsIgnoreCase("apply")) {
			if(!canEdit) {
				sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to edit this report");
				return true;
			}
			if(closed && !report.getReviewedBy().equals(user(sender))) {
				sender.sendMessage(ChatColor.RED + "This report is closed!");
				return true;
			}
			PunishmentCode code = PunishmentCode.parseCode(sender, args[2]);
			if(code == null) return true;
			String extraInfo = StringUtil.concatArgs(args, 3);
			sender.sendMessage(ChatColor.GREEN + "This report has been marked as closed. The following actions were taken:");
			for(User target : report.getTargets()) {
				AppliedPunishmentData result = WrappedUser.of(target).autoPunish(code, "Report #" + report.getId() + (extraInfo.isBlank() ? "" : " - " + extraInfo), user(sender));
				sender.sendMessage(ChatColor.GRAY + "- " + result.type + " applied to " + target.getName() + " (" + StringUtil.parseSecondsToTimespan(result.duration) + ")");
			}
			report.setStatus(ReportStatus.ACTION_TAKEN);
			report.setReviewedBy(user(sender));
			if(report.getData().containsKey("holdId")) {
				int holdId = report.getData().getInteger("holdId");
				holdLoader.getHoldById(holdId).setStatus(HoldStatus.CLOSED_ACTION);
			}
			report.getFiledBy().forEach(u -> WrappedUser.of(u).setReportHandled(true));
			sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + " [+Punishment]", "/viewreport " + report.getId() + " confirm", "Apply another punishment to this report"),
				nextReport);
		}
		else if(args[1].equalsIgnoreCase("note")) {
			if(!canEdit) {
				sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to edit this report");
				return true;
			}
			if(closed) {
				sender.sendMessage(ChatColor.RED + "This report is closed!");
				return true;
			}
			if(args.length == 2) {
				sender.sendMessage(ChatColor.RED + "/vrep <ID> note <Note>");
				return true;
			}
			report.addNote(StringUtil.concatArgs(args, 2) + " (by " + sender.getName() + ")");
			sender.sendMessage(ChatColor.GREEN + "Note added successfully.");
			return true;
		}
		else if(args[1].equalsIgnoreCase("status")) {
			if(!canEdit) {
				sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to edit this report");
				return true;
			}
			if(args.length == 2) {
				sender.sendMessage(ChatColor.RED + "/vrep <ID> status <OPEN|NO_ACTION|ACTION_TAKEN>");
				return true;
			}
			ReportStatus status = StringUtil.parseEnum(sender, ReportStatus.class, args[2]);
			if(status == null) return true;
			report.setStatus(status);
			report.addNote("Status set to " + status + " by " + sender.getName());
			if(status != ReportStatus.OPEN) {
				report.setReviewedBy(user(sender));	
			}

			if(report.getData().containsKey("holdId")) {
				HoldEntry hold = holdLoader.getHoldById(report.getData().getInteger("holdId"));
				if(status == ReportStatus.NO_ACTION) {
					hold.setStatus(HoldStatus.CLOSED_NOACTION);
				}
				else if(status == ReportStatus.ACTION_TAKEN) {
					hold.setStatus(HoldStatus.CLOSED_ACTION);
				}
				else if(status == ReportStatus.OPEN) {
					hold.setStatus(HoldStatus.PENDING);
				}
			}
			
			sender.sendMessage(ChatColor.GREEN + "Status changed successfully.");
			sender.spigot().sendMessage(nextReport);
			return true;
		}
		
		return true;
	}
	
	
}
