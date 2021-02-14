package mc.dragons.npcs;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.NPCAddon;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPC.NPCType;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.util.HologramUtil;
import mc.dragons.core.util.StringUtil;

/**
 * NPCs with this add-on will yeet hostile entities.
 * 
 * @author Adam
 *
 */
public class GuardAddon extends NPCAddon {
	
	private Set<NPC> guards;
	
	public GuardAddon() {
		guards = new HashSet<>();
	}
	
	@Override
	public void onEnable() {
		new BukkitRunnable() {
			@Override public void run() {
				for(NPC guard : guards) {
					Entity eGuard = guard.getEntity();
					for(Entity test : eGuard.getNearbyEntities(10, 10, 10)) {
						if(!(test instanceof LivingEntity)) continue;
						NPC npc = NPCLoader.fromBukkit(test);
						if(npc == null) continue;
						if(npc.getNPCType() != NPCType.HOSTILE) continue;
						LivingEntity leTest = (LivingEntity) test;
						for(Entity passenger : eGuard.getPassengers()) {
							if(passenger.getType() == EntityType.IRON_GOLEM) {
								IronGolem golem = (IronGolem) passenger;
								golem.setTarget((LivingEntity) leTest);
							}
						}
						guard.setTarget(leTest);
						LOGGER.fine("Guard " + StringUtil.entityToString(eGuard) + " is now targeting " + StringUtil.entityToString(test));
						break;
					}
				}
			}
		}.runTaskTimer(Dragons.getInstance(), 0L, 20L * 2);
	}
	
	@Override
	public String getName() {
		return "Guard";
	}

	@Override
	public void initialize(GameObject gameObject) {
		if(!(gameObject instanceof NPC)) return;
		NPC npc = (NPC) gameObject;
		Entity e = npc.getEntity();
		final Entity guard = e.getWorld().spawnEntity(e.getLocation(), EntityType.IRON_GOLEM);
		final CraftEntity obcGuard = (CraftEntity) guard;
		obcGuard.setInvulnerable(true);
		e.addPassenger(guard);
		HologramUtil.makeArmorStandNameTag(e, npc.getDecoratedName(), 0.0, 0.0, 0.0, true);
		Attributable att = (Attributable) e;
		new BukkitRunnable() {
			@Override public void run() {
				obcGuard.getHandle().setInvisible(true);
				LivingEntity leGuard = (LivingEntity) guard;
				leGuard.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 100, false, false), true);
			}
		}.runTaskLater(Dragons.getInstance(), 1L);
		LOGGER.fine("Initialized Guard addon on entity " + StringUtil.entityToString(e) + " with golem " + StringUtil.entityToString(guard));
		att.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.2);
		guards.add(npc);
	}
}
