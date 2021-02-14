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

	public void onMove(NPC npc, Location location) { /* default */ }
	public void onTakeDamage(NPC npc, GameObject source, double damage) { /* default */ }
	public void onDealDamage(NPC npc, GameObject source, double damage) { /* default */ }
	public void onInteract(NPC npc, User user) { /* default */ }
	public void onDeath(NPC npc) { /* default */ }
}
