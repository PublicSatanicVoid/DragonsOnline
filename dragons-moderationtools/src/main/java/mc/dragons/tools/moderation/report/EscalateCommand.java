package mc.dragons.tools.moderation.report;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class EscalateCommand implements CommandExecutor {

	private UserLoader userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	private ReportLoader reportLoader;
	
	public EscalateCommand(Dragons instance) {
		reportLoader = instance.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		User reporter = UserLoader.fromPlayer((Player) sender);
		if(!PermissionUtil.verifyActiveProfileFlag(reporter, SystemProfileFlag.HELPER, true)) return true;
		
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Specify a player and reason! /escalate <player> <reason>");
			return true;
		}
		
		User target = userLoader.loadObject(args[0]);
		if(target == null) {
			sender.sendMessage(ChatColor.RED + "No player by the name of \"" + args[0] + "\" was found in our records!");
			return true;
		}
		
		reportLoader.fileStaffReport(target, reporter, StringUtil.concatArgs(args, 1));
		sender.sendMessage(ChatColor.GREEN + "Escalated issue successfully.");
		
		return true;
	}

}
