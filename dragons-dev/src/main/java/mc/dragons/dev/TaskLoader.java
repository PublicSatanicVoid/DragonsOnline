package mc.dragons.dev;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Location;

import com.mongodb.client.FindIterable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.storage.loader.AbstractLightweightLoader;
import mc.dragons.dev.TaskLoader.Task;

public class TaskLoader extends AbstractLightweightLoader<Task> {
	private static final String TASK_COLLECTION = "tasks";
	private static UserLoader userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	
	public static class Task {
		private static TaskLoader taskLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(TaskLoader.class);
		
		private Document data;
		
		public Task(Document data) {
			this.data = data;
		}
		
		public int getId() { return data.getInteger("_id"); }
		public String getName() { return data.getString("name"); }
		public String getDate() { return data.getString("date"); }
		public User getBy() { return userLoader.loadObject(UUID.fromString(data.getString("by"))); }
		public boolean isApproved() { return data.getBoolean("approved"); }
		public User getReviewedBy() { return userLoader.loadObject(UUID.fromString(data.getString("reviewedBy"))); }
		public List<User> getAssignees() { return data.getList("assignees", String.class).stream().map(uuid -> userLoader.loadObject(UUID.fromString(uuid))).collect(Collectors.toList()); }
		public boolean isDone() { return data.getBoolean("done"); }
		public boolean isClosed() { return data.getBoolean("closed"); }
		public List<String> getNotes() { return data.getList("notes", String.class); }
		public Location getLocation() { return StorageUtil.docToLoc(data.get("location", Document.class)); }
		
		public void save() { taskLoader.updateTask(this); }
		
		public void setApproved(boolean approved, User reviewedBy) {
			data.append("approved", approved);
			data.append("reviewedBy", reviewedBy.getUUID().toString());
			save();
		}
		
		public void addAssignee(User assignee) {
			data.getList("assignees", String.class).add(assignee.getUUID().toString());
			save();
		}
		
		public void setDone(boolean done) {
			data.append("done", done);
			save();
		}
		
		public void setClosed(boolean closed) {
			data.append("closed", closed);
			save();
		}
		
		public void addNote(String note) {
			data.getList("notes", String.class).add(note);
			save();
		}
		
		public void setLocation(Location loc) {
			data.append("location", StorageUtil.locToDoc(loc));
			save();
		}
		
		public Document toDocument() {
			return data;
		}
		
		public static Task fromDocument(Document document) {
			if(document == null) return null;
			return new Task(document);
		}
		
		public static Task newTask(String name, User by) {
			return new Task(new Document("_id", taskLoader.reserveNextId())
					.append("name", name)
					.append("date", Date.from(Instant.now()).toString())
					.append("by", by.getUUID().toString())
					.append("approved", false)
					.append("assignees", new ArrayList<>())
					.append("done", false)
					.append("closed", false)
					.append("notes", new ArrayList<>())
					.append("location", StorageUtil.locToDoc(by.getPlayer().getLocation())));
		}
	}
	
	public TaskLoader() {
		super(Dragons.getInstance().getMongoConfig(), "tasks", TASK_COLLECTION);
	}

	public List<Task> asTasks(FindIterable<Document> tasks) {
		List<Document> result = new ArrayList<>();
		for(Document task : tasks) {
			result.add(task);
		}
		return asTasks(result);
	}
	
	public List<Task> asTasks(List<Document> tasks) {
		return tasks.stream().map(doc -> Task.fromDocument(doc)).sorted((a, b) -> a.getId() - b.getId()).collect(Collectors.toList());
	}
	
	public List<Task> getAllTasks() {
		return asTasks(collection.find());
	}
	
	public List<Task> getAllTasksBy(User by) {
		return asTasks(collection.find(new Document("by", by.getUUID().toString())));
	}
	
	public List<Task> getAllReviewedTasks(boolean reviewed) {
		return asTasks(collection.find(new Document("reviewedBy", new Document("$ne", null))));
	}
	
	public List<Task> getAllApprovedTasks() {
		return asTasks(collection.find(new Document("approved", true).append("done", false)));
	}
	
	public List<Task> getAllWaitingTasks() {
		return asTasks(collection.find(new Document("reviewedBy", null)));
	}
	
	public List<Task> getAllRejectedTasks() {
		return asTasks(collection.find(new Document("approved", false).append("reviewedBy", new Document("$ne", null))));
	}
	
	public List<Task> getAllTasksWith(User assigned) {
		return asTasks(collection.find(new Document("$or", Arrays.asList(new Document("assignees", assigned.getUUID().toString()), new Document("by", assigned.getUUID().toString())))
				.append("approved", true).append("done", false).append("closed", false))); // Apparently this is how we see if a list contains a value in Mongo
	}
	
	public List<Task> getAllCompletedTasks(boolean complete) {
		return asTasks(collection.find(new Document("done", complete).append("approved", true).append("closed", false)));
	}
	
	public List<Task> getAllClosedTasks(boolean closed) {
		return asTasks(collection.find(new Document("closed", closed)));
	}

	public Task getTaskById(int id) {
		return Task.fromDocument(collection.find(new Document("_id", id)).first());
	}
	
	public void updateTask(Task update) {
		collection.updateOne(new Document("_id", update.getId()), new Document("$set", update.toDocument()));
	}
	
	public Task addTask(User by, String name) {
		Task task = Task.newTask(name, by);
		collection.insertOne(task.toDocument());
		return task;
	}

}
