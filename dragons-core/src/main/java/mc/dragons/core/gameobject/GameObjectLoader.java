package mc.dragons.core.gameobject;

import mc.dragons.core.Dragons;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;

/**
 * Loads game objects from persistent storage.
 * 
 * <p>May also be responsible for registering new game objects.
 * 
 * <p>Each loader is bound to a specified game object type.
 * 
 * @author Adam
 *
 * @param <T> The game object type this loader is bound to
 */
public abstract class GameObjectLoader<T extends GameObject> {
	protected Dragons plugin;
	protected StorageManager storageManager;

	protected GameObjectLoader(Dragons instance, StorageManager storageManager) {
		this.plugin = instance;
		this.storageManager = storageManager;
	}

	/**
	 * 
	 * @param storageAccess
	 * @return The game object with the specified StorageAccess
	 */
	public abstract T loadObject(StorageAccess storageAccess);
}
