package mc.dragons.core.tasks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;

public class UpdateScoreboardTask extends BukkitRunnable {
	private Dragons plugin;

	public UpdateScoreboardTask(Dragons instance) {
		this.plugin = instance;
	}

	@Override
	public void run() {
		for (Player player : Bukkit.getOnlinePlayers())
			this.plugin.getSidebarManager().updateScoreboard(player);
	}
}
