package mc.dragons.tools.moderation.punishment.command;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.tools.moderation.hold.HoldLoader;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldStatus;
import mc.dragons.tools.moderation.punishment.StandingLevelType;
import mc.dragons.tools.moderation.report.ReportLoader;
import mc.dragons.tools.moderation.report.ReportLoader.ReportStatus;

public class PurgePunishmentsCommand extends DragonsCommandExecutor {
	private HoldLoader holdLoader = dragons.getLightweightLoaderRegistry().getLoader(HoldLoader.class);
	private ReportLoader reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN) || !requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/purgepunishments <player>");
			return true;
		}
		
		User target = lookupUser(sender, args[0]);
		if(target == null) return true;
		
		Document standingLevelsEmpty = new Document();
		for(StandingLevelType level : StandingLevelType.values()) {
			standingLevelsEmpty.append(level.toString(), new Document("level", 0).append("on", 0L));
		}
		
		target.getStorageAccess().set("punishmentHistory", new ArrayList<>());
		target.getStorageAccess().set("standingLevel", standingLevelsEmpty);
		holdLoader.getActiveHoldsByUser(target).forEach(hold -> {
			hold.setStatus(HoldStatus.CLOSED_NOACTION);
			reportLoader.getReportById(hold.getReportId()).setStatus(ReportStatus.NO_ACTION);
		});
		reportLoader.closeReports(new Document("target", List.of(target.getUUID().toString()))
				.append("status", new Document("$in", List.of(ReportStatus.OPEN.toString(), ReportStatus.SUSPENDED.toString()))));
		
		sender.sendMessage(ChatColor.GREEN + "Purged punishment history of " + target.getName() + " and released all pending holds and watchlist entries");
		
		return true;
	}
}
