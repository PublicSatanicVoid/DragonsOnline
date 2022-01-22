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
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.storage.mongo.pagination.PaginatedResult;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.punishment.PunishMessageHandler;
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
	private static Dragons DRAGONS = Dragons.getInstance();
	private static DragonsModerationTools MODTOOLS = JavaPlugin.getPlugin(DragonsModerationTools.class);
	private static DragonsLogger LOGGER = MODTOOLS.getLogger();
	private static Map<User, WrappedUser> wrappers = Collections.synchronizedMap(new HashMap<>());
	private static ReportLoader reportLoader = DRAGONS.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	private static PunishMessageHandler handler = MODTOOLS.getPunishMessageHandler();
	
	public static class AppliedPunishmentData {
		public PunishmentType type;
		public int id;
		public AppliedPunishmentData(PunishmentType type, int id) {
			this.type = type;
			this.id = id;
		}
	}
	
	/**
	 * The time in seconds to remove one standing level from a player.
	 */
	public static long STANDING_LEVEL_DECAY_PERIOD = 60 * 60 * 24 * 7;

	private User user;
	
	/**
	 * 
	 * @param user
	 * @return The <code>WrappedUser</code> corresponding to the given user
	 */
	public static WrappedUser of(User user) {
		return wrappers.computeIfAbsent(user, u -> new WrappedUser(u));
	}
	
	
	private WrappedUser(User user) {
		this.user = user;
	}
	
	/**
	 * 
	 * @return The user wrapped by this instance
	 */
	public User getUser() {
		return user;
	}
	
	/*
	 * Punishment management
	 */	

	/**
	 * 
	 * @return If this user has been flagged to be notified that
	 * their report was handled
	 */
	public boolean reportWasHandled() {
		return user.getData().getBoolean("reportHandled", false);
	}
	
	/**
	 * Flag this user to be notified that their report was handled
	 * next time they join
	 * 
	 * @param handled
	 */
	public void setReportHandled(boolean handled) {
		user.getStorageAccess().set("reportHandled", handled);
	}
	
	/**
	 * 
	 * @return The record of all punishments applied to the player,
	 * in the order in which they were applied
	 */
	public List<PunishmentData> getPunishmentHistory() {
		List<PunishmentData> history = new ArrayList<>();
		List<Document> results = user.getData().getList("punishmentHistory", Document.class);
		for (Document entry : results) {
			history.add(PunishmentData.fromDocument(entry));
		}
		return history;
	}

	/**
	 * Modifies persistent data to store the punishment on the player.
	 * 
	 * @param punishmentType The type of punishment (warning, kick, mute, ban)
	 * @param code The standard code for the punishment reason
	 * @param standingLevelChange The change in standing level
	 * @param extra Extra information about the punishment (shown to the player)
	 * @param by The user who applied the punishment
	 * @param durationSeconds The time until the punishment expires, in seconds
	 */
	public int savePunishment(PunishmentType punishmentType, PunishmentCode code, int standingLevelChange, String extra, User by, long durationSeconds) {
		long now = Instant.now().getEpochSecond();
		Document punishment = new Document("type", punishmentType.toString())
			.append("code", code.toString())
			.append("standingLevelChange", standingLevelChange)
			.append("extra", extra)
			.append("issuedOn", now)
			.append("issuedBy", by == null ? null : by.getUUID())
			.append("duration", durationSeconds)
			.append("revoked", false);
		user.getStorageAccess().pull("punishmentHistory", List.class);
		List<Document> punishmentHistory = user.getData().getList("punishmentHistory", Document.class);
		punishmentHistory.add(punishment);
		user.setData("punishmentHistory", punishmentHistory);
		return punishmentHistory.size() - 1;
	}
	
	/**
	 * Applies the specified punishment to the player, but does not change persistent data.
	 * 
	 * @param id The zero-based ID of the punishment.
	 * @param punishmentType The type of punishment (warning, kick, mute, ban)
	 * @param reason The displayed reason for the punishment
	 */
	public void applyPunishmentLocally(int id, PunishmentType punishmentType, String reason) {
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
				player.spigot().sendMessage(StringUtil.clickableHoverableText(PunishCommand.RECEIVE_PREFIX + ChatColor.RESET + "[Click here to acknowledge warning]", 
						"/acknowledgewarning " + id, "Click to acknowledge this warning"));
				player.sendMessage("");
			} else if (punishmentType == PunishmentType.MUTE) {
				player.sendMessage(" ");
				player.sendMessage(PunishCommand.RECEIVE_PREFIX + ChatColor.BOLD + "You have been muted.");
				showActiveMuteReasons();
				player.sendMessage(" ");
			}
		}
	}
	
	/**
	 * 
	 * @return Information about all active bans on the player
	 */
	public String getActiveBanReasons() {
		return StringUtil.parseList(getPunishmentHistory().stream()
				.filter(p -> p.getType() == PunishmentType.BAN && !p.hasExpired() && !p.isRevoked())
				.map(p -> ChatColor.RED + p.getReason() + ChatColor.GRAY + (p.isPermanent() ? " (Permanent)" : " (Expires in " + p.getTimeToExpiry() + ")"))
				.collect(Collectors.toList()), "\n\n");
	}
	
	/**
	 * Display information about all active mutes to the player.
	 */
	public void showActiveMuteReasons() {
		for(PunishmentData p : getPunishmentHistory()) {
			if(p.getType() != PunishmentType.MUTE || p.hasExpired() || p.isRevoked()) continue;
			user.getPlayer().sendMessage(PunishCommand.RECEIVE_PREFIX + "- " + p.getReason());
			user.getPlayer().sendMessage(PunishCommand.RECEIVE_PREFIX + ChatColor.GRAY + "  " + (p.isPermanent() ? " (Permanent)" : " (Expires in " + p.getTimeToExpiry() + ")"));
		}
	}
	
	/**
	 * Automatically issue the given punishment type, determining parameters,
	 * updating standing level and punishment record, and forwarding to the
	 * correct server as required.
	 * 
	 * @param code
	 * @param extraInfo
	 * @param issuer
	 * @return
	 */
	public AppliedPunishmentData autoPunish(PunishmentCode code, String extraInfo, User issuer) {
		int standingLevel = getStandingLevel(code.getType()) + code.getStandingLevel();
		long duration = WrappedUser.getDurationByStandingLevel(standingLevel);
		PunishmentType type = code.getType().getPunishmentType();
		if(duration == 0) {
			type = PunishmentType.WARNING;
		}
		return new AppliedPunishmentData(type,
				autoPunish(type, code.getType(), code, extraInfo, issuer, duration));
	}
	
	/**
	 * Automatically issue the given punishment type, updating standing level and
	 * punishment record, and forwarding to the correct server as required.
	 * 
	 * @param type
	 * @param slType
	 * @param code
	 * @param extraInfo
	 * @param issuer
	 * @param duration
	 * @return The zero-based punishment index
	 */
	public int autoPunish(PunishmentType type, StandingLevelType slType, PunishmentCode code, String extraInfo, User issuer, long duration) {
		String reason = code.getDescription() + (extraInfo.isEmpty() ? "" : " (" + extraInfo + ")");
		int id = punish(type, code, code.getStandingLevel(), extraInfo, issuer, duration);
		raiseStandingLevel(slType, code.getStandingLevel()); // Whichever type of punishment is the kind we should be raising the SL of
		if(getUser().getServerName() != null && !DRAGONS.getServerName().equals(getUser().getServerName())) {
			LOGGER.trace("Forwarding punishment on " + getUser().getName() + " to " + getUser().getServerName());
			handler.forwardPunishment(getUser(), id, type, reason, duration);
		}
		return id;
	}
	
	/**
	 * Applies the specified punishment to the player.
	 * 
	 * @param punishmentType The type of punishment (warning, kick, mute, ban)
	 * @param code The standard code for the punishment reason
	 * @param standingLevelChange The change in standing level
	 * @param extra Extra information about the punishment (shown to the player)
	 * @param by The user who applied the punishment
	 * @param durationSeconds The time until the punishment expires, in seconds
	 *
	 * @return The zero-based punishment index
	 */
	public int punish(PunishmentType punishmentType, PunishmentCode code, int standingLevelChange, String extra, User by, long durationSeconds) {
		if(punishmentType == PunishmentType.BAN) { // Banned users are automatically removed from the watchlist
			PaginatedResult<Report> watchlist = reportLoader.getReportsByTypeStatusAndTarget(ReportType.WATCHLIST, ReportStatus.OPEN, user, 1);
			if(watchlist.getTotal() > 0) {
				watchlist.getPage().get(0).setStatus(ReportStatus.ACTION_TAKEN);
				watchlist.getPage().get(0).addNote("Auto-closed due to a ban by " + by.getName() + " (" + code.getCode() + ", " + extra + ")");
			}
		}
		int id = savePunishment(punishmentType, code, standingLevelChange, extra, by, durationSeconds);
		applyPunishmentLocally(id, punishmentType, PunishmentCode.formatReason(code, extra));
		return id;
	}

	/**
	 * Applies the specified non-expiring punishment to the player.
	 * 
	 * @param punishmentType The type of punishment (warning, kick, mute, ban)
	 * @param code The standard code for the punishment reason
	 * @param standingLevelChange The change in standing level
	 * @param extra Extra information about the punishment (shown to the player)
	 * @param by The user who applied the punishment
	 */
	public int punish(PunishmentType punishmentType, PunishmentCode code, int standingLevelChange, String extra, User by) {
		return punish(punishmentType, code, standingLevelChange, extra, by, -1L);
	}

	/**
	 * Marks this warning as acknowledged by the player.
	 * 
	 * @param id The punishment index (zero-based)
	 */
	public void acknowledgeWarning(int id) {
		List<Document> rawHistory = user.getData().getList("punishmentHistory", Document.class);
		rawHistory.get(id).append("acknowledged", true);
		user.getStorageAccess().set("punishmentHistory", rawHistory);
	}
	
	/**
	 * Revokes the specified punishment from the player,
	 * modifying persistent data.
	 * 
	 * @param id
	 * @param code The reason for revoking the punishment
	 * @param by The user who revoked the punishment
	 */
	public void saveUnpunishment(int id, RevocationCode code, User by) {
		PunishmentData data = getPunishmentHistory().get(id);
		user.getStorageAccess().pull("punishmentHistory", List.class);
		List<Document> rawHistory = user.getData().getList("punishmentHistory", Document.class);
		rawHistory.get(id).append("revoked", true)
			.append("revokedCode", code.toString())
			.append("revokedBy", by == null ? null : by.getUUID());
		user.getStorageAccess().set("punishmentHistory", rawHistory);
		int effectiveLevelFromPunishment = WrappedUser.getEffectiveStandingLevel(data.getStandingLevelChange(), data.getIssuedDate(), new Date());
		raiseStandingLevel(data.getCode().getType(), -effectiveLevelFromPunishment);
	}
	
	/**
	 * Permanently deletes the specified punishment from the player.
	 * 
	 * @param id
	 */
	public void deletePunishment(int id) {
		applyUnpunishmentLocally(id);
		user.getStorageAccess().pull("punishmentHistory", List.class);
		List<Document> rawHistory = user.getData().getList("punishmentHistory", Document.class);
		rawHistory.remove(id);
		user.getStorageAccess().set("punishmentHistory", rawHistory);
	}
	
	/**
	 * Locally revokes the specified punishment from the player,
	 * but does not modify persistent data.
	 * 
	 * @param id
	 */
	public void applyUnpunishmentLocally(int id) {
		PunishmentData data = getPunishmentHistory().get(id);
		Player player = user.getPlayer();
		if (player != null && data.getType() == PunishmentType.MUTE && !data.hasExpired()) {
			player.sendMessage("");
			player.sendMessage(PunishCommand.RECEIVE_PREFIX + ChatColor.DARK_GREEN + "Your mute has been revoked.");
			player.sendMessage("");
		}
	}
	
	/**
	 * Revoke the specified punishment from the player. It
	 * stays on record.
	 * 
	 * @param id
	 * @param code The reason for revoking the punishment
	 * @param by The user who revoked the punishment
	 */
	public void unpunish(int id, RevocationCode code, User by) {
		saveUnpunishment(id, code, by);
		applyUnpunishmentLocally(id);
	}

	/**
	 * 
	 * @param punishmentType
	 * @return The soonest-expiring active punishment of the specifed type,
	 * or null if none exists
	 */
	public PunishmentData getActivePunishmentData(PunishmentType punishmentType) {
		PunishmentData active = null;
		user.getStorageAccess().pull("punishmentHistory", List.class);
		for(PunishmentData data : getPunishmentHistory()) {
			if(data.getType() == punishmentType && !data.hasExpired() && !data.isRevoked() && (active == null || data.getExpiry().before(active.getExpiry()))) {
				active = data;
			}
		}
		return active;
	}

	/**
	 * 
	 * @param type
	 * @return The player's standing level for the specified type
	 */
	public int getStandingLevel(StandingLevelType type) {
		return user.getData().getEmbedded(List.of("standingLevel", type.toString(), "level"), 0);
	}
	
	/**
	 * Set the player's standing level for the specified type to the 
	 * specified amount
	 * 
	 * @param type
	 * @param level
	 */
	public void setStandingLevel(StandingLevelType type, int level) {
		user.getStorageAccess().pull("standingLevel", Document.class);
		Document standingLevel = user.getData().get("standingLevel", Document.class);
		Document sub = standingLevel.get(type.toString(), Document.class);
		sub.append("level", Math.max(0, level));
		sub.append("on", Instant.now().getEpochSecond());
		user.getStorageAccess().set("standingLevel", standingLevel);
	}
	
	/**
	 * Increase the player's standing level for the specified type by
	 * the specified amount
	 * 
	 * @param type
	 * @param amount
	 */
 	public void raiseStandingLevel(StandingLevelType type, int amount) {
		user.getStorageAccess().pull("standingLevel", Document.class);
		Document standingLevel = user.getData().get("standingLevel", Document.class);
		Document sub = standingLevel.get(type.toString(), Document.class);
		sub.append("level", Math.max(0, getStandingLevel(type) + amount));
		sub.append("on", Instant.now().getEpochSecond());
		user.getStorageAccess().set("standingLevel", standingLevel);
	}
	
 	/**
 	 * Update the player's standing level for the specified type
 	 * @param type
 	 */
	public void updateStandingLevel(StandingLevelType type) {
		user.getStorageAccess().pull("standingLevel", Document.class);
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
	
	/**
	 * Update the player's standing levels to properly apply decay
	 */
	public void updateStandingLevels() {
		for(StandingLevelType type : StandingLevelType.values()) {
			updateStandingLevel(type);
		}
	}
	
	/**
	 * 
	 * @param level
	 * @return The standard punishment duration for the given standing level
	 */
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
	
	/**
	 * 
	 * @param level The base level
	 * @param on The time the base level was effective
	 * @param after The new effective time
	 * @return <code>level</code> decayed by the time between <code>on</code> and <code>after</code>
	 */
	public static int getEffectiveStandingLevel(int level, Date on, Date after) {
		long diff = (after.getTime() - on.getTime()) / 1000L;
		return Math.max(0, level - (int) Math.floor(diff / (double) STANDING_LEVEL_DECAY_PERIOD));
	}
	
}
