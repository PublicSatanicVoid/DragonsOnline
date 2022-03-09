package mc.dragons.dev.tasks;

import static mc.dragons.core.util.BukkitUtil.sync;
import static mc.dragons.core.util.BukkitUtil.syncPeriodic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;

import com.mongodb.client.FindIterable;

import mc.dragons.core.Dragons;
import mc.dragons.core.events.PlayerEventListeners;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.storage.loader.AbstractLightweightLoader;
import mc.dragons.core.util.HologramUtil;
import mc.dragons.core.util.ParticleUtil;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.dev.DragonsDev;
import mc.dragons.dev.notifier.DiscordNotifier.DiscordRole;
import mc.dragons.dev.tasks.TaskLoader.Task;
import net.md_5.bungee.api.ChatColor;

public class TaskLoader extends AbstractLightweightLoader<Task> {
	private static final String TASK_COLLECTION = "tasks";

	private static UserLoader userLoader = GameObjectType.USER.getLoader();
	private static Map<Integer, Task> taskPool = new HashMap<>();
	private static Set<Task> liveTasks = new CopyOnWriteArraySet<>(); // Tasks that have particles that need updating
	private static int renderParity = 0;
	
	public static final String STAR = "✰";
	public static final String PENCIL = "✎";
	public static final int TASK_MARKER_Y_OFFSET = 3;

	private DragonsLogger LOGGER;
	
	public static enum TaskTag {
		BUILD(DiscordRole.BUILDER, "Build", true), 
		DEV(DiscordRole.DEVELOPER, "Dev", false), 
		GM(DiscordRole.GAME_MASTER, "GM", false), 
		META(DiscordRole.TASK_MANAGER, "Meta", false);

		private DiscordRole notifyRole;
		private String name;
		private boolean isDefault;

		TaskTag(DiscordRole notifyRole, String name, boolean isDefault) {
			this.notifyRole = notifyRole;
			this.name = name;
			this.isDefault = isDefault;
		}

		public DiscordRole getNotifyRole() {
			return notifyRole;
		}

		public String getName() {
			return name;
		}

		public boolean isDefault() {
			return isDefault;
		}

		public static TaskTag[] fromTaskName(String taskName) {
			List<TaskTag> result = new ArrayList<>();
			for (TaskTag tag : values()) {
				if (taskName.contains(tag.getName())) {
					result.add(tag);
				}
			}
			if (result.isEmpty()) {
				for (TaskTag tag : values()) {
					if (tag.isDefault()) {
						result.add(tag);
					}
				}
			}
			return result.toArray(new TaskTag[result.size()]);
		}
	}

	public static class Task {
		private static TaskLoader taskLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(TaskLoader.class);

		private Document data;
		private Slime clickHandler;
		private Location cachedLocation;
		private User cachedBy;
		private User cachedReviewedBy;
		private List<User> cachedAssignees;
		
		private void updateMarker() {
			if(clickHandler != null) {
				clickHandler.remove();
			}
			if(hasMarker()) {
				liveTasks.add(this);
				clickHandler = HologramUtil.clickableSlime("", cachedLocation.clone().add(0, TASK_MARKER_Y_OFFSET - 0.5, 0), user -> Bukkit.dispatchCommand(user.getPlayer(), "taskinfo " + getId()));
			}
			else {
				liveTasks.remove(this);
				if(clickHandler != null) {
					clickHandler.remove();
				}
			}
		}
		
		public Task(Document data) {
			this.data = data;
			cachedLocation = getLocation();
			cachedAssignees = getAssignees();
			sync(() -> updateMarker());
		}
		
		public boolean hasMarker() {
			boolean buildOrGM = false;
			for (TaskTag tag : TaskTag.fromTaskName(getName())) {
				if (tag == TaskTag.BUILD || tag == TaskTag.GM)
					buildOrGM = true;
			}
			return buildOrGM && !isClosed();
		}

		public int getId() {
			return data.getInteger("_id");
		}

		public String getName() {
			return data.getString("name");
		}

		public String getStatus() {
			if (isClosed())
				return "Closed";
			if (isDone())
				return "Done";
			if (isApproved() && getAssignees().size() > 0)
				return "Assigned (" + getAssignees().size() + ")";
			if (isApproved())
				return "Approved";
			if (getReviewedBy() == null)
				return "Waiting";
			return "Rejected";
		}

		public String getStrikethrough() {
			return isClosed() ? ChatColor.STRIKETHROUGH + "" : "";
		}

		public String formatName() {
			String strike = getStrikethrough();
			String formatted = strike + getName();
			for (TaskTag tag : TaskTag.values()) {
				formatted = formatted.replaceFirst(Pattern.quote("[" + tag.getName() + "]"), ChatColor.AQUA + "[" + tag.getName() + "]" + ChatColor.YELLOW + strike);
			}
			return formatted;
		}

