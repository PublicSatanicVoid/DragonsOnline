package mc.dragons.tools.content;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bukkit.ChatColor;

import mc.dragons.core.Dragons;
import mc.dragons.core.storage.loader.AbstractLightweightLoader;
import mc.dragons.tools.content.COLoader.CommunityObjective;

public class COLoader extends AbstractLightweightLoader<CommunityObjective> {
	private List<CommunityObjective> cache;
	private Dragons dragons;
	
	public static enum COStatus {
		FAILED,
		COMPLETED,
		UNLOCKED,
		LOCKED
	}
	
	public static class CommunityObjective {
		private COLoader loader;
		private Document data;
		
		protected CommunityObjective(COLoader loader, Document data) {
			this.loader = loader;
			this.data = data;
			loader.cache.add(this);
		}
		
		public int getId() {
			return data.getInteger("_id");
		}
		
		public String getTitle() {
			return data.getString("title");
		}
		
		public String getDescription() {
			return data.getString("description");
		}
		
		public Date getActivatedOn() {
			return new Date(data.getLong("activatedOn"));
		}
		
		public Date getCompletedOn() {
			return new Date(data.getLong("completedOn"));
		}
		
		public COStatus getStatus() {
			return COStatus.valueOf(data.getString("status"));
		}
		
		public void setStatus(COStatus status) {
			data.append("status", status.toString());
			switch(status) {
			case LOCKED:
				loader.dragons.getInternalMessageHandler().broadcastRawMsgHoverable(
						ChatColor.RED + "" + ChatColor.BOLD + "Community Objective has been locked: " + 
						ChatColor.RED + getTitle(), getDescription());
				break;
			case UNLOCKED:
				loader.dragons.getInternalMessageHandler().broadcastRawMsgHoverable(
						ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Community Objective Unlocked: " +
						ChatColor.LIGHT_PURPLE + getTitle(), getDescription());
				data.append("activatedOn", System.currentTimeMillis());
				break;
			case COMPLETED:
				loader.dragons.getInternalMessageHandler().broadcastRawMsgHoverable(
						ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Community Objective Completed: " +
						ChatColor.GREEN + getTitle(), getDescription());
				data.append("completedOn", System.currentTimeMillis());
				break;
			case FAILED:
				loader.dragons.getInternalMessageHandler().broadcastRawMsgHoverable(
						ChatColor.DARK_RED + "" + ChatColor.BOLD + "Community Objective Failed: " +
						ChatColor.RED + getTitle(), getDescription());
				break;
			default:
				break;
			}
			save();
		}
		
		private void save() {
			loader.collection.updateOne(new Document("_id", getId()), new Document("$set", data));
			loader.dragons.getInternalMessageHandler().broadcastConsoleCmd("reloadobjectives");
		}
	}
	
	public COLoader(Dragons dragons) {
		super(dragons.getMongoConfig(), "communityObjectives", "communityObjectives");
		this.dragons = dragons;
		reloadObjectives();
	}
	
	public CommunityObjective newObjective(String title, String description) {
		Document data = new Document("_id", reserveNextId()).append("title", title)
				.append("description", description).append("status", COStatus.LOCKED.toString())
				.append("activatedOn", 0L).append("completedOn", 0L);
		collection.insertOne(data);
		return new CommunityObjective(this, data);
	}
	
	public List<CommunityObjective> getAllObjectives() {
		return cache;
	}
	
	public List<CommunityObjective> getActiveObjectives() {
		return cache.stream().filter(o -> o.getStatus() == COStatus.UNLOCKED).toList();
	}
	
	public List<CommunityObjective> getCompletedObjectives() {
		return cache.stream().filter(o -> o.getStatus() == COStatus.COMPLETED).toList();
	}
	
	public List<CommunityObjective> getPublicObjectives() {
		return cache.stream().filter(o -> o.getStatus() != COStatus.LOCKED).toList();
	}
	
	public CommunityObjective getObjectiveById(int id) {
		return cache.stream().filter(o -> o.getId() == id).findFirst().orElseGet(() -> null);
	}
	
	public void deleteObjective(CommunityObjective objective) {
		collection.deleteOne(new Document("_id", objective.getId()));
		cache.remove(objective);
		dragons.getInternalMessageHandler().broadcastConsoleCmd("reloadobjectives");
	}
	
	public void reloadObjectives() {
		cache = new ArrayList<>();
		collection.find().map(data -> new CommunityObjective(this, data)).into(new ArrayList<>());
	}
}
