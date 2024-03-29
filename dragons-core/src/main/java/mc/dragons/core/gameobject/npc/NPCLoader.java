package mc.dragons.core.gameobject.npc;

import static mc.dragons.core.util.BukkitUtil.async;
import static mc.dragons.core.util.BukkitUtil.sync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectLoader;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPC.NPCType;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.storage.local.LocalStorageAccess;
import mc.dragons.core.storage.local.LocalStorageManager;
import mc.dragons.core.util.singletons.Singleton;
import mc.dragons.core.util.singletons.Singletons;

public class NPCLoader extends GameObjectLoader<NPC> implements Singleton {
	private DragonsLogger LOGGER = Dragons.getInstance().getLogger();
	
	private boolean allPermanentLoaded = false;

	private GameObjectRegistry masterRegistry;
	private LocalStorageManager localStorageManager;
	private NPCClassLoader npcClassLoader;
	private Map<UUID, NPC> uuidToNpc;
	private List<BukkitRunnable> liveSpawner = new ArrayList<>();

	private NPCLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		masterRegistry = instance.getGameObjectRegistry();
		localStorageManager = instance.getLocalStorageManager();
		npcClassLoader = GameObjectType.NPC_CLASS.<NPCClass, NPCClassLoader>getLoader();
		uuidToNpc = new HashMap<>();
	}

	public static NPCLoader getInstance() {
		Dragons dragons = Dragons.getInstance();
		return Singletons.getInstance(NPCLoader.class, () -> new NPCLoader(dragons, dragons.getPersistentStorageManager()));
	}
	
	@Override
	public NPC loadObject(StorageAccess storageAccess) {
		UUID uuid = storageAccess.getIdentifier().getUUID();
		if(uuidToNpc.containsKey(uuid)) {
			LOGGER.debug("Prevented spawn of duplicate NPC " + uuid);
			return uuidToNpc.get(uuid);
		}
		List<BukkitRunnable> spawner = new ArrayList<>();
		NPC npc = loadObject(storageAccess, spawner);
		spawner.get(0).runTask(Dragons.getInstance());
		return npc;
	}
	
	public NPC loadObject(StorageAccess storageAccess, List<BukkitRunnable> spawnRunnables) {
		lazyLoadAllPermanent();
		UUID uuid = storageAccess.getIdentifier().getUUID();
		if(uuidToNpc.containsKey(uuid)) {
			LOGGER.debug("Prevented spawn of duplicate NPC " + uuid);
			return uuidToNpc.get(uuid);
		}
		LOGGER.trace("Loading NPC " + storageAccess.getIdentifier());
		NPC.NPCType npcType = NPC.NPCType.valueOf((String) storageAccess.get("npcType"));
		Location loc = StorageUtil.docToLoc((Document) storageAccess.get("lastLocation"));
		NPC npc = new NPC(loc, spawnRunnables, npcType.isPersistent() ? storageManager : (StorageManager) localStorageManager,
				npcType.isPersistent() ? storageAccess : (StorageAccess) localStorageManager.downgrade(storageAccess));
		masterRegistry.getRegisteredObjects().add(npc);
		return npc;
	}

	public NPC loadObject(UUID uuid) {
		return loadObject(uuid, null);
	}
	
	public NPC loadObject(UUID uuid, UUID cid) {
		if(uuidToNpc.containsKey(uuid)) {
			LOGGER.debug("Prevented spawn of duplicate NPC " + uuid);
			return uuidToNpc.get(uuid);
		}
		LOGGER.verbose(cid, "loading NPC with uuid " + uuid);
		for (GameObject gameObject : masterRegistry.getRegisteredObjects(new GameObjectType[] { GameObjectType.NPC })) {
			NPC npc = (NPC) gameObject;
			if (npc.getUUID().equals(uuid)) {
				LOGGER.verbose("found in local cache (" + npc + "), returning");
				return npc;
			}
		}
		StorageAccess storageAccess = storageManager.getStorageAccess(GameObjectType.NPC, uuid);
		if (storageAccess == null) {
			LOGGER.warning(cid, "could not load NPC from database: returned null storage access");
			return null;
		}
		LOGGER.verbose(cid, "loaded storage access (" + storageAccess + "), constructing downstream");
		return loadObject(storageAccess);
	}

//	public NPC registerNew(Entity entity, String npcClassName) {
//		return registerNew(entity, npcClassLoader.getNPCClassByClassName(npcClassName));
//	}

	public NPC registerNew(Location spawnLocation, String npcClassName) {
		return registerNew(spawnLocation.getWorld(), spawnLocation, npcClassLoader.getNPCClassByClassName(npcClassName));
	}

