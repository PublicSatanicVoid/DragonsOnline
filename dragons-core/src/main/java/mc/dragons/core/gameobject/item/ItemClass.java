package mc.dragons.core.gameobject.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.Addon;
import mc.dragons.core.addon.ItemAddon;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gui.GUIElement;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.PermissionUtil;

public class ItemClass extends GameObject {
	private List<ItemAddon> addons;

	public ItemClass(StorageManager storageManager, StorageAccess storageAccess) {
		super(storageManager, storageAccess);
		LOGGER.verbose("Constructing item class (" + storageManager + ", " + storageAccess + ")");
		reloadAddons();
	}
	
	@SuppressWarnings("unchecked")
	public void reloadAddons() {
		addons = ((List<String>) getData("addons")).stream()
				.map(addonName -> (ItemAddon) Dragons.getInstance().getAddonRegistry().getAddonByName(addonName)).collect(Collectors.toList());
	}
	
	public boolean verifyAddons() {
		for(Addon addon : getAddons()) {
			if(addon == null) return false;
		}
		return true;
	}
	
	private void saveAddons() {
		setData("addons", addons.stream().map(a -> a.getName()).collect(Collectors.toList()));
	}

	/**
	 * Register an item addon with this item class.
	 * 
	 * @param addon
	 */
	public void addAddon(ItemAddon addon) {
		addons.add(addon);
		saveAddons();
	}

	/**
	 * Unregister an item addon from this item class.
	 * 
	 * @param addon
	 */
	public void removeAddon(ItemAddon addon) {
		addons.remove(addon);
		saveAddons();
	}

	/**
	 * 
	 * @return All item addons registered to this item class.
	 */
	public List<ItemAddon> getAddons() {
		return addons;
	}

	/**
	 * Propagate a left click event to all item addons.
	 * 
	 * @param user
	 */
	public void handleLeftClick(User user) {
		addons.forEach(a -> a.onLeftClick(user));
	}

	/**
	 * Propagate a right click event to all item addons.
	 * 
	 * @param user
	 */
	public void handleRightClick(User user) {
		addons.forEach(a -> a.onRightClick(user));
	}

	/**
	 * 
	 * @return Whether this item class is a weapon.
	 * 
	 * @apiNote Since item type can be overridden on
	 * individual items, so can weapon status.
	 */
	public boolean isWeapon() {
		Material type = getMaterial();
		return Item.isWeapon(type);
	}

	/**
	 * 
	 * @return The internal (GM) name of this item class.
	 */
	public String getClassName() {
		return (String) getData("className");
	}

	/**
	 * 
	 * @return The undecorated display name of this item class.
	 */
	public String getName() {
		return (String) getData("name");
	}

	/**
	 * Update the undecorated display name of this item class.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		setData("name", name);
	}

	/**
	 * 
	 * @return The default display name color of this item class.
	 */
	public ChatColor getNameColor() {
		return ChatColor.valueOf((String) getData("nameColor"));
	}

	/**
	 * Update the default display name color of this item class.
	 * 
	 * @param nameColor
	 */
	public void setNameColor(ChatColor nameColor) {
		setData("nameColor", nameColor.name());
	}

	/**
	 * 
	 * @return The fully decorated name of this item class.
	 */
	public String getDecoratedName() {
		return getNameColor() + getName();
	}

	/**
	 * 
	 * @return The default Bukkit material type for this item class.
	 */
	public Material getMaterial() {
		return Material.valueOf((String) getData("materialType"));
	}

	/**
	 * Update the default Bukkit material type for this item class.
	 * 
	 * @param material
	 */
	public void setMaterial(Material material) {
		setData("materialType", material.toString());
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
	 * @return Whether this item can only be used by GMs.
	 */
	public boolean isGMLocked() {
		return (boolean) getData("gmlock");
	}
	
	/**
	 * 
	 * @param user
	 * @return Whether the specified user can use this item.
	 */
	public boolean canUse(User user) {
		return !isGMLocked() || PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, false);
	}
	
	/**
	 * Specify whether this item can only be used by GMs.
	 * 
	 * @param gmlock
	 */
	public void setGMLocked(boolean gmlock) {
		setData("gmlock", gmlock);
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

	/**
	 * 
	 * @param slot The slot this item goes into in a GUI.
	 * @param quantity The quantity of this item being sold.
	 * @param costPer The cost per item.
	 * @param custom Whether the item is custom.
	 * @param callback Callback when the user clicks this element.
	 * 
	 * @return A GUI element for use in inventory menus representing
	 * 	a buyable version of this item.
	 */
	public GUIElement getAsGuiElement(int slot, int quantity, double costPer, boolean custom, Consumer<User> callback) {
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + costPer + " Gold " + (quantity == 1 ? "" : "x" + quantity + " = " + quantity * costPer + " Gold"));
		lore.addAll(Item.getCompleteLore(getData(), getLore().<String>toArray(new String[getLore().size()]), null, custom, this));
		return new GUIElement(slot, getMaterial(), getDecoratedName(), lore, quantity, callback);
	}
}
