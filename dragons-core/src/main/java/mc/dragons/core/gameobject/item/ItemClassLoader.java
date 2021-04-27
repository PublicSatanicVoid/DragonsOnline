package mc.dragons.core.gameobject.item;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectLoader;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.singletons.Singleton;
import mc.dragons.core.util.singletons.Singletons;

public class ItemClassLoader extends GameObjectLoader<ItemClass> implements Singleton {
	private Logger LOGGER = Dragons.getInstance().getLogger();
	
	private GameObjectRegistry masterRegistry;

	private boolean allLoaded = false;

	private ItemClassLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		masterRegistry = instance.getGameObjectRegistry();
	}

	public static ItemClassLoader getInstance() {
		Dragons dragons = Dragons.getInstance();
		return Singletons.getInstance(ItemClassLoader.class, () -> new ItemClassLoader(dragons, dragons.getPersistentStorageManager()));
	}

	@Override
	public ItemClass loadObject(StorageAccess storageAccess) {
		LOGGER.fine("Loading item class " + storageAccess.getIdentifier());
		ItemClass itemClass = new ItemClass(storageManager, storageAccess);
		masterRegistry.getRegisteredObjects().add(itemClass);
		return itemClass;
	}

	public ItemClass getItemClassByClassName(String itemClassName) {
		lazyLoadAll();
		for (GameObject gameObject : masterRegistry.getRegisteredObjects(new GameObjectType[] { GameObjectType.ITEM_CLASS })) {
			ItemClass itemClass = (ItemClass) gameObject;
			if (itemClass.getClassName().equalsIgnoreCase(itemClassName)) {
				return itemClass;
			}
		}
		return null;
	}

	public ItemClass registerNew(String className, String name, ChatColor nameColor, Material material, int levelMin, double cooldown, double speedBoost, boolean unbreakable, boolean undroppable,
			double damage, double armor, List<String> lore, int maxStackSize) {
		lazyLoadAll();
		LOGGER.fine("Registering new item class (" + className + ")");
		Document data = new Document("_id", UUID.randomUUID()).append("className", className).append("name", name).append("nameColor", nameColor.name()).append("materialType", material.toString())
				.append("lvMin", Integer.valueOf(levelMin)).append("cooldown", Double.valueOf(cooldown)).append("unbreakable", Boolean.valueOf(unbreakable))
				.append("undroppable", Boolean.valueOf(undroppable)).append("damage", Double.valueOf(damage)).append("armor", Double.valueOf(armor)).append("speedBoost", Double.valueOf(speedBoost))
				.append("lore", lore).append("maxStackSize", Integer.valueOf(maxStackSize)).append("addons", new ArrayList<String>()).append("gmlock", false);
		StorageAccess storageAccess = storageManager.getNewStorageAccess(GameObjectType.ITEM_CLASS, data);
		ItemClass itemClass = new ItemClass(storageManager, storageAccess);
		masterRegistry.getRegisteredObjects().add(itemClass);
		return itemClass;
	}

	public void loadAll(boolean force) {
		if (allLoaded && !force) {
			return;
		}
		LOGGER.fine("Loading all item classes...");
		allLoaded = true;
		masterRegistry.removeFromRegistry(GameObjectType.ITEM_CLASS);
		storageManager.getAllStorageAccess(GameObjectType.ITEM_CLASS).stream().forEach(storageAccess -> masterRegistry.getRegisteredObjects().add(loadObject(storageAccess)));
	}

	public void lazyLoadAll() {
		loadAll(false);
	}
}