//	public NPC registerNew(Entity entity, NPCClass npcClass) {
//		return registerNew(entity, npcClass.getClassName(), npcClass.getName(), npcClass.getMaxHealth(), npcClass.getLevel(), npcClass.getNPCType(), npcClass.hasAI(), npcClass.isImmortal());
//	}

	public NPC registerNew(World world, Location spawnLocation, NPCClass npcClass) {
		return registerNew(world, spawnLocation, npcClass.getEntityType(), npcClass.getClassName(), npcClass.getName(), npcClass.getMaxHealth(), npcClass.getLevel(), npcClass.getNPCType(),
				npcClass.hasAI(), npcClass.isImmortal());
	}

	public NPC registerNew(World world, Location spawnLocation, EntityType entityType, String className, String name, double maxHealth, int level, NPC.NPCType npcType, boolean ai, boolean immortal) {
//		Entity e = world.spawnEntity(spawnLocation, entityType);
		return registerNew(spawnLocation, entityType, className, name, maxHealth, level, npcType, ai, immortal);
	}

	public static NPC fromBukkit(Entity entity) {
		if (entity == null) {
			return null;
		}
		if (!entity.hasMetadata("handle")) {
			return null;
		}
		if (entity.getMetadata("handle").size() == 0) {
			return null;
		}
		Object value = entity.getMetadata("handle").get(0).value();
		if (value instanceof NPC) {
			return (NPC) value;
		}
		return null;
	}

	public NPC registerNew(Location location, EntityType entType, String className, String name, double maxHealth, int level, NPC.NPCType npcType, boolean ai, boolean immortal) {
		LOGGER.trace("Registering new NPC of class " + className);
		lazyLoadAllPermanent();
		
		Document data = new Document("_id", UUID.randomUUID()).append("className", className).append("name", name).append("entityType", entType.toString())
				.append("maxHealth", Double.valueOf(maxHealth)).append("lastLocation", StorageUtil.locToDoc(location)).append("level", Integer.valueOf(level))
				.append("npcType", npcType.toString()).append("ai", Boolean.valueOf(ai)).append("immortal", Boolean.valueOf(immortal)).append("lootTable", new Document());
		
		npcClassLoader.getNPCClassByClassName(className).getAddons().forEach(a -> a.onCreateStorageAccess(data));
		StorageAccess storageAccess = npcType.isPersistent() ? storageManager.getNewStorageAccess(GameObjectType.NPC, data)
				: localStorageManager.getNewStorageAccess(GameObjectType.NPC, data);
		
		NPC npc = new NPC(location, liveSpawner, npcType.isPersistent() ? storageManager : localStorageManager, storageAccess);
		liveSpawner.forEach(r -> r.run());
		liveSpawner.clear();
		
		if (storageAccess instanceof LocalStorageAccess) {
			LOGGER.verbose("- Using local storage access for NPC of type " + npcType + " (" + storageAccess + ")");
		}
		if (storageAccess == null) {
			LOGGER.warning("- Could not construct storage access for NPC of type " + npcType + " and class " + className);
		}
		
		npc.setMaxHealth(maxHealth);
		npc.setHealth(maxHealth);
		if(npc.getEntity() != null) {
			npc.getEntity().setMetadata("handle", new FixedMetadataValue(plugin, npc));
		}
		
		masterRegistry.getRegisteredObjects().add(npc);
		return npc;
	}

	/**
	 * Load all persistent NPCs.
	 * 
	 * <p>This is done as asynchronously as possible.
	 * 
	 * @param force Whether to load even if they have already been loaded.
	 */
	public void loadAllPermanent(boolean force) {
		if (allPermanentLoaded && !force) {
			return;
		}
		LOGGER.debug("Loading all persistent NPCs...");
		allPermanentLoaded = true;
		masterRegistry.removeFromRegistry(GameObjectType.NPC);
		List<BukkitRunnable> asyncSpawnerRunnables = new ArrayList<>();
		storageManager
				.getAllStorageAccess(GameObjectType.NPC, new Document("$or", Arrays.<NPCType>asList(NPCType.values()).stream()
						.filter(type -> (type.isPersistent() && type.isLoadedImmediately())).map(type -> new Document("npcType", type.toString())).collect(Collectors.toList())))
				.stream().forEach(storageAccess -> {
					LOGGER.verbose("Loading permanent NPC: " + storageAccess.getIdentifier());
					NPC npc = loadObject(storageAccess, asyncSpawnerRunnables);
					LOGGER.verbose("- Loaded permanent NPC: " + npc.getIdentifier() + " of class " + npc.getNPCClass().getClassName());
					masterRegistry.getRegisteredObjects().add(npc);
				});
		async(() -> {
			long start = System.currentTimeMillis();
			int batchSize = 5;
			int max = (int) Math.ceil((double) asyncSpawnerRunnables.size() / batchSize);
			LOGGER.info("We have " + asyncSpawnerRunnables.size() + " persistent NPCs to spawn");
			for(int i = 1; i <= max; i++) {
				final int fi = i;
				sync(() -> {
					long batchStart = System.currentTimeMillis();
					LOGGER.verbose("==SPAWNING BATCH #" + fi + "==");
					for(int j = (fi - 1) * batchSize; j < Math.min(asyncSpawnerRunnables.size(), fi * batchSize); j++) {
						LOGGER.verbose("===Spawning #" + j);
						asyncSpawnerRunnables.get(j).run();
					}
					long now = System.currentTimeMillis();
					long duration = now - batchStart;
					LOGGER.verbose("===Finished batch #" + fi + " in " + duration + "ms");
					if(duration > 1000) {
						LOGGER.warning("Spawn of batch #" + fi + " took " + duration + "ms (batch size: " + batchSize + ")");
					}
					if(fi == max) {
						LOGGER.info("Spawning of persistent NPCs complete (took " + (now - start) + "ms)");
					}
				}, i * 3);
			}
		}, 1);
		LOGGER.info("Initial entity count: " + Dragons.getInstance().getEntities().size());
	}

	/**
	 * Load all persistent NPCs, if they have not already been loaded.
	 * 
	 * <p>This is done as asynchronously as possible.
	 */
	public void lazyLoadAllPermanent() {
		loadAllPermanent(false);
	}
}
