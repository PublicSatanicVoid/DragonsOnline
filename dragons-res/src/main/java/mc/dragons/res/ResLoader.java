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
import org.bukkit.material.Door;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.storage.impl.MongoConfig;
import mc.dragons.core.util.HologramUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.res.ResLoader.Residence.ResAccess;

public class ResLoader {
	
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
		
		public void save() { ResLoader.updateResidence(this); }
		
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
			return new Residence(document.getInteger("_id"), getResPointByName(document.getString("resPoint")), document.getBoolean("locked"), 
					GameObjectType.USER.<User, UserLoader>getLoader().loadObject(document.get("owner", UUID.class)), document.get("properties", Document.class), ResAccess.valueOf(document.getString("access")));
		}
	}
	
	private static MongoDatabase database;
	private static MongoCollection<Document> resCollection;
	private static MongoCollection<Document> resPointCollection;
	
	private static final String RES_COLLECTION = "res";
	private static final String RES_POINT_COLLECTION = "res_points";
	
	private static int resNextIndex = 0;
	private static Map<Integer, Integer> resIdToLocalIndex = new HashMap<>();
	//private static int RES_WIDTH = 16;
	//private static int RES_LENGTH = 16;
	//private static int RES_HEIGHT = 10;
	private static int RES_SPACING = 5;
	private static Vector RES_SPAWN_OFFSET = new Vector(0, 1, -12);
	
	private static BukkitWorld resWorld = new BukkitWorld(Bukkit.getWorld("res_temp"));
	private static Clipboard residenceSchematic = DragonsResPlugin.loadSchematic("Residence_1", resWorld);
	private static EditSession worldEditSession = DragonsResPlugin.getEditSession(resWorld);
	
	private static List<Integer> generated = new ArrayList<>();
	
	static {
		database = MongoConfig.getDatabase();
		resCollection = database.getCollection(RES_COLLECTION);
		resPointCollection = database.getCollection(RES_POINT_COLLECTION);
	}

	public static List<Residence> asResidences(FindIterable<Document> tasks) {
		List<Document> result = new ArrayList<>();
		for(Document task : tasks) {
			result.add(task);
		}
		return asResidences(result);
	}
	
	public static List<Residence> asResidences(List<Document> tasks) {
		return tasks.stream().map(doc -> Residence.fromDocument(doc)).sorted((a, b) -> a.getId() - b.getId()).collect(Collectors.toList());
	}
	
	public static List<Residence> getAllResidencesOf(User owner) {
		return asResidences(resCollection.find(new Document("owner", owner.getUUID())));
	}

	public static List<Residence> getAllResidencesOf(User owner, ResPoint resPoint) {
		return asResidences(resCollection.find(new Document("owner", owner.getUUID()).append("resPoint", resPoint.getName())));
	}
	
	public static Residence getResidenceById(int id) {
		return Residence.fromDocument(resCollection.find(new Document("_id", id)).first());
	}
	
	public static void updateResidence(Residence update) {
		resCollection.updateOne(new Document("_id", update.getId()), new Document("$set", update.toDocument()));
	}
	
	public static Residence addResidence(User owner, ResPoint resPoint, ResAccess access) {
		int id = MongoConfig.getCounter().reserveNextId("res");
		Residence task = new Residence(id, resPoint, false, owner, new Document(), access);
		resCollection.insertOne(task.toDocument());
		return task;
	}

	public static void deleteResidence(int id) {
		resCollection.deleteOne(new Document("_id", id));
	}
	
	public static void removeResidenceLocally(int id) {
		resIdToLocalIndex.remove(id);
	}
	
	public static Location generateResidence(int id) {
		int index = resIdToLocalIndex.computeIfAbsent(id, u -> resNextIndex++);
		World resWorld = Bukkit.getWorld("res_temp");
		Location corner = resWorld.getSpawnLocation().clone().add((residenceSchematic.getDimensions().getX() + RES_SPACING) * index, 0, 0);
		if(!generated.contains(id)) {
			/*Location otherCorner = corner.clone().add(RES_WIDTH, 0, RES_LENGTH);
			JavaPlugin.getPlugin(DragonsResPlugin.class).getLogger().fine("Generating residence ID #" + id + " index " + (index + 1) + " from "
					+ StringUtil.locToString(corner) + " -> " + StringUtil.locToString(otherCorner));
			// TODO use a schematic instead of this stupid math
			Location buf1 = corner.clone();
			Location buf2 = corner.clone();
			Location buf3 = otherCorner.clone();
			Location buf4 = otherCorner.clone();
			Location buf5 = corner.clone().add(1, 0, 1);			
			for(int x = 0; x < RES_WIDTH; x++) {
				for(int z = 0; z < RES_LENGTH; z++) {
					buf5.getBlock().setType(Material.WOOD);
					buf5.getBlock().getRelative(BlockFace.UP).setType(Material.CARPET);
					buf5.getBlock().getRelative(BlockFace.UP, RES_HEIGHT).setType(Material.WOOD);
					buf5.add(0, 0, 1);
				}
				buf5.setZ(corner.getZ());
				buf5.add(1, 0, 0);
			}
			for(int x = 0; x < RES_WIDTH; x++) {
				for(int y = 0; y < RES_HEIGHT; y++) {
					buf1.getBlock().setType(Material.WOOD);
					buf3.getBlock().setType(Material.WOOD);
					buf1.add(0, 1, 0);
					buf3.add(0, 1, 0);
					if(y == 2 && x == RES_WIDTH / 2) {
						buf1.getBlock().getRelative(BlockFace.DOWN).setType(Material.BIRCH_DOOR, false);
						buf1.getBlock().setType(Material.BIRCH_DOOR, false);
						((Door) buf1.getBlock().getState().getData()).setTopHalf(true);
						buf1.getBlock().getState().update(true);
						buf1.getBlock().getRelative(BlockFace.DOWN).getState().update(true);
					}
				}
				buf1.setY(corner.getY());
				buf1.add(1, 0, 0);
				buf3.setY(corner.getY());
				buf3.add(-1, 0, 0);
			}
			for(int z = 0; z < RES_LENGTH; z++) {
				for(int y = 0; y < RES_HEIGHT; y++) {
					buf2.getBlock().setType(Material.WOOD);
					buf4.getBlock().setType(Material.WOOD);
					buf2.add(0, 1, 0);
					buf4.add(0, 1, 0);
				}
				buf2.add(0, 0, 1);
				buf2.setY(corner.getY());
				buf4.add(0, 0, -1);
				buf4.setY(corner.getY());
			}*/
			
			DragonsResPlugin.pasteSchematic(residenceSchematic, worldEditSession, corner);
			
			// TODO apply properties to load customizations
			generated.add(id);
		}
		return corner.add(RES_SPAWN_OFFSET);
	}
	
	public static void goToResidence(User user, int id, boolean bypass) {
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
	
	public static int getLatestResId() {
		return MongoConfig.getCounter().getCurrentId("res");
	}

	
	public static class ResPoint {
		private String name;
		private String displayName;
		private double price;
		private Location doorLocation;
		
		public ResPoint(String name, String displayName, double price, Location doorLocation) {
			this.name = name;
			this.displayName = displayName;
			this.price = price;
			this.doorLocation = doorLocation;
		}
		
		public String getName() {
			return name;
		}
		
		public String getDisplayName() {
			return displayName;
		}
		
		public double getPrice() {
			return price;
		}
		
		public Location getDoorLocation() {
			return doorLocation;
		}
		
		public void save() {
			resPointCollection.updateOne(new Document("name", name), new Document("$set", toDocument()));
		}
		
		public void setDisplayName(String displayName) {
			this.displayName = displayName;
			save();
		}
		
		public void setPrice(double price) {
			this.price = price;
			save();
		}
		
		public Document toDocument() {
			return new Document("name", name).append("displayName", displayName).append("price", price).append("doorLocation", StorageUtil.locToDoc(doorLocation));
		}
		
		public static ResPoint fromDocument(Document document) {
			return new ResPoint(document.getString("name"), document.getString("displayName"), document.getDouble("price"), 
					StorageUtil.docToLoc(document.get("doorLocation", Document.class)));
		}
	};
	
	public static ResPoint addResPoint(String name, String displayName, double price, Location doorLocation) {
		ResPoint resPoint = new ResPoint(name, displayName, price, doorLocation.getBlock().getLocation());
		resPointCollection.insertOne(resPoint.toDocument());
		createResPointHologram(resPoint);
		return resPoint;
	}
	
	public static ResPoint getResPointByName(String name) {
		FindIterable<Document> result = resPointCollection.find(new Document("name", name));
		if(result.first() == null) return null;
		return ResPoint.fromDocument(result.first());
	}
	
	public static ResPoint getResPointByDoorLocation(Location doorLocation) {
		FindIterable<Document> result = resPointCollection.find(
				new Document("doorLocation", StorageUtil.locToDoc(doorLocation.getBlock().getLocation())));
		if(result.first() == null) return null;
		return ResPoint.fromDocument(result.first());
	}
	
	public static List<ResPoint> getAllResPoints() {
		List<ResPoint> result = new ArrayList<>();
		FindIterable<Document> dbResult = resPointCollection.find();
		for(Document doc : dbResult) {
			result.add(ResPoint.fromDocument(doc));
		}
		return result;
	}
	
	public static void deleteResPoint(String name) {
		resPointCollection.deleteOne(new Document("name", name));
	}
	
	public static void createResPointHologram(ResPoint resPoint) {
		Location doorLoc = resPoint.getDoorLocation();
		Door door = (Door) doorLoc.getBlock().getState().getData();
		Location holoLoc = doorLoc.add((door.getFacing().getModX() - door.getFacing().getModZ()) * 0.5, 0.5, (door.getFacing().getModZ() - door.getFacing().getModX()) * 0.5);
		JavaPlugin.getPlugin(DragonsResPlugin.class).getLogger().info("Door at " + StringUtil.locToString(holoLoc) + " has direction " + door.getFacing().getModX() + ", " + door.getFacing().getModZ());
		HologramUtil.makeHologram(ChatColor.GRAY + "Price: " + ChatColor.YELLOW + resPoint.getPrice() + "g", holoLoc);
		HologramUtil.makeHologram(ChatColor.GOLD + resPoint.getDisplayName(), holoLoc.add(0, 0.3, 0));
//		ArmorStand holo = (ArmorStand) holoLoc.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
//		holo.setMetadata("allow", new FixedMetadataValue(JavaPlugin.getPlugin(DragonsResPlugin.class), true));
//		holo.setCustomNameVisible(true);
//		holo.setCustomName(ChatColor.DARK_GREEN + "Res Price: " + ChatColor.GREEN + resPoint.getPrice() + "g");
//		holo.setGravity(false);
//		holo.setInvulnerable(true);
//		holo.setCollidable(false);
//		holo.setVisible(false);
	}
}
