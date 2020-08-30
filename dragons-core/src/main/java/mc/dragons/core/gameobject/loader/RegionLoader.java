package mc.dragons.core.gameobject.loader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.util.StringUtil;
import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.World;

public class RegionLoader extends GameObjectLoader<Region> {
	private static RegionLoader INSTANCE;

	private Logger LOGGER = Dragons.getInstance().getLogger();

	private boolean allLoaded = false;

	private GameObjectRegistry masterRegistry;

	private Map<String, Set<Region>> worldToRegions;

	private RegionLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		this.masterRegistry = instance.getGameObjectRegistry();
		this.worldToRegions = new HashMap<>();
	}

	public static synchronized RegionLoader getInstance(Dragons instance, StorageManager storageManager) {
		if (INSTANCE == null)
			INSTANCE = new RegionLoader(instance, storageManager);
		return INSTANCE;
	}

	public Region loadObject(StorageAccess storageAccess) {
		lazyLoadAll();
		this.LOGGER.fine("Loading region " + storageAccess.getIdentifier());
		return new Region(this.storageManager, storageAccess);
	}

	public Region getRegionByName(String name) {
		lazyLoadAll();
		for (GameObject gameObject : this.masterRegistry.getRegisteredObjects(new GameObjectType[] { GameObjectType.REGION })) {
			Region region = (Region) gameObject;
			if (region.getName().equalsIgnoreCase(name))
				return region;
		}
		return null;
	}

	public Set<Region> getRegionsByWorld(World world) {
		return getRegionsByWorld(world.getName());
	}

	public Set<Region> getRegionsByWorld(String worldName) {
		return this.worldToRegions.getOrDefault(worldName, new HashSet<>());
	}

	public Set<Region> getRegionsByLocation(Location loc) {
		lazyLoadAll();
		Set<Region> result = new HashSet<>();
		for (Region region : getRegionsByWorld(loc.getWorld())) {
			if (region.contains(loc))
				result.add(region);
		}
		return result;
	}

	public Region getSmallestRegionByLocation(Location loc, boolean includeHidden) {
		lazyLoadAll();
		Set<Region> candidates = getRegionsByLocation(loc);
		if (candidates.isEmpty())
			return null;
		Optional<Region> result = candidates.stream().filter(r -> includeHidden ? true : (!Boolean.parseBoolean(r.getFlags().getString("hidden"))))
				.sorted((a, b) -> (a.getArea() > b.getArea()) ? 1 : -1).findFirst();
		if (result.isEmpty())
			return null;
		return result.get();
	}

	@Deprecated
	public Set<Region> getRegionsByLocationXZ(Location loc) {
		lazyLoadAll();
		Set<Region> result = new HashSet<>();
		for (Region region : getRegionsByWorld(loc.getWorld())) {
			if (region.containsXZ(loc))
				result.add(region);
		}
		return result;
	}

	public Region registerNew(String name, Location corner1, Location corner2) {
		lazyLoadAll();
		this.LOGGER.fine("Registering new region " + name + " (" + StringUtil.locToString(corner1) + " [" + corner1.getWorld().getName() + "] -> " + StringUtil.locToString(corner2) + " ["
				+ corner2.getWorld().getName() + "]");
		if (corner1.getWorld() != corner2.getWorld())
			throw new IllegalArgumentException("Corners must be in the same world");
		StorageAccess storageAccess = this.storageManager.getNewStorageAccess(GameObjectType.REGION);
		storageAccess.set("name", name);
		storageAccess.set("world", corner1.getWorld().getName());
		storageAccess.set("corner1", StorageUtil.vecToDoc(corner1.toVector()));
		storageAccess.set("corner2", StorageUtil.vecToDoc(corner2.toVector()));
		storageAccess.set("flags", (new Document("fullname", "New Region")).append("desc", "").append("lvmin", "0").append("lvrec", "0").append("showtitle", "false").append("allowhostile", "true")
				.append("pvp", "true").append("pve", "true").append("hidden", "false").append("spawncap", "-1").append("nospawn", "false").append("3d", "false").append("nearbyspawncap", "-1"));
		storageAccess.set("spawnRates", new Document());
		Region region = new Region(this.storageManager, storageAccess);
		this.masterRegistry.getRegisteredObjects().add(region);
		Set<Region> regions = this.worldToRegions.getOrDefault(region.getWorld().getName(), new HashSet<>());
		regions.add(region);
		this.worldToRegions.put(region.getWorld().getName(), regions);
		return region;
	}

	public void loadAll(boolean force) {
		if (this.allLoaded && !force)
			return;
		this.LOGGER.fine("Loading all regions...");
		this.allLoaded = true;
		this.masterRegistry.removeFromRegistry(GameObjectType.REGION);
		this.storageManager.getAllStorageAccess(GameObjectType.REGION).stream().forEach(storageAccess -> {
			Region region = loadObject(storageAccess);
			this.masterRegistry.getRegisteredObjects().add(region);
			Set<Region> regions = this.worldToRegions.getOrDefault(region.getWorld().getName(), new HashSet<>());
			regions.add(region);
			this.worldToRegions.put(region.getWorld().getName(), regions);
		});
	}

	public void lazyLoadAll() {
		loadAll(false);
	}
}
