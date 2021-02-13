package mc.dragons.core.gameobject;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import mc.dragons.core.Dragons;
import mc.dragons.core.storage.StorageManager;

public class GameObjectRegistry {
	protected Dragons plugin;
	protected StorageManager storageManager;

	protected Set<GameObject> registeredObjects;

	public GameObjectRegistry(Dragons instance, StorageManager storageManager) {
		plugin = instance;
		this.storageManager = storageManager;
		registeredObjects = new HashSet<>();
	}

	public GameObject registerNew() {
		return null;
	}

	public Set<GameObject> getRegisteredObjects() {
		return registeredObjects;
	}

	public Set<GameObject> getRegisteredObjects(GameObjectType... types) {
		return registeredObjects.stream().filter(obj -> {
			for(GameObjectType type : types) {
				if(type == obj.getType()) {
					return true;
				}
			}
			return false;
		}).collect(Collectors.toSet());
	}

	public void removeFromDatabase(GameObject gameObject) {
		storageManager.removeObject(gameObject);
		registeredObjects.remove(gameObject);
	}

	public void removeFromRegistry(GameObjectType type) {
		registeredObjects.removeIf(obj -> (obj.getType() == type));
	}
}
