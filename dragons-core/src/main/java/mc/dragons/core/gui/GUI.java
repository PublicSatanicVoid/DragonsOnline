package mc.dragons.core.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

import mc.dragons.core.gameobject.user.User;

public class GUI {
	private Map<Integer, GUIElement> elements;
	private int rows;
	private String name;

	public GUI(int rows, String menuName) {
		this.elements = new HashMap<>();
		this.rows = rows;
		this.name = menuName;
	}

	public GUI add(GUIElement element) {
		this.elements.put(Integer.valueOf(element.getSlot()), element);
		return this;
	}

	public Map<Integer, GUIElement> getElements() {
		return this.elements;
	}

	public int getRows() {
		return this.rows;
	}

	public String getMenuName() {
		return this.name;
	}

	public void open(User user) {
		Inventory inventory = Bukkit.createInventory(user.getPlayer(), this.rows * 9, this.name);
		for (Entry<Integer, GUIElement> element : this.elements.entrySet())
			inventory.setItem(element.getKey(), element.getValue().getItemStack());
		user.openGUI(this, inventory);
	}

	public void click(User user, int slot) {
		user.debug("clicked slot " + slot);
		GUIElement clicked = this.elements.getOrDefault(Integer.valueOf(slot), null);
		user.debug(" - clicked=" + clicked);
		if (clicked == null)
			return;
		clicked.click(user);
	}

	public Document toDocument() {
		Document document = (new Document("name", this.name)).append("rows", Integer.valueOf(this.rows));
		List<Document> items = new ArrayList<>();
		for (Entry<Integer, GUIElement> element : this.elements.entrySet())
			items.add(element.getValue().toDocument());
		document.append("items", items);
		return document;
	}
}
