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

	public abstract void onMove(NPC npc, Location location);
	public abstract void onTakeDamage(NPC npc, GameObject source, double damage);
	public abstract void onDealDamage(NPC npc, GameObject source, double damage);
	public abstract void onInteract(NPC npc, User user);
	public abstract void onDeath(NPC npc);
}
