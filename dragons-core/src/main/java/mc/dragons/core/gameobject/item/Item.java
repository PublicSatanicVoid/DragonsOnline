package mc.dragons.core.gameobject.item;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
	private static ItemClassLoader itemClassLoader = GameObjectType.ITEM_CLASS.<ItemClass, ItemClassLoader>getLoader();

	private ItemStack itemStack;
	private ItemClass itemClass;

	private List<String> getCompleteLore() {
		return itemClass.getCompleteLore(getLore().<String>toArray(new String[getLore().size()]), getUUID(), isCustom());
	}

	private List<String> getCompleteLore(String[] customLore) {
		return itemClass.getCompleteLore(customLore, getUUID(), isCustom());
	}
	
	public Item(ItemStack itemStack, StorageManager storageManager, StorageAccess storageAccess) {
		super(storageManager, storageAccess);
		LOGGER.fine("Constructing RPG Item (" + itemStack.getType() + " x" + itemStack.getAmount() + ", " + storageManager + ", " + storageAccess + ")");
		itemClass = itemClassLoader.getItemClassByClassName(getClassName());
		ItemMeta meta = itemStack.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + getDecoratedName());
		meta.setLore(getCompleteLore());
		meta.addItemFlags(new ItemFlag[] { ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE });
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
