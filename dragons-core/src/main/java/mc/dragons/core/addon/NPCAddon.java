package mc.dragons.core.addon;

import org.bukkit.Location;

import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.user.User;

public abstract class NPCAddon implements Addon {
	@Override
	public final AddonType getType() {
		return AddonType.NPC;
	}

	/**
	 * Called whenever an NPC with this add-on moves.
	 * 
	 * @implSpec Expect this to be called very frequently!
	 * 
	 * @param npc
	 * @param location The location the NPC moved to.
	 */
	public void onMove(NPC npc, Location location) { /* default */ }
	
	/**
	 * Called whenever an NPC with this add-on takes damage.
	 * 
	 * @param npc The NPC that took damage
	 * @param source The damage source, or null if there was none
	 * @param damage The amount of damage done
	 */
	public void onTakeDamage(NPC npc, GameObject source, double damage) { /* default */ }
	
	/**
	 * Called whenever an NPC with this add-on deals damage.
	 * 
	 * @param npc The NPC that dealt damage
	 * @param target The game object that was damaged
	 * @param damage The amount of damage dealt
	 */
	public void onDealDamage(NPC npc, GameObject target, double damage) { /* default */ }
	
	/**
	 * Called whenever a user interacts with an NPC with this add-on.
	 * 
	 * @param npc
	 * @param user
	 */
	public void onInteract(NPC npc, User user) { /* default */ }
	
	/**
	 * Called whenever an NPC with this add-on dies.
	 * 
	 * @param npc
	 */
	public void onDeath(NPC npc) { /* default */ }
}
