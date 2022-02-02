package mc.dragons.tools.moderation.report;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.tools.moderation.util.CmdUtil;
import mc.dragons.tools.moderation.util.CmdUtil.CmdData;

public class EscalateCommand extends DragonsCommandExecutor {
	private ReportLoader reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);;
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.HELPER)) return true;
		User reporter = user(sender);
		
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Specify a player and code! /escalate <player1 [player2 ...]> <code> [extra info]");
			return true;
		}
		
		CmdData data = CmdUtil.parse(sender, "/escalate <players> <code> ", args);
		if(data == null) return true;
		
		if(reportLoader.fileStaffReport(data.targets, reporter, data.formatInternalReason(), "") == null) {
			sender.sendMessage(ChatColor.RED + "You have nobody to escalate this report to!");
		}
		else {
			sender.sendMessage(ChatColor.GREEN + "Escalated issue successfully.");
		}
		
		return true;
	}
}
