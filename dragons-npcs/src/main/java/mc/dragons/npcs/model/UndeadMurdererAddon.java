package mc.dragons.npcs.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.npcs.ComplexAddon;

public class UndeadMurdererAddon extends ComplexAddon {
	
	public UndeadMurdererAddon() {
		super("UndeadMurderer");
	}

	@Override
	public void initializeParts(NPC npc) {
		ArmorStand body = (ArmorStand) npc.getEntity().getWorld().spawnEntity(npc.getEntity().getLocation(), EntityType.ARMOR_STAND);
		body.getEquipment().setHelmet(new ItemStack(Material.CLAY));
		body.getEquipment().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
		body.getEquipment().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
		body.getEquipment().setBoots(new ItemStack(Material.LEATHER_BOOTS));
		body.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_AXE));
		body.setBasePlate(false);
		body.setCustomNameVisible(true);
		body.setGravity(false);
		npc.setExternalHealthIndicator(body);
		registerPart(npc, body);
	}

	@Override
	public void onMove(NPC npc, Location loc) {
		loc.setYaw(npc.getEntity().getLocation().getYaw());
		super.onMove(npc, loc);
	}
	
}
