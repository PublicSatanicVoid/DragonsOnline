package mc.dragons.tools.moderation.report;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;

import com.google.common.collect.Iterables;
import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.chat.MessageData;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.loader.AbstractLightweightLoader;
import mc.dragons.core.storage.mongo.MongoConfig;
import mc.dragons.core.storage.mongo.pagination.PaginatedResult;
import mc.dragons.core.storage.mongo.pagination.PaginationUtil;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.report.ReportLoader.Report;

public class ReportLoader extends AbstractLightweightLoader<Report> {
	public static final int PAGE_SIZE = 10;
	
	private UserLoader userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	private Map<Integer, Report> reportPool = new HashMap<>();
	
	public class Report {
		private Document data;
		
		public Report(Document data) {
			this.data = data;
		}
		
		public int getId() {
			return data.getInteger("_id");
		}
		
		public ReportType getType() {
			return ReportType.valueOf(data.getString("type"));
		}
		
		public ReportStatus getStatus() {
			return ReportStatus.valueOf(data.getString("status"));
		}
		
		public List<User> getTargets() {
			return data.getList("target", String.class).stream().map(uuid -> userLoader.loadObject(UUID.fromString(uuid))).collect(Collectors.toList());
		}
		
		public User getFiledBy() {
			if(data.getString("filedBy") == null) return null;
			return userLoader.loadObject(UUID.fromString(data.getString("filedBy")));
		}
		
		public User getReviewedBy() {
			if(data.getString("reviewedBy") == null) return null;
			return userLoader.loadObject(UUID.fromString(data.getString("reviewedBy")));
		}
		
		public String getFiledOn() {
			return data.getString("filedOn");
		}
		
		public Document getData() {
			return data.get("data", Document.class);
		}
		
		public String getPreview() {
			switch(getType()) {
			case CHAT:
				return "\"" + getData().getString("message") + "\"";
			case REGULAR:
			case STAFF_ESCALATION:
			case HOLD:
				return getData().getString("reason");
			case AUTOMATED:
				return "Auto-generated user report";
			default:
				return "No preview available";
			}
		}
	
		public List<String> getNotes() {
			return data.getList("notes", String.class);
		}
		
		public void setStatus(ReportStatus status) {
			data.append("status", status.toString());
			save();
		}
		
		public void setReviewedBy(User user) {
			if(user == null) {
				data.remove("reviewedBy");
			}
			else {
				data.append("reviewedBy", user.getUUID().toString());
			}
			save();
		}
		
		public void addNote(String note) {
			List<String> notes = getNotes();
			notes.add(note);
			data.append("notes", notes);
			save();
		}
		
		public Document toDocument() {
			return data;
		}
		
		private void save() {
			collection.updateOne(new Document("_id", getId()), new Document("$set", data));
		}
		
	}
	
	public static enum ReportType {
		
		/* When a lower-ranked staff member escalates an issue for a higher-ranked staff member to take action on. */
		STAFF_ESCALATION,
		
		/* When one or more users are placed in an account hold. */
		HOLD,
		
		/* When a user reports a chat message. */
		CHAT,
		
		/* When the system internally reports a player (e.g. hacking violation that needs manual review */
		AUTOMATED,
		
		/* A regular report filed by a user. */
		REGULAR
	}
	
	public static enum ReportStatus {
		OPEN,
		NO_ACTION,
		ACTION_TAKEN
	}
	
	public ReportLoader(MongoConfig config) {
		super(config, "report", "reports");
	}

	public Report fromDocument(Document data) {
		if(data == null) return null;
		return reportPool.computeIfAbsent(data.getInteger("_id"), id -> new Report(data));
	}
	
	private PaginatedResult<Report> parseResults(FindIterable<Document> results, int page) {
		int total = Iterables.size(results);
		return new PaginatedResult<>(PaginationUtil.sortAndPaginate(results, page, PAGE_SIZE, "priority", false)
				.map(data -> fromDocument(data))
				.into(new ArrayList<>()), total, page, PAGE_SIZE);
	}
	
	public Report getReportById(int id) {
		Document data = collection.find(new Document("_id", id)).first();
		return fromDocument(data);
	}
	
	public PaginatedResult<Report> getAllReports(int page) {
		return parseResults(collection.find(), page);
	}
	
	public PaginatedResult<Report> getReportsByType(ReportType type, int page) {
		return parseResults(collection.find(new Document("type", type.toString())), page);
	}
	
	public PaginatedResult<Report> getReportsByStatus(ReportStatus status, int page) {
		return parseResults(collection.find(new Document("status", status.toString())), page);
	}
	
	public PaginatedResult<Report> getReportsByTypeAndStatus(ReportType type, ReportStatus status, int page) {
		return parseResults(collection.find(new Document("type", type.toString()).append("status", status.toString())), page);
	}
	
	public PaginatedResult<Report> getReportsByFiler(User filer, int page) {
		return parseResults(collection.find(new Document("filedBy", filer.getUUID().toString())), page);
	}
	
