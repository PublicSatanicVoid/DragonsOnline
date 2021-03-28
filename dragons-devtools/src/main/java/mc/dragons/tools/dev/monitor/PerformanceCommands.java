package mc.dragons.tools.dev.monitor;

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import com.sun.management.OperatingSystemMXBean;
import com.sun.management.ThreadMXBean;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.tasks.LagMeter;
import mc.dragons.core.util.MathUtil;
import mc.dragons.core.util.StringUtil;

public class PerformanceCommands extends DragonsCommandExecutor {

	private static final int BYTES_IN_MB = (int) Math.pow(10, 6);
	private static final int NS_IN_MS = (int) Math.pow(10, 6);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return true;

		OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
		ThreadMXBean threadBean = ManagementFactory.getPlatformMXBean(ThreadMXBean.class);
		
		if(label.equalsIgnoreCase("worldperformance")) {
			sender.sendMessage(ChatColor.DARK_GREEN + "World performance statistics for " + instance.getServerName()
					+ " @ " + new SimpleDateFormat("MM/dd/yyyy HH:mm").format(Date.from(Instant.now())));
			for(World w : Bukkit.getWorlds()) {
				long populatedChunks = Arrays.stream(w.getLoadedChunks())
						.filter(ch -> Arrays.stream(ch.getEntities()).filter(e -> e.getType() == EntityType.PLAYER).count() > 0)
						.count();
				sender.sendMessage(ChatColor.YELLOW + "" + w.getName() + ": " + ChatColor.GREEN + "Ent:" + ChatColor.GRAY + w.getEntities().size() 
						+ ChatColor.GREEN + " Liv:" + ChatColor.GRAY + w.getLivingEntities().size() + ChatColor.GREEN + " Plr:" + ChatColor.GRAY + w.getPlayers().size()
						+ ChatColor.GREEN + "  Chk:" + ChatColor.GRAY + w.getLoadedChunks().length + ChatColor.GREEN + " Pop:" + ChatColor.GRAY + populatedChunks
						+ ChatColor.GREEN + " Rat:" + ChatColor.GRAY + MathUtil.round(100 * (double) populatedChunks / w.getLoadedChunks().length) + "%");
			}
		}
		
		else if(label.equalsIgnoreCase("serverperformance")) {
			new BukkitRunnable() { // some OperatingSystemMXBean operations lag the thread, so run asynchronously
				@Override public void run() {
					sender.sendMessage(ChatColor.DARK_GREEN + "Server performance statistics for " + instance.getServerName()
							+ " @ " + new SimpleDateFormat("MM/dd/yyyy HH:mm").format(Date.from(Instant.now())));
					sender.sendMessage(ChatColor.GREEN + "Bukkit Version: " + ChatColor.GRAY + Bukkit.getVersion());
					sender.sendMessage(ChatColor.GREEN + "Uptime: " + ChatColor.GRAY + StringUtil.parseSecondsToTimespan(instance.getUptime() / 1000));
					sender.sendMessage(ChatColor.GREEN + "Server Architecture: " + ChatColor.GRAY + osBean.getArch());
					sender.sendMessage(ChatColor.GREEN + "Operating System: " + ChatColor.GRAY + osBean.getName() + " v" + osBean.getVersion());
					sender.sendMessage(ChatColor.GREEN + "Available Processors: " + ChatColor.GRAY + osBean.getAvailableProcessors());
					sender.sendMessage(ChatColor.GREEN + "Process Committed Virtual Memory: " + ChatColor.GRAY + (osBean.getCommittedVirtualMemorySize() / BYTES_IN_MB) + "MB");
					sender.sendMessage(ChatColor.GREEN + "Process Free Physical Memory: " + ChatColor.GRAY + (osBean.getFreePhysicalMemorySize() / BYTES_IN_MB) + "MB");
					sender.sendMessage(ChatColor.GREEN + "Process CPU Load: " + ChatColor.GRAY + Math.round(100 * osBean.getProcessCpuLoad()) + "%");
					sender.sendMessage(ChatColor.GREEN + "Process CPU Time: " + ChatColor.GRAY + (osBean.getProcessCpuTime() / NS_IN_MS) + "ms");
					sender.sendMessage(ChatColor.GREEN + "Estimated Current TPS: " + ChatColor.GRAY + LagMeter.getRoundedTPS());
					sender.sendMessage(ChatColor.GREEN + "Daemon Thread Count: " + ChatColor.GRAY + threadBean.getDaemonThreadCount());
					sender.sendMessage(ChatColor.GREEN + "Peak Thread Count: " + ChatColor.GRAY + threadBean.getPeakThreadCount());
					sender.sendMessage(ChatColor.GREEN + "Thread Count: " + ChatColor.GRAY + threadBean.getThreadCount());
					sender.sendMessage(ChatColor.GREEN + "Free Swap Space: " + ChatColor.GRAY + (osBean.getFreeSwapSpaceSize() / BYTES_IN_MB) + "MB");
					sender.sendMessage(ChatColor.GREEN + "Total Physical Memory: " + ChatColor.GRAY + (osBean.getTotalPhysicalMemorySize() / BYTES_IN_MB) + "MB");
					sender.sendMessage(ChatColor.GREEN + "System CPU Load: " + ChatColor.GRAY + Math.round(100 * osBean.getSystemCpuLoad()) + "%");
					sender.sendMessage(ChatColor.GREEN + "System Load Average: " + ChatColor.GRAY + Math.round(100 * osBean.getSystemLoadAverage()) + "%");
				}
			}.runTaskAsynchronously(instance);
		}
		
