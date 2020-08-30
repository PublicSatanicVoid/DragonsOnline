package mc.dragons.core.gameobject.structure;

import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.storage.StorageManager;

public class Structure extends GameObject {
	public Structure(StorageManager storageManager) {
		super(GameObjectType.STRUCTURE, storageManager);
		LOGGER.fine("Constructing structure (" + storageManager + ")");
	}
}
