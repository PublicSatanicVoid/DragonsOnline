package mc.dragons.core.tasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;

/**
 * Periodically updates users' sidebars with current contextual data.
 * 
 * @author Adam
 *
 */
public class UpdateScoreboardTask extends BukkitRunnable {
	private Dragons plugin;

	public UpdateScoreboardTask(Dragons instance) {
		plugin = instance;
	}

	@Override
	public void run() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			User user = UserLoader.fromPlayer(player);
			boolean onDuty = user.getSystemProfile() != null;
			plugin.getSidebarManager().updateScoreboard(player);
			if(onDuty) {
				String statusBar = ChatColor.GRAY + "You are" + ChatColor.GREEN + " ON DUTY" + ChatColor.GRAY;
				boolean vanish = user.isVanished();
				boolean godmode = user.isGodMode();
				if(vanish || godmode) {
					statusBar += " and in";
				}
				if(vanish) {
					statusBar += ChatColor.GOLD + " VANISH" + ChatColor.GRAY;
					if(godmode) {
						statusBar += " and";
					}
				}
				if(godmode) {
					statusBar += ChatColor.GOLD + " GODMODE" + ChatColor.GRAY;
				}
				user.sendActionBar(statusBar);
			}
		}
	}
}
