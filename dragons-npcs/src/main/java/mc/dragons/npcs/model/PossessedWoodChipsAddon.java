package mc.dragons.npcs.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.npcs.util.NPCUtil;

public class PossessedWoodChipsAddon extends WalkingArmorStandAddon {
	private Dragons dragons;
	
	public PossessedWoodChipsAddon(Dragons instance) {
		super("PossessedWoodChips");
		dragons = instance;
	}
	
	@Override
	public void initializeParts(NPC npc) {
		super.initializeParts(npc);
		getBody(npc).getEquipment().setHelmet(new ItemStack(Material.OAK_WOOD));
		//getBody(npc).setHeadPose(NPCUtil.randomRotation());
		for(int i = 0; i < 10; i++) {
			ArmorStand woodChip = newPart(npc);
			woodChip.getEquipment().setHelmet(new ItemStack(Material.OAK_BUTTON));
			woodChip.setHeadPose(NPCUtil.randomRotation());
			woodChip.setMetadata("woodChip", new FixedMetadataValue(dragons, true));
		}
	}
	 
	@Override
	public void onMove(NPC npc, Location loc) {
		super.onMove(npc, loc);
		if(npc.getEntity().isDead() || !npc.getEntity().isValid()) return;
		for(ArmorStand part : parts.get(npc)) {
			if(part.hasMetadata("woodChip")) {
				part.setHeadPose(NPCUtil.randomRotation());
				if(part.getLocation().distanceSquared(loc) > 2.0) {
					part.teleport(loc.clone().add(NPCUtil.randomVector()));
				}
				else {
					part.teleport(part.getLocation().add(NPCUtil.randomVector(0.2)));
				}
			}
		}
		for(int i = 0; i < 2; i++) {
			npc.getEntity().getWorld().spawnParticle(Particle.VILLAGER_ANGRY, npc.getEntity().getLocation().add(NPCUtil.randomVector()), 1);
		}
	}
}
