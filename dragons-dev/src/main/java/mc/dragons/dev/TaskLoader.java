package mc.dragons.dev;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import mc.dragons.core.Dragons;
import mc.dragons.core.storage.impl.MongoConfig;
import mc.dragons.core.storage.impl.loader.AbstractLightweightLoader;
import mc.dragons.dev.TaskLoader.Task;

public class TaskLoader extends AbstractLightweightLoader<Task> {
	
	public static class Task {
		private static TaskLoader taskLoader;
		
		private int id;
		private String name;
		private String date;
		private String by;
		private boolean approved;
		private String reviewedBy;
		private List<String> assignees;
		private boolean done;
		private boolean closed;
		
		public Task(int id, String name, String date, String by, boolean approved, String reviewedBy, List<String> assignees, boolean done, boolean closed) {
			if(taskLoader == null) {
				taskLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(TaskLoader.class);
			}
			
			this.id = id;
			this.name = name;
			this.date = date;
			this.by = by;
			this.approved = approved;
			this.reviewedBy = reviewedBy;
			this.assignees = assignees;
			this.done = done;
			this.closed = closed;
		}
		
		public int getId() { return id; }
		public String getName() { return name; }
		public String getDate() { return date; }
		public String getBy() { return by; }
		public boolean isApproved() { return approved; }
		public String getReviewedBy() { return reviewedBy; }
		public List<String> getAssignees() { return assignees; }
		public boolean isDone() { return done; }
		public boolean isClosed() { return closed; }
		
		public void save() { taskLoader.updateTask(this); }
		
		public void setApproved(boolean approved, String reviewedBy) {
			this.approved = approved;
			this.reviewedBy = reviewedBy;
			save();
		}
		
		public void addAssignee(String assignee) {
			assignees.add(assignee);
			save();
		}
		
		public void setDone(boolean done) {
			this.done = done;
			save();
		}
		
		public void setClosed(boolean closed) {
			this.closed = closed;
			save();
		}
		
		public Document toDocument() {
			return new Document("_id", id).append("name", name).append("date", date)
					.append("by", by).append("approved", approved).append("reviewedBy", reviewedBy)
					.append("assignees", assignees).append("done", done).append("closed", closed);
		}
		
		public static Task fromDocument(Document document) {
			if(document == null) return null;
			return new Task(document.getInteger("_id"), document.getString("name"), document.getString("date"), 
					document.getString("by"), document.getBoolean("approved"), document.getString("reviewedBy"),
					document.getList("assignees", String.class), document.getBoolean("done"), document.getBoolean("closed"));
		}
	}
	
	private static final String TASK_COLLECTION = "tasks";
	
	public TaskLoader() {
		super("tasks", TASK_COLLECTION);
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
	
	public List<Task> getAllTasksBy(String by) {
		return asTasks(collection.find(new Document("by", by)));
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
	
	public List<Task> getAllTasksWith(String assigned) {
		return asTasks(collection.find(new Document("$or", Arrays.asList(new Document("assignees", assigned), new Document("by", assigned)))
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
	
	public Task addTask(String by, String name) {
		String date = Date.from(Instant.now()).toString();
		int id = MongoConfig.getCounter().reserveNextId("tasks");
		Task task = new Task(id, name, date, by, false, null, new ArrayList<>(), false, false);
		collection.insertOne(task.toDocument());
		return task;
	}

}
