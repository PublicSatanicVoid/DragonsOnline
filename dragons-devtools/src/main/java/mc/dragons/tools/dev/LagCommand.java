package mc.dragons.tools.dev;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.tasks.LagMeter;
import mc.dragons.core.util.MathUtil;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class LagCommand implements CommandExecutor {

	private Map<User, Long> cooldown = new HashMap<>();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, false)) {
				if(cooldown.containsKey(user)) {
					if(System.currentTimeMillis() - cooldown.get(user) < 1000 * 2) {
						sender.sendMessage(ChatColor.RED + "Please don't spam this command!");
						return true;
					}
				}
				cooldown.put(user, System.currentTimeMillis());
			}
		}
		
		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("-gc")) {
				if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true)) return true;
				sender.sendMessage(ChatColor.GREEN + "Running GC...");
				Runtime.getRuntime().gc();
				sender.sendMessage(ChatColor.GREEN + "... Complete!");
				return true;
			}
		}
		
		sender.sendMessage(ChatColor.GREEN + "Showing lag data for server " + Dragons.getInstance().getServerName());
		List<Double> tpsRecord = Dragons.getInstance().getTPSRecord();
		final int[] tps_thresholds = new int[] { 5, 10, 15, 18, 19 };
		String[] graph = new String[tps_thresholds.length];
		int time_back = 900; // 900s = 15min
		int time_step = 30;
		if (time_back > tpsRecord.size()) {
			while (time_back > tpsRecord.size()) {
				time_back -= time_step;
			}
			if (time_back == 0) {
				sender.sendMessage(ChatColor.GRAY + "Not enough data points to show TPS graph.");
			}
		}
		if (time_back != 0) {
			sender.sendMessage(ChatColor.GREEN + "TPS Graph:" + ChatColor.GRAY + " (Last "
					+ ((double) time_back / 60) + " mins, intervals of " + ((double) time_step / 60)
					+ " mins)");
		}
		int i = 0;
		for (int tps_threshold : tps_thresholds) {
			graph[i] = ChatColor.GRAY + (tps_threshold >= 10 ? "" : "0") + tps_threshold + " ";
			for (int j = time_back; j > 0; j -= time_step) {
				double sum = 0.0D;
				int count = 0;
				double min = 20.0D;
				for (int k = j; k > j - time_step; k--) {
					count++;
					sum += tpsRecord.get(tpsRecord.size() - k);
					double sample = tpsRecord.get(tpsRecord.size() - k);
					if(sample < min) min = sample;
				}
				double avg = sum / count;
				if (min >= tps_threshold) {
					graph[i] += LagMeter.getTPSColor(min) + "#";
				} else if(avg >= tps_threshold) {
					graph[i] += ChatColor.GRAY + "#";
				} else if (i == 0 && min < 5.0D) {
					graph[i] += ChatColor.DARK_RED + "_";
				} else {
					if (player != null)
						graph[i] += ChatColor.DARK_GRAY + "#";
					else
						graph[i] += " ";
				}
			}
			i++;
		}

		ArrayUtils.reverse(graph);
		if (time_back != 0) {
			for (String line : graph) {
				sender.sendMessage(line);
			}
		}
		int lastSpike = -1;
		double minTPS = 20.0;
		double maxTPS = 0;
		double totalTPS = 0;
		int count = 0;
		for (i = 0; i < tpsRecord.size(); i++, count++) {
			if (tpsRecord.get(i) < 17) {
				lastSpike = i;
			}
			if (tpsRecord.get(i) < minTPS) {
				minTPS = tpsRecord.get(i);
			}
			if (tpsRecord.get(i) > maxTPS) {
				maxTPS = tpsRecord.get(i);
			}
			totalTPS += tpsRecord.get(i);
		
		}
		if (lastSpike != -1) {
			lastSpike = tpsRecord.size() - lastSpike;
			sender.sendMessage(ChatColor.GREEN + "Last Spike: " + ChatColor.GRAY + "" + lastSpike
					+ "s ago");
		} else {
			sender.sendMessage(ChatColor.GREEN + "Last Spike: " + ChatColor.GRAY + "> "
					+ (tpsRecord.size() / 60) + " minutes ago");
		}
		sender.sendMessage(ChatColor.GREEN + "Server Uptime: " + ChatColor.GRAY + StringUtil.parseSecondsToTimespan(Dragons.getInstance().getUptime() / 1000));
		double avgTPS = MathUtil.round(totalTPS / count, 2);
		sender.sendMessage(ChatColor.GREEN + "Min, Avg, Max, Current TPS: " + ChatColor.GRAY + "" + minTPS + ", " + avgTPS + ", " + maxTPS + ", " + LagMeter.getRoundedTPS());
		//sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + " TPS values are estimates only and may be inaccurate."); // We capped TPS so it doesn't go crazy on us
		sender.sendMessage(ChatColor.GREEN + "Used Memory / Total Memory: " + ChatColor.GRAY
				+ MathUtil.round((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / Math.pow(10, 6)) + "MB / "
				+ MathUtil.round(Runtime.getRuntime().totalMemory() / Math.pow(10, 6)) + "MB ("
				+ MathUtil.round(100 * (double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / Runtime.getRuntime().totalMemory()) + "%)");
		sender.sendMessage(ChatColor.GREEN + "CPU Cores: " + ChatColor.GRAY + ""
				+ Runtime.getRuntime().availableProcessors() + ChatColor.GREEN + " - Active Threads: " + ChatColor.GRAY + Thread.activeCount()
				+ ChatColor.GREEN + " - Chunks: " + ChatColor.GRAY + "" + Dragons.getInstance().getLoadedChunks().size() + ChatColor.GREEN
				+ " - Entities: " + ChatColor.GRAY + Dragons.getInstance().getEntities().size());
	
		return true;
	}

}
