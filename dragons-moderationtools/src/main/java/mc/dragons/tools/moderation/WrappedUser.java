package mc.dragons.tools.moderation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.storage.mongo.pagination.PaginatedResult;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.punishment.PunishmentCode;
import mc.dragons.tools.moderation.punishment.PunishmentData;
import mc.dragons.tools.moderation.punishment.PunishmentType;
import mc.dragons.tools.moderation.punishment.RevocationCode;
import mc.dragons.tools.moderation.punishment.StandingLevelType;
import mc.dragons.tools.moderation.punishment.command.PunishCommand;
import mc.dragons.tools.moderation.report.ReportLoader;
import mc.dragons.tools.moderation.report.ReportLoader.Report;
import mc.dragons.tools.moderation.report.ReportLoader.ReportStatus;
import mc.dragons.tools.moderation.report.ReportLoader.ReportType;

/**
 * Wrapper for Users to extend functionality for moderation.
 * 
 * @author Adam
 *
 */
public class WrappedUser {
	private static Map<User, WrappedUser> wrappers = Collections.synchronizedMap(new HashMap<>());
	private static ReportLoader reportLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	
	public static long STANDING_LEVEL_DECAY_PERIOD = 60 * 60 * 24 * 7;

	private User user;
	
	public static WrappedUser of(User user) {
		return wrappers.computeIfAbsent(user, u -> new WrappedUser(u));
	}
	
	
	private WrappedUser(User user) {
		this.user = user;
	}
	
	public User getUser() {
		return user;
	}
	
	/*
	 * Punishment management
	 */	

	public boolean reportWasHandled() {
		return user.getData().getBoolean("reportHandled", false);
	}
	
	public void setReportHandled(boolean handled) {
		user.getStorageAccess().set("reportHandled", handled);
	}
	
	public List<PunishmentData> getPunishmentHistory() {
		List<PunishmentData> history = new ArrayList<>();
		List<Document> results = user.getData().getList("punishmentHistory", Document.class);
		for (Document entry : results) {
			history.add(PunishmentData.fromDocument(entry));
		}
		return history;
	}

	public void savePunishment(PunishmentType punishmentType, PunishmentCode code, int standingLevelChange, String extra, User by, long durationSeconds) {
		long now = Instant.now().getEpochSecond();
		Document punishment = new Document("type", punishmentType.toString())
			.append("code", code.toString())
			.append("standingLevelChange", standingLevelChange)
			.append("extra", extra)
			.append("issuedOn", now)
			.append("issuedBy", by == null ? null : by.getUUID())
			.append("duration", durationSeconds)
			.append("revoked", false);
		List<Document> punishmentHistory = user.getData().getList("punishmentHistory", Document.class);
		punishmentHistory.add(punishment);
		user.setData("punishmentHistory", punishmentHistory);
	}
	
