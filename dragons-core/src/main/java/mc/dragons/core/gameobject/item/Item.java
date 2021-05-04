package mc.dragons.core.gameobject.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;

/**
 * Represents a general item in the RPG.
 * 
 * <p>In addition to regular properties of Minecraft items,
 * RPG items have additional properties, like XP/skill
 * requirements, use effects, etc.
 * 
 * <p>More than one RPG item may be mapped to the same
 * Minecraft item, and vice versa.
 * 
 * @author Adam
 *
 */
public class Item extends GameObject {
	public static NamespacedKey ITEM_UUID_KEY = new NamespacedKey(Dragons.getInstance(), "dragons-uuid");
	
	private static ItemClassLoader itemClassLoader = GameObjectType.ITEM_CLASS.<ItemClass, ItemClassLoader>getLoader();

	private ItemStack itemStack;
	private ItemClass itemClass;

	
	public static boolean isWeapon(Material type) {
		return type == Material.BOW || type == Material.DIAMOND_SWORD || type == Material.GOLDEN_SWORD || type == Material.IRON_SWORD || type == Material.STONE_SWORD 
				|| type == Material.WOODEN_SWORD || type == Material.STICK;
	}
	
	/**
	 * 
	 * @param data
	 * @param customLore
	 * @param uuid
	 * @param custom
	 * @param itemClass
	 * @return The complete Bukkit lore of the item, generated from its custom data.
	 */
	public static List<String> getCompleteLore(Document data, String[] customLore, UUID uuid, boolean custom, ItemClass itemClass) {
		List<String> lore = new ArrayList<>(Arrays.asList(new String[] { ChatColor.GRAY + "Lv Min: " + data.getInteger("lvMin") }));
		if (customLore.length > 0) {
			lore.add("");
		}
		lore.addAll(Arrays.<String>asList(customLore).stream().map(line -> ChatColor.DARK_PURPLE + " " + ChatColor.ITALIC + line).collect(Collectors.toList()));
		List<String> statsMeta = new ArrayList<>();
		double damage = data.getDouble("damage");
		double armor = data.getDouble("armor");
		boolean isWeapon = Item.isWeapon(Material.valueOf(data.getString("materialType")));
		double cooldown = data.getDouble("cooldown");
		double speedBoost = data.getDouble("speedBoost");
		boolean unbreakable = data.getBoolean("unbreakable");
		boolean undroppable = data.getBoolean("undroppable");
		boolean gmLock = itemClass.isGMLocked();
		if (damage > 0.0D) {
			statsMeta.add(ChatColor.GREEN + " " + damage + " Damage");
		}
		if (armor > 0.0D) {
			statsMeta.add(ChatColor.GREEN + " " + armor + " Armor");
		}
		if (isWeapon) {
			statsMeta.add(ChatColor.GREEN + " " + cooldown + "s Attack Speed");
		}
		if (speedBoost != 0.0D) {
			statsMeta.add(" " + (speedBoost < 0.0D ? ChatColor.RED : ChatColor.GREEN + "+") + speedBoost + " Walk Speed");
		}
		if (unbreakable || undroppable || gmLock) {
			statsMeta.add("");
		}
		if (unbreakable) {
			statsMeta.add(ChatColor.BLUE + "Unbreakable");
		}
		if (undroppable) {
			statsMeta.add(ChatColor.BLUE + "Undroppable");
		}
		if(gmLock) {
			statsMeta.add(ChatColor.RED + "GM Locked");
		}
		if (custom) {
			statsMeta.addAll(Arrays.asList(new String[] { "", ChatColor.AQUA + "Custom Item" }));
		}
		if (statsMeta.size() > 0) {
			lore.addAll(Arrays.asList(new String[] { "", ChatColor.GRAY + "When equipped:" }));
			lore.addAll(statsMeta);
		}
		return lore;
	}
	
	private List<String> getCompleteLore() {
		return getCompleteLore(getData(), getLore().<String>toArray(new String[getLore().size()]), getUUID(), isCustom(), itemClass);
	}

	private List<String> getCompleteLore(String[] customLore) {
		return getCompleteLore(getData(), customLore, getUUID(), isCustom(), itemClass);
	}
	
