package mc.dragons.res;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import com.mongodb.client.FindIterable;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.storage.loader.AbstractLightweightLoader;
import mc.dragons.core.storage.mongo.MongoConfig;
import mc.dragons.res.ResLoader.Residence;
import mc.dragons.res.ResLoader.Residence.ResAccess;
import mc.dragons.res.ResPointLoader.ResPoint;

public class ResLoader extends AbstractLightweightLoader<Residence> {
	private static final String RES_COUNTER = "res";
	private static final String RES_COLLECTION = "res";

	private static int RES_SPACING = 5;
	private static Vector RES_SPAWN_OFFSET = new Vector(0, 1, -12);
	
	private static int resNextIndex = 0;
	private static Map<Integer, Integer> resIdToLocalIndex = new HashMap<>();
	
	private static BukkitWorld resWorld = new BukkitWorld(Bukkit.getWorld("res_temp"));
	private static Clipboard residenceSchematic = DragonsResPlugin.loadSchematic("Residence_1", resWorld);
	private static EditSession worldEditSession = DragonsResPlugin.getEditSession(resWorld);
	
	private static List<Integer> generated = new ArrayList<>();
	
	public ResLoader(MongoConfig config) {
		super(config, RES_COUNTER, RES_COLLECTION);
	}

	public static class Residence {
		
		public static enum ResAccess {
			PRIVATE,
			FRIENDS_ONLY,
			GUILD_ONLY,
			ALL
		};
		
		private int id;
		private ResPoint resPoint;
		private boolean locked;
		private User owner;
		private Document properties;
		private ResAccess access;
		
		public Residence(int id, ResPoint resPoint, boolean locked, User owner, Document properties, ResAccess access) {
			this.id = id;
			this.resPoint = resPoint;
			this.locked = locked;
			this.owner = owner;
			this.properties = properties;
			this.access = access;
		}
		
		public int getId() { return id; }
		public ResPoint getResPoint() { return resPoint; }
		public boolean isLocked() { return locked; }
		public User getOwner() { return owner; }
		public Document getProperties() { return properties; }
		public ResAccess getAccessLevel() { return access; }
		
		public void save() { Dragons.getInstance().getLightweightLoaderRegistry().getLoader(ResLoader.class).updateResidence(this); }
		
		public void setAccessLevel(ResAccess access) {
			this.access = access;
			save();
		}
		
		public void setLocked(boolean locked) {
			this.locked = locked;
			save();
		}
		
		public Document toDocument() {
			return new Document("_id", id).append("owner", owner.getUUID()).append("resPoint", resPoint.getName()).append("access", access.toString())
					.append("properties", properties).append("locked", locked);
		}
		
		public static Residence fromDocument(Document document) {
			if(document == null) return null;
			return new Residence(document.getInteger("_id"), Dragons.getInstance().getLightweightLoaderRegistry().getLoader(ResPointLoader.class)
					.getResPointByName(document.getString("resPoint")), document.getBoolean("locked"), 
					GameObjectType.USER.<User, UserLoader>getLoader().loadObject(document.get("owner", UUID.class)), document.get("properties", Document.class), 
					ResAccess.valueOf(document.getString("access")));
		}
	}
	
	public List<Residence> asResidences(FindIterable<Document> tasks) {
		List<Document> result = new ArrayList<>();
		for(Document task : tasks) {
			result.add(task);
		}
		return asResidences(result);
	}
	
	public List<Residence> asResidences(List<Document> tasks) {
		return tasks.stream().map(doc -> Residence.fromDocument(doc)).sorted((a, b) -> a.getId() - b.getId()).collect(Collectors.toList());
	}
	
	public List<Residence> getAllResidencesOf(User owner) {
		return asResidences(collection.find(new Document("owner", owner.getUUID())));
	}

	public List<Residence> getAllResidencesOf(User owner, ResPoint resPoint) {
		return asResidences(collection.find(new Document("owner", owner.getUUID()).append("resPoint", resPoint.getName())));
	}
	
	public Residence getResidenceById(int id) {
		return Residence.fromDocument(collection.find(new Document("_id", id)).first());
	}
	
	public void updateResidence(Residence update) {
		collection.updateOne(new Document("_id", update.getId()), new Document("$set", update.toDocument()));
	}
	
	public Residence addResidence(User owner, ResPoint resPoint, ResAccess access) {
		int id = reserveNextId();
		Residence task = new Residence(id, resPoint, false, owner, new Document(), access);
		collection.insertOne(task.toDocument());
		return task;
	}

	public void deleteResidence(int id) {
		collection.deleteOne(new Document("_id", id));
	}
	
	public void removeResidenceLocally(int id) {
		resIdToLocalIndex.remove(id);
	}
	
	public Location generateResidence(int id) {
		int index = resIdToLocalIndex.computeIfAbsent(id, u -> resNextIndex++);
		World resWorld = Bukkit.getWorld("res_temp");
		Location corner = resWorld.getSpawnLocation().clone().add((residenceSchematic.getDimensions().getX() + RES_SPACING) * index, 0, 0);
		if(!generated.contains(id)) {			
			DragonsResPlugin.pasteSchematic(residenceSchematic, worldEditSession, corner);
			// TODO apply properties to load customizations
			generated.add(id);
		}
		return corner.add(RES_SPAWN_OFFSET);
	}
	
	public void goToResidence(User user, int id, boolean bypass) {
		Residence res = getResidenceById(id);
		if(res.isLocked() && !bypass) {
			user.getPlayer().sendMessage(ChatColor.RED + "This residence has been locked by an administrator.");
			return;
		}				
		boolean canAccess = bypass;
		ResAccess access = res.getAccessLevel();
		if(access == ResAccess.ALL) {
			canAccess = true;
		}
		if(access == ResAccess.PRIVATE && res.getOwner().getIdentifier().equals(user.getIdentifier())) {
			canAccess = true;
		}
		if(!canAccess) {
			user.getPlayer().sendMessage(ChatColor.RED + "You do not have access to this residence! (Guild-only and friend-only status doesn't work yet)");
			return;
		}
		Location spawn = generateResidence(id);
		
		// send user to residence
		if(!user.getPlayer().getWorld().getName().equals("res_temp")) {
			user.getStorageAccess().set("resExitTo", StorageUtil.locToDoc(user.getPlayer().getLocation()));
		}
		user.getStorageAccess().set("lastResId", id);
		user.getPlayer().teleport(spawn);
		user.getPlayer().sendMessage(ChatColor.GREEN + "Welcome to your residence (ID #" + id + ")");
	}
}
