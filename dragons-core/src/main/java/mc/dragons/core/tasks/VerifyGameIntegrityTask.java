package mc.dragons.core.tasks;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;

public class VerifyGameIntegrityTask extends BukkitRunnable {
	private Dragons plugin;

	public VerifyGameIntegrityTask(Dragons instance) {
		this.plugin = instance;
	}

	@Override
	public void run() {
		run(false);
	}

	public void run(boolean force) {
		if (this.plugin.getServerOptions().isVerifyIntegrityEnabled() || force)
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "verifygameintegrity -resolve -silent");
	}
}