	public Item(ItemStack itemStack, StorageManager storageManager, StorageAccess storageAccess) {
		super(storageManager, storageAccess);
		LOGGER.verbose("Constructing RPG Item (" + itemStack.getType() + " x" + itemStack.getAmount() + ", " + storageManager + ", " + storageAccess + ")");
		itemClass = itemClassLoader.getItemClassByClassName(getClassName());
		ItemMeta meta = itemStack.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + getDecoratedName());
		meta.setLore(getCompleteLore());
		meta.setUnbreakable(isUnbreakable());
		meta.addItemFlags(new ItemFlag[] { ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE });
		meta.getPersistentDataContainer().set(ITEM_UUID_KEY, PersistentDataType.STRING, getUUID().toString());
		itemStack.setItemMeta(meta);
		itemStack.setAmount(getQuantity());
		this.itemStack = itemStack;
		getItemClass().getAddons().forEach(addon -> addon.initialize(this));
	}

	/**
	 * Propagate changes to the item's configuration to its Bukkit item metadata.
	 */
	public void updateItemStackData() {
		ItemMeta meta = itemStack.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + getDecoratedName());
		meta.setLore(getCompleteLore());
		meta.addItemFlags(new ItemFlag[] { ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE });
		meta.getPersistentDataContainer().set(ITEM_UUID_KEY, PersistentDataType.STRING, getUUID().toString());
		itemStack.setItemMeta(meta);
		itemStack.setAmount(getQuantity());
	}

	/**
	 * 
	 * @return Whether this item overrides attributes of its base item class.
	 */
	public boolean isCustom() {
		return (boolean) getData("isCustom");
	}

	/**
	 * Indicate whether this item overrides attributes of its base item class.
	 * 
	 * @param custom
	 */
	public void setCustom(boolean custom) {
		setData("isCustom", custom);
	}

	/**
	 * 
	 * @return The internal (GM) name for this item's item class.
	 */
	public String getClassName() {
		return (String) getData("className");
	}

	/**
	 * 
	 * @return The item class associated with this item.
	 */
	public ItemClass getItemClass() {
		return itemClass;
	}

	/**
	 * 
	 * @return The quantity of this item.
	 */
	public int getQuantity() {
		return (int) getData("quantity");
	}
	
	/**
	 * Set the quantity of this item.
	 * 
	 * @param quantity
	 */
	public void setQuantity(int quantity) {
		setData("quantity", quantity);
		itemStack.setAmount(quantity);
	}

	/**
	 * Persist the live Bukkit item stack quantity to backend storage.
	 */
	public void resyncQuantityFromBukkit() {
		setData("quantity", itemStack.getAmount());
	}

	/**
	 * 
	 * @return The raw configured display name of this item,
	 * without decorations or modifiers.
	 */
	public String getName() {
		return (String) getData("name");
	}

	/**
	 * 
	 * @return The default color of this item's display name.
	 */
	public ChatColor getNameColor() {
		return ChatColor.valueOf((String) getData("nameColor"));
	}

	/**
	 * Change the default color of this item's display name.
	 * 
	 * @param nameColor
	 */
	public void setNameColor(ChatColor nameColor) {
		setData("nameColor", nameColor.name());
	}

	/**
	 * Set the undecorated display name of this item.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		setData("name", name);
	}

	/**
	 * 
	 * @return The Bukkit walk speed modifier applied to players
	 * holding this item.
	 */
	public double getSpeedBoost() {
		return (double) getData("speedBoost");
	}

	/**
	 * Set the Bukkit walk speed modifier applied to players
	 * holding this item.
	 * 
	 * @param speedBoost
	 */
	public void setSpeedBoost(double speedBoost) {
		setData("speedBoost", speedBoost);
	}

	/**
	 * Update the name of this item persistently and locally.
	 * 
	 * @param name
	 * @return
	 */
	public ItemStack rename(String name) {
		setName(name);
		return localRename(name);
	}

	/**
	 * Update the name of this item locally.
	 * 
	 * @implSpec Does not persist these changes.
	 * 
	 * @param name
	 * @return
	 */
	public ItemStack localRename(String name) {
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setDisplayName(name);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	/**
	 * Update the <i>custom</i> lore of this item persistently and locally.
	 * 
	 * @apiNote Generic lore about damage, speed modifiers, etc. is generated
	 * automatically.
	 * 
	 * @param lore
	 * @return
	 */
	public ItemStack relore(String[] lore) {
		setLore(Arrays.asList(lore));
		return localRelore(lore);
	}

	/**
	 * Update the <i>custom</i> lore of this item locally.
	 * 
	 * @apiNote Generic lore about damage, speed modifiers, etc. is generated 
	 * automatically.
	 * @implSpec Does not persist these changes.
	 * 
	 * @param lore
	 * @return
	 */
	public ItemStack localRelore(String[] lore) {
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setLore(getCompleteLore(lore));
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	/**
	 * 
	 * @return The fully decorated name of this item, excluding cooldowns.
	 */
	public String getDecoratedName() {
		return getNameColor() + getName();
	}

	/**
	 * 
	 * @return The Bukkit type of this item.
	 */
	public Material getMaterial() {
		return Material.valueOf((String) getData("materialType"));
	}

	/**
	 * Set the Bukkit type of this item persistently and locally.
	 * 
	 * @param material
	 */
	public void setMaterial(Material material) {
		setData("materialType", material.toString());
		itemStack.setType(material);
	}

	/**
	 * 
	 * @return The minimum level required to use this item.
	 * 
	 * @apiNote If the minimum level is not met, no modifiers
	 * will be applied from this item.
	 */
	public int getLevelMin() {
		return (int) getData("lvMin");
	}

	/**
	 * Update the minimum level required to use this item.
	 * 
	 * @param lvMin
	 */
	public void setLevelMin(int lvMin) {
		setData("lvMin", Integer.valueOf(lvMin));
	}

	/**
	 * 
	 * @return The cooldown in seconds before this item can be used again.
	 */
	public double getCooldown() {
		return (double) getData("cooldown");
	}

	/**
	 * Update the cooldown in seconds before this item can be used again.
	 * 
	 * @param cooldown
	 */
	public void setCooldown(double cooldown) {
		setData("cooldown", Double.valueOf(cooldown));
	}

	/**
	 * 
	 * @return Whether this item is unbreakable (has infinite durability).
	 */
	public boolean isUnbreakable() {
		return (boolean) getData("unbreakable");
	}

	/**
	 * Update this item's unbreakability status.
	 * 
	 * @param unbreakable
	 */
	public void setUnbreakable(boolean unbreakable) {
		setData("unbreakable", unbreakable);
	}

	/**
	 * 
	 * @return Whether this item is undroppable.
	 * 
	 * @apiNote Staff members in creative mode override this restriction.
	 */
	public boolean isUndroppable() {
		return (boolean) getData("undroppable");
	}

	/**
	 * Update this item's undroppability status.
	 * 
	 * @apiNote Staff members in creative mode override this restriction.
	 * 
	 * @param undroppable
	 */
	public void setUndroppable(boolean undroppable) {
		setData("undroppable", undroppable);
	}

	/**
	 * 
	 * @return The base damage of this item.
	 */
	public double getDamage() {
		return (double) getData("damage");
	}

	/**
	 * Update the base damage of this item.
	 * 
	 * @param damage
	 */
	public void setDamage(double damage) {
		setData("damage", damage);
	}

	/**
	 * 
	 * @return The base armor (damage absorption) of this item.
	 * 
	 * @apiNote Armor is probabilistic, so an armor class of X
	 * means that damage will be reduced by between 0 and X points.
	 */
	public double getArmor() {
		return (double) getData("armor");
	}
	
	/**
	 * 
	 * Update the base armor (damage absorption) of this item.
	 * 
	 * @apiNote Armor is probabilistic, so an armor class of X
	 * means that damage will be reduced by between 0 and X points.
	 */
	public void setArmor(double armor) {
		setData("armor", armor);
	}

	/**
	 * 
	 * @return The <i>custom</i> lore of this item, not including
	 * any generic lore.
	 */
	@SuppressWarnings("unchecked")
	public List<String> getLore() {
		return (List<String>) getData("lore");
	}

	/**
	 * Update the <i>custom</i> lore of this item, not including 
	 * any generic lore.
	 * 
	 * @param lore
	 */
	public void setLore(List<String> lore) {
		setData("lore", lore);
	}

	/**
	 * 
	 * @return The Bukkit item stack associated with this item.
	 */
	public ItemStack getItemStack() {
		return itemStack;
	}

	/**
	 * Change the Bukkit item stack associated with this item.
	 * 
	 * <p>May cause synchronization issues or duplication if the
	 * previously associated item is not disposed of or unregistered
	 * properly.
	 * 
	 * @param itemStack
	 */
	public void setItemStack(ItemStack itemStack) {
		this.itemStack = itemStack;
		updateItemStackData();
	}

	/**
	 * Log a use of this item, for cooldown purposes.
	 * 
	 * @implSpec Currently not reflected in persistent data.
	 */
	public void registerUse() {
		getLocalData().append("lastUsed", System.currentTimeMillis());
	}

	/**
	 * 
	 * @return The cooldown remaining in seconds until this item can
	 * be used again.
	 */
	public double getCooldownRemaining() {
		return Math.max(0.0D, getCooldown() - (System.currentTimeMillis() - (long)(getLocalData().getOrDefault("lastUsed", 0L))) / 1000.0D);
	}

	/**
	 * 
	 * @return Whether there is any cooldown remaining on this item.
	 */
	public boolean hasCooldownRemaining() {
		return Math.abs(getCooldownRemaining()) > 0.001D;
	}

	/**
	 * Persist the item quantity to the database.
	 */
	@Override
	public void autoSave() {
		super.autoSave();
		setData("quantity", itemStack.getAmount());
	}

	/**
	 * 
	 * @return The maximum size this item can be stacked.
	 * 
	 * @apiNote This overrides any Bukkit defaults, meaning
	 * that (for example) swords can be configured to be stackable
	 * up to 64.
	 * 
	 */
	public int getMaxStackSize() {
		return (int) getData("maxStackSize");
	}

	/**
	 * Update the maximum stack size of this item.
	 * 
	 * @apiNote This overrides any Bukkit defaults, meaning
	 * that (for example) swords can be configured to be stackable
	 * up to 64.
	 * 
	 * @param maxStackSize
	 */
	public void setMaxStackSize(int maxStackSize) {
		setData("maxStackSize", maxStackSize);
	}
}
