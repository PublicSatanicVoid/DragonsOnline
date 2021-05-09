package mc.dragons.core.addon;

import java.util.List;

import org.bson.Document;
import org.bukkit.Location;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCClassLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.storage.StorageManager;

public abstract class NPCAddon implements Addon {
	private NPCClassLoader npcClassLoader = GameObjectType.NPC_CLASS.getLoader();
	private StorageManager storageManager = Dragons.getInstance().getPersistentStorageManager();
	private DragonsLogger LOGGER = Dragons.getInstance().getLogger();
	
	@Override
	public final AddonType getType() {
		return AddonType.NPC;
	}

	@Override
	public void apply() {
		storageManager.getAllStorageAccess(GameObjectType.NPC_CLASS, new Document("addons", new Document("$in", List.of(getName()))))
			.stream()
			.map(storageAccess -> npcClassLoader.loadObject(storageAccess))
			.forEach(npcClass -> {
				LOGGER.debug("Applying NPC add-on " + getName() + " to NPC class " + npcClass.getClassName());
				npcClass.reloadAddons();
			});
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
