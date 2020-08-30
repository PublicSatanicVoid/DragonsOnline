package mc.dragons.core.gameobject.loader;

import java.util.logging.Logger;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.structure.Structure;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;

public class StructureLoader extends GameObjectLoader<Structure> {
	private static StructureLoader INSTANCE;

	private Logger LOGGER = Dragons.getInstance().getLogger();

	private StructureLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
	}

	public static synchronized StructureLoader getInstance(Dragons instance, StorageManager storageManager) {
		if (INSTANCE == null)
			INSTANCE = new StructureLoader(instance, storageManager);
		return INSTANCE;
	}

	public Structure loadObject(StorageAccess storageAccess) {
		this.LOGGER.fine("Loading structure " + storageAccess.getIdentifier());
		return new Structure(this.storageManager);
	}
}
