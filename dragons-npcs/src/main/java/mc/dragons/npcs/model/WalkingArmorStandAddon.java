package mc.dragons.npcs.model;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.util.EulerAngle;

import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.npcs.ComplexAddon;

public class WalkingArmorStandAddon extends ComplexAddon {

	private static final double DELTA_ANGLE = 0.12;
	private static final double MAX_ANGLE = 0.6;
	private static final double MAX_ARM_ANGLE = 0.4;
	private static final double ARM_ANGLE_REDUCTION_FACTOR = MAX_ARM_ANGLE / MAX_ANGLE;
	
	private Map<ArmorStand, Double> angle;
	private Map<ArmorStand, Integer> parity;
	
	private void calculateNextAngle(ArmorStand armorStand) {
		double currentAngle = angle.get(armorStand);
		int currentParity = parity.get(armorStand);
		double nextAngle = currentAngle + currentParity * DELTA_ANGLE;
		int nextParity = currentParity;
		if(Math.abs(nextAngle) >= MAX_ANGLE) {
			nextParity *= -1;
		}
		angle.put(armorStand, nextAngle);
		parity.put(armorStand, nextParity);
	}
	
	protected WalkingArmorStandAddon(String name) {
		super(name);
		angle = new HashMap<>();
		parity = new HashMap<>();
	}
	
	public WalkingArmorStandAddon() {
		this("WalkingArmorStand");
	}

	public ArmorStand getBody(NPC npc) {
		return parts.get(npc).get(0);
	}
	
	@Override
	public void onMove(NPC npc, Location loc) {
		super.onMove(npc, loc);
		if(npc.getEntity().isDead() || !npc.getEntity().isValid()) return;
		ArmorStand armorStand = getBody(npc);
		double currentAngle = angle.get(armorStand);
		armorStand.setArms(true);
		armorStand.setLeftLegPose(new EulerAngle(currentAngle, 0, 0));
		armorStand.setRightLegPose(new EulerAngle(-currentAngle, 0, 0));
		armorStand.setLeftArmPose(new EulerAngle(-currentAngle * ARM_ANGLE_REDUCTION_FACTOR, 0, 0));
		armorStand.setRightArmPose(new EulerAngle(currentAngle * ARM_ANGLE_REDUCTION_FACTOR, 0, 0));
		calculateNextAngle(armorStand);
	}

	@Override
	public void initializeParts(NPC npc) {
		Location loc = npc.getEntity().getLocation();
		ArmorStand armorStand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
		armorStand.setBasePlate(false);
		angle.put(armorStand, -MAX_ANGLE);
		parity.put(armorStand, 1);
		armorStand.setGravity(false);
		npc.setExternalHealthIndicator(armorStand);
		registerPart(npc, armorStand);	
	}

}
