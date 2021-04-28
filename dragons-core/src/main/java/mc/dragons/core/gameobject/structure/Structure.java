package mc.dragons.core.gameobject.structure;

import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.storage.StorageManager;

/**
 * Represents a structure in the RPG. Structures are pre-made
 * builds, such as houses or towers, which can be generated in
 * certain areas by players as a substitute for unrestricted
 * building.
 * 
 * @author Adam
 *
 */
public class Structure extends GameObject {
	public Structure(StorageManager storageManager) {
		super(GameObjectType.STRUCTURE, storageManager);
		LOGGER.verbose("Constructing structure (" + storageManager + ")");
		// TODO: Specific instantiation via storageAccess
	}
}
