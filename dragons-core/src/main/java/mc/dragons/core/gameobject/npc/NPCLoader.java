package mc.dragons.core.gameobject.npc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Bukkit;
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
import mc.dragons.core.storage.local.LocalStorageManager;
import mc.dragons.core.util.StringUtil;
import mc.dragons.core.util.singletons.Singleton;
import mc.dragons.core.util.singletons.Singletons;

public class NPCLoader extends GameObjectLoader<NPC> implements Singleton {
	private DragonsLogger LOGGER = Dragons.getInstance().getLogger();
	
	private boolean allPermanentLoaded = false;

	private GameObjectRegistry masterRegistry;
	private LocalStorageManager localStorageManager;
	private NPCClassLoader npcClassLoader;

	private NPCLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		masterRegistry = instance.getGameObjectRegistry();
		localStorageManager = instance.getLocalStorageManager();
		npcClassLoader = GameObjectType.NPC_CLASS.<NPCClass, NPCClassLoader>getLoader();
	}

	public static NPCLoader getInstance() {
		Dragons dragons = Dragons.getInstance();
		return Singletons.getInstance(NPCLoader.class, () -> new NPCLoader(dragons, dragons.getPersistentStorageManager()));
	}
	
	@Override
	public NPC loadObject(StorageAccess storageAccess) {
		List<BukkitRunnable> spawner = new ArrayList<>();
		NPC npc = loadObject(storageAccess, spawner);
		spawner.get(0).runTask(Dragons.getInstance());
		return npc;
	}
	
	public NPC loadObject(StorageAccess storageAccess, List<BukkitRunnable> spawnRunnables) {
		lazyLoadAllPermanent();
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

	public NPC registerNew(Entity entity, String npcClassName) {
		return registerNew(entity, npcClassLoader.getNPCClassByClassName(npcClassName));
	}

	public NPC registerNew(World world, Location spawnLocation, String npcClassName) {
		return registerNew(world, spawnLocation, npcClassLoader.getNPCClassByClassName(npcClassName));
	}

	public NPC registerNew(Entity entity, NPCClass npcClass) {
		return registerNew(entity, npcClass.getClassName(), npcClass.getName(), npcClass.getMaxHealth(), npcClass.getLevel(), npcClass.getNPCType(), npcClass.hasAI(), npcClass.isImmortal());
	}

	public NPC registerNew(World world, Location spawnLocation, NPCClass npcClass) {
		return registerNew(world, spawnLocation, npcClass.getEntityType(), npcClass.getClassName(), npcClass.getName(), npcClass.getMaxHealth(), npcClass.getLevel(), npcClass.getNPCType(),
				npcClass.hasAI(), npcClass.isImmortal());
	}

	public NPC registerNew(World world, Location spawnLocation, EntityType entityType, String className, String name, double maxHealth, int level, NPC.NPCType npcType, boolean ai, boolean immortal) {
		Entity e = world.spawnEntity(spawnLocation, entityType);
		return registerNew(e, className, name, maxHealth, level, npcType, ai, immortal);
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

	public NPC registerNew(Entity entity, String className, String name, double maxHealth, int level, NPC.NPCType npcType, boolean ai, boolean immortal) {
		LOGGER.trace("Registering new NPC of class " + className + " using Bukkit entity " + StringUtil.entityToString(entity));
		lazyLoadAllPermanent();
		Document data = new Document("_id", UUID.randomUUID()).append("className", className).append("name", name).append("entityType", entity.getType().toString())
				.append("maxHealth", Double.valueOf(maxHealth)).append("lastLocation", StorageUtil.locToDoc(entity.getLocation())).append("level", Integer.valueOf(level))
				.append("npcType", npcType.toString()).append("ai", Boolean.valueOf(ai)).append("immortal", Boolean.valueOf(immortal)).append("lootTable", new Document());
		npcClassLoader.getNPCClassByClassName(className).getAddons().forEach(a -> a.onCreateStorageAccess(data));
		StorageAccess storageAccess = npcType.isPersistent() ? storageManager.getNewStorageAccess(GameObjectType.NPC, data)
				: localStorageManager.getNewStorageAccess(GameObjectType.NPC, data);
		NPC npc = new NPC(entity, npcType.isPersistent() ? storageManager : (StorageManager) localStorageManager, storageAccess);
		if (storageAccess instanceof mc.dragons.core.storage.local.LocalStorageAccess) {
			LOGGER.verbose("- Using local storage access for NPC of type " + npcType + " (" + storageAccess + ")");
		}
		if (storageAccess == null) {
			LOGGER.warning("- Could not construct storage access for NPC of type " + npcType + " and class " + className);
		}
		npc.setMaxHealth(maxHealth);
		npc.setHealth(maxHealth);
		entity.setMetadata("handle", new FixedMetadataValue(plugin, npc));
		masterRegistry.getRegisteredObjects().add(npc);
		return npc;
	}

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
		Bukkit.getScheduler().runTaskLaterAsynchronously(Dragons.getInstance(), () -> {
			int batchSize = 5;
			LOGGER.info("We have " + asyncSpawnerRunnables.size() + " persistent NPCs to spawn");
			for(int i = 0; i < (int) Math.ceil((double) asyncSpawnerRunnables.size() / batchSize); i++) {
				final int fi = i;
				Bukkit.getScheduler().runTaskLater(Dragons.getInstance(), () -> {
					long start = System.currentTimeMillis();
					LOGGER.verbose("==SPAWNING BATCH #" + fi + "==");
					for(int j = fi * batchSize; j < Math.min(asyncSpawnerRunnables.size(), (fi + 1) * batchSize); j++) {
						LOGGER.verbose("===Spawning #" + j);
						asyncSpawnerRunnables.get(j).run();
					}
					long duration = System.currentTimeMillis() - start;
					LOGGER.verbose("===Finished batch #" + fi + " in " + duration + "ms");
					if(duration > 1000) {
						LOGGER.warning("Spawn of batch #" + fi + " took " + duration + "ms (batch size: " + batchSize + ")");
					}
				}, i * 2);
			}
		}, 1L);
		LOGGER.info("Initial entity count: " + Dragons.getInstance().getEntities().size());
	}

	public void lazyLoadAllPermanent() {
		loadAllPermanent(false);
	}
}
