package mc.dragons.tools.moderation.punishment.command;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.DragonsModerationTools;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.punishment.PunishMessageHandler;
import mc.dragons.tools.moderation.punishment.PunishmentCode;
import mc.dragons.tools.moderation.punishment.PunishmentType;
import mc.dragons.tools.moderation.punishment.StandingLevelType;
import mc.dragons.tools.moderation.report.ReportLoader;

public class PunishCommand extends DragonsCommandExecutor {
	private ReportLoader reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	private PunishMessageHandler handler;
	
	public PunishCommand(DragonsModerationTools plugin) {
		handler = plugin.getPunishMessageHandler();
	}
			
	public void punish(CommandSender sender, User target, PunishmentCode code, String extraInfo) {
		StandingLevelType levelType = code.getType();
		int level = code.getStandingLevel();
		WrappedUser wrapped = WrappedUser.of(target);
		wrapped.updateStandingLevels();
		int oldLevel = wrapped.getStandingLevel(levelType);
		int effectiveLevel = level + oldLevel;
		
		long duration = WrappedUser.getDurationByStandingLevel(effectiveLevel);
		PunishmentType type = code.getType().getPunishmentType();
		if(duration == 0L) {
			type = PunishmentType.WARNING;
			duration = -1L;
		}
		
		String reason = code.getDescription() + (extraInfo.isEmpty() ? "" : " (" + extraInfo + ")");
		
		boolean canApply = hasPermission(sender, code.getRequiredFlagToApply()) && hasPermission(sender, code.getRequiredPermissionToApply());		
		
		if(canApply) {
			// Punish immediately
			wrapped.punish(type, code, level, extraInfo, user(sender), duration);
			wrapped.raiseStandingLevel(levelType, level);
			
			// Check if we need to tell a different server to immediately apply the punishment
			if(target.getServer() != null && !dragons.getServerName().equals(target.getServer())) {
				LOGGER.trace("Forwarding punishment on " + target.getName() + " to " + target.getServer());
				handler.forwardPunishment(target, type, reason, duration);
			}
			
			sender.sendMessage(ChatColor.GREEN + "Punishment applied to " + target.getName() + ": " + code.getName());
		}
		else {
			// Generate escalation report
			if(reportLoader.fileStaffReport(target, user(sender), code.getName() + " " + extraInfo, "punish " + target.getUUID() + " " + code.getCode() + (extraInfo.isEmpty() ? "" : " - " + extraInfo) + " -uuid") != null) {
				sender.sendMessage(ChatColor.GREEN + "Escalated this issue for review by a senior staff member.");
			}
		}
		
		if(level == 10 && canApply) {
			// Generate escalation report
			if(reportLoader.fileStaffReport(target, user(sender), "Post Review: " + code.getName() + " " + extraInfo, "") != null) {
				sender.sendMessage(ChatColor.GRAY + " A senior staff member will review this punishment.");
			}
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, SystemProfileFlag.HELPER)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a player and code! /punish <player> <code> [extra info]");
			return true;
		}
		
		if(args.length == 1) {
			sender.sendMessage(ChatColor.DARK_GREEN + "Please select the violation code for " + args[0] + ":");
			for(PunishmentCode code : PunishmentCode.values()) {
				if(code.isHidden()) continue;
				sender.spigot().sendMessage(StringUtil.clickableHoverableText(" " + code.getCode() + ChatColor.GRAY + " - " + code.getName(), "/punish " + args[0] + " " + code.getCode() + " ", true,
					new String[] {
						ChatColor.YELLOW + "" + ChatColor.BOLD + code.getName(),
						ChatColor.GRAY + code.getDescription(),
						"",
						ChatColor.DARK_GRAY + "Level " + code.getStandingLevel() + " - " + code.getType(),
						hasPermission(sender, code.getRequiredFlagToApply()) ? ChatColor.DARK_GRAY + "Applied Immediately" : ChatColor.RED + "Requires review by a senior staff member",
						ChatColor.DARK_GRAY + "" + ChatColor.UNDERLINE + "Click to Apply Punishment"
					}));
			}
			return true;
		}
		
		int uuidFlagIndex = StringUtil.getFlagIndex(args, "-uuid", 2);
		User target;
		if(uuidFlagIndex == -1) {
			 target = lookupUser(sender, args[0]);
		}
		else {
			target = userLoader.loadObject(UUID.fromString(args[0]));
		}
		
		PunishmentCode code = PunishmentCode.parseCode(sender, args[1]);
		if(target == null || code == null) return true;

		String extraInfo = StringUtil.concatArgs(args, 2, uuidFlagIndex);
		
		punish(sender, target, code, extraInfo);
		
		
		return true;
	}

}
