package mc.dragons.tools.moderation.report;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.punishment.PunishmentCode;

public class EscalateCommand extends DragonsCommandExecutor {
	private ReportLoader reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);;
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, SystemProfileFlag.HELPER)) return true;
		User reporter = user(sender);
		
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Specify a player and code! /escalate <player> <code> [extra info]");
			return true;
		}
		
		User target = lookupUser(sender, args[0]);
		PunishmentCode code = PunishmentCode.parseCode(sender, args[1]);
		if(target == null || code == null) return true;
		
		String extraInfo = StringUtil.concatArgs(args, 1);
		String reason = code.getDescription() + (extraInfo.isEmpty() ? "" : " (" + extraInfo + ")");
		
		if(reportLoader.fileStaffReport(target, reporter, reason, "") == null) {
			sender.sendMessage(ChatColor.RED + "You have nobody to escalate this report to!");
		}
		else {
			sender.sendMessage(ChatColor.GREEN + "Escalated issue successfully.");
		}
		
		return true;
	}

}
