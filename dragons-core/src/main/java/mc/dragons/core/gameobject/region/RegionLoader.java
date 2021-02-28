package mc.dragons.core.gameobject.region;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.World;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectLoader;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.util.StringUtil;

public class RegionLoader extends GameObjectLoader<Region> {
	private static RegionLoader INSTANCE;

	private Logger LOGGER = Dragons.getInstance().getLogger();

	private boolean allLoaded = false;
	private GameObjectRegistry masterRegistry;
	private Map<String, Set<Region>> worldToRegions;

	private RegionLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		masterRegistry = instance.getGameObjectRegistry();
		worldToRegions = new HashMap<>();
	}

	public static synchronized RegionLoader getInstance(Dragons instance, StorageManager storageManager) {
		if (INSTANCE == null) {
			INSTANCE = new RegionLoader(instance, storageManager);
		}
		return INSTANCE;
	}

	@Override
	public Region loadObject(StorageAccess storageAccess) {
		lazyLoadAll();
		LOGGER.fine("Loading region " + storageAccess.getIdentifier());
		return new Region(storageManager, storageAccess);
	}

	public Region getRegionByName(String name) {
		lazyLoadAll();
		for (GameObject gameObject : masterRegistry.getRegisteredObjects(new GameObjectType[] { GameObjectType.REGION })) {
			Region region = (Region) gameObject;
			if (region.getName().equalsIgnoreCase(name)) {
				return region;
			}
		}
		return null;
	}

	public Set<Region> getRegionsByWorld(World world) {
		return getRegionsByWorld(world.getName());
	}

	public Set<Region> getRegionsByWorld(String worldName) {
		return worldToRegions.getOrDefault(worldName, new HashSet<>());
	}

	public Set<Region> getRegionsByLocation(Location loc) {
		lazyLoadAll();
		Set<Region> result = new HashSet<>();
		for (Region region : getRegionsByWorld(loc.getWorld())) {
			if (region.contains(loc)) {
				result.add(region);
			}
		}
		return result;
	}

	public Region getSmallestRegionByLocation(Location loc, boolean includeHidden) {
		lazyLoadAll();
		Set<Region> candidates = getRegionsByLocation(loc);
		if (candidates.isEmpty()) {
			return null;
		}
		Optional<Region> result = candidates.stream().filter(r -> includeHidden ? true : !Boolean.parseBoolean(r.getFlags().getString("hidden")))
				.sorted((a, b) -> a.getArea() > b.getArea() ? 1 : -1).findFirst();
		if (result.isEmpty()) {
			return null;
		}
		return result.get();
	}

	@Deprecated
	public Set<Region> getRegionsByLocationXZ(Location loc) {
		lazyLoadAll();
		Set<Region> result = new HashSet<>();
		for (Region region : getRegionsByWorld(loc.getWorld())) {
			if (region.containsXZ(loc)) {
				result.add(region);
			}
		}
		return result;
	}

	public Region registerNew(String name, Location corner1, Location corner2) {
		lazyLoadAll();
		LOGGER.fine("Registering new region " + name + " (" + StringUtil.locToString(corner1) + " [" + corner1.getWorld().getName() + "] -> " + StringUtil.locToString(corner2) + " ["
				+ corner2.getWorld().getName() + "]");
		if (corner1.getWorld() != corner2.getWorld()) {
			throw new IllegalArgumentException("Corners must be in the same world");
		}
		StorageAccess storageAccess = storageManager.getNewStorageAccess(GameObjectType.REGION);
		storageAccess.set("name", name);
		storageAccess.set("world", corner1.getWorld().getName());
		storageAccess.set("corner1", StorageUtil.vecToDoc(corner1.toVector()));
		storageAccess.set("corner2", StorageUtil.vecToDoc(corner2.toVector()));
		Document flags = new Document();
		for(String[] flag : Region.DEFAULT_FLAGS) {
			flags.append(flag[0], flag[1]);
		}
		storageAccess.set("flags", flags);
		storageAccess.set("spawnRates", new Document());
		Region region = new Region(storageManager, storageAccess);
		masterRegistry.getRegisteredObjects().add(region);
		Set<Region> regions = worldToRegions.getOrDefault(region.getWorld().getName(), new HashSet<>());
		regions.add(region);
		worldToRegions.put(region.getWorld().getName(), regions);
		return region;
	}

	public void loadAll(boolean force) {
		if (allLoaded && !force) {
			return;
		}
		LOGGER.fine("Loading all regions...");
		allLoaded = true;
		masterRegistry.removeFromRegistry(GameObjectType.REGION);
		storageManager.getAllStorageAccess(GameObjectType.REGION).stream().forEach(storageAccess -> {
			Region region = loadObject(storageAccess);
			masterRegistry.getRegisteredObjects().add(region);
			Set<Region> regions = worldToRegions.getOrDefault(region.getWorld().getName(), new HashSet<>());
			regions.add(region);
			worldToRegions.put(region.getWorld().getName(), regions);
		});
	}

	public void lazyLoadAll() {
		loadAll(false);
	}
}
