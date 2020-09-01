package mc.dragons.core.tasks;

import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.util.MathUtil;

public class LagMeter extends BukkitRunnable {
	public static long lastTick;

	public static long lastTick2;

	public static double getEstimatedTPS() {
		if (lastTick == 0L || lastTick2 == 0L)
			return 20.0D;
		return 1000.0D / (lastTick - lastTick2);
	}

	public static double getRoundedTPS() {
		return MathUtil.round(Math.min(20.0D, getEstimatedTPS()));
	}

	@Override
	public void run() {
		lastTick2 = lastTick;
		lastTick = System.currentTimeMillis();
	}

	public static String getTPSColor(double tps) {
		if (tps >= 18.0D)
			return ChatColor.DARK_GREEN.toString();
		if (tps >= 15.0D)
			return ChatColor.YELLOW.toString();
		return ChatColor.DARK_RED.toString();
	}
}
