package mc.dragons.tools.dev.monitor;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.sun.management.OperatingSystemMXBean;
import com.sun.management.ThreadMXBean;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.networking.MessageConstants;
import mc.dragons.core.networking.MessageDispatcher;
import mc.dragons.core.tasks.LagMeter;
import mc.dragons.core.util.MathUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.core.util.TableGenerator;
import mc.dragons.core.util.TableGenerator.Alignment;

public class PerformanceCommands extends DragonsCommandExecutor {

	private static final int BYTES_IN_MB = (int) Math.pow(10, 6);
	private static final int NS_IN_MS = (int) Math.pow(10, 6);
	
	private static final String HEADER_PREFIX = ChatColor.GREEN + "";
	
	private static final String COL_FLOOR = HEADER_PREFIX + "Floor";
	private static final String COL_ENTITIES = HEADER_PREFIX + "Ent(Liv)(Plr)";
	private static final String COL_CHUNKS = HEADER_PREFIX + "Chk(Pop)(Rat)";
	
	private static final String COL_ID = HEADER_PREFIX + "ID";
	private static final String COL_NAME = HEADER_PREFIX + "Name";
	private static final String COL_STATE = HEADER_PREFIX + "State";
	private static final String COL_PRIORITY = HEADER_PREFIX + "Priority";
	private static final String COL_DAEMON = HEADER_PREFIX + "Daemon";
	
	private List<Long> tickTimings = new ArrayList<>();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return true;
		
		if(label.equalsIgnoreCase("worldperformance")) {
			sender.sendMessage(ChatColor.DARK_GREEN + "World performance statistics for " + dragons.getServerName()
					+ " @ " + StringUtil.dateFormatNow());
			TableGenerator tg = new TableGenerator(Alignment.LEFT, Alignment.LEFT, Alignment.LEFT);
			tg.addRow(COL_FLOOR, COL_ENTITIES, COL_CHUNKS);
			String floorPrefix = ChatColor.YELLOW + "";
			String dataPrefix = ChatColor.GRAY + "";
			for(World w : Bukkit.getWorlds()) {
				long populatedChunks = Arrays.stream(w.getLoadedChunks())
						.filter(ch -> Arrays.stream(ch.getEntities()).filter(e -> e.getType() == EntityType.PLAYER).count() > 0)
						.count();
				tg.addRow(floorPrefix + w.getName(), 
						dataPrefix + w.getEntities().size() + " (" + dataPrefix + w.getLivingEntities().size() + ") (" + dataPrefix + w.getPlayers().size() + ")", 
						dataPrefix + w.getLoadedChunks().length + " (" + dataPrefix + populatedChunks + ") (" + 
						dataPrefix + MathUtil.round(100 * (double) populatedChunks / w.getLoadedChunks().length) + "%)");
			}
			tg.display(sender);
		}
		
		else if(label.equalsIgnoreCase("getsystemproperties")) {
			sender.sendMessage(ChatColor.GREEN + "Listing all system properties:");
			System.getProperties().forEach((key, value) -> {
				sender.sendMessage(ChatColor.GRAY + "" + key + " = " + value);
			});
		}
		
