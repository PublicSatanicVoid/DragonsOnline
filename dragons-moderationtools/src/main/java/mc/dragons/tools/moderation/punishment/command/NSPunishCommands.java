package mc.dragons.tools.moderation.punishment.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.DragonsModerationTools;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.punishment.PunishMessageHandler;
import mc.dragons.tools.moderation.punishment.PunishmentCode;
import mc.dragons.tools.moderation.punishment.PunishmentType;
import mc.dragons.tools.moderation.punishment.StandingLevelType;

public class NSPunishCommands extends DragonsCommandExecutor {
	private PunishMessageHandler handler;
	
	public NSPunishCommands(DragonsModerationTools plugin) {
		handler = plugin.getPunishMessageHandler();
	}

	// Complicated logic for bans and mutes
	private void manual(CommandSender sender, String[] args, PunishmentType type) {
		if(!requirePermission(sender, SystemProfileFlag.MODERATION)) return;
		
		String cmd = type == PunishmentType.BAN ? "ban" : "mute";
		String action = type == PunishmentType.BAN ? "Banned" : "Muted";
		StandingLevelType slType = type == PunishmentType.BAN ? StandingLevelType.BAN : StandingLevelType.MUTE;
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "/" + cmd + " <player> <code> [extra info] [-d <#y#w#d#h#m#s|permanent>]");
			return;
		}
		
		User target = lookupUser(sender, args[0]);
		if(target == null) return;
		
		PunishmentCode code = PunishmentCode.parseCode(sender, args[1]);
		if(code == null) return;

		int durationIndex = StringUtil.getFlagIndex(args, "-d", 2);
		String extraInfo = StringUtil.concatArgs(args, 2, durationIndex);
		String reason = code.getDescription() + (extraInfo.isEmpty() ? "" : " (" + extraInfo + ")");
		
		WrappedUser wtarget = WrappedUser.of(target);
		long duration;
		if(durationIndex == -1) {
			duration = WrappedUser.getDurationByStandingLevel(wtarget.getStandingLevel(slType) + code.getStandingLevel());
			if(duration == 0) {
				sender.sendMessage(ChatColor.RED + "Insufficient standing level to apply mute automatically, please specify duration");
				sender.sendMessage(ChatColor.RED + "/" + cmd + " <player> <code> [extra info] -d <#y#w#d#h#m#s>");
				return;
			}
		}
		else if(args[durationIndex + 1].equalsIgnoreCase("permanent")) {
			duration = -1;
		}
		else {
			duration = StringUtil.parseTimespanToSeconds(args[durationIndex + 1]);
		}
		int id = wtarget.punish(type, code, code.getStandingLevel(), extraInfo, user(sender), duration);
		wtarget.raiseStandingLevel(slType, code.getStandingLevel()); // Whichever type of punishment is the kind we should be raising the SL of
		if(wtarget.getUser().getServerName() != null && !dragons.getServerName().equals(wtarget.getUser().getServerName())) {
			LOGGER.trace("Forwarding punishment on " + wtarget.getUser().getName() + " to " + wtarget.getUser().getServerName());
			handler.forwardPunishment(wtarget.getUser(), id, type, reason, duration);
		}
		
		sender.sendMessage(ChatColor.GREEN + action + " " + target.getName() + ": " + code.getName()
				+ " (" + StringUtil.parseSecondsToTimespan(duration) + ")");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(label.equalsIgnoreCase("safekick")) {
			if(!requirePermission(sender, PermissionLevel.HELPER)) return true;
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/safekick <player> [reason]");
				sender.sendMessage(ChatColor.GRAY + "Local server use only");
				sender.sendMessage(ChatColor.GRAY + "Punishment record is NOT affected by this command");
				return true;
			}
			Player target = lookupPlayer(sender, args[0]);
			if(target == null) return true;
			String reason = "";
			if(args.length > 1) {
				reason = "Reason: " + StringUtil.concatArgs(args, 1) + "\n";
			}
			target.kickPlayer(ChatColor.YELLOW + "You were kicked!\n\n" + reason + "\nThis is NOT a punishment related kick.");
			sender.sendMessage(ChatColor.GREEN + "Kicked " + target.getName() + ".");
		}
		
		else if(label.equalsIgnoreCase("kick")) {
			if(!requirePermission(sender, SystemProfileFlag.HELPER)) return true;
			if(args.length < 2) {
				sender.sendMessage(ChatColor.RED + "/kick <player> <code> [extra info]");
				return true;
			}
			
			User target = lookupUser(sender, args[0]);
			if(target == null) return true;
			
			PunishmentCode code = PunishmentCode.parseCode(sender, args[1]);
			if(code == null) return true;
			
			String extraInfo = StringUtil.concatArgs(args, 2);
			String reason = code.getDescription() + (extraInfo.isEmpty() ? "" : " (" + extraInfo + ")");
			
			WrappedUser wtarget = WrappedUser.of(target);
			int id = wtarget.punish(PunishmentType.KICK, code, code.getStandingLevel(), extraInfo, user(sender));
			wtarget.raiseStandingLevel(code.getType(), code.getStandingLevel());
			if(wtarget.getUser().getServerName() != null && !dragons.getServerName().equals(wtarget.getUser().getServerName())) {
				LOGGER.trace("Forwarding punishment on " + wtarget.getUser().getName() + " to " + wtarget.getUser().getServerName());
				handler.forwardPunishment(wtarget.getUser(), id,  PunishmentType.KICK, reason, -1L);
			}
			
			sender.sendMessage(ChatColor.GREEN + "Kicked " + target.getName() + ": " + code.getName());
		}
		
		else if(label.equalsIgnoreCase("warn")) {
			if(!requirePermission(sender, SystemProfileFlag.HELPER)) return true;
			if(args.length < 2) {
				sender.sendMessage(ChatColor.RED + "/warn <player> <code> [extra info]");
				return true;
			}
			
			User target = lookupUser(sender, args[0]);
			if(target == null) return true;
			
			PunishmentCode code = PunishmentCode.parseCode(sender, args[1]);
			if(code == null) return true;
			
			String extraInfo = StringUtil.concatArgs(args, 2);
			String reason = code.getDescription() + (extraInfo.isEmpty() ? "" : " (" + extraInfo + ")");
			
			WrappedUser wtarget = WrappedUser.of(target);
			int id = wtarget.punish(PunishmentType.WARNING, code, code.getStandingLevel(), extraInfo, user(sender));
			wtarget.raiseStandingLevel(code.getType(), code.getStandingLevel());
			if(wtarget.getUser().getServerName() != null && !dragons.getServerName().equals(wtarget.getUser().getServerName())) {
				LOGGER.trace("Forwarding punishment on " + wtarget.getUser().getName() + " to " + wtarget.getUser().getServerName());
				handler.forwardPunishment(wtarget.getUser(), id,  PunishmentType.KICK, reason, -1L);
			}
			
			sender.sendMessage(ChatColor.GREEN + "Kicked " + target.getName() + ": " + code.getName());
		}
		
		else if(label.equalsIgnoreCase("mute")) {
			manual(sender, args, PunishmentType.MUTE);
		}
		
		else if(label.equalsIgnoreCase("ban")) {
			manual(sender, args, PunishmentType.BAN);
		}
		
		return true;
	}

}