		public String format() {
			return ChatColor.DARK_GRAY + "#" + ChatColor.GOLD + ChatColor.BOLD + getId() + ChatColor.DARK_GRAY + " | " + ChatColor.YELLOW + formatName() + ChatColor.GRAY + " ("
					+ getStatus().toUpperCase() + ")";
		}

		public String getDate() {
			return data.getString("date");
		}

		public User getBy() {
			return cachedBy == null ? cachedBy = userLoader.loadObject(UUID.fromString(data.getString("by"))) : cachedBy;
		}

		public boolean isApproved() {
			return data.getBoolean("approved");
		}

		public User getReviewedBy() {
			String reviewedBy = data.getString("reviewedBy");
			if (reviewedBy == null)
				return null;
			return cachedReviewedBy == null ? cachedReviewedBy = userLoader.loadObject(UUID.fromString(reviewedBy)) : cachedReviewedBy;
		}

		public List<User> getAssignees() {
			return cachedAssignees == null 
					? cachedAssignees = data.getList("assignees", String.class).stream().map(uuid -> userLoader.loadObject(UUID.fromString(uuid))).collect(Collectors.toList())
					: cachedAssignees;
		}

		public boolean isDone() {
			return data.getBoolean("done");
		}

		public boolean isClosed() {
			return data.getBoolean("closed");
		}

		public List<String> getNotes() {
			return data.getList("notes", String.class);
		}

		public Location getLocation() {
			return cachedLocation == null ? cachedLocation = StorageUtil.docToLoc(data.get("location", Document.class)) : cachedLocation;
		}

		public int getStars() {
			return data.getInteger("stars", 0);
		}
		
		public String getStarString() {
			String result = "";
			for(int i = 0; i < getStars(); i++) {
				result += ChatColor.GOLD + STAR;
			}
			for(int i = getStars(); i < DragonsDev.MAX_STARS; i++) {
				result += ChatColor.GRAY + STAR;
			}
			return result;
		}
		
		public void save() {
			taskLoader.updateTask(this);
		}

		public void setName(String name) {
			data.append("name", name);
			save();
			updateMarker();
		}
		
		public void setApproved(boolean approved, User reviewedBy) {
			data.append("approved", approved);
			data.append("reviewedBy", reviewedBy.getUUID().toString());
			cachedReviewedBy = reviewedBy;
			save();
			updateMarker();
		}

		public void addAssignee(User assignee) {
			data.getList("assignees", String.class).add(assignee.getUUID().toString());
			cachedAssignees.add(assignee);
			save();
			updateMarker();
		}

		public void removeAssignee(User assignee) {
			data.getList("assignees", String.class).remove(assignee.getUUID().toString());
			cachedAssignees.remove(assignee);
			save();
			updateMarker();
		}

		public void setDone(boolean done) {
			data.append("done", done);
			save();
			updateMarker();
		}

		public void setClosed(boolean closed) {
			data.append("closed", closed);
			save();
			updateMarker();
		}
		
		public void setStars(int stars) {
			data.append("stars", stars);
			save();
		}

		public void addNote(String note) {
			data.getList("notes", String.class).add(note);
			save();
		}

		public void setLocation(Location loc) {
			data.append("location", StorageUtil.locToDoc(loc));
			cachedLocation = loc;
			save();
			updateMarker();
		}

		public TaskTag[] getTags() {
			return TaskTag.fromTaskName(getName());
		}

		public DiscordRole[] getNotifyRoles() {
			return Arrays.stream(getTags()).map(tag -> tag.getNotifyRole()).collect(Collectors.toList()).toArray(new DiscordRole[] {});
		}

		public Document toDocument() {
			return data;
		}

		public static Task fromDocument(Document document) {
			if (document == null)
				return null;
			return taskPool.computeIfAbsent(document.getInteger("_id"), id -> new Task(document));
		}

		public static Task newTask(String name, User by) {
			Task task = new Task(new Document("_id", taskLoader.reserveNextId()).append("name", name).append("date", Date.from(Instant.now()).toString()).append("by", by.getUUID().toString())
					.append("reviewedBy", null).append("approved", false).append("assignees", new ArrayList<>()).append("done", false).append("closed", false).append("notes", new ArrayList<>())
					.append("location", StorageUtil.locToDoc(by.getPlayer().getLocation())).append("stars", 0));
			taskPool.put(task.getId(), task);
			return task;
		}
	}

