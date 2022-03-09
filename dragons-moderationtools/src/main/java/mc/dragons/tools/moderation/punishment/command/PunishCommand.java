package mc.dragons.tools.moderation.punishment.command;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.hold.HoldLoader;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldEntry;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldType;
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
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, SystemProfileFlag.HELPER)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a player and code! /punish <player1 [player2 ...]> <code> [extra info]");
			return true;
		}
		
		CmdData data = CmdUtil.parse(sender, "/punish <players> <code> ", args);
		if(data == null) return true;
		
		int level = data.code.getStandingLevel();
		boolean canApply = data.code.canApply(user(sender));
		boolean notMod = !hasPermission(sender, SystemProfileFlag.MODERATION);
		List<WrappedUser> wrapped = data.targets.stream().map(u -> WrappedUser.of(u)).collect(Collectors.toList());
		int minEffectiveLevel = -1;
		
		for(WrappedUser w : wrapped) {
			w.updateStandingLevels();
			
			if(canApply) {
				// Punish immediately
				PunishmentType type = w.autoPunish(data.code, data.extraInfo, user(sender)).type;
				
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
			
			if(level >= 10 && notMod) {
				Report review = reportLoader.fileStaffReport(data.targets, user(sender), "Post Review: " + data.formatInternalReason(), "");
				sender.sendMessage(ChatColor.GRAY + " A senior staff member will review this punishment. (Escalation Report ID: " + review.getId() + ")");
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
		
		
		return true;
	}

}