	public void applyPunishmentLocally(PunishmentType punishmentType, String reason, long durationSeconds) {
		Player player = user.getPlayer();
		if (player != null) {
			if (punishmentType == PunishmentType.BAN) {
				player.kickPlayer(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You have been banned.\n\n" + getActiveBanReasons());
			} else if (punishmentType == PunishmentType.KICK) {
				player.kickPlayer(ChatColor.DARK_RED + "You were kicked!\n\n" + (reason.equals("") ? "" : ChatColor.GRAY + "Reason: " + ChatColor.WHITE + reason + "\n\n") + ChatColor.YELLOW
						+ "Repeated kicks may result in a ban.");
			} else if (punishmentType == PunishmentType.WARNING) {
				player.sendMessage(" ");
				player.sendMessage(PunishCommand.RECEIVE_PREFIX + ChatColor.BOLD + "You have received a warning.");
				if (!reason.equals(" ")) {
					player.sendMessage(PunishCommand.RECEIVE_PREFIX + "Reason: " + reason);
				}
				player.sendMessage(PunishCommand.RECEIVE_PREFIX + ChatColor.GRAY + "Repeated warnings may result in a mute or ban.");
				player.sendMessage("");
			} else if (punishmentType == PunishmentType.MUTE) {
				player.sendMessage(" ");
				player.sendMessage(PunishCommand.RECEIVE_PREFIX + ChatColor.BOLD + "You have been muted.");
				showActiveMuteReasons();
				player.sendMessage(" ");
			}
		}
	}
	
	public String getActiveBanReasons() {
		return StringUtil.parseList(getPunishmentHistory().stream()
				.filter(p -> p.getType() == PunishmentType.BAN && !p.hasExpired() && !p.isRevoked())
				.map(p -> ChatColor.RED + p.getReason() + ChatColor.GRAY + (p.isPermanent() ? " (Permanent)" : " (Expires in " + p.getTimeToExpiry() + ")"))
				.collect(Collectors.toList()), "\n\n");
	}
	
	public void showActiveMuteReasons() {
		for(PunishmentData p : getPunishmentHistory()) {
			if(p.getType() != PunishmentType.MUTE || p.hasExpired() || p.isRevoked()) continue;
			user.getPlayer().sendMessage(PunishCommand.RECEIVE_PREFIX + "- " + p.getReason());
			user.getPlayer().sendMessage(PunishCommand.RECEIVE_PREFIX + ChatColor.GRAY + "  " + (p.isPermanent() ? " (Permanent)" : " (Expires in " + p.getTimeToExpiry() + ")"));
		}
	}
	
	public void punish(PunishmentType punishmentType, PunishmentCode code, int standingLevelChange, String extra, User by, long durationSeconds) {
		if(punishmentType == PunishmentType.BAN) { // Banned users are automatically removed from the watchlist
			PaginatedResult<Report> watchlist = reportLoader.getReportsByTypeStatusAndTarget(ReportType.WATCHLIST, ReportStatus.OPEN, user, 1);
			if(watchlist.getTotal() > 0) {
				watchlist.getPage().get(0).setStatus(ReportStatus.ACTION_TAKEN);
			}
		}
		savePunishment(punishmentType, code, standingLevelChange, extra, by, durationSeconds);
		applyPunishmentLocally(punishmentType, PunishmentCode.formatReason(code, extra), durationSeconds);
	}

	public void punish(PunishmentType punishmentType, PunishmentCode code, int standingLevelChange, String extra, User by) {
		punish(punishmentType, code, standingLevelChange, extra, by, -1L);
	}

	public void saveUnpunishment(int id, RevocationCode code, User by) {
		PunishmentData data = getPunishmentHistory().get(id);
		List<Document> rawHistory = user.getData().getList("punishmentHistory", Document.class);
		rawHistory.get(id).append("revoked", true)
			.append("revokedCode", code.toString())
			.append("revokedBy", by == null ? null : by.getUUID());
		user.getStorageAccess().set("punishmentHistory", rawHistory);
		int effectiveLevelFromPunishment = WrappedUser.getEffectiveStandingLevel(data.getStandingLevelChange(), data.getIssuedDate(), new Date());
		raiseStandingLevel(data.getCode().getType(), -effectiveLevelFromPunishment);
	}
	
	public void deletePunishment(int id) {
		applyUnpunishmentLocally(id);
		List<Document> rawHistory = user.getData().getList("punishmentHistory", Document.class);
		rawHistory.remove(id);
		user.getStorageAccess().set("punishmentHistory", rawHistory);
	}
	
	public void applyUnpunishmentLocally(int id) {
		PunishmentData data = getPunishmentHistory().get(id);
		Player player = user.getPlayer();
		if (player != null && data.getType() == PunishmentType.MUTE && !data.hasExpired()) {
			player.sendMessage("");
			player.sendMessage(PunishCommand.RECEIVE_PREFIX + ChatColor.DARK_GREEN + "Your mute has been revoked.");
			player.sendMessage("");
		}
	}
	
	public void unpunish(int id, RevocationCode code, User by) {
		saveUnpunishment(id, code, by);
		applyUnpunishmentLocally(id);
	}

	public PunishmentData getActivePunishmentData(PunishmentType punishmentType) {
		PunishmentData active = null;
		for(PunishmentData data : getPunishmentHistory()) {
			if(data.getType() == punishmentType && !data.hasExpired() && !data.isRevoked() && (active == null || data.getExpiry().before(active.getExpiry()))) {
				active = data;
			}
		}
		return active;
	}

	public int getStandingLevel(StandingLevelType type) {
		return user.getData().getEmbedded(List.of("standingLevel", type.toString(), "level"), 0);
	}
	
	public void setStandingLevel(StandingLevelType type, int level) {
		Document standingLevel = user.getData().get("standingLevel", Document.class);
		Document sub = standingLevel.get(type.toString(), Document.class);
		sub.append("level", Math.max(0, level));
		sub.append("on", Instant.now().getEpochSecond());
		user.getStorageAccess().set("standingLevel", standingLevel);
	}
	
 	public void raiseStandingLevel(StandingLevelType type, int amount) {
		Document standingLevel = user.getData().get("standingLevel", Document.class);
		Document sub = standingLevel.get(type.toString(), Document.class);
		sub.append("level", Math.max(0, getStandingLevel(type) + amount));
		sub.append("on", Instant.now().getEpochSecond());
		user.getStorageAccess().set("standingLevel", standingLevel);
	}
	
	public void updateStandingLevel(StandingLevelType type) {
		Document standingLevel = user.getData().get("standingLevel", new Document());
		Document sub = standingLevel.get(type.toString(), new Document());
		long last = sub.getLong("on");
		long diff = Instant.now().getEpochSecond() - last;
		int level = getStandingLevel(type);
		if(diff > STANDING_LEVEL_DECAY_PERIOD && level > 0) {
			sub.append("level", level - 1);
			sub.append("on",Instant.now().getEpochSecond());
		}
		user.getStorageAccess().set("standingLevel", standingLevel);
	}
	
	public void updateStandingLevels() {
		for(StandingLevelType type : StandingLevelType.values()) {
			updateStandingLevel(type);
		}
	}
	
	public static long getDurationByStandingLevel(int level) {
		switch(level) {
		case 1:
			return 0; // Warning
		case 2:
			return 60 * 60 * 4;
		case 3:
			return 60 * 60 * 24;
		case 4:
			return 60 * 60 * 24 * 7;
		case 5:
			return 60 * 60 * 24 * 14;
		case 6:
			return 60 * 60 * 24 * 30;
		case 7:
			return 60 * 60 * 24 * 30 * 3;
		case 8:
			return 60 * 60 * 24 * 30 * 6;
		case 9:
			return 60 * 60 * 24 * 30 * 12;
		default:
			return -1; // Permanent
		}
	}
	
	public static int getEffectiveStandingLevel(int level, Date on, Date after) {
		long diff = (after.getTime() - on.getTime()) / 1000L;
		return Math.max(0, level - (int) Math.floor(diff / (double) STANDING_LEVEL_DECAY_PERIOD));
	}
	
}
