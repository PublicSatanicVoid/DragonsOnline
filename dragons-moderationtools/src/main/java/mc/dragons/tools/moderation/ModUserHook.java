package mc.dragons.tools.moderation;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.networking.StaffAlertMessageHandler;
import mc.dragons.core.storage.mongo.pagination.PaginatedResult;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.analysis.IPAnalysisUtil;
import mc.dragons.tools.moderation.hold.HoldLoader;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldEntry;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldStatus;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldType;
import mc.dragons.tools.moderation.punishment.PunishmentData;
import mc.dragons.tools.moderation.punishment.PunishmentType;
import mc.dragons.tools.moderation.punishment.StandingLevelType;
import mc.dragons.tools.moderation.punishment.command.PunishCommand;
import mc.dragons.tools.moderation.report.ReportLoader;
import mc.dragons.tools.moderation.report.ReportLoader.Report;
import mc.dragons.tools.moderation.report.ReportLoader.ReportStatus;
import mc.dragons.tools.moderation.report.ReportLoader.ReportType;

public class ModUserHook implements UserHook {
	private HoldLoader holdLoader;
	private ReportLoader reportLoader;
	private StaffAlertMessageHandler alertHandler;
	
	public ModUserHook(Dragons instance) {
		holdLoader = instance.getLightweightLoaderRegistry().getLoader(HoldLoader.class);
		reportLoader = instance.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
		alertHandler = instance.getStaffAlertHandler();
	}
	
	@Override
	public void onCreateStorageAccess(Document data) {
		Document blankStandingLevel = new Document();
		for(StandingLevelType type : StandingLevelType.values()) {
			blankStandingLevel.append(type.toString(), new Document("level", 0).append("on", 0L));
		}
		data.append("standingLevel", blankStandingLevel);
	}
	
