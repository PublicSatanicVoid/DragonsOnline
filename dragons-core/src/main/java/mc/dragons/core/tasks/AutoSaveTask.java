package mc.dragons.core.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;

/**
 * Periodically saves all relevant game objects,
 * specifically users and NPCs.
 * 
 * @author Adam
 *
 */
public class AutoSaveTask extends BukkitRunnable {
	private Dragons plugin;
	private GameObjectRegistry registry;

	public AutoSaveTask(Dragons instance) {
		this.plugin = instance;
		this.registry = instance.getGameObjectRegistry();
	}

	@Override
	public void run() {
		run(false);
	}

	public void run(boolean forceSave) {
		if (!this.plugin.getServerOptions().isAutoSaveEnabled() && !forceSave)
			return;
		int n = 0;
		for (GameObject gameObject : this.registry.getRegisteredObjects(new GameObjectType[] { GameObjectType.USER, GameObjectType.NPC })) {
			gameObject.autoSave();
			n++;
		}
		this.plugin.getLogger().info("Auto-saved " + n + " game objects");
	}
}
