package mc.dragons.core.gameobject.region;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.storage.StorageUtil;

/**
 * Represents a named, cubic region in a world. 
 * 
 * Regions are used to tie specific functionality
 * to certain areas, as well as notify players 
 * where they are.
 * 
 * @author Adam
 *
 */
public class Region extends GameObject {
	private CachedRegionData regionData;
	private Floor floor;

	private class CachedRegionData {
		private Location min;
		private Location max;
		private double area;
		private Map<String, Double> spawnRates;

		public CachedRegionData(StorageAccess storageAccess) {
			World world = Bukkit.getWorld((String) storageAccess.get("world"));
			Document doc1 = (Document) storageAccess.get("corner1");
			Document doc2 = (Document) storageAccess.get("corner2");
			Vector vec1 = StorageUtil.docToVec(doc1);
			Vector vec2 = StorageUtil.docToVec(doc2);
			min = Vector.getMinimum(vec1, vec2).toLocation(world);
			max = Vector.getMaximum(vec1, vec2).toLocation(world);
			area = (max.getX() - min.getX()) * (max.getZ() - min.getZ());
			spawnRates = new HashMap<>();
			for (Entry<String, Object> entry : (Iterable<Entry<String, Object>>) ((Document) Region.this.getData("spawnRates")).entrySet()) {
				spawnRates.put(entry.getKey(), Double.valueOf((double) entry.getValue()));
			}
		}

		public Location getMin() {
			return min;
		}

		public Location getMax() {
			return max;
		}

		public double getArea() {
			return area;
		}

		public Map<String, Double> getSpawnRates() {
			return spawnRates;
		}
	}

	public Region(StorageManager storageManager, StorageAccess storageAccess) {
		super(GameObjectType.REGION, storageAccess.getIdentifier().getUUID(), storageManager);
		LOGGER.fine("Constructing region (" + storageManager + ", " + storageAccess + ")");
		regionData = new CachedRegionData(storageAccess);
		floor = FloorLoader.fromWorld(getWorld());
	}

	public Location getMin() {
		return regionData.getMin();
	}

	public Location getMax() {
		return regionData.getMax();
	}

	public double getArea() {
		return regionData.getArea();
	}

	public String getName() {
		return (String) getData("name");
	}

	public Document getFlags() {
		return (Document) getData("flags");
	}

	public void setFlag(String flag, Object value) {
		Document update = storageAccess.getDocument();
		Document flags = (Document) update.get("flags");
		flags.append(flag, value);
		storageAccess.update(update);
	}

	public Map<String, Double> getSpawnRates() {
		return regionData.getSpawnRates();
	}

	public double getSpawnRate(String npcClass) {
		for (Entry<String, Double> entry : regionData.getSpawnRates().entrySet()) {
			if (entry.getKey().equalsIgnoreCase(npcClass)) {
				return entry.getValue();
			}
		}
		return 0.0D;
	}

	public void setSpawnRate(String npcClass, double spawnRate) {
		Document update = storageAccess.getDocument();
		Document spawnRates = (Document) update.get("spawnRates");
		spawnRates.append(npcClass, Double.valueOf(spawnRate));
		storageAccess.update(update);
		regionData = new CachedRegionData(storageAccess);
	}

	public World getWorld() {
		return getMin().getWorld();
	}

	public Floor getFloor() {
		return floor;
	}

	public boolean containsXZ(Location test) {
		if (getWorld() != test.getWorld()) {
			return false;
		}
		return test.getX() >= getMin().getX() && test.getX() <= getMax().getX() && test.getZ() >= getMin().getZ() && test.getZ() <= getMax().getZ();
	}

	public boolean containsXYZ(Location test) {
		return containsXZ(test) && test.getY() >= getMin().getY() && test.getY() <= getMax().getY();
	}

	public boolean contains(Location test) {
		if (Boolean.valueOf(getFlags().getString("3d")).booleanValue()) {
			return containsXYZ(test);
		}
		return containsXZ(test);
	}

	public void updateCorners(Location corner1, Location corner2) {
		if (corner1.getWorld() != corner2.getWorld()) {
			throw new IllegalArgumentException("Corners must be in the same world");
		}
		storageAccess.set("world", corner1.getWorld().getName());
		storageAccess.set("corner1", StorageUtil.vecToDoc(corner1.toVector()));
		storageAccess.set("corner2", StorageUtil.vecToDoc(corner2.toVector()));
		regionData = new CachedRegionData(storageAccess);
		floor = FloorLoader.fromWorld(getWorld());
	}
}
