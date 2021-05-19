package mc.dragons.tools.moderation.hold;

import java.time.Instant;
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

public class HoldLoader extends AbstractLightweightLoader<HoldEntry> {
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
	
	public class HoldEntry {
		private Document data;
		
		HoldEntry(Document data) {
			this.data = data;
		}
		
		public int getId() {
			return data.getInteger("_id");
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
		
		public void punishAll(User by, PunishmentType type, PunishmentCode code, String extra) {
			punishAll(by, type, code, extra, -1L);
		}
		
		public void punishAll(User by, PunishmentType type, PunishmentCode code, String extra, long durationSeconds) {
			for(User user : getUsers()) {
				WrappedUser.of(user).punish(type, code, code.getStandingLevel(), extra, by, durationSeconds);
				// Check if we need to tell a different server to immediately apply the punishment
				if(user.getServer() != null && !dragons.getServerName().equals(user.getServer())) {
					punishHandler.forwardPunishment(user, type, PunishmentCode.formatReason(code, extra), durationSeconds);
				}
			}
			addNote("Punishment Applied by " + by.getName() + ": " + type + " (" + PunishmentCode.formatReason(code, extra) + ") - " + StringUtil.parseSecondsToTimespan(durationSeconds));
			data.append("reviewedBy", by.getUUID());
			save();
		}
		
		public HoldStatus getStatus() {
			return HoldStatus.valueOf(data.getString("status"));
		}
		
		public void setStatus(HoldStatus status) {
			data.append("status", status.toString());
			save();
			switch(status) {
			case PENDING:
				holdHandler.sendHold(getUsers(), getNotes().isEmpty() ? "" : getNotes().get(0));
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
	
	public HoldEntry getHoldByUser(User user) {
		FindIterable<Document> result = collection.find(new Document("users", new Document("$in", List.of(user.getUUID()))).append("status", HoldStatus.PENDING.toString()));
		return fromDocument(result.first());
	}
	
	public HoldEntry newHold(List<User> on, User by, String reason) {
		int id = reserveNextId();
		Document data = new Document("_id", id)
				.append("users", on.stream().map(u -> u.getUUID()).collect(Collectors.toList()))
				.append("by", by.getUUID())
				.append("reviewedBy", null)
				.append("notes", List.of("Initial reason: " + reason))
				.append("status", HoldStatus.PENDING.toString())
				.append("startedOn", Instant.now().getEpochSecond());
		collection.insertOne(data);
		reportLoader.fileHoldReport(on, by, reason, id);
		holdHandler.sendHold(on, reason);
		return fromDocument(data);
	}
}
