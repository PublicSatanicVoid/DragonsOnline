package mc.dragons.tools.moderation.punishment.command;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.DragonsModerationTools;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.hold.HoldLoader;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldEntry;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldType;
import mc.dragons.tools.moderation.punishment.PunishMessageHandler;
import mc.dragons.tools.moderation.punishment.PunishmentData;
import mc.dragons.tools.moderation.punishment.PunishmentType;
import mc.dragons.tools.moderation.punishment.StandingLevelType;
import mc.dragons.tools.moderation.report.ReportLoader;
import mc.dragons.tools.moderation.report.ReportLoader.Report;
import mc.dragons.tools.moderation.util.CmdUtil;
import mc.dragons.tools.moderation.util.CmdUtil.CmdData;

public class PunishCommand extends DragonsCommandExecutor {
	public static final String RECEIVE_PREFIX = ChatColor.DARK_GRAY + "| " + ChatColor.RED;
	
	private ReportLoader reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	private HoldLoader holdLoader = dragons.getLightweightLoaderRegistry().getLoader(HoldLoader.class);
	private PunishMessageHandler handler;
	
	public PunishCommand(DragonsModerationTools plugin) {
		handler = plugin.getPunishMessageHandler();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, SystemProfileFlag.HELPER)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a player and code! /punish <player1 [player2 ...]> <code> [extra info]");
			return true;
		}
		
		CmdData data = CmdUtil.parse(sender, "/punish <players> <code> ", args);
		if(data == null) return true;
		
		StandingLevelType levelType = data.code.getType();
		int level = data.code.getStandingLevel();

		String reason = data.code.getDescription() + (data.extraInfo.isEmpty() ? "" : " (" + data.extraInfo + ")");
		boolean canApply = data.code.canApply(user(sender));

		List<WrappedUser> wrapped = data.targets.stream().map(u -> WrappedUser.of(u)).collect(Collectors.toList());
		int minEffectiveLevel = -1;
		for(WrappedUser w : wrapped) {
			w.updateStandingLevels();
			int oldLevel = w.getStandingLevel(levelType);
			int effectiveLevel = level + oldLevel;
			if(minEffectiveLevel == -1 || effectiveLevel < minEffectiveLevel) minEffectiveLevel = effectiveLevel;
			long duration = WrappedUser.getDurationByStandingLevel(effectiveLevel);
			PunishmentType type = data.code.getType().getPunishmentType();
			if(duration == 0L) {
				type = PunishmentType.WARNING;
				duration = -1L;
			}
			
			for(PunishmentData record : w.getPunishmentHistory()) {
				if(!record.isRevoked() && !record.hasExpired() && record.getType().hasDuration() && record.getCode() == data.code) {
					sender.sendMessage(ChatColor.RED + "This player already has an active punishment of type " + data.code.getCode());
					return true;
				}
			}
			
			if(canApply) {
				// Punish immediately
				int id = w.punish(type, data.code, level, data.extraInfo, user(sender), duration);
				w.raiseStandingLevel(levelType, level);
				
				// Check if we need to tell a different server to immediately apply the punishment
				if(w.getUser().getServerName() != null && !dragons.getServerName().equals(w.getUser().getServerName())) {
					LOGGER.trace("Forwarding punishment on " + w.getUser().getName() + " to " + w.getUser().getServerName());
					handler.forwardPunishment(w.getUser(), id,  type, reason, duration);
				}
				
				String punishment = "Punished";
				if(type == PunishmentType.BAN) {
					punishment = "Banned";
				}
				else if(type == PunishmentType.MUTE) {
					punishment = "Muted";
				}
				else if(type == PunishmentType.KICK) {
					punishment = "Kicked";
				}
				else if(type == PunishmentType.WARNING) {
					punishment = "Warned";
				}
				
				sender.sendMessage(ChatColor.GREEN + "Punishment applied to " + w.getUser().getName() + ": " + punishment + " for " + data.code.getName());
			}
		}

		if(!canApply) {
			// Generate escalation report
			Report r = reportLoader.fileStaffReport(data.targets, user(sender), data.formatInternalReason(), 
				"punish " + StringUtil.parseList(data.targets.stream().map(u -> u.getUUID().toString()).collect(Collectors.toList()), " ") 
					+ " " + data.code.getCode() + (data.extraInfo.isBlank() ? "" : " " + data.extraInfo)+ " -uuid");
			boolean held = false;
			if(r != null && minEffectiveLevel > 1) {
				held = true;
				HoldEntry hold = holdLoader.newHold(data.targets, user(sender), data.formatInternalReason(), r, true, data.code.getType() == StandingLevelType.BAN ? HoldType.SUSPEND : HoldType.MUTE);
				r.getData().append("holdId", hold.getId());
				r.save();
			}
			sender.sendMessage(ChatColor.GREEN + (held ? "Placed a " + HoldLoader.HOLD_DURATION_HOURS + "-hour hold and escalated" : "Escalated") + " this issue for review by a senior staff member.");
		}
		
		if(level >= 10 && canApply) {
			Report review = reportLoader.fileStaffReport(data.targets, user(sender), "Post Review: " + data.formatInternalReason(), "");
			sender.sendMessage(ChatColor.GRAY + " A senior staff member will review this punishment. (Escalation Report ID: " + review.getId() + ")");
		}
		
		return true;
	}

}
