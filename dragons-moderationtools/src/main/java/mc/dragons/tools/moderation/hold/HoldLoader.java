package mc.dragons.tools.moderation.hold;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.storage.loader.AbstractLightweightLoader;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.DragonsModerationTools;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldEntry;
import mc.dragons.tools.moderation.punishment.PunishMessageHandler;
import mc.dragons.tools.moderation.punishment.PunishmentCode;
import mc.dragons.tools.moderation.punishment.PunishmentType;
import mc.dragons.tools.moderation.report.ReportLoader;
import mc.dragons.tools.moderation.report.ReportLoader.Report;
import mc.dragons.tools.moderation.report.ReportLoader.ReportStatus;

public class HoldLoader extends AbstractLightweightLoader<HoldEntry> {
	public static final long HOLD_DURATION_HOURS = 48;
	
	private UserLoader userLoader = GameObjectType.USER.getLoader();
	private Dragons dragons;
	private PunishMessageHandler punishHandler;
	private HoldMessageHandler holdHandler;
	private ReportLoader reportLoader;
	
	public HoldLoader(DragonsModerationTools plugin) {
		super(plugin.getDragonsInstance().getMongoConfig(), "holds", "holds");
		dragons = plugin.getDragonsInstance();
		punishHandler = plugin.getPunishMessageHandler();
		holdHandler = plugin.getHoldMessageHandler();
		reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	}
	
	public enum HoldStatus {
		PENDING,
		CLOSED_ACTION,
		CLOSED_NOACTION
	}
	
	public enum HoldType {
		SUSPEND,
		MUTE
	}
	
	public class HoldEntry {
		private Document data;
		
		HoldEntry(Document data) {
			this.data = data;
		}
		
		public int getId() {
			return data.getInteger("_id");
		}
		
		public int getReportId() {
			return data.getInteger("reportId");
		}
		
		public List<User> getUsers() {
			return data.getList("users", UUID.class).stream().map(uuid -> userLoader.loadObject(uuid)).collect(Collectors.toList());
		}
		
		public User getBy() {
			return userLoader.loadObject(data.get("by", UUID.class));
		}
		
		public User getReviewedBy() {
			return userLoader.loadObject(data.get("reviewedBy", UUID.class));
		}
		
		public long getStartedOn() {
			return data.getLong("startedOn");
		}
		
		public String getMaxExpiry() {
			return StringUtil.parseSecondsToTimespan(getStartedOn() + HOLD_DURATION_HOURS * 60 * 60 - Instant.now().getEpochSecond());
		}
		
		public void punishAll(User by, PunishmentType type, PunishmentCode code, String extra) {
			punishAll(by, type, code, extra, -1L);
		}
		
		public void punishAll(User by, PunishmentType type, PunishmentCode code, String extra, long durationSeconds) {
			for(User user : getUsers()) {
				int id = WrappedUser.of(user).punish(type, code, code.getStandingLevel(), extra, by, durationSeconds);
				// Check if we need to tell a different server to immediately apply the punishment
				if(user.getServerName() != null && !dragons.getServerName().equals(user.getServerName())) {
					punishHandler.forwardPunishment(user, id, type, PunishmentCode.formatReason(code, extra), durationSeconds);
				}
			}
			addNote("Punishment Applied by " + by.getName() + ": " + type + " (" + PunishmentCode.formatReason(code, extra) + ") - " + StringUtil.parseSecondsToTimespan(durationSeconds));
			data.append("reviewedBy", by.getUUID());
			save();
		}
		
