package mc.dragons.tools.dev;

import java.lang.management.ManagementFactory;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitWorker;

import com.sun.management.ThreadMXBean;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;
import net.md_5.bungee.api.ChatColor;

public class DebugCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		User user = null;
		if(sender instanceof Player) {
			user = UserLoader.fromPlayer((Player) sender);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true)) return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/debug level <LogLevel>");
			sender.sendMessage(ChatColor.YELLOW + "/debug dump gameobjects");
			sender.sendMessage(ChatColor.YELLOW + "/debug dump entities");
			sender.sendMessage(ChatColor.YELLOW + "/debug dump threads");
			sender.sendMessage(ChatColor.YELLOW + "/debug dump workers [Plugin]");
			sender.sendMessage(ChatColor.YELLOW + "/debug dump pendingtasks [Plugin]");
			sender.sendMessage(ChatColor.YELLOW + "/debug errors [-stop]");
			sender.sendMessage(ChatColor.YELLOW + "/debug attach <Player> [-stop]");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("level")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Specify a log level! /debug level <OFF|SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST|ALL>");
				return true;
			}
			Level level = null;
			try {
				level = Level.parse(args[1].toUpperCase());
			}
			catch(Exception e) {
				sender.sendMessage(ChatColor.RED + "Invalid log level! /debug level <OFF|SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST|ALL>");
				return true;
			}
			Dragons.getInstance().getServerOptions().setLogLevel(level);
			sender.sendMessage(ChatColor.GREEN + "Set log level to " + level);
		}
		
		else if(args[0].equalsIgnoreCase("dump")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Specify a dump option! /debug dump <gameobjects|entities>");
				return true;
			}
			if(args[1].equalsIgnoreCase("gameobjects")) {
				Logger logger = Dragons.getInstance().getLogger();
				logger.info("=== BEGIN COMPLETE GAME OBJECT DUMP ===");
				logger.info("GameObjectMemoryAddress\tGameObjectType\tGameObjectUUID");
				logger.info("-----------------------\t--------------\t--------------");
				for(GameObject gameObject : Dragons.getInstance().getGameObjectRegistry().getRegisteredObjects()) {
					logger.info(gameObject + "\t" + gameObject.getType() + "\t" + gameObject.getUUID());
				}
				logger.info("=== END COMPLETE GAME OBJECT DUMP ===");
				sender.sendMessage(ChatColor.GREEN + "Dumped all game objects to console.");
			}
			else if(args[1].equalsIgnoreCase("entities")) {
				Logger logger = Dragons.getInstance().getLogger();
				logger.info("=== BEGIN COMPLETE ENTITY DUMP ===");
				logger.info("EntityType\tEntityID\tValid\tGameObjectType\tGameObjectUUID");
				logger.info("----------\t--------\t-----\t--------------\t--------------");
				for(World world : Bukkit.getWorlds()) {
					logger.info("World: " + world.getName());
					for(Entity entity : world.getEntities()) {
						GameObject gameObject = null;
						if(entity instanceof Player) {
							gameObject = UserLoader.fromPlayer((Player) entity);
						}
						else if(entity instanceof Item) {
							gameObject = ItemLoader.fromBukkit(((Item) entity).getItemStack());
						}
						else {
							gameObject = NPCLoader.fromBukkit(entity);
						}
						logger.info(entity.getType() + "\t" + entity.getEntityId() + "\t" + entity.isValid() + "\t" + (gameObject == null ? "null\tnull" : gameObject.getType() + "\t" + gameObject.getUUID()));
					}
				}
				logger.info("=== END COMPLETE ENTITY DUMP ===");
				sender.sendMessage(ChatColor.GREEN + "Dumped all entities to console.");
			}
			else if(args[1].equalsIgnoreCase("threads")) {
				ThreadMXBean threadBean = ManagementFactory.getPlatformMXBean(ThreadMXBean.class);
				Logger logger = Dragons.getInstance().getLogger();
				logger.info("=== BEGIN COMPLETE THREAD DUMP ===");
				for(Entry<Thread, StackTraceElement[]> entry: Thread.getAllStackTraces().entrySet()) {
					logger.info("THREAD " + entry.getKey().getId() + ": " + entry.getKey().getName() + " (Priority: " + entry.getKey().getPriority() + ", "
							+ "State: " + entry.getKey().getState() + ", CPU Time: " + threadBean.getThreadCpuTime(entry.getKey().getId()) + ")");
					for(StackTraceElement elem : entry.getValue()) {
						logger.info("    " + elem.toString());
					}
				}
				logger.info("=== END COMPLETE THREAD DUMP ===");
				sender.sendMessage(ChatColor.GREEN + "Dumped all threads to console.");
			}
			else if(args[1].equalsIgnoreCase("workers")) {
				Logger logger = Dragons.getInstance().getLogger();
				String filter = "";
				Plugin pluginFor = null;
				if(args.length > 2) {
					filter = " for plugin " + args[2];
					pluginFor = Bukkit.getPluginManager().getPlugin(args[2]);
					if(pluginFor == null) {
						sender.sendMessage(ChatColor.RED + "Invalid plugin! /debug dump workers [Plugin]");
						return true;
					}
				}
				logger.info("=== BEGIN COMPLETE WORKER DUMP" + filter.toUpperCase() + " ===");
				for(BukkitWorker worker : Bukkit.getScheduler().getActiveWorkers()) {
					if(pluginFor != null) {
						if(worker.getOwner() != pluginFor) {
							continue;
						}
					}
					logger.info("WORKER " + worker.getTaskId() + ": Class " + worker.getClass().getName() + ", Owner " + worker.getOwner().getName()
							+ ", Thread " + worker.getThread().getId() + " (" + worker.getThread().getName() + ") ");
				}
				logger.info("=== END COMPLETE WORKER DUMP" + filter.toUpperCase() + " ===");
				sender.sendMessage(ChatColor.GREEN + "Dumped all workers " + filter + " to console.");
			}
			else if(args[1].equalsIgnoreCase("pendingtasks")) {
				Logger logger = Dragons.getInstance().getLogger();
				String filter = "";
				Plugin pluginFor = null;
				if(args.length > 2) {
					filter = " for plugin " + args[2];
					pluginFor = Bukkit.getPluginManager().getPlugin(args[2]);
					if(pluginFor == null) {
						sender.sendMessage(ChatColor.RED + "Invalid plugin! /debug dump pendingtasks [Plugin]");
						return true;
					}
				}
				logger.info("=== BEGIN COMPLETE PENDING TASK DUMP" + filter.toUpperCase() + " ===");
				for(BukkitTask task : Bukkit.getScheduler().getPendingTasks()) {
					if(pluginFor != null) {
						if(task.getOwner() != pluginFor) {
							continue;
						}
					}
					logger.info("TASK " + task.getTaskId() + ": Class " + task.getClass().getName() + ", Owner " + task.getOwner().getName()
							+ ", Sync=" + task.isSync() + ", Cancelled=" + task.isCancelled());
				}
				logger.info("=== END COMPLETE PENDING TASK DUMP" + filter.toUpperCase() + " ===");
				sender.sendMessage(ChatColor.GREEN + "Dumped all pending tasks" + filter + " to console.");
			}
			else {
				sender.sendMessage(ChatColor.RED + "Invalid dump option! /debug dump <gameobjects|entities|threads|workers [Plugin]|pendingtasks [Plugin]>");
			}
		}
		
		else if(args[0].equalsIgnoreCase("errors") || args[0].equalsIgnoreCase("exceptions")) {
			boolean on = args.length == 1;
			if(args.length > 1) {
				if(args[1].equalsIgnoreCase("-stop")) {
					on = false;
				}
			}
			user.setDebuggingErrors(on);
			sender.sendMessage(ChatColor.GREEN + "You will " + (on ? "now" : "no longer") + " receive errors from console in chat.");
		}
		
		else if(args[0].equalsIgnoreCase("attach")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Specify a player to debug! (Must be online) /debug attach <Player> [-stop]");
				return true;
			}
			
			Player target = Bukkit.getPlayerExact(args[1]);
			if(target == null) {
				sender.sendMessage(ChatColor.RED + "Invalid player! (Must be online) /debug attach <Player> [-stop]");
				return true;
			}
			
			User targetUser = UserLoader.fromPlayer(target);
			
			if(args.length > 2) {
				if(args[2].equalsIgnoreCase("-stop")) {
					targetUser.removeDebugTarget(sender);
					sender.sendMessage(ChatColor.GREEN + "Stopped debugging " + target.getName());
					return true;
				}
				sender.sendMessage(ChatColor.RED + "Invalid options! /debug attach <Player> [-stop]");
				return true;
			}
			
			targetUser.addDebugTarget(sender);
			sender.sendMessage(ChatColor.GREEN + "Began debugging " + target.getName());
		}
		
		return true;
	}

}
