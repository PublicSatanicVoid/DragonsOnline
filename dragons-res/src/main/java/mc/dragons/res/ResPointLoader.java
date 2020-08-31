package mc.dragons.res;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.material.Door;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import com.mongodb.client.FindIterable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.storage.impl.MongoConfig;
import mc.dragons.core.storage.impl.loader.AbstractLightweightLoader;
import mc.dragons.core.util.HologramUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.res.ResPointLoader.ResPoint;

public class ResPointLoader extends AbstractLightweightLoader<ResPoint> {

	private static final String RES_POINT_COLLECTION = "res_points";
	
	private Map<String, ResPoint> resPoints;
	
	public ResPointLoader(MongoConfig config) {
		super(config, "#unused#", RES_POINT_COLLECTION);
		resPoints = new LinkedHashMap<>();
	}

	public static class ResPoint {
		private String name;
		private String displayName;
		private double price;
		private Location doorLocation;
		
		private ArmorStand[] ownedHolograms;
		private ArmorStand[] notOwnedHolograms;
		
		private static ResPointLoader resPointLoader;

		private static void lazyLoadResPointLoader() {
			if(resPointLoader == null) {
				resPointLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(ResPointLoader.class);
			}
		}
		
		public ResPoint(String name, String displayName, double price, Location doorLocation) {
			this.name = name;
			this.displayName = displayName;
			this.price = price;
			this.doorLocation = doorLocation;
			lazyLoadResPointLoader();
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
		
		public ArmorStand[] getOwnedHolograms() {
			return ownedHolograms;
		}
		
		public ArmorStand[] getNotOwnedHolograms() {
			return notOwnedHolograms;
		}
		
		public void save() {
			resPointLoader.collection.updateOne(new Document("name", name), new Document("$set", toDocument()));
		}
		
		public void setDisplayName(String displayName) {
			this.displayName = displayName;
			save();
		}
		
		public void setPrice(double price) {
			this.price = price;
			save();
		}
		
		public void setHolograms(ArmorStand[] owned, ArmorStand[] notOwned) {
			this.ownedHolograms = owned;
			this.notOwnedHolograms = notOwned;
		}
		
		public Document toDocument() {
			return new Document("name", name).append("displayName", displayName).append("price", price).append("doorLocation", StorageUtil.locToDoc(doorLocation));
		}
		
		public static ResPoint fromDocument(Document document) {
			lazyLoadResPointLoader();
			return resPointLoader.resPoints.computeIfAbsent(document.getString("name"), n -> 
				 new ResPoint(document.getString("name"), document.getString("displayName"), document.getDouble("price"), 
						StorageUtil.docToLoc(document.get("doorLocation", Document.class)))
			);
		}
	};

	public ResPoint addResPoint(String name, String displayName, double price, Location doorLocation) {
		ResPoint resPoint = new ResPoint(name, displayName, price, doorLocation.getBlock().getLocation());
		collection.insertOne(resPoint.toDocument());
		createResPointHologram(resPoint);
		resPoints.put(name, resPoint);
		return resPoint;
	}
	
	public ResPoint getResPointByName(String name) {
		return resPoints.computeIfAbsent(name, n -> {
			FindIterable<Document> result = collection.find(new Document("name", name));
			if(result.first() == null) return null;
			return ResPoint.fromDocument(result.first());
		});
	}
	
	public ResPoint getResPointByDoorLocation(Location doorLocation) {
		FindIterable<Document> result = collection.find(
				new Document("doorLocation", StorageUtil.locToDoc(doorLocation.getBlock().getLocation())));
		if(result.first() == null) return null;
		return ResPoint.fromDocument(result.first());
	}
	
	public void loadAllResPoints() {
		FindIterable<Document> dbResult = collection.find();
		for(Document doc : dbResult) {
			resPoints.put(doc.getString("name"), ResPoint.fromDocument(doc));
		}
	}
	
	public Collection<ResPoint> getAllResPoints() {
		return resPoints.values();
	}
	
	public void deleteResPoint(String name) {
		collection.deleteOne(new Document("name", name));
		resPoints.remove(name);
	}
	
	public void createResPointHologram(ResPoint resPoint) {
		Location doorLoc = resPoint.getDoorLocation();
		MaterialData materialData = doorLoc.getBlock().getState().getData();
		if(!(materialData instanceof Door)) {
			JavaPlugin.getPlugin(DragonsResPlugin.class).getLogger().warning("Could not load res point at " + StringUtil.locToString(doorLoc));
			HologramUtil.makeHologram(ChatColor.RED + "Invalid res point!", doorLoc.add(0, 1, 0));
			return;
		}
		Door door = (Door) materialData;
		Location holoLoc = doorLoc.add((door.getFacing().getModX() - door.getFacing().getModZ()) * 0.5, 0.5, (door.getFacing().getModZ() - door.getFacing().getModX()) * 0.5);
		JavaPlugin.getPlugin(DragonsResPlugin.class).getLogger().info("Door at " + StringUtil.locToString(holoLoc) + " has direction " + door.getFacing().getModX() + ", " + door.getFacing().getModZ());
		ArmorStand[] notOwnedHologram = {
				HologramUtil.makeHologram(ChatColor.GRAY + "Price: " + ChatColor.YELLOW + resPoint.getPrice() + "g", holoLoc),
				HologramUtil.makeHologram(ChatColor.GOLD + resPoint.getDisplayName(), holoLoc.clone().add(0, 0.3, 0)) 
		};
		ArmorStand[] ownedHologram = {
				HologramUtil.makeHologram(ChatColor.GREEN + "You own this residence", holoLoc),
				HologramUtil.makeHologram(ChatColor.DARK_GREEN + resPoint.getDisplayName(), holoLoc.clone().add(0, 0.3, 0))
		};
		Dragons.getInstance().getLogger().info("SETTING RES POINT HOLOGRAMS FOR " + resPoint.getName());
		Dragons.getInstance().getLogger().info("-Owned=" + ownedHologram);
		Dragons.getInstance().getLogger().info("-NotOwned=" + notOwnedHologram);
		resPoint.setHolograms(ownedHologram, notOwnedHologram);
	}
	
	public void updateResHologramOn(User user, ResPoint resPoint) {
		if(user.getPlayer() == null) return;
		if(!user.getPlayer().isOnline()) return;
		boolean owned = !Dragons.getInstance().getLightweightLoaderRegistry().getLoader(ResLoader.class).getAllResidencesOf(user, resPoint).isEmpty();
		ArmorStand[] show = null;
		ArmorStand[] hide = null;
		if(owned) {
			show = resPoint.getOwnedHolograms();
			hide = resPoint.getNotOwnedHolograms();
		}
		else {
			show = resPoint.getNotOwnedHolograms();
			hide = resPoint.getOwnedHolograms();
		}
		for(ArmorStand shown : show) {
			Dragons.getInstance().getEntityHider().showEntity(user.getPlayer(), shown);
		}
		for(ArmorStand hidden : hide) {
			Dragons.getInstance().getEntityHider().hideEntity(user.getPlayer(), hidden);
		}
	}
}
