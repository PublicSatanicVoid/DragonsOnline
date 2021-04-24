package mc.dragons.core.gameobject.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectLoader;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;

public class ItemLoader extends GameObjectLoader<Item> {
	private static ItemLoader INSTANCE;
	private static Map<String, Item> uuidToItem = new HashMap<>();

	private Logger LOGGER = Dragons.getInstance().getLogger();
	
	private GameObjectRegistry masterRegistry;
	private ItemClassLoader itemClassLoader;

	private ItemLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		masterRegistry = instance.getGameObjectRegistry();
		itemClassLoader = GameObjectType.ITEM_CLASS.<ItemClass, ItemClassLoader>getLoader();
	}

	public static synchronized ItemLoader getInstance(Dragons instance, StorageManager storageManager) {
		if (INSTANCE == null) {
			INSTANCE = new ItemLoader(instance, storageManager);
		}
		return INSTANCE;
	}

	@Override
	public Item loadObject(StorageAccess storageAccess) {
		if (storageAccess == null) {
			return null;
		}
		LOGGER.fine("Loading item by storage access " + storageAccess.getIdentifier());
		Material type = Material.valueOf((String) storageAccess.get("materialType"));
		ItemStack itemStack = new ItemStack(type);
		Item item = new Item(itemStack, storageManager, storageAccess);
		masterRegistry.getRegisteredObjects().add(item);
		uuidToItem.put(item.getUUID().toString(), item);
		return new Item(itemStack, storageManager, storageAccess);
	}

	public Item loadObject(UUID uuid) {
		LOGGER.fine("Loading item by UUID " + uuid);
		return loadObject(storageManager.getStorageAccess(GameObjectType.ITEM, uuid));
	}
	
	/**
	 * Loads all items matching the given UUIDs in a single database query.
	 * 
	 * <p>Indexed by UUID for convenience.
	 * 
	 * @param uuids
	 * @return
	 */
	public Map<UUID, Item> loadObjects(Set<UUID> uuids) {
		LOGGER.fine("Loading items by UUID " + uuids.toArray());
		Map<UUID, Item> result = new HashMap<>();
		Set<StorageAccess> results = storageManager.getAllStorageAccess(GameObjectType.ITEM, new Document("_id", new Document("$in", uuids)));
		for(StorageAccess sa : results) {
			result.put(sa.getIdentifier().getUUID(), loadObject(sa));
		}
		return result;
	}

	public Item registerNew(ItemClass itemClass) {
		return registerNew(itemClass.getClassName(), itemClass.getName(), false, itemClass.getNameColor(), itemClass.getMaterial(), itemClass.getLevelMin(), itemClass.getCooldown(),
				itemClass.getSpeedBoost(), itemClass.isUnbreakable(), itemClass.isUndroppable(), itemClass.getDamage(), itemClass.getArmor(), itemClass.getLore(), itemClass.getMaxStackSize());
	}
	
	public Item registerNew(String itemClassName) {
		return registerNew(itemClassLoader.getItemClassByClassName(itemClassName));
	}

	public Item registerNew(Item item) {
		return registerNew(item.getData());
	}

	public Item registerNew(String className, String name, boolean custom, ChatColor nameColor, Material material, int levelMin, double cooldown, double speedBoost, boolean unbreakable,
			boolean undroppable, double damage, double armor, List<String> lore, int maxStackSize) {
		return registerNew(new Document("_id", UUID.randomUUID())
				.append("className", className)
				.append("name", name)
				.append("isCustom", custom)
				.append("nameColor", nameColor.name())
				.append("materialType", material.toString())
				.append("lvMin", levelMin)
				.append("cooldown", cooldown)
				.append("speedBoost", speedBoost)
				.append("unbreakable", unbreakable)
				.append("undroppable", undroppable)
				.append("damage", damage)
				.append("armor", armor)
				.append("lore", lore)
				.append("quantity", 1)
				.append("maxStackSize", maxStackSize));
	}
	
	public Item registerNew(Document data) {
		data.append("_id", UUID.randomUUID());
		String className = data.getString("className");
		Material material = Material.valueOf(data.getString("materialType"));
		LOGGER.fine("Registering new item of class " + className);
		Document clone = new Document(data);
		itemClassLoader.getItemClassByClassName(className).getAddons().forEach(a -> a.onCreateStorageAccess(clone));
		StorageAccess storageAccess = storageManager.getNewStorageAccess(GameObjectType.ITEM, clone);
		ItemStack itemStack = new ItemStack(material);
		Item item = new Item(itemStack, storageManager, storageAccess);
		uuidToItem.put(item.getUUID().toString(), item);
		masterRegistry.getRegisteredObjects().add(item);
		return item;
	}

	public static Item fromBukkit(ItemStack itemStack) {
		if(itemStack == null) return null;
		ItemMeta meta = itemStack.getItemMeta();
		if(meta == null) return null;
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		if(pdc == null) return null;
		String uuid = pdc.get(Item.ITEM_UUID_KEY, PersistentDataType.STRING);
		if(uuid == null) return null;
		return uuidToItem.get(uuid);
	}
}