		/**
		 * The logic gets a little complicated here because the report status may be overridden
		 * if the correlated report is updated without updating the corresponding hold.
		 * 
		 * If action has already been taken on the hold, then that supersedes the hold status.
		 * If the hold is still pending, closing or deleting the report will override the hold.
		 * 
		 * @return
		 */
		public HoldStatus getStatus() {
			HoldStatus status = HoldStatus.valueOf(data.getString("status"));
			if(status == HoldStatus.PENDING) {
				Report report = reportLoader.getReportById(getReportId());
				if(report == null || report.getStatus() == ReportStatus.NO_ACTION || report.getStatus() == ReportStatus.SUSPENDED) {
					setStatus(HoldStatus.CLOSED_NOACTION);
					return HoldStatus.CLOSED_NOACTION;
				}
				else if(report.getStatus() == ReportStatus.ACTION_TAKEN) {
					setStatus(HoldStatus.CLOSED_ACTION);
					return HoldStatus.CLOSED_ACTION;
				}
			}
			return status;
		}
		
		public HoldType getType() {
			return HoldType.valueOf(data.getString("type"));
		}
		
		public void setStatus(HoldStatus status) {
			data.append("status", status.toString());
			save();
			switch(status) {
			case PENDING:
				holdHandler.sendHold(getUsers(), getType(), getNotes().isEmpty() ? "" : getNotes().get(0));
				break;
			case CLOSED_NOACTION:
			case CLOSED_ACTION:
				holdHandler.sendReleaseHold(getUsers());
				break;
			default:
				break;
			}
		}
		
		public List<String> getNotes() {
			return data.getList("notes", String.class);
		}
		
		public void addNote(String note) {
			data.getList("notes", String.class).add(note);
			save();
		}
		
		private void save() {
			collection.replaceOne(new Document("_id", getId()), data);
		}
	}
	
	public HoldEntry fromDocument(Document data) {
		if(data == null) return null;
		Integer id = data.getInteger("_id");
		if(id == null) return null;
		return new HoldEntry(data);
	}
	
	public HoldEntry getHoldById(int id) {
		FindIterable<Document> result = collection.find(new Document("_id", id));
		return fromDocument(result.first());
	}
	
	public HoldEntry getHoldByUser(User user, HoldType type) {
		FindIterable<Document> result = collection.find(new Document("type", type.toString()).append("users", new Document("$in", List.of(user.getUUID())))
			.append("status", HoldStatus.PENDING.toString()).append("startedOn", new Document("$gt", Instant.now().getEpochSecond() - HOLD_DURATION_HOURS * 60 * 60)));
		return fromDocument(result.first());
	}
	
	public List<HoldEntry> getActiveHoldsByUser(User user) {
		FindIterable<Document> result = collection.find(new Document("users", new Document("$in", List.of(user.getUUID()))).append("status", HoldStatus.PENDING.toString())
			.append("startedOn", new Document("$gt", Instant.now().getEpochSecond() - HOLD_DURATION_HOURS * 60 * 60)));
		return result.map(d -> fromDocument(d)).into(new ArrayList<>());
	}
	
	public List<HoldEntry> getActiveHoldsByFiler(User user) {
		FindIterable<Document> result = collection.find(new Document("by", user.getUUID()).append("status", HoldStatus.PENDING.toString())
			.append("startedOn", new Document("$gt", Instant.now().getEpochSecond() - HOLD_DURATION_HOURS * 60 * 60)));
		return result.map(d -> fromDocument(d)).into(new ArrayList<>());
	}
	
	public HoldEntry newHold(List<User> on, User by, String reason, Report report, boolean escalate, HoldType type) {
		int id = reserveNextId();
		Report r = report == null ? reportLoader.fileHoldReport(on, by, reason, id, escalate) : report;
		Document data = new Document("_id", id)
				.append("users", on.stream().map(u -> u.getUUID()).collect(Collectors.toList()))
				.append("by", by.getUUID())
				.append("reviewedBy", null)
				.append("notes", List.of("Initial reason: " + reason))
				.append("status", HoldStatus.PENDING.toString())
				.append("reportId", r.getId())
				.append("type", type.toString())
				.append("startedOn", Instant.now().getEpochSecond());
		collection.insertOne(data);
		holdHandler.sendHold(on, type, reason);
		return fromDocument(data);
	}
}
