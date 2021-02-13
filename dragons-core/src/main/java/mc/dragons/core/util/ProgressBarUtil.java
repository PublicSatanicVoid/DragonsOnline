package mc.dragons.core.util;

import org.bukkit.ChatColor;

/**
 * Generate text-based progress bars expressing a given percentage.
 * 
 * @author Adam
 *
 */
public class ProgressBarUtil {
	public static final int HEALTH_BARS = 30;
	public static final String HEALTH_BAR_PIECE = "|";
	public static final ChatColor FULL_COLOR = ChatColor.DARK_GREEN;
	public static final ChatColor HIGH_COLOR = ChatColor.GREEN;
	public static final ChatColor WARNING_COLOR = ChatColor.RED;
	public static final ChatColor CRITICAL_COLOR = ChatColor.DARK_RED;
	public static final ChatColor EMPTY_COLOR = ChatColor.GRAY;
	public static final ChatColor NEUTRAL_COLOR = ChatColor.GRAY;
	public static final int HIGH_THRESHOLD = 25;
	public static final int WARNING_THRESHOLD = 10;
	public static final int CRITICAL_THRESHOLD = 4;
	public static final int PROGRESS_BARS = 40;
	public static final String PROGRESS_BAR_PIECE = "|";

	public static String getHealthBar(double health, double max) {
		String healthBar = "";
		double ratio = health / max;
		int full = (int) Math.ceil(ratio * 30.0D);
		ChatColor activeColor = full > 25 ? FULL_COLOR : full > 10 ? HIGH_COLOR : full > 4 ? WARNING_COLOR : CRITICAL_COLOR;
		for (int i = 0; i < 30; i++) {
			if (i < full) {
				healthBar = String.valueOf(healthBar) + activeColor + "|";
			} else {
				healthBar = String.valueOf(healthBar) + EMPTY_COLOR + "|";
			}
		}
		return healthBar;
	}

	public static String getCountdownBar(double percent) {
		String progressBar = "";
		int full = (int) Math.ceil(percent * 40.0D);
		for (int i = 0; i < 40; i++) {
			if (i < full) {
				progressBar = String.valueOf(progressBar) + NEUTRAL_COLOR + "|";
			} else {
				progressBar = String.valueOf(progressBar) + FULL_COLOR + "|";
			}
		}
		return progressBar;
	}
}