	@Override
	public void onInitialize(User user) {
		WrappedUser wrapped = WrappedUser.of(user);
		wrapped.updateStandingLevels();
		
		if(user.getPlayer() != null) {
			PunishmentData banData = wrapped.getActivePunishmentData(PunishmentType.BAN);
			if (banData != null) {
				String reasons = StringUtil.parseList(wrapped.getPunishmentHistory().stream()
					.filter(p -> p.getType() == PunishmentType.BAN && !p.hasExpired() && !p.isRevoked())
					.map(p -> ChatColor.RED + p.getReason() + ChatColor.GRAY + (p.isPermanent() ? " (Permanent)" : " (Expires in " + p.getTimeToExpiry() + ")"))
					.collect(Collectors.toList()), "\n\n");
				user.getPlayer().kickPlayer(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You are banned.\n\n" + reasons);
				return;
			}
			HoldEntry entry = holdLoader.getHoldByUser(user, HoldType.SUSPEND);
			if(entry != null && entry.getStatus() == HoldStatus.PENDING && (Instant.now().getEpochSecond() - entry.getStartedOn()) < 60 * 60 * HoldLoader.HOLD_DURATION_HOURS) {
				user.getPlayer().kickPlayer(ChatColor.RED + "Your account was flagged for suspicious activity and is suspended pending review.\n"
					+ "This suspension will be resolved in at most " + entry.getMaxExpiry());
			}
		}
	}
	
	@Override
	public boolean checkAllowChat(User user, String message) {
		WrappedUser wrapped = WrappedUser.of(user);
		PunishmentData muteData = wrapped.getActivePunishmentData(PunishmentType.MUTE);
		if (muteData != null) {
			user.getPlayer().sendMessage(PunishCommand.RECEIVE_PREFIX + ChatColor.BOLD + "You are muted" + ChatColor.RED + ".");
			wrapped.showActiveMuteReasons();
			return false;
		}
		HoldEntry entry = holdLoader.getHoldByUser(user, HoldType.MUTE);
		if(entry != null && entry.getStatus() == HoldStatus.PENDING && (Instant.now().getEpochSecond() - entry.getStartedOn()) < 60 * 60 * HoldLoader.HOLD_DURATION_HOURS) {
			user.getPlayer().sendMessage(PunishCommand.RECEIVE_PREFIX + "Your account was flagged for suspicious activity.");
			user.getPlayer().sendMessage(PunishCommand.RECEIVE_PREFIX + ChatColor.GRAY + "This suspension will be resolved in at most " + entry.getMaxExpiry());
			return false;
		}
		return true;
	}
	
	@Override
	public void onVerifiedJoin(User user) {
		WrappedUser wrapped = WrappedUser.of(user);
		Player player = user.getPlayer();
		Set<User> alts = IPAnalysisUtil.scanAlts(Dragons.getInstance().getPersistentStorageManager(), user)
				.stream()
				.filter(u -> WrappedUser.of(u).getActivePunishmentData(PunishmentType.BAN) != null)
				.collect(Collectors.toSet());
		if(alts.size() > 0) {
			alertHandler.sendSuspiciousJoinMessage(ChatColor.GRAY + "[" + ChatColor.RED + "Join" + ChatColor.GRAY + "] User " 
					+ user.getName() + " shares an IP address with " + alts.size() + " currently banned user" + (alts.size() > 1 ? "s" : ""));
		}
		
		PaginatedResult<Report> watchlist = reportLoader.getReportsByTypeStatusAndTarget(ReportType.WATCHLIST, List.of(ReportStatus.OPEN, ReportStatus.SUSPENDED), user, 1);
		if(watchlist.getTotal() > 0) {
			watchlist.getPage().get(0).setStatus(ReportStatus.OPEN);
			alertHandler.sendSuspiciousJoinMessage(ChatColor.GRAY + "[" + ChatColor.RED + "Join" + ChatColor.GRAY + "] User " 
					+ user.getName() + " is currently on the watchlist (Report #" + watchlist.getPage().get(0).getId() + ")");
		}
		
		if(wrapped.reportWasHandled()) {
			player.sendMessage(ChatColor.GOLD + "" + ChatColor.ITALIC + "Your recent report was handled and closed. Thank you!");
			wrapped.setReportHandled(false);
		}

		PaginatedResult<Report> waiting = reportLoader.getAuthorizedUnreviewedReports(1, user.getActivePermissionLevel(), user.getUUID());
		if(waiting.getTotal() > 0) {
			player.sendMessage(ChatColor.LIGHT_PURPLE + "There are " + waiting.getTotal() + " reports in your moderation queue! To get started, do " + ChatColor.DARK_PURPLE + "/modqueue");
		}
		
		int i = 0;
		for(PunishmentData punishment : wrapped.getPunishmentHistory()) {
			if(punishment.getType() == PunishmentType.WARNING && !punishment.isWarningAcknowledged()) {
				String reason = punishment.getReason();
				player.sendMessage(" ");
				player.sendMessage(PunishCommand.RECEIVE_PREFIX + ChatColor.BOLD + "You received a warning since the last time you were online.");
				if (!reason.equals(" ")) {
					player.sendMessage(PunishCommand.RECEIVE_PREFIX + "Reason: " + reason);
				}
				player.sendMessage(PunishCommand.RECEIVE_PREFIX + ChatColor.GRAY + "Repeated warnings may result in a mute or ban.");
				player.spigot().sendMessage(StringUtil.clickableHoverableText(PunishCommand.RECEIVE_PREFIX + ChatColor.RESET + "[Click here to acknowledge warning]", 
						"/acknowledgewarning " + i, "Click to acknowledge this warning"));
				player.sendMessage("");
			}
			i++;
		}
	}
	
	@Override
	public void onQuit(User user) {
		PaginatedResult<Report> watchlist = reportLoader.getReportsByTypeStatusAndTarget(ReportType.WATCHLIST, List.of(ReportStatus.OPEN, ReportStatus.SUSPENDED), user, 1);
		if(watchlist.getTotal() > 0) {
			watchlist.getPage().get(0).setStatus(ReportStatus.SUSPENDED);
		}
	}
	
}
