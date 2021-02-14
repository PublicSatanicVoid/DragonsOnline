package mc.dragons.core.gameobject.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.ItemAddon;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gui.GUIElement;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.PermissionUtil;

public class ItemClass extends GameObject {
	private List<ItemAddon> addons;

	@SuppressWarnings("unchecked")
	public ItemClass(StorageManager storageManager, StorageAccess storageAccess) {
		super(storageManager, storageAccess);
		LOGGER.fine("Constrcting item class (" + storageManager + ", " + storageAccess + ")");
		addons = ((List<String>) getData("addons")).stream()
				.map(addonName -> (ItemAddon) Dragons.getInstance().getAddonRegistry().getAddonByName(addonName)).collect(Collectors.toList());
	}
	
	private void saveAddons() {
		setData("addons", addons.stream().map(a -> a.getName()).collect(Collectors.toList()));
	}

	public void addAddon(ItemAddon addon) {
		addons.add(addon);
		saveAddons();
	}

	public void removeAddon(ItemAddon addon) {
		addons.remove(addon);
		saveAddons();
	}

	public List<ItemAddon> getAddons() {
		return addons;
	}

	public void handleLeftClick(User user) {
		addons.forEach(a -> a.onLeftClick(user));
	}

	public void handleRightClick(User user) {
		addons.forEach(a -> a.onRightClick(user));
	}


	public boolean isWeapon() {
		Material type = getMaterial();
		return Item.isWeapon(type);
	}

	public String getClassName() {
		return (String) getData("className");
	}

	public String getName() {
		return (String) getData("name");
	}

	public void setName(String name) {
		setData("name", name);
	}

	public ChatColor getNameColor() {
		return ChatColor.valueOf((String) getData("nameColor"));
	}

	public void setNameColor(ChatColor nameColor) {
		setData("nameColor", nameColor.name());
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
	
	public boolean isGMLocked() {
		return (boolean) getData("gmlock");
	}
	
	public boolean canUse(User user) {
		return !isGMLocked() || PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, false);
	}
	
	public void setGMLocked(boolean gmlock) {
		setData("gmlock", gmlock);
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

	public double getSpeedBoost() {
		return (double) getData("speedBoost");
	}

	public void setSpeedBoost(double speedBoost) {
		setData("speedBoost", speedBoost);
	}

	@SuppressWarnings("unchecked")
	public List<String> getLore() {
		return (List<String>) getData("lore");
	}

	public void setLore(List<String> lore) {
		setData("lore", lore);
	}

	public int getMaxStackSize() {
		return (int) getData("maxStackSize");
	}

	public void setMaxStackSize(int maxStackSize) {
		setData("maxStackSize", maxStackSize);
	}

	public GUIElement getAsGuiElement(int slot, int quantity, double costPer, boolean custom, Consumer<User> callback) {
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + costPer + " Gold " + (quantity == 1 ? "" : "x" + quantity + " = " + quantity * costPer + " Gold"));
		lore.addAll(Item.getCompleteLore(getData(), getLore().<String>toArray(new String[getLore().size()]), null, custom, this));
		return new GUIElement(slot, getMaterial(), getDecoratedName(), lore, quantity, callback);
	}
}
