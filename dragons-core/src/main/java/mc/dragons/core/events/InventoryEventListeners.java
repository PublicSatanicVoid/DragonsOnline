package mc.dragons.core.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gui.GUI;

public class InventoryEventListeners implements Listener {
	private ItemLoader itemLoader = GameObjectType.ITEM.<Item, ItemLoader>getLoader();

	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent e) {
		if (e.getInventory().getType() == InventoryType.MERCHANT)
			e.setCancelled(true);
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent e) {
		Player player = (Player) e.getPlayer();
		User user = UserLoader.fromPlayer(player);
		user.debug("inventory close event. invTitle=" + e.getInventory().getTitle() + ", viewtype=" + e.getView().getType());
		if (user.hasOpenGUI()) {
			user.debug("- has open gui");
			if (!user.hasHotfixedGUI(user.getCurrentGUI())) {
				user.debug("- hotfix for random server-side closing crap");
				user.hotfixGUI();
				return;
			}
			if (e.getInventory().getTitle().equals(user.getCurrentGUI().getMenuName())) {
				user.debug("- matches gui name " + user.getCurrentGUI().getMenuName() + ", marking closed");
				user.closeGUI(false);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		Player player = (Player) e.getWhoClicked();
		User user = UserLoader.fromPlayer(player);
		user.debug("Inventory click event. Action=" + e.getAction() + ", clickType=" + e.getClick() + ", slot=" + e.getSlot());
		if (user.hasOpenGUI()) {
			Inventory clickedInventory = e.getClickedInventory();
			if (clickedInventory == null)
				return;
			GUI gui = user.getCurrentGUI();
			user.debug("gui name=" + gui.getMenuName() + ", clicked name=" + clickedInventory.getName());
			if (clickedInventory.getName().equals(gui.getMenuName())) {
				gui.click(user, e.getSlot());
				e.setCancelled(true);
				user.debug("cancelling click event since it's a gui");
			}
			return;
		}
		if (e.getAction() == InventoryAction.SWAP_WITH_CURSOR && e.getClick() == ClickType.LEFT) {
			user.debug("- Merge candidate");
			ItemStack mergeWith = player.getInventory().getItem(e.getSlot());
			ItemStack mergeFrom = e.getCursor();
			if (mergeWith == null || mergeFrom == null) {
				user.debug("- Cannot merge: one or more item stacks is null (with=" + mergeWith + ",from=" + mergeFrom + ")");
				return;
			}
			Item rpgMergeWith = ItemLoader.fromBukkit(mergeWith);
			Item rpgMergeFrom = ItemLoader.fromBukkit(mergeFrom);
			if (rpgMergeWith == null || rpgMergeFrom == null) {
				user.debug("- Cannot merge: one or more rpg items is null (with=" + rpgMergeWith + ",from=" + rpgMergeFrom + ")");
				return;
			}
			if (rpgMergeWith.getClassName().contentEquals(rpgMergeFrom.getClassName()) && !rpgMergeWith.isCustom() && !rpgMergeFrom.isCustom()) {
				int maxStackSize = rpgMergeWith.getMaxStackSize();
				user.debug("- Merge allowed");
				int quantity = rpgMergeWith.getItemStack().getAmount() + rpgMergeFrom.getItemStack().getAmount();
				if (quantity > maxStackSize) {
					rpgMergeFrom.setQuantity(quantity - maxStackSize);
					e.setCursor(rpgMergeFrom.getItemStack());
				} else {
					user.takeItem(rpgMergeFrom, rpgMergeFrom.getQuantity(), true, false, false);
					e.setCursor(null);
				}
				rpgMergeWith.setQuantity(Math.min(quantity, maxStackSize));
				e.setCurrentItem(rpgMergeWith.getItemStack());
				user.debug("- Merge complete (total quantity=" + quantity + ")");
			}
		}
		if (e.getAction() == InventoryAction.PICKUP_HALF || e.getAction() == InventoryAction.PICKUP_SOME) {
			user.debug("- Split candidate");
			ItemStack splitFrom = e.getCurrentItem();
			Item rpgSplitFrom = ItemLoader.fromBukkit(splitFrom);
			user.debug("- from=" + rpgSplitFrom);
			if (rpgSplitFrom == null) {
				user.debug("- Cannot split: split from is null");
				return;
			}
			int quantity = splitFrom.getAmount();
			int fromQuantity = quantity / 2;
			int toQuantity = quantity / 2;
			if (quantity % 2 == 1)
				fromQuantity++;
			Item rpgSplitToCopy = this.itemLoader.registerNew(rpgSplitFrom);
			rpgSplitToCopy.setQuantity(toQuantity);
			rpgSplitToCopy.setItemStack(splitFrom.clone());
			rpgSplitFrom.setQuantity(fromQuantity);
			e.setCursor(rpgSplitToCopy.getItemStack());
			e.setCurrentItem(rpgSplitFrom.getItemStack());
			user.debug("- Split complete (fromQuantity=" + rpgSplitFrom.getQuantity() + "[" + splitFrom.getAmount() + "], toQuantity=" + rpgSplitToCopy.getQuantity() + "["
					+ rpgSplitToCopy.getItemStack().getAmount() + "])");
		}
	}
}