		else if(label.equalsIgnoreCase("getprocessid")) {
			sender.sendMessage(ChatColor.GREEN + "Process ID: " + ChatColor.GRAY + ProcessHandle.current().pid());
		}
		
		else if(label.equalsIgnoreCase("requestgc")) {
			long before = System.currentTimeMillis();
			System.gc();
			sender.sendMessage(ChatColor.GREEN + "Ran garbage collector in " + (System.currentTimeMillis() - before) + "ms");
		}
		
		else if(label.equalsIgnoreCase("generatedump")) {
			CommandSender console = Bukkit.getConsoleSender();
			instance.getLogger().info("");
			instance.getLogger().info("");
			instance.getLogger().info("");
			instance.getLogger().info("==== BEGIN FULL SERVER DATA DUMP ====");
			instance.getLogger().info("");
			instance.getLogger().info("=== BEGIN USER DUMP ===");
			for(User user : UserLoader.allUsers()) {
				if(user.getPlayer() == null) {
					instance.getLogger().info("USER " + user.getName() + " - " + user + " - OFFLINE");
				}
				else {
				instance.getLogger().info("USER " + user.getName() + " - " + user + " - World " + user.getPlayer().getWorld()
						+ ", Loc " + StringUtil.locToString(user.getPlayer().getLocation()) + ", Access Level " + user.getActivePermissionLevel());
				}
			}
			instance.getLogger().info("=== END USER DUMP ===");
			instance.getLogger().info("");
			Bukkit.dispatchCommand(console, "debug dump gameobjects");
			instance.getLogger().info("");
			Bukkit.dispatchCommand(console, "debug dump entities");
			instance.getLogger().info("");
			Bukkit.dispatchCommand(console, "debug dump threads");
			instance.getLogger().info("");
			Bukkit.dispatchCommand(console, "debug dump workers");
			instance.getLogger().info("");
			Bukkit.dispatchCommand(console, "debug dump pendingtasks");
			instance.getLogger().info("");
			Bukkit.dispatchCommand(console, "lag");
			instance.getLogger().info("");
			Bukkit.dispatchCommand(console, "worldperformance");
			instance.getLogger().info("");
			Bukkit.dispatchCommand(console, "serverperformance");
			instance.getLogger().info("");
			instance.getLogger().info("==== END FULL SERVER DATA DUMP ====");
			instance.getLogger().info("");
			instance.getLogger().info("");
			instance.getLogger().info("");
			
		}
		
		return true;
	}

}