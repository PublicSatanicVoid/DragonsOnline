package mc.dragons.core.gameobject.region;

import java.util.HashMap;
import java.util.Map;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.loader.FloorLoader;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.storage.StorageUtil;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

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
			this.min = Vector.getMinimum(vec1, vec2).toLocation(world);
			this.max = Vector.getMaximum(vec1, vec2).toLocation(world);
			this.area = (this.max.getX() - this.min.getX()) * (this.max.getZ() - this.min.getZ());
			this.spawnRates = new HashMap<>();
			for (Map.Entry<String, Object> entry : (Iterable<Map.Entry<String, Object>>) ((Document) Region.this.getData("spawnRates")).entrySet())
				this.spawnRates.put(entry.getKey(), Double.valueOf(((Double) entry.getValue()).doubleValue()));
		}

		public Location getMin() {
			return this.min;
		}

		public Location getMax() {
			return this.max;
		}

		public double getArea() {
			return this.area;
		}

		public Map<String, Double> getSpawnRates() {
			return this.spawnRates;
		}
	}

	public Region(StorageManager storageManager, StorageAccess storageAccess) {
		super(GameObjectType.REGION, storageAccess.getIdentifier().getUUID(), storageManager);
		LOGGER.fine("Constructing region (" + storageManager + ", " + storageAccess + ")");
		this.regionData = new CachedRegionData(storageAccess);
		this.floor = FloorLoader.fromWorld(getWorld());
	}

	public Location getMin() {
		return this.regionData.getMin();
	}

	public Location getMax() {
		return this.regionData.getMax();
	}

	public double getArea() {
		return this.regionData.getArea();
	}

	public String getName() {
		return (String) getData("name");
	}

	public Document getFlags() {
		return (Document) getData("flags");
	}

	public void setFlag(String flag, Object value) {
		Document update = this.storageAccess.getDocument();
		Document flags = (Document) update.get("flags");
		flags.append(flag, value);
		this.storageAccess.update(update);
	}

	public Map<String, Double> getSpawnRates() {
		return this.regionData.getSpawnRates();
	}

	public double getSpawnRate(String npcClass) {
		for (Map.Entry<String, Double> entry : this.regionData.getSpawnRates().entrySet()) {
			if (((String) entry.getKey()).equalsIgnoreCase(npcClass))
				return ((Double) entry.getValue()).doubleValue();
		}
		return 0.0D;
	}

	public void setSpawnRate(String npcClass, double spawnRate) {
		Document update = this.storageAccess.getDocument();
		Document spawnRates = (Document) update.get("spawnRates");
		spawnRates.append(npcClass, Double.valueOf(spawnRate));
		this.storageAccess.update(update);
		this.regionData = new CachedRegionData(this.storageAccess);
	}

	public World getWorld() {
		return getMin().getWorld();
	}

	public Floor getFloor() {
		return this.floor;
	}

	public boolean containsXZ(Location test) {
		if (getWorld() != test.getWorld())
			return false;
		return (test.getX() >= getMin().getX() && test.getX() <= getMax().getX() && test.getZ() >= getMin().getZ() && test.getZ() <= getMax().getZ());
	}

	public boolean containsXYZ(Location test) {
		return (containsXZ(test) && test.getY() >= getMin().getY() && test.getY() <= getMax().getY());
	}

	public boolean contains(Location test) {
		if (Boolean.valueOf(getFlags().getString("3d")).booleanValue())
			return containsXYZ(test);
		return containsXZ(test);
	}

	public void updateCorners(Location corner1, Location corner2) {
		if (corner1.getWorld() != corner2.getWorld())
			throw new IllegalArgumentException("Corners must be in the same world");
		this.storageAccess.set("world", corner1.getWorld().getName());
		this.storageAccess.set("corner1", StorageUtil.vecToDoc(corner1.toVector()));
		this.storageAccess.set("corner2", StorageUtil.vecToDoc(corner2.toVector()));
		this.regionData = new CachedRegionData(this.storageAccess);
		this.floor = FloorLoader.fromWorld(getWorld());
	}
}
