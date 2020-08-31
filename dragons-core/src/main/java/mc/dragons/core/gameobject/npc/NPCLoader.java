package mc.dragons.core.gameobject.npc;

import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectLoader;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.storage.impl.LocalStorageManager;
import mc.dragons.core.util.StringUtil;

public class NPCLoader extends GameObjectLoader<NPC> {
	private static NPCLoader INSTANCE;

	private Logger LOGGER = Dragons.getInstance().getLogger();

	private GameObjectRegistry masterRegistry;

	private boolean allPermanentLoaded = false;

	private LocalStorageManager localStorageManager;

	private NPCClassLoader npcClassLoader;

	private NPCLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		this.masterRegistry = instance.getGameObjectRegistry();
		this.localStorageManager = instance.getLocalStorageManager();
		this.npcClassLoader = GameObjectType.NPC_CLASS.<NPCClass, NPCClassLoader>getLoader();
	}

	public static synchronized NPCLoader getInstance(Dragons instance, StorageManager storageManager) {
		if (INSTANCE == null)
			INSTANCE = new NPCLoader(instance, storageManager);
		return INSTANCE;
	}

	public NPC loadObject(StorageAccess storageAccess) {
		lazyLoadAllPermanent();
		this.LOGGER.fine("Loading NPC " + storageAccess.getIdentifier());
		NPC.NPCType npcType = NPC.NPCType.valueOf((String) storageAccess.get("npcType"));
		Location loc = StorageUtil.docToLoc((Document) storageAccess.get("lastLocation"));
		Entity e = loc.getWorld().spawnEntity(loc, EntityType.valueOf((String) storageAccess.get("entityType")));
		NPC npc = new NPC(e, npcType.isPersistent() ? this.storageManager : (StorageManager) this.localStorageManager,
				npcType.isPersistent() ? storageAccess : (StorageAccess) this.localStorageManager.downgrade(storageAccess));
		e.setMetadata("handle", (MetadataValue) new FixedMetadataValue((Plugin) this.plugin, npc));
		this.masterRegistry.getRegisteredObjects().add(npc);
		return npc;
	}

	public NPC loadObject(UUID uuid) {
		for (GameObject gameObject : this.masterRegistry.getRegisteredObjects(new GameObjectType[] { GameObjectType.NPC })) {
			NPC npc = (NPC) gameObject;
			if (npc.getUUID().equals(uuid))
				return npc;
		}
		StorageAccess storageAccess = this.storageManager.getStorageAccess(GameObjectType.NPC, uuid);
		if (storageAccess == null)
			return null;
		return loadObject(storageAccess);
	}

	public NPC registerNew(Entity entity, String npcClassName) {
		return registerNew(entity, this.npcClassLoader.getNPCClassByClassName(npcClassName));
	}

	public NPC registerNew(World world, Location spawnLocation, String npcClassName) {
		return registerNew(world, spawnLocation, this.npcClassLoader.getNPCClassByClassName(npcClassName));
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
		if (entity == null)
			return null;
		if (!entity.hasMetadata("handle"))
			return null;
		if (entity.getMetadata("handle").size() == 0)
			return null;
		Object value = ((MetadataValue) entity.getMetadata("handle").get(0)).value();
		if (value instanceof NPC)
			return (NPC) value;
		return null;
	}

	public NPC registerNew(Entity entity, String className, String name, double maxHealth, int level, NPC.NPCType npcType, boolean ai, boolean immortal) {
		this.LOGGER.fine("Registering new NPC of class " + className + " using Bukkit entity " + StringUtil.entityToString(entity));
		lazyLoadAllPermanent();
		Document data = (new Document("_id", UUID.randomUUID())).append("className", className).append("name", name).append("entityType", entity.getType().toString())
				.append("maxHealth", Double.valueOf(maxHealth)).append("lastLocation", StorageUtil.locToDoc(entity.getLocation())).append("level", Integer.valueOf(level))
				.append("npcType", npcType.toString()).append("ai", Boolean.valueOf(ai)).append("immortal", Boolean.valueOf(immortal)).append("lootTable", new Document());
		this.npcClassLoader.getNPCClassByClassName(className).getAddons().forEach(a -> a.onCreateStorageAccess(data));
		StorageAccess storageAccess = npcType.isPersistent() ? this.storageManager.getNewStorageAccess(GameObjectType.NPC, data)
				: this.localStorageManager.getNewStorageAccess(GameObjectType.NPC, data);
		NPC npc = new NPC(entity, npcType.isPersistent() ? this.storageManager : (StorageManager) this.localStorageManager, storageAccess);
		if (storageAccess instanceof mc.dragons.core.storage.impl.LocalStorageAccess)
			this.LOGGER.fine("- Using local storage access for NPC of type " + npcType + " (" + storageAccess + ")");
		if (storageAccess == null)
			this.LOGGER.warning("- Whoops! The storage access was null!");
		npc.setMaxHealth(maxHealth);
		npc.setHealth(maxHealth);
		entity.setMetadata("handle", (MetadataValue) new FixedMetadataValue((Plugin) this.plugin, npc));
		this.masterRegistry.getRegisteredObjects().add(npc);
		return npc;
	}

	public void loadAllPermanent(boolean force) {
		if (this.allPermanentLoaded && !force)
			return;
		this.LOGGER.fine("Loading all permanent NPCs...");
		this.allPermanentLoaded = true;
		this.masterRegistry.removeFromRegistry(GameObjectType.NPC);
		this.storageManager
				.getAllStorageAccess(GameObjectType.NPC, new Document("$or", Arrays.<NPC.NPCType>asList(NPC.NPCType.values()).stream()
						.filter(type -> (type.isPersistent() && type.isLoadedImmediately())).map(type -> new Document("npcType", type.toString())).collect(Collectors.toList())))
				.stream().forEach(storageAccess -> {
					NPC npc = loadObject(storageAccess);
					Dragons.getInstance().getLogger().fine("Loaded permanent NPC: " + npc.getIdentifier() + " of class " + npc.getNPCClass().getClassName());
					this.masterRegistry.getRegisteredObjects().add(npc);
				});
		Dragons.getInstance().getLogger().info("Initial entity count: " + Dragons.getInstance().getEntities().size());
	}

	public void lazyLoadAllPermanent() {
		loadAllPermanent(false);
	}
}
