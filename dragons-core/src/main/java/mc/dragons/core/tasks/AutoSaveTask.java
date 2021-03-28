package mc.dragons.core.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectRegistry;

/**
 * Periodically saves all relevant game objects,
 * specifically users and NPCs.
 * 
 * @author Adam
 *
 */
public class AutoSaveTask extends BukkitRunnable {
	private GameObjectRegistry registry;

	public AutoSaveTask(Dragons instance) {
		registry = instance.getGameObjectRegistry();
	}

	@Override
	public void run() {
		run(false);
	}

	public void run(boolean forceSave) {
		registry.executeAutoSave(forceSave);
	}
}
