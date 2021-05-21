package mc.dragons.tools.moderation.report;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.punishment.PunishmentCode;
import mc.dragons.tools.moderation.util.CmdUtil;
import mc.dragons.tools.moderation.util.CmdUtil.CmdData;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class ReportCommand extends DragonsCommandExecutor {
	public static String[][] GENERIC_REASONS = {
			{ "Suspected Hacking or Cheating", "Modded game client or other non-Vanilla behavior" },
			{ "Residence Content Violation", "A residence owned by this user does not follow community standards" },
			{ "Guild Content Violation", "A guild owned by this user does not follow community standards" },
			{ "Trolling or Abuse", "Misusing game features to harass others or ruin the gameplay experience" },
			{ "Language or Spamming", "Inappropriate, offensive, or spam messages" },
			{ "Other Violation", ChatColor.RED + "Please use /report <player> <reason for reporting> instead!" }
	};
	
	private static String CONFIRMATION_FLAG = " --internal-confirm-and-submit";
	
	private ReportLoader reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		boolean helper = hasPermission(sender, SystemProfileFlag.HELPER);
		if(args.length < 1) {
			sender.sendMessage(ChatColor.RED + "/report <player> [reason for reporting]");
			if(helper) {
				sender.sendMessage(ChatColor.RED + "/report <player1 [player2 ...]> <code> [extra info]" + ChatColor.GRAY + " - recommended for staff");
			}
			return true;
		}
		
		User reporter = user(sender);
		
		CmdData data = null;
		if(helper) {
			data = CmdUtil.parse(sender, "/report <players> <code> ", args);
			if(data == null) return true;
		}
		else {
			data = new CmdData();
			User target = userLoader.loadObject(args[0]);
			if(target == null) return true;
			data.targets.add(target);
		}
		
		if(data.targets.contains(reporter) && !reporter.getLocalData().getBoolean("canSelfReport", false)) {
			sender.sendMessage(ChatColor.RED + "You can't report yourself!");
			return true;
		}
		
		if(args.length == 1) {
			sender.sendMessage(" ");
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "What are you reporting " + StringUtil.parseList(data.targets.stream().map(u -> u.getName()).collect(Collectors.toList())) + " for?");
			if(helper) {
				for(PunishmentCode c : PunishmentCode.values()) {
					if(c.isHidden()) continue;
					sender.spigot().sendMessage(StringUtil.clickableHoverableText(" " + c.getCode() + ChatColor.GRAY + " - " + c.getName(), "/report " + args[0] + " " + c.getCode() + " ", true,
						new String[] {
							ChatColor.YELLOW + "" + ChatColor.BOLD + c.getName(),
							ChatColor.GRAY + c.getDescription(),
							"",
							ChatColor.DARK_GRAY + "Level " + c.getStandingLevel() + " - " + c.getType(),
							hasPermission(sender, c.getRequiredFlagToApply()) ? ChatColor.DARK_GRAY + "Can Be Applied Immediately" : ChatColor.RED + "Requires review by a senior staff member",
							ChatColor.DARK_GRAY + "" + ChatColor.UNDERLINE + "Click to Apply Punishment"
						}));
				}
			}
			else {
				for(String[] reason : GENERIC_REASONS) {
					TextComponent option = new TextComponent(ChatColor.GRAY + " • " + ChatColor.AQUA + reason[0]);
					option.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
							new Text(ChatColor.GRAY + reason[1])));
					option.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/report " + args[0] + " " + reason[0]));
					reporter.getPlayer().spigot().sendMessage(option);
				}
			}
			sender.sendMessage(" ");
			sender.sendMessage(ChatColor.GRAY + "Click on one of the reasons above to continue with the report.");
			sender.sendMessage(" ");
			return true;
		}
		
		// This command is also used as a central gateway for moderation actions,
		// which helpers are encouraged to use if they don't know exactly what to do
		// in a given situation

		if(helper) {		
			List<WrappedUser> wrapped = data.targets.stream().map(u -> WrappedUser.of(u)).collect(Collectors.toList());
			String targetsString = StringUtil.parseList(data.targets.stream().map(u -> u.getName()).collect(Collectors.toList()), " ");
			String targetsCommas = StringUtil.parseList(data.targets.stream().map(u -> u.getName()).collect(Collectors.toList()));
			wrapped.forEach(u -> u.updateStandingLevels());
			int minEffectiveLevel = -1;
			for(WrappedUser w : wrapped) {
				int oldLevel = w.getStandingLevel(data.code.getType());
				int effectiveLevel = data.code.getStandingLevel() + oldLevel;
				if(minEffectiveLevel == -1 || effectiveLevel < minEffectiveLevel) minEffectiveLevel = effectiveLevel;
			}
			boolean canApply = hasPermission(sender, data.code.getRequiredFlagToApply()) && hasPermission(sender, data.code.getRequiredPermissionToApply());
			if(!canApply && minEffectiveLevel <= 1) { // Only one option
				Bukkit.dispatchCommand(sender, "/escalate " + targetsString + " " + data.code.getCode() + " " + data.extraInfo);
				return true;
			}
			sender.sendMessage(ChatColor.DARK_GREEN + "What action would you like to take?");
			if(canApply) {
				sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + " • " + ChatColor.GREEN + "Apply punishment immediately.",
					"/punish " + targetsString + " " + data.code.getCode() + " " + data.extraInfo, "Click to punish " + targetsCommas + " for " + data.code.getName()));
			}
			if(minEffectiveLevel > 1) {
				if(canApply) { // If they can't apply, then this will do the same thing as Place hold and escalate
					sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + " • " + ChatColor.GREEN + "Hold account(s) for my review.", 
						"/hold " + targetsString + " " + data.code.getCode() + " " + data.extraInfo, "Click to place a hold on " + targetsCommas,
						"You will be able to take action on this hold at a later date."));
				}
				sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + " • " + ChatColor.GREEN + "Hold account(s) for senior staff review.", 
					"/hold " + targetsString + " " + data.code.getCode() + (data.extraInfo.isBlank() ? "" : " " + data.extraInfo) + " --forceEscalate", 
					"Click to escalate this issue and place a hold on " + targetsCommas));
			}
			sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + " • " + ChatColor.GREEN + "Forward this issue to senior staff.",
					"/escalate " + targetsString + " " + data.code.getCode() + " " + data.extraInfo, "Click to escalate this issue. No hold will be placed."));
			return true;
		}
		
		String reason = StringUtil.concatArgs(args, 1);
		User target = data.targets.get(0);
		
		if(reason.equalsIgnoreCase("Language or Spamming")) {
			Bukkit.dispatchCommand(sender, "chatreport " + args[0]);
			return true;
		}
		
		if(reason.equalsIgnoreCase("Other Violation")) {
			sender.sendMessage(ChatColor.RED + "To report a different violation, use /report <player> <reason for reporting>");
			return true;
		}
		
		
		if(!reason.contains(CONFIRMATION_FLAG)) {
			sender.sendMessage(" ");
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Please review your report before submitting.");
			sender.sendMessage(ChatColor.GREEN + "Reporting: " + ChatColor.GRAY + target.getName());
			sender.sendMessage(ChatColor.GREEN + "Reason: " + ChatColor.GRAY + reason);
			sender.sendMessage(" ");
			sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[Submit]", "/report " + args[0] + " " + reason + CONFIRMATION_FLAG, 
					"By submitting, you confirm that this report is accurate to the best of your knowledge.\n" +
					"You will not be able to modify this report once it has been submitted."),
				new TextComponent(" "),
				StringUtil.hoverableText(ChatColor.GRAY + "" + ChatColor.BOLD + "[Cancel]", "You can always create a new report with " + ChatColor.YELLOW + "/report <player>" + ChatColor.GRAY + "."));
			sender.sendMessage(" ");
			return true;
		}
		
		reason = reason.replaceAll(Pattern.quote(CONFIRMATION_FLAG), "");
		
		boolean escalated = false;
		if(hasPermission(sender, SystemProfileFlag.HELPER)) {
			if(reportLoader.fileStaffReport(target, reporter, reason, "") != null) {
				escalated = true;
			}
		}
		if(!escalated) {
			reportLoader.fileUserReport(target, reporter, reason);
		}
		sender.sendMessage(ChatColor.GREEN + "Your report against " + target.getName() + " was filed successfully. We will review it as soon as possible." + 
				(reporter.isVerified() ? " As a verified user, your reports are prioritized for review." : ""));
		sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + " [Click to Block Player]", "/block " + target.getName(), "Click to block " + target.getName()));
				
		
		return true;
	}

}
