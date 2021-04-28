package mc.dragons.core.gameobject.structure;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectLoader;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.singletons.Singleton;
import mc.dragons.core.util.singletons.Singletons;

public class StructureLoader extends GameObjectLoader<Structure> implements Singleton {
	private DragonsLogger LOGGER = Dragons.getInstance().getLogger();

	private StructureLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
	}

	public static StructureLoader getInstance() {
		Dragons dragons = Dragons.getInstance();
		return Singletons.getInstance(StructureLoader.class, () -> new StructureLoader(dragons, dragons.getPersistentStorageManager()));
	}

	@Override
	public Structure loadObject(StorageAccess storageAccess) {
		LOGGER.debug("Loading structure " + storageAccess.getIdentifier());
		return new Structure(storageManager);
	}
}
