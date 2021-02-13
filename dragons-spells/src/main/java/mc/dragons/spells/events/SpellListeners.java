package mc.dragons.spells.events;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftInventoryCustom;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.ItemStack;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.spells.SpellCastAddon;
import mc.dragons.spells.SpellConfig;
import mc.dragons.spells.spells.Spell;
import mc.dragons.spells.spells.Spell.BindStatus;

public class SpellListeners implements Listener {
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		Player player = (Player) e.getWhoClicked();
		User user = UserLoader.fromPlayer(player);
		if(e.getInventory().getType() == InventoryType.FURNACE) {
			// 0 = smelting
			// 1 = fuel
			// 2 = result
			CraftInventory cc = (CraftInventory) e.getInventory();
			int i = 0;
			for(ItemStack is : cc.getContents()) {
				user.debug(i + ": " + is);
				i++;
			}
			
		}
		if(e.getInventory().getName().equals(SpellConfig.ENCHANTER_MENU_TITLE)) {
			user.debug("Interacted with the Enchanter menu. SlotType="+e.getSlotType());
			CraftInventoryCustom cc = (CraftInventoryCustom) e.getInventory();
			if(e.getSlotType() == SlotType.RESULT) {
				user.debug("RESULT");
				user.debug("curr="+e.getCurrentItem());
				user.debug("curs="+e.getCursor());
				Item current = ItemLoader.fromBukkit(e.getCurrentItem());
				if(current == null) return;
				user.debug("receiving the updated item");
				current.setItemStack(e.getCurrentItem().clone());
				e.setCurrentItem(new ItemStack(Material.AIR));
				e.setCursor(current.getItemStack());
				cc.setItem(0, new ItemStack(Material.AIR));
				cc.setItem(1, new ItemStack(Material.AIR));
				cc.setItem(2, new ItemStack(Material.AIR));
				e.setResult(Result.DENY);
				e.setCancelled(false);
				return;
			}
			ItemStack cursorCopy = e.getCursor().clone();
			Item item = ItemLoader.fromBukkit(e.getCursor());
			if(item == null) return;
			if(e.getSlotType() == SlotType.FUEL && item.getItemClass().getAddons().contains(Dragons.getInstance().getAddonRegistry().getAddonByName("SpellScroll"))) {
				user.debug("Using alternative fuel in furnace for enchanting");
				user.debug("curr=" + e.getCurrentItem() + ", cursor=" + e.getCursor());
				item.setItemStack(cursorCopy);
				if(e.getInventory().getType() == InventoryType.FURNACE) {
					int i = 0;
					for(ItemStack is : cc.getContents()) {
						user.debug(i + ": " + is);
						i++;
					}
					e.setCurrentItem(cursorCopy);
					e.setCursor(new ItemStack(Material.AIR));
					e.setCancelled(true);
				}
			}
			if(e.getSlotType() == SlotType.CRAFTING && item.getItemClass().getAddons().contains(Dragons.getInstance().getAddonRegistry().getAddonByName("SpellCast"))) {
				if(cc.getItem(1) == null) return; // No fuel
				String spellDisplayName = ChatColor.stripColor(cc.getItem(1).getItemMeta().getDisplayName().replace("Scroll", "").trim());
				user.debug("binding spell " + spellDisplayName);
				Spell spell = SpellCastAddon.getSpellRegistry().getSpellByDisplayName(spellDisplayName);
				user.debug("spell="+spell);
				if(spell.bindStatus(item) == BindStatus.CAPABLE) {
					double gold = user.getGold();
					if(gold < 100.0) {
						player.sendMessage(ChatColor.RED + "Binding spells costs 100 Gold. (Need " + (100 - gold) + " more)");
						player.getInventory().addItem(cc.getItem(1) == null ? new ItemStack(Material.AIR) : cc.getItem(1));
						player.getInventory().addItem(e.getCursor());
						e.setCursor(new ItemStack(Material.AIR));
						e.setCurrentItem(new ItemStack(Material.AIR));
						e.setCancelled(true);
						player.closeInventory();
						return;
					}
					user.takeGold(100.0);
					spell.bind(item);
					cc.setItem(0, new ItemStack(Material.AIR));
					cc.setItem(1, new ItemStack(Material.AIR));
					cc.setItem(2, item.getItemStack());
					e.setCursor(new ItemStack(Material.AIR));
					e.setResult(Result.ALLOW);
					e.setCancelled(true);
					user.debug("bound spell and updated inventory");
					player.sendMessage(ChatColor.DARK_PURPLE + "Bound spell " + ChatColor.LIGHT_PURPLE + spell.getSpellDisplayName() 
						+ ChatColor.DARK_PURPLE + " to " + ChatColor.LIGHT_PURPLE + item.getName());
				}
			}
			return;
		}
	}
}
