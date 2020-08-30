package mc.dragons.core.addon;

import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.user.User;
import org.bukkit.Location;

public abstract class NPCAddon implements Addon {
	public final AddonType getType() {
		return AddonType.NPC;
	}

	public abstract void onMove(NPC paramNPC, Location paramLocation);

	public abstract void onTakeDamage(NPC paramNPC, GameObject paramGameObject, double paramDouble);

	public abstract void onDealDamage(NPC paramNPC, GameObject paramGameObject, double paramDouble);

	public abstract void onInteract(NPC paramNPC, User paramUser);

	public abstract void onDeath(NPC paramNPC);
}
