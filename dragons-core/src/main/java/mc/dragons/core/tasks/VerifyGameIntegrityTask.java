package mc.dragons.core.tasks;

import mc.dragons.core.Dragons;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class VerifyGameIntegrityTask extends BukkitRunnable {
	private Dragons plugin;

	public VerifyGameIntegrityTask(Dragons instance) {
		this.plugin = instance;
	}

	public void run() {
		run(false);
	}

	public void run(boolean force) {
		if (this.plugin.getServerOptions().isVerifyIntegrityEnabled() || force)
			Bukkit.dispatchCommand((CommandSender) Bukkit.getConsoleSender(), "verifygameintegrity -resolve -silent");
	}
}
