package mc.dragons.npcs;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import mc.dragons.core.addon.NPCAddon;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.user.User;

public class EnchanterAddon extends NPCAddon {
	
	@Override
	public String getName() {
		return "Enchanter";
	}

	@Override
	public void initialize(GameObject gameObject) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMove(NPC npc, Location loc) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTakeDamage(NPC on, GameObject from, double amount) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDealDamage(NPC from, GameObject to, double amount) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeath(NPC gameObject) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInteract(NPC with, User from) {
		from.debug("Interacted with Enchanter NPC " + with.getIdentifier());
		Inventory enchanterInventory = Bukkit.createInventory(from.getPlayer(), InventoryType.FURNACE, ChatColor.LIGHT_PURPLE + "Enchanter");
		from.getPlayer().openInventory(enchanterInventory);
	}

	@Override
	public void onEnable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCreateStorageAccess(Document data) {
		// TODO Auto-generated method stub
		
	}

}
