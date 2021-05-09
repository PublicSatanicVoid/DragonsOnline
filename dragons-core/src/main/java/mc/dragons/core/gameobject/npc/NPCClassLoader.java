package mc.dragons.core.gameobject.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.entity.EntityType;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectLoader;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPCConditionalActions.NPCTrigger;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.singletons.Singleton;
import mc.dragons.core.util.singletons.Singletons;

public class NPCClassLoader extends GameObjectLoader<NPCClass> implements Singleton {
	private DragonsLogger LOGGER = Dragons.getInstance().getLogger();
	
	private GameObjectRegistry masterRegistry;
	private boolean allLoaded = false;
	private Map<String, NPCClass> cachedNPCClasses;

	private NPCClassLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		masterRegistry = instance.getGameObjectRegistry();
		cachedNPCClasses = new HashMap<>();
	}
	
	public static NPCClassLoader getInstance() {
		Dragons dragons = Dragons.getInstance();
		return Singletons.getInstance(NPCClassLoader.class, () -> new NPCClassLoader(dragons, dragons.getPersistentStorageManager()));
	}
	
	@Override
	public NPCClass loadObject(StorageAccess storageAccess) {
		lazyLoadAll();
		LOGGER.trace("Loading NPC class " + storageAccess.getIdentifier());		
		for(GameObject gameObject : masterRegistry.getRegisteredObjects(GameObjectType.NPC_CLASS)) {
			NPCClass npcClass = (NPCClass) gameObject;
			if(npcClass.getIdentifier().equals(storageAccess.getIdentifier())) {
				return npcClass;
			}
		}
		NPCClass npcClass = new NPCClass(storageManager, storageAccess);
		masterRegistry.getRegisteredObjects().add(npcClass);
		cachedNPCClasses.put(npcClass.getClassName(), npcClass);
		return npcClass;
	}

	public NPCClass getNPCClassByClassName(String npcClassName) {
		lazyLoadAll();
		return cachedNPCClasses.computeIfAbsent(npcClassName, name -> {
			for (GameObject gameObject : masterRegistry.getRegisteredObjects(new GameObjectType[] { GameObjectType.NPC_CLASS })) {
				NPCClass npcClass = (NPCClass) gameObject;
				if (npcClass.getClassName().equalsIgnoreCase(name)) {
					return npcClass;
				}
			}
			return null;
		});
	}

	public NPCClass registerNew(String className, String name, EntityType entityType, double maxHealth, int level, NPC.NPCType npcType) {
		lazyLoadAll();
		LOGGER.trace("Registering new NPC class (" + className + ")");
		Document emptyConditionals = new Document();
		for(NPCTrigger trigger : NPCTrigger.values()) {
			emptyConditionals.append(trigger.toString(), new ArrayList<>());
		}
		Document data = new Document("_id", UUID.randomUUID()).append("className", className).append("name", name).append("entityType", entityType.toString())
				.append("maxHealth", Double.valueOf(maxHealth)).append("level", Integer.valueOf(level)).append("ai", Boolean.valueOf(npcType.hasAIByDefault()))
				.append("immortal", Boolean.valueOf(npcType.isImmortalByDefault())).append("attributes", new Document()).append("npcType", npcType.toString()).append("lootTable", new Document())
				.append("conditionals", emptyConditionals).append("addons", new ArrayList<String>());
		StorageAccess storageAccess = storageManager.getNewStorageAccess(GameObjectType.NPC_CLASS, data);
		NPCClass npcClass = new NPCClass(storageManager, storageAccess);
		masterRegistry.getRegisteredObjects().add(npcClass);
		cachedNPCClasses.put(npcClass.getClassName(), npcClass);
		return npcClass;
	}

	/**
	 * Load all NPC classes synchronously.
	 * 
	 * @param force Whether to load even if they have already been loaded.
	 */
	public void loadAll(boolean force) {
		if (allLoaded && !force) {
			return;
		}
		LOGGER.debug("Loading all NPC classes...");
		allLoaded = true;
		masterRegistry.removeFromRegistry(GameObjectType.NPC_CLASS);
		storageManager.getAllStorageAccess(GameObjectType.NPC_CLASS).stream().forEach(storageAccess -> masterRegistry.getRegisteredObjects().add(loadObject(storageAccess)));
	}

	/**
	 * Load all item classes synchronously, if they have not already been loaded.
	 */
	public void lazyLoadAll() {
		loadAll(false);
	}
}
