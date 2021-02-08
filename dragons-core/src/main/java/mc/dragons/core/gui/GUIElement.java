package mc.dragons.core.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import mc.dragons.core.gameobject.user.User;

/**
 * A clickable item within an inventory menu.
 * An item can be associated with a callback,
 * which is called when the item is clicked.
 * 
 * @author Adam
 *
 */
public class GUIElement {
	private int slot;
	private ItemStack itemStack;
	private Consumer<User> callback;

	public GUIElement(int slot, ItemStack itemStack, Consumer<User> callback) {
		this.slot = slot;
		this.itemStack = itemStack;
		this.callback = callback;
	}

	public GUIElement(int slot, ItemStack itemStack) {
		this(slot, itemStack, u -> { });
	}

	public GUIElement(int slot, Material type, String name, List<String> lore, int quantity, Consumer<User> callback) {
		this.slot = slot;
		this.callback = callback;
		ItemStack is = new ItemStack(type, quantity);
		ItemMeta meta = is.getItemMeta();
		meta.setDisplayName(name);
		meta.setLore((lore == null) ? new ArrayList<String>() : lore);
		meta.addItemFlags(new ItemFlag[] { ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE });
		is.setItemMeta(meta);
		this.itemStack = is;
	}

	public GUIElement(int slot, Material type, String name, String lore, int quantity, Consumer<User> callback) {
		this(slot, type, name, lore.equals("") ? new ArrayList<>() : Arrays.<String>asList(lore.split("\n")), quantity, callback);
	}

	public GUIElement(int slot, Material type, String name, String lore, int quantity) {
		this(slot, type, name, lore, quantity, u -> { });
	}

	public GUIElement(int slot, Material type, String name, String lore) {
		this(slot, type, name, lore, 1);
	}

	public GUIElement(int slot, Material type, String name, String lore, Consumer<User> callback) {
		this(slot, type, name, lore, 1, callback);
	}

	public GUIElement(int slot, Material type, String name) {
		this(slot, type, name, "");
	}

	public GUIElement(int slot, Material type, String name, Consumer<User> callback) {
		this(slot, type, name, "", callback);
	}

	public int getSlot() {
		return this.slot;
	}

	public ItemStack getItemStack() {
		return this.itemStack;
	}

	public void click(User user) {
		user.debug("clicked gui element " + this.itemStack.getType() + " x" + this.itemStack.getAmount() + " [" + this.itemStack.getItemMeta().getDisplayName() + "]");
		this.callback.accept(user);
	}

	public Document toDocument() {
		return (new Document("slot", Integer.valueOf(this.slot))).append("type", this.itemStack.getType().toString()).append("amount", Integer.valueOf(this.itemStack.getAmount()))
				.append("name", this.itemStack.getItemMeta().getDisplayName()).append("lore", this.itemStack.getItemMeta().getLore());
	}
}
