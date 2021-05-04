package mc.dragons.core.gameobject.floor;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.World;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectLoader;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.singletons.Singleton;
import mc.dragons.core.util.singletons.Singletons;

public class FloorLoader extends GameObjectLoader<Floor> implements Singleton {
	private DragonsLogger LOGGER = Dragons.getInstance().getLogger();
	
	private static Map<String, Floor> worldNameToFloor;
	private static Map<String, Floor> floorNameToFloor;
	
	private GameObjectRegistry masterRegistry;
	
	private boolean allLoaded = false;

	private FloorLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		masterRegistry = instance.getGameObjectRegistry();
		worldNameToFloor = new HashMap<>();
		floorNameToFloor = new CaseInsensitiveMap<>();
	}

	public static FloorLoader getInstance() {
		Dragons dragons = Dragons.getInstance();
		return Singletons.getInstance(FloorLoader.class, () -> new FloorLoader(dragons, dragons.getPersistentStorageManager()));
	}

	@Override
	public Floor loadObject(StorageAccess storageAccess) {
		lazyLoadAll();
		LOGGER.trace("Loading floor " + storageAccess.getIdentifier());
		Floor floor = new Floor(storageManager, storageAccess, false);
		masterRegistry.getRegisteredObjects().add(floor);
		worldNameToFloor.put(floor.getWorldName(), floor);
		floorNameToFloor.put(floor.getFloorName(), floor);
		return floor;
	}

	public Floor registerNew(String floorName, String worldName, String displayName, int levelMin, boolean superflat) {
		lazyLoadAll();
		LOGGER.trace("Registering new floor " + floorName + " (world " + worldName + ", displayName " + displayName + ", lvMin " + levelMin + ", superflat=" + superflat + ")");
		StorageAccess storageAccess = storageManager.getNewStorageAccess(GameObjectType.FLOOR,
				new Document("floorName", floorName)
				.append("worldName", worldName)
				.append("displayName", displayName)
				.append("levelMin", levelMin)
				.append("status", Floor.DEFAULT_STATUS.toString())
				.append("volatile", false));
		Floor floor = new Floor(storageManager, storageAccess, superflat);
		masterRegistry.getRegisteredObjects().add(floor);
		worldNameToFloor.put(worldName, floor);
		floorNameToFloor.put(floorName, floor);
		return floor;
	}

	/**
	 * Statically associate the specified Bukkit world with the specified floor.
	 * 
	 * @param world
	 * @param floor
	 */
	public static void link(World world, Floor floor) {
		worldNameToFloor.put(world.getName(), floor);
		floorNameToFloor.put(floor.getFloorName(), floor);
	}

	/**
	 * 
	 * @param worldName
	 * @return The floor associated with the specified world.
	 */
	public static Floor fromWorldName(String worldName) {
		return worldNameToFloor.get(worldName);
	}

	/**
	 * 
	 * @param world
	 * @return The floor associated with the specified world.
	 */
	public static Floor fromWorld(World world) {
		return worldNameToFloor.get(world.getName());
	}

	/**
	 * 
	 * @param loc
	 * @return The floor associated with the world of the specified location.
	 */
	public static Floor fromLocation(Location loc) {
		return worldNameToFloor.get(loc.getWorld().getName());
	}

	/**
	 * 
	 * @param floorName
	 * @return The floor associated with the specified internal (GM) name.
	 */
	public static Floor fromFloorName(String floorName) {
		return floorNameToFloor.get(floorName);
	}

	/**
	 * Update static associations between floor name and floor when the
	 * floor name has changed.
	 * 
	 * @param floorName
	 * @param newFloorName
	 */
	public static void updateFloorName(String floorName, String newFloorName) {
		floorNameToFloor.put(newFloorName, floorNameToFloor.remove(floorName));
	}

	/**
	 * Load all floors synchronously.
	 * 
	 * @param force Whether to load even if they have already been loaded.
	 */
	public void loadAll(boolean force) {
		if (allLoaded && !force) {
			return;
		}
		LOGGER.debug("Loading all floors...");
		allLoaded = true;
		masterRegistry.removeFromRegistry(GameObjectType.FLOOR);
		storageManager.getAllStorageAccess(GameObjectType.FLOOR).stream().forEach(storageAccess -> masterRegistry.getRegisteredObjects().add(loadObject(storageAccess)));
	}

	/**
	 * Load all floors synchronously, if they have not already been loaded.
	 */
	public void lazyLoadAll() {
		loadAll(false);
	}
}
