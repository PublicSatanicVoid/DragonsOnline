package mc.dragons.tools.moderation.report;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.ChatColor;

import com.google.common.collect.Iterables;
import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.chat.ChatChannel;
import mc.dragons.core.gameobject.user.chat.MessageData;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.storage.loader.AbstractLightweightLoader;
import mc.dragons.core.storage.mongo.MongoConfig;
import mc.dragons.core.storage.mongo.pagination.PaginatedResult;
import mc.dragons.core.storage.mongo.pagination.PaginationUtil;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.tools.moderation.report.ReportLoader.Report;

public class ReportLoader extends AbstractLightweightLoader<Report> {

	public static final int PAGE_SIZE = 10;
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
	
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
		
		public User getTarget() {
			return userLoader.loadObject(UUID.fromString(data.getString("target")));
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
	
		public List<String> getNotes() {
			return data.getList("notes", String.class);
		}
		
		public void setStatus(ReportStatus status) {
			data.append("status", status.toString());
			save();
		}
		
		public void setReviewedBy(User user) {
			if(user == null) return;
			data.append("reviewedBy", user.getUUID().toString());
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
		return parseResults(collection.find(new Document("target", target.getUUID().toString())), page);
	}
	
	private Report fileReport(Document data) {
		Document fullData = new Document(data);
		fullData.append("_id", reserveNextId())
			.append("filedOn", DATE_FORMAT.format(Date.from(Instant.now())))
			.append("status", ReportStatus.OPEN.toString())
			.append("notes", new ArrayList<>());
		Report report = new Report(fullData);
		reportPool.put(report.getId(), report);
		collection.insertOne(report.toDocument());
		return report;
	}
	
	private void reportNotify(String message) {
		for(User user : UserLoader.allUsers()) {
			if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.MODERATION, false)) continue;
			user.sendMessage(ChatChannel.STAFF, null, ChatColor.DARK_AQUA + "[Report] " + ChatColor.GRAY + message);
		}
	}
	
	public void fileChatReport(User target, User by, MessageData message) {
		Document data = new Document()
				.append("type", ReportType.CHAT.toString())
				.append("target", target.getUUID().toString())
				.append("priority", by.isVerified() ? 1 : 0)
				.append("filedBy", by.getUUID().toString())
				.append("data", new Document("message", message.getMessage()));
		Report report = fileReport(data);
		reportNotify("[" + report.getId() + "] " + target.getName() + " was chat reported: \"" + message.getMessage() + "\" (reported by " + by.getName() + ")");
	}
	
	public void fileStaffReport(User target, User staff, String message) {
		Document data = new Document()
				.append("type", ReportType.STAFF_ESCALATION.toString())
				.append("target", target.getUUID().toString())
				.append("priority", 1)
				.append("filedBy", staff.getUUID().toString())
				.append("data", new Document("message", message));
		Report report = fileReport(data);
		reportNotify("[" + report.getId() + "] " + staff.getName() + " escalated an issue with " + target.getName() + ": " + message);
	}
	
	public void fileInternalReport(User target, Document reportData) {
		Document data = new Document()
				.append("type", ReportType.AUTOMATED.toString())
				.append("target", target.getUUID().toString())
				.append("priority", 0)
				.append("data", reportData);
		Report report = fileReport(data);
		reportNotify("[" + report.getId() + "] " + target.getName() + " was reported internally. ");
	}
	
	public void fileUserReport(User target, User by, String reason) {
		Document data = new Document()
				.append("type", ReportType.REGULAR.toString())
				.append("target", target.getUUID().toString())
				.append("priority", by.isVerified() ? 1 : 0)
				.append("filedBy", by.getUUID().toString())
				.append("data", new Document("reason", reason));
		Report report = fileReport(data);
		reportNotify("[" + report.getId() + "] " + target.getName() + " was reported: " + reason + " (reported by " + by.getName() + ")");
	}
	
	public boolean deleteReport(int id) {
		DeleteResult result = collection.deleteOne(new Document("_id", id));
		return result.getDeletedCount() > 0L;
	}
}
