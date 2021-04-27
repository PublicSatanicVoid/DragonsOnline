package mc.dragons.tools.moderation.report;

import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.gameobject.user.punishment.PunishmentType;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.report.ReportLoader.Report;
import mc.dragons.tools.moderation.report.ReportLoader.ReportStatus;
import mc.dragons.tools.moderation.report.ReportLoader.ReportType;
import net.md_5.bungee.api.chat.TextComponent;

public class ViewReportCommand extends DragonsCommandExecutor {
	private ReportLoader reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);

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
		
		if(args.length == 1) {
			sender.sendMessage(ChatColor.DARK_GREEN + "Report #" + report.getId() + ": " + report.getType() + "/" + report.getStatus());
			TextComponent confirm = StringUtil.clickableHoverableText(ChatColor.GREEN + "[Confirm] ", "/viewreport " + id + " confirm", "Confirm report and apply punishment");
			TextComponent insufficient = StringUtil.clickableHoverableText(ChatColor.YELLOW + "[Insufficient] ", "/viewreport " + id + " insufficient", "Insufficient evidence to confirm report");
			TextComponent escalate = StringUtil.clickableHoverableText(ChatColor.GOLD + "[Escalate] ", "/viewreport " + id + " escalate", "Escalate report for review by a senior staff member");
			TextComponent noAction = StringUtil.clickableHoverableText(ChatColor.GRAY + "[No Action] ", "/viewreport " + id + " status NO_ACTION", "Take no action on this report");
			TextComponent addNote = StringUtil.clickableHoverableText(ChatColor.WHITE + "" + ChatColor.ITALIC + "   [+Add Note]", "/viewreport " + id + " note ", true, "Add a note to this report");
			sender.spigot().sendMessage(confirm, insufficient, escalate, noAction, addNote);
			sender.sendMessage(ChatColor.GRAY + "Filed Against: " + ChatColor.RESET + report.getTarget().getName());
			sender.sendMessage(ChatColor.GRAY + "Filed By: " + ChatColor.RESET + report.getFiledBy().getName());
			sender.sendMessage(ChatColor.GRAY + "Filing Date: " + ChatColor.RESET + report.getFiledOn());
			if(report.getReviewedBy() != null) {
				sender.sendMessage(ChatColor.GRAY + "Primary Reviewer: " + ChatColor.RESET + report.getReviewedBy().getName());
			}
			else {
				sender.sendMessage(ChatColor.GRAY + "Unreviewed.");
			}
			if(report.getData().size() > 0) {
				sender.sendMessage(ChatColor.GRAY + "Data: ");
				for(Entry<String, Object> entry : report.getData().entrySet()) {
					sender.sendMessage(ChatColor.GRAY + "- " + entry.getKey() + ChatColor.GRAY + ": " + ChatColor.RESET + entry.getValue());
				}
			}
			if(report.getNotes().size() > 0) {
				sender.sendMessage(ChatColor.GRAY + "Notes: ");
				for(String note : report.getNotes()) {
					sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.RESET + note);
				}
			}
			return true;
		}
		else if(args[1].equalsIgnoreCase("confirm")) {
			if(report.getType() == ReportType.CHAT) {
				TextComponent language = StringUtil.clickableHoverableText(ChatColor.RED + "[Language] ", "/viewreport " + id + " punish Chat Violation: Language", "Language violation");
				TextComponent spam = StringUtil.clickableHoverableText(ChatColor.RED + "[Spamming] ", "/viewreport " + id + " punish Chat Violation: Spam", "Spamming");
				TextComponent scam = StringUtil.clickableHoverableText(ChatColor.RED + "[Scamming] ", "/viewreport " + id + " punish Chat Violation: Scamming", "Scamming or Attempted Fraud");
				TextComponent advert = StringUtil.clickableHoverableText(ChatColor.RED + "[Advertising] ", "/viewreport " + id + " punish Chat Violation: Advertising", "Advertising Unauthorized Third-Party Content");
				TextComponent unspecified = StringUtil.clickableHoverableText(ChatColor.GRAY + "[Unspecified]", "/viewreport " + id + " punish Chat Violation", "Unspecified / Other");
				sender.spigot().sendMessage(language, spam, scam, advert, unspecified);
			}
			else {
				TextComponent cheating = StringUtil.clickableHoverableText(ChatColor.RED + "[Cheating] ", "/viewreport " + id + " punish Rule Violation: Cheating or Related", "Cheating or Related");
				TextComponent content = StringUtil.clickableHoverableText(ChatColor.RED + "[Content] ", "/viewreport " + id + " punish Rule Violation: Unauthorized Content", "Unauthorized Content (Residence, etc)");
				TextComponent trolling = StringUtil.clickableHoverableText(ChatColor.RED + "[Trolling] ", "/viewreport " + id + " punish Rule Violation: Trolling or Related", "Trolling or Related");
				TextComponent comp = StringUtil.clickableHoverableText(ChatColor.RED + "[Compromised Account] ", "/viewreport " + id + " punish Compromised Account", "Compromised Account");
				TextComponent unspecified = StringUtil.clickableHoverableText(ChatColor.GRAY + "[Unspecified]", "/viewreport " + id + " punish Rule Violation", "Compromised Account");
				sender.spigot().sendMessage(cheating, content, trolling,  comp, unspecified);
			}
		}
		else if(args[1].equalsIgnoreCase("insufficient")) {
			report.addNote("Insufficient evidence");
			report.setStatus(ReportStatus.NO_ACTION);
			report.setReviewedBy(user(sender));
			sender.sendMessage(ChatColor.GREEN + "Marked report #" + report.getId() + " as insufficient evidence.");
		}
		else if(args[1].equalsIgnoreCase("escalate")) {
			report.addNote("Staff escalation by " + sender.getName());
			report.setStatus(ReportStatus.OPEN);
			report.setReviewedBy(null);
			sender.sendMessage(ChatColor.GREEN + "Marked report #" + report.getId() + " as escalated. It will remain in the report queue.");
		}
		else if(args[1].equalsIgnoreCase("punish")) {
			String reason = StringUtil.concatArgs(args, 2);
			TextComponent warn = StringUtil.clickableHoverableText(ChatColor.WHITE + "[Warn] ", "/viewreport " + id + " apply WARNING " + reason, "Warning");
			TextComponent kick = StringUtil.clickableHoverableText(ChatColor.YELLOW + " [Kick] ", "/viewreport " + id + " apply KICK" + reason, "Kick");
			TextComponent mute = StringUtil.clickableHoverableText(ChatColor.GOLD + " [Mute] ", "/viewreport " + id + " duration MUTE " + reason, "Mute (you will be prompted for duration)");
			TextComponent ban = StringUtil.clickableHoverableText(ChatColor.RED + "[Ban] ", "/viewreport " + id + " duration BAN " + reason, "Ban (you will be prompted for duration)");
			sender.spigot().sendMessage(warn, kick, mute, ban);
		}
		else if(args[1].equalsIgnoreCase("duration")) {
			String reason = StringUtil.concatArgs(args, 3);
			TextComponent hr1 = StringUtil.clickableHoverableText(ChatColor.YELLOW + "[1 Hour] ", "/viewreport " + id + " apply " + args[2] + " 1h " + reason, "1 Hour");
			TextComponent hr6 = StringUtil.clickableHoverableText(ChatColor.YELLOW + "[6 Hours] ", "/viewreport " + id + " apply " + args[2] + " 6h " + reason, "6 Hours");
			TextComponent day1 = StringUtil.clickableHoverableText(ChatColor.YELLOW + "[1 Day] ", "/viewreport " + id + " apply " + args[2] + " 1d " + reason, "1 Day");
			TextComponent day7 = StringUtil.clickableHoverableText(ChatColor.YELLOW + "[7 Days] ", "/viewreport " + id + " apply " + args[2] + " 7d " + reason, "7 Days");
			TextComponent month1 = StringUtil.clickableHoverableText(ChatColor.GOLD + " [1 Month] ", "/viewreport " + id + " apply " + args[2] + " 30d " + reason, "1 Month");
			TextComponent month3 = StringUtil.clickableHoverableText(ChatColor.YELLOW + "[3 Months] ", "/viewreport " + id + " apply " + args[2] + " 90d " + reason, "3 Months");
			TextComponent permanent = StringUtil.clickableHoverableText(ChatColor.RED + " [Permanent] ", "/viewreport " + id + " apply " + args[2] + " permanent " + reason, "Permanent");
			sender.spigot().sendMessage(hr1, hr6, day1, day7, month1, month3, permanent);
		}
		else if(args[1].equalsIgnoreCase("apply")) {
			PunishmentType type = PunishmentType.valueOf(args[2]);
			if(!hasPermission(sender, type.getRequiredFlagToApply())) {
				sender.sendMessage(ChatColor.RED + "Applying this punishment (" + type + ") requires permission flag " + type.getRequiredFlagToApply().getName());
				return true;
			}
			String reason = StringUtil.concatArgs(args, 4);
			report.addNote("Punishment applied: " + type + " (" + args[3] + ") by " + sender.getName());
			report.addNote("  User-facing reason: " + reason);
			report.setStatus(ReportStatus.ACTION_TAKEN);
			report.setReviewedBy(user(sender));
			boolean duration = false;
			if(type.hasDuration()) {
				if(!args[3].equalsIgnoreCase("permanent")) {
					report.getTarget().punish(type, reason, StringUtil.parseTimespanToSeconds(args[3]));
					duration = true;
				}
			}
			if(!duration) {
				report.getTarget().punish(type, reason);
			}
			sender.sendMessage(ChatColor.GREEN + "Punishment applied successfully. Report #" + report.getId() + " is now closed.");
		}
		else if(args[1].equalsIgnoreCase("note")) {
			if(args.length == 2) {
				sender.sendMessage(ChatColor.RED + "/vrep <ID> note <Note>");
				return true;
			}
			report.addNote(StringUtil.concatArgs(args, 2) + " (by " + sender.getName() + ")");
			sender.sendMessage(ChatColor.GREEN + "Note added successfully.");
			return true;
		}
		else if(args[1].equalsIgnoreCase("status")) {
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
			sender.sendMessage(ChatColor.GREEN + "Status changed successfully.");
			return true;
		}
		
		return true;
	}
	
	
}