	public PaginatedResult<Report> getReportsByTarget(User target, int page) {
		return parseResults(collection.find(new Document("target", new Document("$in", List.of(target.getUUID().toString())))), page);
	}
	
	public PaginatedResult<Report> getUnreviewedReports(int page) {
		return parseResults(collection.find(new Document("reviewedBy", null)), page);
	}
	
	public PaginatedResult<Report> getAuthorizedUnreviewedReports(int page, PermissionLevel editLevelMax, UUID uuid) {
		return parseResults(collection.find(new Document("$and", List.of(new Document("reviewedBy", null), new Document("filedBy", new Document("$ne", uuid.toString())),
			new Document("$or", List.of(new Document("data.permissionReq", null), 
				new Document("data.permissionReq", new Document("$in", PermissionUtil.getAllowedLevels(editLevelMax).stream().map(level -> level.toString()).collect(Collectors.toList())))))))),
			page);
	}
	
	private List<String> getStateTokens(List<User> users) {
		return users.stream().map(u -> u.getState().toString()).collect(Collectors.toList());
	}
	
	private Report fileReport(Document data) {
		Document fullData = new Document(data);
		fullData.append("_id", reserveNextId())
			.append("filedOn", StringUtil.DATE_FORMAT.format(Date.from(Instant.now())))
			.append("status", ReportStatus.OPEN.toString())
			.append("notes", new ArrayList<>());
		Report report = new Report(fullData);
		reportPool.put(report.getId(), report);
		collection.insertOne(report.toDocument());
		return report;
	}
	
	private void reportNotify(int id, String message) {
		Dragons.getInstance().getStaffAlertHandler().sendReportMessage(id, message);
	}
	
	public Report fileChatReport(User target, User by, MessageData message) {
		Document data = new Document()
				.append("type", ReportType.CHAT.toString())
				.append("target", List.of(target.getUUID().toString()))
				.append("priority", by.isVerified() ? 1 : 0)
				.append("filedBy", by.getUUID().toString())
				.append("data", new Document("message", message.getMessage()).append("states", getStateTokens(List.of(target))));
		Report report = fileReport(data);
		reportNotify(report.getId(), target.getName() + " was chat reported: \"" + message.getMessage() + "\" (reported by " + by.getName() + ")");
		return report;
	}
	
	public Report fileStaffReport(User target, User staff, String message, String confirmCommand) {
		if(staff.getActivePermissionLevel().ordinal() == PermissionLevel.SYSOP.ordinal()) {
			return null;
		}
		PermissionLevel permissionReq = null;
		for(PermissionLevel level : PermissionLevel.values()) {
			if(level.ordinal() == staff.getActivePermissionLevel().ordinal() + 1) {
				permissionReq = level;
				break;
			}
		}
		Document data = new Document()
				.append("type", ReportType.STAFF_ESCALATION.toString())
				.append("target", List.of(target.getUUID().toString()))
				.append("priority", 1)
				.append("filedBy", staff.getUUID().toString())
				.append("data", new Document("message", message).append("confirmCommand", confirmCommand).append("permissionReq", permissionReq.toString()).append("states", getStateTokens(List.of(target))));
		Report report = fileReport(data);
		reportNotify(report.getId(), staff.getName() + " escalated an issue with " + target.getName() + ": " + message);
		return report;
	}
	
	public Report fileHoldReport(List<User> targets, User staff, String reason, int holdId) {
		Document data = new Document()
				.append("type", ReportType.HOLD.toString())
				.append("target", targets.stream().map(u -> u.getUUID().toString()).collect(Collectors.toList()))
				.append("priority", 1)
				.append("filedBy", staff.getUUID().toString())
				.append("data", new Document("reason", reason).append("holdId", holdId).append("states", getStateTokens(targets)));
		Report report = fileReport(data);
		reportNotify(report.getId(), staff.getName() + " placed a hold on " + StringUtil.parseList(targets.stream().map(u -> u.getName()).collect(Collectors.toList())) + ": " + reason);
		return report;
	}
	
	public Report fileInternalReport(User target, Document reportData) {
		Document data = new Document()
				.append("type", ReportType.AUTOMATED.toString())
				.append("target", List.of(target.getUUID().toString()))
				.append("priority", 0)
				.append("data", reportData.append("states", getStateTokens(List.of(target))));
		Report report = fileReport(data);
		reportNotify(report.getId(), target.getName() + " was reported internally.");
		return report;
	}
	
	public Report fileUserReport(User target, User by, String reason) {
		Document data = new Document()
				.append("type", ReportType.REGULAR.toString())
				.append("target", List.of(target.getUUID().toString()))
				.append("priority", by.isVerified() ? 1 : 0)
				.append("filedBy", by.getUUID().toString())
				.append("data", new Document("reason", reason).append("states", getStateTokens(List.of(target))));
		Report report = fileReport(data);
		reportNotify(report.getId(), target.getName() + " was reported: " + reason + " (reported by " + by.getName() + ")");
		return report;
	}
	
	public boolean deleteReport(int id) {
		DeleteResult result = collection.deleteOne(new Document("_id", id));
		return result.getDeletedCount() > 0L;
	}
}
