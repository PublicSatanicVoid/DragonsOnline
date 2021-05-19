package mc.dragons.tools.moderation.report;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.storage.mongo.pagination.PaginatedResult;
import mc.dragons.tools.moderation.report.ReportLoader.Report;

public class ModQueueCommand extends DragonsCommandExecutor {
	private ReportLoader reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
		PaginatedResult<Report> waiting = reportLoader.getAuthorizedUnreviewedReports(1, user(sender).getActivePermissionLevel(), user(sender).getUUID());
		
		if(waiting.getTotal() == 0) {
			sender.sendMessage(ChatColor.GRAY + "Moderation queue is empty! ^-^");
		}
		else {
			Report assign = waiting.getPage().get(0);
			assign.setReviewedBy(user(sender));
			Bukkit.dispatchCommand(sender, "viewreport " + assign.getId());
		}
		return true;
	}
}
