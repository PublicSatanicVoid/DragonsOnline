package mc.dragons.core.gameobject;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import mc.dragons.core.Dragons;
import mc.dragons.core.storage.StorageManager;

/**
 * Central registry of all game objects.
 * 
 * <p>Includes persistent and non-persistent objects.
 * 
 * <p>Local objects on other servers are not registered.
 * 
 * @author Adam
 *
 */
public class GameObjectRegistry {
	protected Dragons plugin;
	protected StorageManager storageManager;

	protected Set<GameObject> registeredObjects;

	public GameObjectRegistry(Dragons instance, StorageManager persistentStorageManager) {
		plugin = instance;
		storageManager = persistentStorageManager;
		registeredObjects = new CopyOnWriteArraySet<>();
	}

	/**
	 * 
	 * @return All registered game objects
	 */
	public Set<GameObject> getRegisteredObjects() {
		return registeredObjects;
	}

	/**
	 * 
	 * @param types
	 * @return All registered game objects matching any of the specified types
	 */
	public Set<GameObject> getRegisteredObjects(GameObjectType... types) {
		List<GameObjectType> arrTypes = List.of(types);
		return registeredObjects.stream().filter(obj -> arrTypes.contains(obj.getType())).collect(Collectors.toSet());
	}

	/**
	 * Removes the specified game object from the database, if it is persistent.
	 * @param gameObject
	 */
	public void removeFromDatabase(GameObject gameObject) {
		storageManager.removeObject(gameObject);
		registeredObjects.remove(gameObject);
	}

	/**
	 * Removes all game objects of the specified type from the local registry, 
	 * but does not remove them from the database.
	 * @param type
	 */
	public void removeFromRegistry(GameObjectType type) {
		registeredObjects.removeIf(obj -> (obj.getType() == type));
	}
	
	/**
	 * Auto-saves all game objects that are eligible for auto-saving.
	 * @param forceSave Whether to override a server option disabling auto-saving
	 */
	public void executeAutoSave(boolean forceSave) {
		if (!plugin.getServerOptions().isAutoSaveEnabled() && !forceSave) {
			return;
		}
		int n = 0;
		for (GameObject gameObject : getRegisteredObjects(new GameObjectType[] { GameObjectType.USER, GameObjectType.NPC })) {
			gameObject.autoSave();
			n++;
		}
		plugin.getLogger().debug("Auto-saved " + n + " game objects");
	}
}
