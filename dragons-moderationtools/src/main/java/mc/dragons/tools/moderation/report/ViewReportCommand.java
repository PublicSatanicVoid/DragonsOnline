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
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class ViewReportCommand extends DragonsCommandExecutor {

	private ReportLoader reportLoader = instance.getLightweightLoaderRegistry().getLoader(ReportLoader.class);

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/vr <ID>");
			sender.sendMessage(ChatColor.RED + "/vr <ID> addnote <Note>");
			sender.sendMessage(ChatColor.RED + "/vr <ID> status <OPEN|NO_ACTION|ACTION_TAKEN>");
			return true;
		}
		
		Integer id = parseIntType(sender, args[0]);
		if(id == null) return true;
		
		Report report = reportLoader.getReportById(id);
		if(report == null) {
			sender.sendMessage(ChatColor.RED + "No report with ID " + args[0] + " was found!");
			return true;
		}
		
		if(args.length == 1) {
			sender.sendMessage(ChatColor.DARK_GREEN + "Report #" + report.getId() + ": " + report.getType() + "/" + report.getStatus());
			TextComponent confirm = new TextComponent(ChatColor.GREEN + "[Confirm] ");
			confirm.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Confirm report and apply punishment").create()));
			confirm.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " confirm"));
			TextComponent insufficient = new TextComponent(ChatColor.YELLOW + " [Insufficient] ");
			insufficient.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Insufficient evidence to confirm or reject report").create()));
			insufficient.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " insufficient"));
			TextComponent escalate = new TextComponent(ChatColor.GOLD + " [Escalate] ");
			escalate.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Escalate report for review by a senior staff member").create()));
			escalate.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " escalate"));
			TextComponent noAction = new TextComponent(ChatColor.GRAY + " [No Action]");
			noAction.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Take no action on this report").create()));
			noAction.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " status NO_ACTION"));
			sender.spigot().sendMessage(confirm, insufficient, escalate, noAction);
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
				TextComponent language = new TextComponent(ChatColor.RED + "[Language] ");
				language.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Language violation").create()));
				language.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " punish Chat Violation: Language"));
				TextComponent spam = new TextComponent(ChatColor.RED + " [Spam] ");
				spam.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Spamming").create()));
				spam.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " punish Chat Violation: Spam"));
				TextComponent scam = new TextComponent(ChatColor.RED + " [Scamming] ");
				scam.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Scamming").create()));
				scam.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " punish Chat Violation: Scamming"));
				TextComponent advert = new TextComponent(ChatColor.RED + " [Advertising] ");
				advert.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Advertising").create()));
				advert.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " punish Chat Violation: Advertising"));
				TextComponent unspecified = new TextComponent(ChatColor.GRAY + " [Unspecified]");
				unspecified.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Unspecified / Other").create()));
				unspecified.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " punish Chat Violation"));
				sender.spigot().sendMessage(language, spam, scam, advert, unspecified);
			}
			else {
				TextComponent cheating = new TextComponent(ChatColor.RED + "[Cheating] ");
				cheating.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Cheating").create()));
				cheating.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " punish Rule Violation: Cheating or Related"));
				TextComponent trolling = new TextComponent(ChatColor.RED + " [Trolling] ");
				trolling.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Trolling").create()));
				trolling.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " punish Rule Violation: Trolling"));
				TextComponent comp = new TextComponent(ChatColor.RED + " [Compromised] ");
				comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Compromised Account").create()));
				comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " punish Compromised Account"));
				TextComponent unspecified = new TextComponent(ChatColor.GRAY + " [Unspecified] ");
				unspecified.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Unspecified / Other").create()));
				unspecified.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " punish Rule Violation"));
				sender.spigot().sendMessage(cheating, trolling,  comp, unspecified);
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
			sender.sendMessage(ChatColor.GREEN + "Marked report #" + report.getId() + " as escalated. It will remain in the report queue.");
		}
		else if(args[1].equalsIgnoreCase("punish")) {
			String reason = StringUtil.concatArgs(args, 2);
			TextComponent warn = new TextComponent(ChatColor.WHITE + "[Warn] ");
			warn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Warn " + report.getTarget().getName()).create()));
			warn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " apply WARNING " + reason));
			TextComponent kick = new TextComponent(ChatColor.YELLOW + " [Kick] ");
			kick.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Kick " + report.getTarget().getName()).create()));
			kick.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " apply KICK " + reason));
			TextComponent mute = new TextComponent(ChatColor.GOLD + " [Mute] ");
			mute.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Mute " + report.getTarget().getName()).create()));
			mute.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " duration MUTE " + reason));
			TextComponent ban = new TextComponent(ChatColor.RED + " [Ban]");
			ban.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "Ban " + report.getTarget().getName()).create()));
			ban.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " duration BAN " + reason));
			sender.spigot().sendMessage(warn, kick, mute, ban);
		}
		else if(args[1].equalsIgnoreCase("duration")) {
			String reason = StringUtil.concatArgs(args, 3);
			TextComponent hr1 = new TextComponent(ChatColor.YELLOW + "[1 Hour] ");
			hr1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "1 Hour").create()));
			hr1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " apply " + args[2] + " 1h " + reason));
			TextComponent hr6 = new TextComponent(ChatColor.YELLOW + " [6 Hours] ");
			hr6.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "6 Hours").create()));
			hr6.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " apply " + args[2] + " 6h " + reason));
			TextComponent day1 = new TextComponent(ChatColor.YELLOW + " [1 Day] ");
			day1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "1 Day").create()));
			day1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " apply " + args[2] + " 1d " + reason));
			TextComponent day7 = new TextComponent(ChatColor.YELLOW + " [7 Days] ");
			day7.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "7 Days").create()));
			day7.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " apply " + args[2] + " 7d " + reason));
			TextComponent month1 = new TextComponent(ChatColor.GOLD + " [1 Month] ");
			month1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "1 Month").create()));
			month1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " apply " + args[2] + " 30d " + reason));
			TextComponent month3 = new TextComponent(ChatColor.GOLD + " [3 Months] ");
			month3.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "3 Months").create()));
			month3.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " apply " + args[2] + " 90d " + reason));
			TextComponent permanent = new TextComponent(ChatColor.RED + " [Permanent]");
			permanent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.RED + "Permanent").create()));
			permanent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + id + " apply " + args[2] + " permanent " + reason));
			sender.spigot().sendMessage(hr1, hr6, day1, day7, month1, month3, permanent);
		}
		else if(args[1].equalsIgnoreCase("apply")) {
			PunishmentType type = PunishmentType.valueOf(args[2]);
			if(!hasPermission(sender, type.getRequiredFlagToApply())) {
				sender.sendMessage(ChatColor.RED + "Applying this punishment (" + type + ") requires permission flag " + type.getRequiredFlagToApply().getName());
				return true;
			}
			String reason = StringUtil.concatArgs(args, 4);
			report.addNote("Punishment applied: " + type + " (" + args[3] + ")");
			report.addNote("User-facing reason: " + reason);
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
		else if(args[1].equalsIgnoreCase("addnote")) {
			if(args.length == 2) {
				sender.sendMessage(ChatColor.RED + "/vr <ID> -addnote <Note>");
				return true;
			}
			report.addNote(StringUtil.concatArgs(args, 2) + " (by " + sender.getName() + ")");
			sender.sendMessage(ChatColor.GREEN + "Note added successfully.");
			return true;
		}
		else if(args[1].equalsIgnoreCase("status")) {
			if(args.length == 2) {
				sender.sendMessage(ChatColor.RED + "/vr <ID> -setstatus <OPEN|NO_ACTION|ACTION_TAKEN>");
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
