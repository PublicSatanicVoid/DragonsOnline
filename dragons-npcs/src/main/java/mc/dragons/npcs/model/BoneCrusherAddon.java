package mc.dragons.npcs.model;

import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.npcs.ComplexAddon;

public class BoneCrusherAddon extends ComplexAddon {

	public BoneCrusherAddon() {
		super("BoneCrusher");
	}

	@Override
	public void initializeParts(NPC npc) {
		ArmorStand spineBuf = null;
		for(double count = 0, spineXoffset=-2.0, spineYoffset=0.0, spineZoffset=-2.0, spineXrot=0.0, spineYrot=0.0, spineZrot=0.0;
				spineXoffset < 2.0;
				count++, spineXoffset += 0.4, spineYoffset += 0.25 * Math.cos(spineXoffset*Math.PI/4), spineZoffset += 0.0, spineYrot += Math.acos(spineXoffset*Math.PI/4)) {
			ArmorStand spinePart = newPart(npc, spineXoffset, spineYoffset, spineZoffset);
			spinePart.getEquipment().setHelmet(new ItemStack(Material.BONE_BLOCK));
			spinePart.setHeadPose(new EulerAngle(spineXrot, spineYrot, spineZrot));
			spineBuf = spinePart;
			if(count % 8.0 == 0.0) {
				for(double ribXoffset = spineXoffset, ribYoffset = spineYoffset, ribZoffset = spineZoffset - 2.0;
						ribZoffset <= spineZoffset + 2.0;
						ribXoffset -= 0.0, ribYoffset += 0.0, ribZoffset += 0.4) {
					ArmorStand ribPart = newPart(npc, ribXoffset, ribYoffset, ribZoffset);
					ribPart.getEquipment().setHelmet(new ItemStack(Material.BONE_BLOCK));
					ribPart.setHeadPose(new EulerAngle(Math.PI / 4, 0.0, 0.0));
				}
			}
		}
		spineBuf.setCustomNameVisible(true);
		npc.setExternalHealthIndicator(spineBuf);
	}

}
