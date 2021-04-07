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
	
	public static List<String> getCompleteLore(Document data, String[] customLore, UUID uuid, boolean custom, ItemClass itemClass) {
		//String dataTag = uuid == null ? "" : HiddenStringUtil.encodeString(uuid.toString());
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
		LOGGER.fine("Constructing RPG Item (" + itemStack.getType() + " x" + itemStack.getAmount() + ", " + storageManager + ", " + storageAccess + ")");
		itemClass = itemClassLoader.getItemClassByClassName(getClassName());
		ItemMeta meta = itemStack.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + getDecoratedName());
		meta.setLore(getCompleteLore());
		meta.addItemFlags(new ItemFlag[] { ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE });
		meta.getPersistentDataContainer().set(ITEM_UUID_KEY, PersistentDataType.STRING, getUUID().toString());
		itemStack.setItemMeta(meta);
		itemStack.setAmount(getQuantity());
		if (isUnbreakable()) {
			Dragons.getInstance().getBridge().setItemStackUnbreakable(itemStack, true);
		}
		this.itemStack = itemStack;
		getItemClass().getAddons().forEach(addon -> addon.initialize(this));
	}

	public void updateItemStackData() {
		ItemMeta meta = itemStack.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + getDecoratedName());
		meta.setLore(getCompleteLore());
		meta.addItemFlags(new ItemFlag[] { ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE });
		meta.getPersistentDataContainer().set(ITEM_UUID_KEY, PersistentDataType.STRING, getUUID().toString());
		itemStack.setItemMeta(meta);
		itemStack.setAmount(getQuantity());
	}

	public boolean isCustom() {
		return (boolean) getData("isCustom");
	}

	public void setCustom(boolean custom) {
		setData("isCustom", custom);
	}

	public String getClassName() {
		return (String) getData("className");
	}

	public ItemClass getItemClass() {
		return itemClass;
	}

	public int getQuantity() {
		return (int) getData("quantity");
	}

	public void setQuantity(int quantity) {
		setData("quantity", quantity);
		itemStack.setAmount(quantity);
	}

	public void resyncQuantityFromBukkit() {
		setData("quantity", itemStack.getAmount());
	}

	public String getName() {
		return (String) getData("name");
	}

	public ChatColor getNameColor() {
		return ChatColor.valueOf((String) getData("nameColor"));
	}

	public void setNameColor(ChatColor nameColor) {
		setData("nameColor", nameColor.name());
	}

	public void setName(String name) {
		setData("name", name);
	}

	public double getSpeedBoost() {
		return (double) getData("speedBoost");
	}

	public void setSpeedBoost(double speedBoost) {
		setData("speedBoost", speedBoost);
	}

	public ItemStack rename(String name) {
		setName(name);
		return localRename(name);
	}

	public ItemStack localRename(String name) {
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setDisplayName(name);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public ItemStack relore(String[] lore) {
		setLore(Arrays.asList(lore));
		return localRelore(lore);
	}

	public ItemStack localRelore(String[] lore) {
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setLore(getCompleteLore(lore));
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public String getDecoratedName() {
		return getNameColor() + getName();
	}

	public Material getMaterial() {
		return Material.valueOf((String) getData("materialType"));
	}

	public void setMaterial(Material material) {
		setData("materialType", material.toString());
	}

	public int getLevelMin() {
		return (int) getData("lvMin");
	}

	public void setLevelMin(int lvMin) {
		setData("lvMin", Integer.valueOf(lvMin));
	}

	public double getCooldown() {
		return (double) getData("cooldown");
	}

	public void setCooldown(double cooldown) {
		setData("cooldown", Double.valueOf(cooldown));
	}

	public boolean isUnbreakable() {
		return (boolean) getData("unbreakable");
	}

	public void setUnbreakable(boolean unbreakable) {
		setData("unbreakable", unbreakable);
	}

	public boolean isUndroppable() {
		return (boolean) getData("undroppable");
	}

	public void setUndroppable(boolean undroppable) {
		setData("undroppable", undroppable);
	}

	public double getDamage() {
		return (double) getData("damage");
	}

	public void setDamage(double damage) {
		setData("damage", damage);
	}

	public double getArmor() {
		return (double) getData("armor");
	}

	public void setArmor(double armor) {
		setData("armor", armor);
	}

	@SuppressWarnings("unchecked")
	public List<String> getLore() {
		return (List<String>) getData("lore");
	}

	public void setLore(List<String> lore) {
		setData("lore", lore);
	}

	public ItemStack getItemStack() {
		return itemStack;
	}

	public void setItemStack(ItemStack itemStack) {
		this.itemStack = itemStack;
		updateItemStackData();
	}

	public void registerUse() {
		getLocalData().append("lastUsed", System.currentTimeMillis());
	}

	public double getCooldownRemaining() {
		return Math.max(0.0D, getCooldown() - (System.currentTimeMillis() - (long)(getLocalData().getOrDefault("lastUsed", 0L))) / 1000.0D);
	}

	public boolean hasCooldownRemaining() {
		return Math.abs(getCooldownRemaining()) > 0.001D;
	}

	@Override
	public void autoSave() {
		super.autoSave();
		setData("quantity", itemStack.getAmount());
	}

	public int getMaxStackSize() {
		return (int) getData("maxStackSize");
	}

	public void setMaxStackSize(int maxStackSize) {
		setData("maxStackSize", maxStackSize);
	}
}
