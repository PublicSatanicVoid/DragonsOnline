 package mc.dragons.core.gameobject.loader;
 
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.UUID;
 import java.util.logging.Logger;

 import org.bson.Document;
 import org.bukkit.ChatColor;
 import org.bukkit.Material;
 import org.bukkit.inventory.ItemStack;

 import mc.dragons.core.Dragons;
 import mc.dragons.core.gameobject.GameObjectType;
 import mc.dragons.core.gameobject.item.Item;
 import mc.dragons.core.gameobject.item.ItemClass;
 import mc.dragons.core.storage.StorageAccess;
 import mc.dragons.core.storage.StorageManager;
 import mc.dragons.core.util.HiddenStringUtil;
 
 public class ItemLoader extends GameObjectLoader<Item> {
   private static ItemLoader INSTANCE;
   
   private Logger LOGGER = Dragons.getInstance().getLogger();
   
   private GameObjectRegistry masterRegistry;
   
   private ItemClassLoader itemClassLoader;
   
   private static Map<String, Item> uuidToItem;
   
   private ItemLoader(Dragons instance, StorageManager storageManager) {
     super(instance, storageManager);
     this.masterRegistry = instance.getGameObjectRegistry();
     this.itemClassLoader = (ItemClassLoader)GameObjectType.ITEM_CLASS.<ItemClass, ItemClassLoader>getLoader();
     uuidToItem = new HashMap<>();
   }
   
   public static synchronized ItemLoader getInstance(Dragons instance, StorageManager storageManager) {
     if (INSTANCE == null)
       INSTANCE = new ItemLoader(instance, storageManager); 
     return INSTANCE;
   }
   
   public Item loadObject(StorageAccess storageAccess) {
     if (storageAccess == null)
       return null; 
     this.LOGGER.fine("Loading item by storage access " + storageAccess.getIdentifier());
     Material type = Material.valueOf((String)storageAccess.get("materialType"));
     ItemStack itemStack = new ItemStack(type);
     Item item = new Item(itemStack, this.storageManager, storageAccess);
     this.masterRegistry.getRegisteredObjects().add(item);
     uuidToItem.put(item.getUUID().toString(), item);
     return new Item(itemStack, this.storageManager, storageAccess);
   }
   
   public Item loadObject(UUID uuid) {
     this.LOGGER.fine("Loading item by UUID " + uuid);
     return loadObject(this.storageManager.getStorageAccess(GameObjectType.ITEM, uuid));
   }
   
   public Item registerNew(ItemClass itemClass) {
     return registerNew(itemClass.getClassName(), itemClass.getName(), false, itemClass.getNameColor(), itemClass.getMaterial(), itemClass.getLevelMin(), itemClass.getCooldown(), itemClass.getSpeedBoost(), 
         itemClass.isUnbreakable(), itemClass.isUndroppable(), itemClass.getDamage(), itemClass.getArmor(), itemClass.getLore(), itemClass.getMaxStackSize());
   }
   
   public Item registerNew(Item item) {
     return registerNew(item.getClassName(), item.getName(), item.isCustom(), item.getNameColor(), item.getMaterial(), item.getLevelMin(), item.getCooldown(), 
         item.getSpeedBoost(), item.isUnbreakable(), item.isUndroppable(), item.getDamage(), item.getArmor(), item.getLore(), item.getMaxStackSize());
   }
   
   public Item registerNew(String className, String name, boolean custom, ChatColor nameColor, Material material, int levelMin, double cooldown, double speedBoost, boolean unbreakable, boolean undroppable, double damage, double armor, List<String> lore, int maxStackSize) {
     this.LOGGER.fine("Registering new item of class " + className);
     Document data = (new Document("_id", UUID.randomUUID()))
       .append("className", className)
       .append("name", name)
       .append("isCustom", Boolean.valueOf(custom))
       .append("nameColor", nameColor.name())
       .append("materialType", material.toString())
       .append("lvMin", Integer.valueOf(levelMin))
       .append("cooldown", Double.valueOf(cooldown))
       .append("speedBoost", Double.valueOf(speedBoost))
       .append("unbreakable", Boolean.valueOf(unbreakable))
       .append("undroppable", Boolean.valueOf(undroppable))
       .append("damage", Double.valueOf(damage))
       .append("armor", Double.valueOf(armor))
       .append("lore", lore)
       .append("quantity", Integer.valueOf(1))
       .append("maxStackSize", Integer.valueOf(maxStackSize));
     this.itemClassLoader.getItemClassByClassName(className).getAddons().forEach(a -> a.onCreateStorageAccess(data));
     StorageAccess storageAccess = this.storageManager.getNewStorageAccess(GameObjectType.ITEM, data);
     ItemStack itemStack = new ItemStack(material);
     Item item = new Item(itemStack, this.storageManager, storageAccess);
     uuidToItem.put(item.getUUID().toString(), item);
     this.masterRegistry.getRegisteredObjects().add(item);
     return item;
   }
   
   public static Item fromBukkit(ItemStack itemStack) {
     if (itemStack == null)
       return null; 
     if (itemStack.getItemMeta() == null)
       return null; 
     if (itemStack.getItemMeta().getLore() == null)
       return null; 
     if (itemStack.getItemMeta().getLore().size() < 1)
       return null; 
     return uuidToItem.get(HiddenStringUtil.extractHiddenString(itemStack.getItemMeta().getLore().get(0)));
   }
 }