		else if(label.equalsIgnoreCase("serverperformance")) {
			new BukkitRunnable() { // some OperatingSystemMXBean operations lag the thread, so run asynchronously
				@Override public void run() {
					Properties props = System.getProperties();
					sender.sendMessage(ChatColor.DARK_GREEN + "Server performance statistics generated at " + StringUtil.dateFormatNow());
					sender.sendMessage(ChatColor.GREEN + "Bukkit Version: " + ChatColor.GRAY + Bukkit.getVersion());
					sender.sendMessage(ChatColor.GREEN + "Uptime: " + ChatColor.GRAY + StringUtil.parseSecondsToTimespan(dragons.getUptime() / 1000));
					sender.sendMessage(ChatColor.GREEN + "Estimated Current TPS: " + ChatColor.GRAY + LagMeter.getRoundedTPS());
					sender.sendMessage(ChatColor.GREEN + "Server Architecture: " + ChatColor.GRAY + props.getProperty("os.arch", "Unknown"));
					sender.sendMessage(ChatColor.GREEN + "Operating System: " + ChatColor.GRAY + props.getProperty("os.name", "Unknown") + " v" + props.getProperty("os.version", "Unknown") 
						+ " (" + props.getProperty("os.arch", "Unknown Architecture") + ")");
					sender.sendMessage(ChatColor.GREEN + "Java Version: " + ChatColor.GRAY + props.getProperty("java.version", "Unknown") + " (" + props.getProperty("java.vendor", "Unknown Vendor") + ")");
					sender.sendMessage(ChatColor.GREEN + "JVM Version: " + ChatColor.GRAY + props.getProperty("java.vm.name", "Unknown Name") + props.getProperty("java.vm.version", "Unknown") 
						+ " (" + props.getProperty("java.vm.vendor", "Unknown Vendor") + ")");
					try {
						OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
						sender.sendMessage(ChatColor.GREEN + "Available Processors: " + ChatColor.GRAY + osBean.getAvailableProcessors());
						sender.sendMessage(ChatColor.GREEN + "Process Committed Virtual Memory: " + ChatColor.GRAY + (osBean.getCommittedVirtualMemorySize() / BYTES_IN_MB) + "MB");
//						sender.sendMessage(ChatColor.GREEN + "Process Free Physical Memory: " + ChatColor.GRAY + (osBean.getFreePhysicalMemorySize() / BYTES_IN_MB) + "MB");
						sender.sendMessage(ChatColor.GREEN + "Process CPU Load: " + ChatColor.GRAY + Math.round(100 * osBean.getProcessCpuLoad()) + "%");
						sender.sendMessage(ChatColor.GREEN + "Process CPU Time: " + ChatColor.GRAY + (osBean.getProcessCpuTime() / NS_IN_MS) + "ms");
						sender.sendMessage(ChatColor.GREEN + "Free Swap Space: " + ChatColor.GRAY + (osBean.getFreeSwapSpaceSize() / BYTES_IN_MB) + "MB");
//						sender.sendMessage(ChatColor.GREEN + "Total Physical Memory: " + ChatColor.GRAY + (osBean.getTotalPhysicalMemorySize() / BYTES_IN_MB) + "MB");
//						sender.sendMessage(ChatColor.GREEN + "System CPU Load: " + ChatColor.GRAY + Math.round(100 * osBean.getSystemCpuLoad()) + "%");
						sender.sendMessage(ChatColor.GREEN + "System Load Average: " + ChatColor.GRAY + Math.round(100 * osBean.getSystemLoadAverage()) + "%");
						ThreadMXBean threadBean = ManagementFactory.getPlatformMXBean(ThreadMXBean.class);
						sender.sendMessage(ChatColor.GREEN + "Thread Count: " + ChatColor.GRAY + threadBean.getThreadCount());
						sender.sendMessage(ChatColor.GREEN + "Daemon Thread Count: " + ChatColor.GRAY + threadBean.getDaemonThreadCount());
						sender.sendMessage(ChatColor.GREEN + "Peak Thread Count: " + ChatColor.GRAY + threadBean.getPeakThreadCount());
					}
					catch(Exception e) {
						sender.sendMessage(ChatColor.GRAY + "Some server statistics are unavailable for this platform: Unsupported MXBean(s)");
					}
				}
			}.runTaskAsynchronously(dragons);
		}
		
