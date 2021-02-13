package mc.dragons.core.tasks;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;

/**
 * Periodically verifies game integrity, checking cache consistency
 * and metadata validity.
 * 
 * @author Adam
 *
 */
public class VerifyGameIntegrityTask extends BukkitRunnable {
	private Dragons plugin;

	public VerifyGameIntegrityTask(Dragons instance) {
		plugin = instance;
	}

	@Override
	public void run() {
		run(false);
	}

	public void run(boolean force) {
		if (plugin.getServerOptions().isVerifyIntegrityEnabled() || force) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "verifygameintegrity -resolve -silent");
		}
	}
}
