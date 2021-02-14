package mc.dragons.npcs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.NPCAddon;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.npc.NPC;

/**
 * Complex NPCs are NPCs that do not appear to follow any "vanilla" entity models
 * (vanilla models being zombie, skeleton, etc.)
 * 
 * This is accomplished through the use of Minecraft's armor stands, which allow models to be
 * built "pixel-by-pixel" out of scaled-down rotatable blocks.
 * 
 * @author Adam
 *
 */
public abstract class ComplexAddon extends NPCAddon {
	private Map<NPC, Location> lastLocations;
	private String modelName;
	
	protected Map<NPC, List<ArmorStand>> parts;
	protected boolean hideEntity = true;
	
	protected ArmorStand newPart(NPC npc) {
		return newPart(npc, (Math.random() - 0.5) * 2, (Math.random() - 0.5) * 2, (Math.random() - 0.5) * 2);
	}
	
	protected ArmorStand newPart(NPC npc, double xOffset, double yOffset, double zOffset) {
		ArmorStand part = (ArmorStand) npc.getEntity().getWorld().spawnEntity(npc.getEntity().getLocation().add(xOffset, yOffset, zOffset), EntityType.ARMOR_STAND);
		part.setArms(false);
		part.setBasePlate(false);
		part.setGravity(false);
		part.setMarker(false);
		part.setVisible(false);
		part.setMetadata("partOf", new FixedMetadataValue(Dragons.getInstance(), npc));
		part.setSmall(false);
		parts.get(npc).add(part);
		return part;
	}
	
	protected void registerPart(NPC npc, ArmorStand part) {
		part.setRemoveWhenFarAway(false);
		part.setCollidable(true);
		part.setMetadata("partOf", new FixedMetadataValue(Dragons.getInstance(), npc));
		parts.get(npc).add(part);
	}
	
	protected ComplexAddon(String modelName) {
		this.modelName = modelName;
		parts = new HashMap<>();
		lastLocations = new HashMap<>();
	}
	
	@Override
	public String getName() {
		return "Complex." + modelName;
	}

	@Override
	public void initialize(GameObject gameObject) {
		NPC npc = (NPC) gameObject;
		LivingEntity le = (LivingEntity) npc.getEntity();
		le.setMetadata("complex", new FixedMetadataValue(Dragons.getInstance(), true));
		if(hideEntity) {
			le.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 100, false, false), true);
			le.setSilent(true);
		}
		parts.put(npc, new ArrayList<>());
		lastLocations.put(npc, npc.getEntity().getLocation());
		LOGGER.finer("Initializing complex parts for NPC " + npc.getIdentifier().toString());
		initializeParts(npc);
	}
	
	public abstract void initializeParts(NPC npc);

	@Override
	public void onMove(NPC npc, Location loc) {
		LOGGER.finer("Handling move of complex parts for NPC " + npc.getIdentifier().toString());
		if(!parts.containsKey(npc)) {
			return;
		}
		Vector move = loc.clone().subtract(lastLocations.get(npc)).toVector();
		// Update the parts
		for(ArmorStand part : parts.get(npc)) {
			Location to = part.getLocation().add(move);
			to.setYaw(loc.getYaw());
			part.teleport(to);
		}
		lastLocations.put(npc, loc);
	}

	@Override
	public void onDeath(NPC npc) {
		LOGGER.finer("Despawning complex parts for NPC " + npc.getIdentifier().toString());
		for(ArmorStand part : parts.get(npc)) {
			part.remove();
		}
		parts.remove(npc);
	}

}
