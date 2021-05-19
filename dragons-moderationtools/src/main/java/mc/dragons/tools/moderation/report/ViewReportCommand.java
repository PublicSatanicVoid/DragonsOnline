package mc.dragons.tools.moderation.report;

import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.StateLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.hold.HoldLoader;
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
		if(!requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
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
		
		PermissionLevel req = PermissionLevel.MODERATOR;
		if(report.getData().containsKey("permissionReq")) {
			req = PermissionLevel.valueOf(report.getData().getString("permissionReq"));
		}
		boolean canEdit = hasPermission(sender, req);
		boolean closed = report.getStatus() != ReportStatus.OPEN;
		
		if(args.length == 1) {
			sender.sendMessage(ChatColor.DARK_GREEN + "Report #" + report.getId() + ": " + report.getType() + "/" + report.getStatus());

			if(canEdit) {
				if(closed) {
					sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.YELLOW + "[Re-Open]", "/viewreport " + id + " status OPEN", "Re-open this report for further action or review"));
				}
				else {
					TextComponent confirm = StringUtil.clickableHoverableText(ChatColor.GREEN + "[Confirm] ", "/viewreport " + id + " confirm", "Confirm report and apply punishment");
					TextComponent insufficient = StringUtil.clickableHoverableText(ChatColor.YELLOW + "[Insufficient] ", "/viewreport " + id + " insufficient", "Insufficient evidence to confirm report");
					TextComponent escalate = StringUtil.clickableHoverableText(ChatColor.GOLD + "[Escalate] ", "/viewreport " + id + " escalate", "Escalate report for review by a senior staff member");
					TextComponent noAction = StringUtil.clickableHoverableText(ChatColor.GRAY + "[No Action] ", "/viewreport " + id + " status NO_ACTION", "Take no action on this report");
					TextComponent addNote = StringUtil.clickableHoverableText(ChatColor.WHITE + "" + ChatColor.ITALIC + "   [+Add Note]", "/viewreport " + id + " note ", true, "Add a note to this report");
					sender.spigot().sendMessage(confirm, insufficient, escalate, noAction, addNote);
				}
			}
			else {
				sender.sendMessage(ChatColor.GRAY + "- You do not have sufficient permission to modify this report -");
			}
			sender.sendMessage(ChatColor.GRAY + "Filed Against: " + ChatColor.RESET + StringUtil.parseList(report.getTargets().stream().map(u -> u.getName()).collect(Collectors.toList())));
			if(report.getFiledBy() != null) {
				sender.sendMessage(ChatColor.GRAY + "Filed By: " + ChatColor.RESET + report.getFiledBy().getName());
			}
			sender.sendMessage(ChatColor.GRAY + "Filing Date: " + ChatColor.RESET + report.getFiledOn());
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
				sender.sendMessage(ChatColor.GRAY + "Data: ");
				for(Entry<String, Object> entry : report.getData().entrySet()) {
					sender.sendMessage(ChatColor.GRAY + "- " + entry.getKey() + ChatColor.GRAY + ": " + ChatColor.RESET + entry.getValue());
				}
			}
			if(report.getData().containsKey("states")) {
				sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + "[Snapshots]", "/viewreport " + id + " snapshots", "Click to view snapshots of the reported player(s)"));
			}
			if(report.getNotes().size() > 0) {
				sender.sendMessage(ChatColor.GRAY + "Notes:");
				for(String note : report.getNotes()) {
					sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.RESET + note);
				}
			}
			return true;
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
			if(closed) {
				sender.sendMessage(ChatColor.RED + "This report is closed!");
				return true;
			}
			if(report.getData().containsKey("confirmCommand") && !report.getData().getString("confirmCommand").isEmpty()) {
				player(sender).performCommand(report.getData().getString("confirmCommand"));
			}
			else {
				sender.sendMessage(ChatColor.DARK_GREEN + "Select a punishment code to apply:");
				for(PunishmentCode code : PunishmentCode.values()) {
					if(code.isHidden()) continue;
					sender.spigot().sendMessage(StringUtil.clickableHoverableText(" " + code.getCode() + ChatColor.GRAY + " - " + code.getName(), "/viewreport " + id + " apply " + code.getCode(), 
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
			sender.sendMessage(ChatColor.GREEN + "Escalated this report for further action by a senior staff member.");
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
			if(report.getType() == ReportType.HOLD) { 
				holdLoader.getHoldById(report.getData().getInteger("holdId")).setStatus(HoldStatus.CLOSED_NOACTION);
			}
			report.setReviewedBy(user(sender));
			sender.sendMessage(ChatColor.GREEN + "Marked report as insufficient evidence.");
		}
		else if(args[1].equalsIgnoreCase("apply")) {
			if(!canEdit) {
				sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to edit this report");
				return true;
			}
			if(closed) {
				sender.sendMessage(ChatColor.RED + "This report is closed!");
				return true;
			}
			PunishmentCode code = PunishmentCode.parseCode(sender, args[2]);
			if(code == null) return true;
			for(User target : report.getTargets()) {
				WrappedUser.of(target).punish(code.getType().getPunishmentType(), code, code.getStandingLevel(), "Report #" + report.getId(), user(sender));
			}
			report.setStatus(ReportStatus.ACTION_TAKEN);
			report.setReviewedBy(user(sender));
			if(report.getType() == ReportType.HOLD) {
				int holdId = report.getData().getInteger("holdId");
				holdLoader.getHoldById(holdId).setStatus(HoldStatus.CLOSED_ACTION);
			}
			sender.sendMessage(ChatColor.GREEN + "This report has been marked as closed.");
			WrappedUser.of(report.getFiledBy()).setReportHandled(true);
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
			ReportType type = report.getType();
			if(status == null) return true;
			report.setStatus(status);
			report.addNote("Status set to " + status + " by " + sender.getName());
			if(status != ReportStatus.OPEN) {
				report.setReviewedBy(user(sender));	
			}
			
			if(type == ReportType.HOLD) {
				if(status == ReportStatus.NO_ACTION) {
					holdLoader.getHoldById(report.getData().getInteger("holdId")).setStatus(HoldStatus.CLOSED_NOACTION);
				}
				else if(status == ReportStatus.ACTION_TAKEN) {
					holdLoader.getHoldById(report.getData().getInteger("holdId")).setStatus(HoldStatus.CLOSED_ACTION);
				}
				else if(status == ReportStatus.OPEN) {
					holdLoader.getHoldById(report.getData().getInteger("holdId")).setStatus(HoldStatus.PENDING);
				}
			}
			
			sender.sendMessage(ChatColor.GREEN + "Status changed successfully.");
			return true;
		}
		
		return true;
	}
	
	
}
