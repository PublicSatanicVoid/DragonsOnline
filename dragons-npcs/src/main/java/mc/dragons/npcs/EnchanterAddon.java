package mc.dragons.npcs;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import mc.dragons.core.addon.NPCAddon;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.spells.SpellConstants;

/**
 * Clicking an NPC with this addon will allow the user to
 * bind spells to a capable item by opening up an enchantment
 * GUI.
 * 
 * @author Adam
 *
 */
public class EnchanterAddon extends NPCAddon {
	
	@Override
	public String getName() {
		return "Enchanter";
	}

	@Override
	public void onInteract(NPC with, User from) {
		from.debug("Interacted with Enchanter NPC " + with.getIdentifier());
		Inventory enchanterInventory = Bukkit.createInventory(from.getPlayer(), InventoryType.FURNACE, SpellConstants.ENCHANTER_MENU_TITLE);
		from.getPlayer().openInventory(enchanterInventory);
	}
}
