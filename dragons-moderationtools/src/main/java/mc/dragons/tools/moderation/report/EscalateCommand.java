package mc.dragons.tools.moderation.report;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;

public class EscalateCommand extends DragonsCommandExecutor {
	private ReportLoader reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);;
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, SystemProfileFlag.HELPER)) return true;
		User reporter = user(sender);
		
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Specify a player and reason! /escalate <player> <reason>");
			return true;
		}
		
		User target = lookupUser(sender, args[0]);
		if(target == null) return true;
		
		reportLoader.fileStaffReport(target, reporter, StringUtil.concatArgs(args, 1));
		sender.sendMessage(ChatColor.GREEN + "Escalated issue successfully.");
		
		return true;
	}

}
