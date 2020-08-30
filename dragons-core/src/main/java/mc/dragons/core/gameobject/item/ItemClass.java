package mc.dragons.core.gameobject.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import mc.dragons.core.Dragons;
import mc.dragons.core.addon.ItemAddon;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gui.GUIElement;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.HiddenStringUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public class ItemClass extends GameObject {
	private List<ItemAddon> addons;

	@SuppressWarnings("unchecked")
	public ItemClass(StorageManager storageManager, StorageAccess storageAccess) {
		super(storageManager, storageAccess);
		LOGGER.fine("Constrcting item class (" + storageManager + ", " + storageAccess + ")");
		this.addons = ((List<String>) getData("addons")).stream().map(addonName -> (ItemAddon) Dragons.getInstance().getAddonRegistry().getAddonByName(addonName)).collect(Collectors.toList());
	}

	private void saveAddons() {
		setData("addons", this.addons.stream().map(a -> a.getName()).collect(Collectors.toList()));
	}

	public void addAddon(ItemAddon addon) {
		this.addons.add(addon);
		saveAddons();
	}

	public void removeAddon(ItemAddon addon) {
		this.addons.remove(addon);
		saveAddons();
	}

	public List<ItemAddon> getAddons() {
		return this.addons;
	}

	public void handleLeftClick(User user) {
		this.addons.forEach(a -> a.onLeftClick(user));
	}

	public void handleRightClick(User user) {
		this.addons.forEach(a -> a.onRightClick(user));
	}

	public List<String> getCompleteLore(String[] customLore, UUID uuid, boolean custom) {
		String dataTag = (uuid == null) ? "" : HiddenStringUtil.encodeString(uuid.toString());
		List<String> lore = new ArrayList<>(Arrays.asList(new String[] { ChatColor.GRAY + "Lv Min: " + getLevelMin() + dataTag }));
		if (customLore.length > 0)
			lore.add("");
		lore.addAll((Collection<? extends String>) Arrays.<String>asList(customLore).stream().map(line -> ChatColor.DARK_PURPLE + " " + ChatColor.ITALIC + line).collect(Collectors.toList()));
		List<String> statsMeta = new ArrayList<>();
		if (getDamage() > 0.0D)
			statsMeta.add(ChatColor.GREEN + " " + getDamage() + " Damage");
		if (getArmor() > 0.0D)
			statsMeta.add(ChatColor.GREEN + " " + getArmor() + " Armor");
		if (isWeapon())
			statsMeta.add(ChatColor.GREEN + " " + getCooldown() + "s Attack Speed");
		if (getSpeedBoost() != 0.0D)
			statsMeta.add(" " + ((getSpeedBoost() < 0.0D) ? ChatColor.RED : (ChatColor.GREEN + "+")) + getSpeedBoost() + " Walk Speed");
		if (isUnbreakable() || isUndroppable())
			statsMeta.add("");
		if (isUnbreakable())
			statsMeta.add(ChatColor.BLUE + "Unbreakable");
		if (isUndroppable())
			statsMeta.add(ChatColor.BLUE + "Undroppable");
		if (custom)
			statsMeta.addAll(Arrays.asList(new String[] { "", ChatColor.AQUA + "Custom Item" }));
		if (statsMeta.size() > 0) {
			lore.addAll(Arrays.asList(new String[] { "", ChatColor.GRAY + "When equipped:" }));
			lore.addAll(statsMeta);
		}
		return lore;
	}

	public boolean isWeapon() {
		Material type = getMaterial();
		return !(type != Material.BOW && type != Material.DIAMOND_SWORD && type != Material.GOLD_SWORD && type != Material.IRON_SWORD && type != Material.STONE_SWORD && type != Material.WOOD_SWORD
				&& type != Material.STICK);
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
		return ((Integer) getData("lvMin")).intValue();
	}

	public void setLevelMin(int lvMin) {
		setData("lvMin", Integer.valueOf(lvMin));
	}

	public double getCooldown() {
		return ((Double) getData("cooldown")).doubleValue();
	}

	public void setCooldown(double cooldown) {
		setData("cooldown", Double.valueOf(cooldown));
	}

	public boolean isUnbreakable() {
		return ((Boolean) getData("unbreakable")).booleanValue();
	}

	public void setUnbreakable(boolean unbreakable) {
		setData("unbreakable", Boolean.valueOf(unbreakable));
	}

	public boolean isUndroppable() {
		return ((Boolean) getData("undroppable")).booleanValue();
	}

	public void setUndroppable(boolean undroppable) {
		setData("undroppable", Boolean.valueOf(undroppable));
	}

	public double getDamage() {
		return ((Double) getData("damage")).doubleValue();
	}

	public void setDamage(double damage) {
		setData("damage", Double.valueOf(damage));
	}

	public double getArmor() {
		return ((Double) getData("armor")).doubleValue();
	}

	public void setArmor(double armor) {
		setData("armor", Double.valueOf(armor));
	}

	public double getSpeedBoost() {
		return ((Double) getData("speedBoost")).doubleValue();
	}

	public void setSpeedBoost(double speedBoost) {
		setData("speedBoost", Double.valueOf(speedBoost));
	}

	@SuppressWarnings("unchecked")
	public List<String> getLore() {
		return (List<String>) getData("lore");
	}

	public void setLore(List<String> lore) {
		setData("lore", lore);
	}

	public int getMaxStackSize() {
		return ((Integer) getData("maxStackSize")).intValue();
	}

	public void setMaxStackSize(int maxStackSize) {
		setData("maxStackSize", Integer.valueOf(maxStackSize));
	}

	public GUIElement getAsGuiElement(int slot, int quantity, double costPer, boolean custom, Consumer<User> callback) {
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + costPer + " Gold " + ((quantity == 1) ? "" : ("x" + quantity + " = " + (quantity * costPer) + " Gold")));
		lore.addAll(getCompleteLore(getLore().<String>toArray(new String[getLore().size()]), (UUID) null, custom));
		return new GUIElement(slot, getMaterial(), getDecoratedName(), lore, quantity, callback);
	}
}
