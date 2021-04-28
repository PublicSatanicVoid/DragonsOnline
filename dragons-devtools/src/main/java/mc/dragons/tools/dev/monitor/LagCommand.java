package mc.dragons.tools.dev.monitor;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.tasks.LagMeter;
import mc.dragons.core.util.MathUtil;
import mc.dragons.core.util.StringUtil;

public class LagCommand extends DragonsCommandExecutor {

	private Map<User, Long> cooldown = new HashMap<>();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		boolean advanced = true;
		
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!hasPermission(sender, PermissionLevel.DEVELOPER)) {
				if(cooldown.containsKey(user) && System.currentTimeMillis() - cooldown.get(user) < 1000 * 2) {
					sender.sendMessage(ChatColor.RED + "Please don't spam this command!");
					return true;
				}
				cooldown.put(user, System.currentTimeMillis());
				advanced = false;
			}
		}
	
		if(advanced) {
			sender.sendMessage(ChatColor.DARK_GRAY + "-------------------------------------------------------------");
			sender.sendMessage(ChatColor.GREEN + "Showing lag data for server " + Dragons.getInstance().getServerName()
				+ " @ " + new SimpleDateFormat("MM/dd/yyyy HH:mm").format(Date.from(Instant.now())));
		}
		List<Double> tpsRecord = Dragons.getInstance().getTPSRecord();
		final int[] tps_thresholds = new int[] { 5, 10, 15, 18, 19 };
		String[] graph = new String[tps_thresholds.length];
		int time_back = 900; // 900s = 15min
		int time_step = 30;
		if (time_back > tpsRecord.size()) {
			while (time_back > tpsRecord.size()) {
				time_back -= time_step;
			}
			if (time_back == 0 && advanced) {
				sender.sendMessage(ChatColor.GRAY + "Not enough data points to show TPS graph.");
			}
		}
		if (time_back != 0 && advanced) {
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
		if (time_back != 0 && advanced) {
			for (String line : graph) {
				sender.sendMessage(line);
			}
		}
		int lastSpikeAgo = -1;
		double lastSpikeTPS = 0.0;
		double minTPS = 20.0;
		double maxTPS = 0;
		double totalTPS = 0;
		int count = 0;
		int secondsAgoToRecent = 60;
		int recentMinAgo = -1;
		double recentMinTPS = 20.0;
		for (i = 0; i < tpsRecord.size(); i++, count++) {
			if (tpsRecord.get(i) < 17) {
				lastSpikeAgo = tpsRecord.size() - 1 - i;
				lastSpikeTPS = tpsRecord.get(i);
			}
			if (tpsRecord.get(i) < minTPS) {
				minTPS = tpsRecord.get(i);
			}				
			if(tpsRecord.get(i) < recentMinTPS && tpsRecord.size() - 1 - i <= secondsAgoToRecent) {
				recentMinTPS = tpsRecord.get(i);
				recentMinAgo = tpsRecord.size() - 1 - i;
			}
			if (tpsRecord.get(i) > maxTPS) {
				maxTPS = tpsRecord.get(i);
			}
			totalTPS += tpsRecord.get(i);
		}
		
		if (lastSpikeAgo != -1) {
			if(advanced) {
				sender.sendMessage(ChatColor.GREEN + "Last Spike: " + ChatColor.GRAY + "" + lastSpikeTPS + "TPS, " + StringUtil.parseSecondsToTimespan(lastSpikeAgo)
					+ " ago");
			}
		} else if(advanced) {
			sender.sendMessage(ChatColor.GREEN + "Last Spike: " + ChatColor.GRAY + "None");
		}		
		
		double avgTPS = MathUtil.round(totalTPS / count, 2);
		double memoryUsedMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / Math.pow(10, 6);
		double memoryAvailableMB = Runtime.getRuntime().totalMemory() / Math.pow(10, 6);
		double memoryUsedPerc = memoryUsedMB / memoryAvailableMB * 100;
		
		if(advanced) {
			sender.sendMessage(ChatColor.GREEN + "Server Uptime: " + ChatColor.GRAY + StringUtil.parseSecondsToTimespan(Dragons.getInstance().getUptime() / 1000));
			sender.sendMessage(ChatColor.GREEN + "Min, Avg, Max, Current TPS: " + ChatColor.GRAY + "" + minTPS + ", " + avgTPS + ", " + maxTPS + ", " + LagMeter.getRoundedTPS());
			sender.sendMessage(ChatColor.GREEN + "Used Memory / Total Memory: " + ChatColor.GRAY
					+ MathUtil.round(memoryUsedMB) + "MB / " + MathUtil.round(memoryAvailableMB) + "MB (" + MathUtil.round(memoryUsedPerc) + "%)");
			sender.sendMessage(ChatColor.GREEN + "CPU Cores: " + ChatColor.GRAY + ""
					+ Runtime.getRuntime().availableProcessors() + ChatColor.GREEN + " - Active Threads: " + ChatColor.GRAY + Thread.activeCount()
					+ ChatColor.GREEN + " - Chunks: " + ChatColor.GRAY + "" + Dragons.getInstance().getLoadedChunks().size() + ChatColor.GREEN
					+ " - Entities: " + ChatColor.GRAY + Dragons.getInstance().getEntities().size());
		}
		
		boolean warn = memoryUsedPerc > 70.0 || recentMinTPS < 10.0 || avgTPS < 17.0;
		
		if(warn) {
			UUID cid = LOGGER.newCID();
			LOGGER.warning(cid, "Server " + Dragons.getInstance().getServerName() + " may be experiencing performance issues.");
			LOGGER.warning(cid, "N=" + tpsRecord.size() + " - Uptime: " + Dragons.getInstance().getUptime()/1000 + "s - Last Spike: " + lastSpikeTPS + "TPS, " + lastSpikeAgo + "s ago - Memory used %: " + MathUtil.round(memoryUsedPerc) + 
					" - Min TPS: " + minTPS + " - Recent Min: " + recentMinTPS + "TPS, " + recentMinAgo + "s ago - Avg TPS: " + avgTPS + " - Curr TPS: " + LagMeter.getRoundedTPS() + " - Chunks: " + Dragons.getInstance().getLoadedChunks().size() + " - Entities: " + Dragons.getInstance().getEntities().size());
			sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "/!\\ " + ChatColor.RED + "Server may be experiencing performance issues. ");
			sender.sendMessage(ChatColor.RED + "     " + StringUtil.toHdFont("Correlation ID: " + cid));
		}
		else {
			sender.sendMessage(ChatColor.GREEN + "No issues detected with recent server performance." + (advanced ? "" : " Approx TPS: " + LagMeter.getRoundedTPS()));
		}

		if(advanced) {
			sender.sendMessage(ChatColor.DARK_GRAY + "-------------------------------------------------------------");
		}
		return true;
	}

}