		else if(label.equalsIgnoreCase("tickperformance") || label.equalsIgnoreCase("tickperf")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/tickperformance start <recPeriodInSeconds>");
				sender.sendMessage(ChatColor.RED + "/tickperformance clear");
				sender.sendMessage(ChatColor.RED + "/tickperformance view [-verbose]" + ChatColor.GRAY + " (after data has been collected)");
			}
			else if(args[0].equalsIgnoreCase("start")) {
				if(args.length == 1) {
					sender.sendMessage(ChatColor.RED + "/tickperformance start <recPeriodInSeconds>");
					return true;
				}
				if(tickTimings.size() > 0) {
					sender.sendMessage(ChatColor.RED + "Please clear existing tick performance data before running this again! /tickperformance clear");
					return true;
				}
				Integer seconds = parseInt(sender, args[1]);
				if(seconds == null) return true;
				new BukkitRunnable() {
					long start = System.currentTimeMillis();
					@Override public void run() {
						long now = System.currentTimeMillis();
						tickTimings.add(now);
						if((now - start) / 1000 >= seconds) {
							sender.sendMessage(ChatColor.GREEN + "Tick timings data collection has completed. Do /tickperformance view to view the data.");
							cancel();
						}
					}
				}.runTaskTimer(dragons, 1L, 1L);
				sender.sendMessage(ChatColor.GREEN + "Began tick timings data collection. Server may experience mild lag while this runs.");
			}
			else if(args[0].equalsIgnoreCase("clear")) {
				tickTimings.clear();
				sender.sendMessage(ChatColor.GREEN + "Cleared tick timings data.");
			}
			else if(args[0].equalsIgnoreCase("view")) {
				if(tickTimings.size() == 0) {
					sender.sendMessage(ChatColor.RED + "No tick performance data available! To gather data, do /tickperformance start <recPeriodInSeconds>");
					return true;
				}
				sender.sendMessage(ChatColor.DARK_GREEN + "Recorded tick data:");
				long prev = tickTimings.get(0);
				long longest = 0;
				long shortest = -1;
				long sum = 0;
				int nLong = 0;
				boolean verbose = args.length > 1 && args[1].equalsIgnoreCase("-verbose");
				for(int i = 1; i < tickTimings.size(); i++) {
					long ms = tickTimings.get(i) - prev;
					sum += ms;
					if(ms > longest) longest = ms;
					if(shortest == -1 || ms < shortest) shortest = ms;
					if(ms > 5 + 1000 / 20) nLong++;
					if(verbose)
						sender.sendMessage(ChatColor.GRAY + "#" + i + ": " + (ms <= 5 + 1000 / 20 ? ChatColor.GREEN : ChatColor.RED) + ms + "ms");
					prev = tickTimings.get(i);
				}
				double avg = (double) sum / (tickTimings.size() - 1);
				sender.sendMessage(ChatColor.GREEN + "Ticks Recorded: " + ChatColor.GRAY + (tickTimings.size() - 1));
				sender.sendMessage(ChatColor.GREEN + "Long Ticks: " + ChatColor.GRAY + nLong);
				sender.sendMessage(ChatColor.GREEN + "Shortest Tick: " + ChatColor.GRAY + shortest + "ms");
				sender.sendMessage(ChatColor.GREEN + "Longest Tick: " + ChatColor.GRAY + longest + "ms");
				sender.sendMessage(ChatColor.GREEN + "Average Tick: " + ChatColor.GRAY + MathUtil.round(avg) + "ms");
			}
		}
		
		else if(label.equalsIgnoreCase("getprocessid")) {
			if(!requirePermission(sender, SystemProfileFlag.DEVELOPMENT)) return true; 
			sender.sendMessage(ChatColor.GREEN + "Process ID: " + ChatColor.GRAY + ProcessHandle.current().pid());
		}
		
		else if(label.equalsIgnoreCase("getstacktrace")) {
			if(!requirePermission(sender, SystemProfileFlag.DEVELOPMENT)) return true; 
			UUID cid = LOGGER.newCID();
			Thread thread = Thread.currentThread();
			if(args.length > 0) {
				Integer id = parseInt(sender, args[0]);
				for(Thread test : Thread.getAllStackTraces().keySet()) {
					if(test.getId() == id) {
						thread = test;
						break;
					}
				}
			}
			LOGGER.info(cid, "Stack trace requested for thread " + thread.getId() + " " + thread.getName() + " (" + thread.getState() + ")");
			sender.sendMessage(ChatColor.GREEN + "Stack trace for thread " + thread.getId() + " " + thread.getName() + " (" + thread.getState() + ")");
			for(StackTraceElement elem : thread.getStackTrace()) {
				sender.sendMessage(ChatColor.GRAY + elem.toString());
				LOGGER.info(cid, elem.toString());
			}
			sender.sendMessage(ChatColor.YELLOW + "Stack trace logged with correlation ID " + StringUtil.toHdFont(cid.toString()));
		}
		
		else if(label.equalsIgnoreCase("getactivethreads")) {
			if(!requirePermission(sender, SystemProfileFlag.DEVELOPMENT)) return true; 
			sender.sendMessage(ChatColor.DARK_GREEN + "" + Thread.getAllStackTraces().size() + " active threads:");
			
			// adapted from https://stackoverflow.com/a/46979843/8463670
			Thread.getAllStackTraces().keySet().stream().collect(Collectors.groupingBy(Thread::getThreadGroup)).forEach((group, threads) -> {
				sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "GROUP " + group.getName() + " - " + group.activeCount() + " thr, " + group.activeGroupCount() + " sub" 
						+ ", par=" + (group.getParent() == null ? "none" : group.getParent().getName()) + ", maxpriority=" + group.getMaxPriority() + ", daemon=" + group.isDaemon());
				TableGenerator tg = new TableGenerator(Alignment.LEFT, Alignment.LEFT, Alignment.LEFT, Alignment.LEFT, Alignment.LEFT);
				tg.addRow(COL_ID, COL_NAME, COL_STATE, COL_PRIORITY, COL_DAEMON);
				for(Thread thread : threads) {
					tg.addRowEx("/getstacktrace " + thread.getId(), "Click to view stack trace for thread #" + thread.getId() + " " + thread.getName() + " (" + thread.getState() + ")", 
							"" + thread.getId(), StringUtil.truncateWithEllipsis(thread.getName(), 30), thread.getState().toString(), "" + thread.getPriority(), "" + thread.isDaemon());
				}
				tg.display(sender);
			});
		}
		
		else if(label.equalsIgnoreCase("requestgc")) {
			if(!requirePermission(sender, SystemProfileFlag.DEVELOPMENT)) return true; 
			long before = System.currentTimeMillis();
			System.gc();
			sender.sendMessage(ChatColor.GREEN + "Ran garbage collector in " + (System.currentTimeMillis() - before) + "ms");
		}
		
		else if(label.equalsIgnoreCase("generatedump")) {
			CommandSender console = Bukkit.getConsoleSender();
			dragons.getLogger().info("");
			dragons.getLogger().info("");
			dragons.getLogger().info("");
			dragons.getLogger().info("==== BEGIN FULL SERVER DATA DUMP ====");
			dragons.getLogger().info("");
			dragons.getLogger().info("=== BEGIN USER DUMP ===");
			for(User user : UserLoader.allUsers()) {
				if(user.getPlayer() == null) {
					dragons.getLogger().info("USER " + user.getName() + " - " + user + " - OFFLINE");
				}
				else {
				dragons.getLogger().info("USER " + user.getName() + " - " + user + " - World " + user.getPlayer().getWorld()
						+ ", Loc " + StringUtil.locToString(user.getPlayer().getLocation()) + ", Access Level " + user.getActivePermissionLevel());
				}
			}
			dragons.getLogger().info("=== END USER DUMP ===");
			dragons.getLogger().info("");
			Bukkit.dispatchCommand(console, "debug dump gameobjects");
			dragons.getLogger().info("");
			Bukkit.dispatchCommand(console, "debug dump entities");
			dragons.getLogger().info("");
			Bukkit.dispatchCommand(console, "debug dump threads");
			dragons.getLogger().info("");
			Bukkit.dispatchCommand(console, "debug dump workers");
			dragons.getLogger().info("");
			Bukkit.dispatchCommand(console, "debug dump pendingtasks");
			dragons.getLogger().info("");
			Bukkit.dispatchCommand(console, "lag");
			dragons.getLogger().info("");
			Bukkit.dispatchCommand(console, "worldperformance");
			dragons.getLogger().info("");
			Bukkit.dispatchCommand(console, "serverperformance");
			dragons.getLogger().info("");
			dragons.getLogger().info("==== END FULL SERVER DATA DUMP ====");
			dragons.getLogger().info("");
			dragons.getLogger().info("");
			dragons.getLogger().info("");	
		}
		
		else if(label.equalsIgnoreCase("clearnetworkmessagecache") || label.equalsIgnoreCase("clearnmc")) {
			if(!requirePermission(sender, SystemProfileFlag.DEVELOPMENT)) return true; 
			MongoCollection<Document> messages = dragons.getMongoConfig().getDatabase().getCollection(MessageConstants.MESSAGE_COLLECTION);
			if(args.length == 0) {
				DeleteResult result = messages.deleteMany(new Document());
				sender.sendMessage(ChatColor.GREEN + "Successfully cleared the network-wide message cache (n=" + result.getDeletedCount() + ")");
			}
			else {
				Integer seconds = parseInt(sender, args[0]);
				if(seconds == null) return true;
				DeleteResult result = messages.deleteMany(new Document("timestamp", new Document("$lt", System.currentTimeMillis() - seconds * 1000)));
				sender.sendMessage(ChatColor.GREEN + "Successfully removed " + result.getDeletedCount() + " messages from the cache");
			}
		}
		
		else if(label.equalsIgnoreCase("printnetworkmessages")) {
			if(!requirePermission(sender, SystemProfileFlag.DEVELOPMENT)) return true; 
			MessageDispatcher dispatcher = dragons.getMessageDispatcher();
			dispatcher.setDebug(!dispatcher.isDebug());
			sender.sendMessage(ChatColor.GREEN + (dispatcher.isDebug() ? "Enabled" : "Disabled") + " network message logging.");
		}
		
		else if(label.equalsIgnoreCase("manifest")) {
			Map<String, List<UUID>> manifest = User.getConnectionMessageHandler().getManifest();
			sender.sendMessage(ChatColor.DARK_GREEN + "Displaying network-wide user manifest.");
			manifest.forEach((server, users) -> {
				sender.sendMessage(ChatColor.YELLOW + "Server " + server + " - " + users.size() + " online");
				sender.sendMessage(ChatColor.GRAY + "  " + StringUtil.parseList(users.stream().map(uuid -> userLoader.loadObject(uuid).getName()).collect(Collectors.toList())));
			});
		}
		
		return true;
	}

}
