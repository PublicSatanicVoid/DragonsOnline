package mc.dragons.tools.moderation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.hold.HoldLoader;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldEntry;
import mc.dragons.tools.moderation.report.ReportLoader;
import mc.dragons.tools.moderation.report.ReportLoader.Report;
import mc.dragons.tools.moderation.report.ReportLoader.ReportType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class PeriodicNotificationTask extends BukkitRunnable {
	private HoldLoader holdLoader;
	private ReportLoader reportLoader;
	
	public PeriodicNotificationTask(Dragons instance) {
		holdLoader = instance.getLightweightLoaderRegistry().getLoader(HoldLoader.class);
		reportLoader = instance.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	}
	
	@Override
	public void run() {
		for(User user : UserLoader.allUsers()) {
			if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.HELPER, false)) continue;
			List<HoldEntry> holds = holdLoader.getActiveHoldsByFiler(user).stream()
				.filter(h -> {
					Report report = reportLoader.getReportById(h.getReportId());
					return report.getType() == ReportType.HOLD && 
						(!report.getData().containsKey("permissionReq") 
							|| PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.valueOf(report.getData().getString("permissionReq")), false)); // escalations don't notify because someone else needs to review them.
				}).collect(Collectors.toList());
			List<TextComponent> components = new ArrayList<>();
			components.add(StringUtil.plainText(ChatColor.GOLD + "" + ChatColor.BOLD + "! " + ChatColor.YELLOW + "You have placed " + ChatColor.GOLD + holds.size() 
				+ ChatColor.YELLOW + " hold(s) that still need reviewing: "));
			holds.stream().map(h -> StringUtil.clickableHoverableText(ChatColor.GRAY + "" + h.getId() + "." + h.getReportId() + " ", "/viewreport " + h.getReportId(), 
				"Click to view the report for hold " + h.getId() + "-" + h.getReportId()))
			.forEach(c -> components.add(c));
			if(holds.size() > 0) {
				user.getPlayer().spigot().sendMessage(components.toArray(new BaseComponent[] {}));
			}
		}
	}
}