	public TaskLoader(Dragons dragons) {
		super(dragons.getMongoConfig(), "tasks", TASK_COLLECTION);
		LOGGER = dragons.getLogger();

		syncPeriodic(() -> {
			renderParity = ++renderParity % 2;
			for (Task task : liveTasks) {
				if(task.clickHandler == null || task.clickHandler.isDead()) {
					LOGGER.debug("Regenerating marker for task #" + task.getId());
					task.updateMarker();
				}
				if(PlayerEventListeners.getRightClickHandlers(task.clickHandler).size() == 0) {
					LOGGER.debug("Re-adding click handler for task #" + task.getId());
					PlayerEventListeners.addRightClickHandler(task.clickHandler, user -> Bukkit.dispatchCommand(user.getPlayer(), "taskinfo " + task.getId()));
				}
				Location loc = task.getLocation();
				Color mainColor = Color.LIME;
				TaskTag[] tags = TaskTag.fromTaskName(task.getName());
				if(tags.length > 0) {
					TaskTag primary = tags[0];
					switch(primary) {
					case BUILD:
						mainColor = Color.BLUE;
						break;
					case GM:
						mainColor = Color.ORANGE;
						break;
					default:
						break;
					}
				}
	
				for (User user : UserLoader.allUsers()) {
					Player player = user.getPlayer();
					if(player == null) continue;
					if(!player.getWorld().equals(task.getLocation().getWorld())) continue;
					Color color = null;
					boolean tm = PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.TASK_MANAGER, false);
					if(!task.isApproved() && !task.isClosed() && tm) {
						color = Color.GRAY;
					}
					else if(task.isDone() && !task.isClosed() && tm) {
						color = Color.PURPLE;
					}
					else if(task.isApproved() && !task.isDone() && task.getAssignees().size() == 0) {
						color = renderParity == 0 ? mainColor : Color.RED;
					}
					else if(task.isApproved() && !task.isDone()) {
						color = mainColor;
					}
					else if(task.getAssignees().contains(user)) {
						color = Color.LIME;
					}
					if(color != null) {
						ParticleUtil.drawSphere(player, Particle.REDSTONE, loc.getX(), loc.getY() + TASK_MARKER_Y_OFFSET, loc.getZ(), 0.4, 2, new DustOptions(color, 0.75f));
					}
				}
			}
		}, 20, 20);
	}
	
	public void clearCache() {
		taskPool.clear();
		liveTasks.clear();
	}

	public List<Task> asTasks(FindIterable<Document> tasks) {
		return tasks.map(doc -> Task.fromDocument(doc)).into(new ArrayList<>());
	}

	public List<Task> asTasks(List<Document> tasks) {
		return asTasks(tasks, ((a, b) -> b.getId() - a.getId()));
	}
	
	public List<Task> asTasks(List<Document> tasks, Comparator<Task> comparator) {
		return tasks.stream().map(doc -> Task.fromDocument(doc)).sorted(comparator).collect(Collectors.toList());
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

	public List<Task> getAllApprovedUnassignedTasks() {
		return asTasks(collection.find(new Document("approved", true).append("done", false).append("assignees", new Document("$size", 0))));
	}

	public List<Task> getAllAssignedInProgressTasks() {
		return asTasks(collection.find(new Document("approved", true).append("done", false).append("assignees", new Document("$not", new Document("$size", 0)))));
	}

	public List<Task> getAllWaitingTasks() {
		return asTasks(collection.find(new Document("reviewedBy", null)));
	}

	public List<Task> getAllInProgressTasks() {
		return asTasks(collection.find(new Document("closed", false).append("approved", true)));
	}

	public List<Task> getAllRejectedTasks() {
		return asTasks(collection.find(new Document("approved", false).append("reviewedBy", new Document("$ne", null))));
	}

	public List<Task> getAllTasksWith(User assigned) {
		return asTasks(collection.find(new Document("assignees", assigned.getUUID().toString()).append("approved", true).append("done", false).append("closed", false)));
	}

	public List<Task> getAllCompletedTasks(boolean complete) {
		return asTasks(collection.find(new Document("done", complete).append("approved", true).append("closed", false)));
	}

	public List<Task> getAllClosedTasks(boolean closed) {
		return asTasks(collection.find(new Document("closed", closed).append("approved", true)));
	}

	public List<Task> searchTasks(String query) {
		return asTasks(collection.find(new Document("$text", new Document("$search", query))));
	}

	public Task getTaskById(int id) {
		return Task.fromDocument(collection.find(new Document("_id", id)).first());
	}

	public void updateTask(Task update) {
		collection.updateOne(new Document("_id", update.getId()), new Document("$set", update.toDocument()));
	}

	public void deleteTask(Task delete) {
		collection.deleteOne(new Document("_id", delete.getId()));
	}

	public Task addTask(User by, String name) {
		Task task = Task.newTask(name, by);
		collection.insertOne(task.toDocument());
		return task;
	}

}
