package mc.dragons.dev;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.FloorLoader;

public class DragonsDevPlugin extends JavaPlugin implements CommandExecutor {
	
	private static int ADVICE_BROADCASTER_PERIOD_SECONDS = 60 * 2;
	private static String BACKUP_FOLDER = "C:\\Users\\User\\DragonsBackups\\";
	private static int BACKUP_PERIOD_MINUTES = 30;
	
	private DiscordNotifier buildNotifier;
	
	public void onEnable() {
		saveDefaultConfig();
		
		buildNotifier = new DiscordNotifier(getConfig().getString("discord-notifier-webhook-url"));
		buildNotifier.setEnabled(getConfig().getBoolean("discord-notifier-enabled"));
		
		Dragons.getInstance().getLightweightLoaderRegistry().register(new TaskLoader());
		Dragons.getInstance().getUserHookRegistry().registerHook(new DevUserHook());
		
		CommandExecutor taskCommands = new TaskCommands();
		getCommand("task").setExecutor(taskCommands);
		getCommand("tasks").setExecutor(taskCommands);
		getCommand("taskinfo").setExecutor(taskCommands);
		getCommand("assign").setExecutor(taskCommands);
		getCommand("done").setExecutor(taskCommands);
		getCommand("approve").setExecutor(taskCommands);
		getCommand("reject").setExecutor(taskCommands);
		getCommand("close").setExecutor(taskCommands);
		getCommand("taskloc").setExecutor(taskCommands);
		getCommand("tasknote").setExecutor(taskCommands);
		getCommand("gototask").setExecutor(taskCommands);
		getCommand("togglediscordnotifier").setExecutor(taskCommands);
		getCommand("discordnotifyraw").setExecutor(taskCommands);
		getCommand("deletetask").setExecutor(taskCommands);
		getCommand("taskhelp").setExecutor(taskCommands);
		getCommand("backup").setExecutor(new BackupCommand());
		getCommand("starttrial").setExecutor(new StartTrialCommand());
		getCommand("tips").setExecutor(new TipsCommand());
		
		new AdviceBroadcaster().runTaskTimer(this, 20L * ADVICE_BROADCASTER_PERIOD_SECONDS, 20L * ADVICE_BROADCASTER_PERIOD_SECONDS);
		new BukkitRunnable() {
			@Override public void run() {
				backupFloors();
			}
		}.runTaskTimer(this, 20L * 60 * BACKUP_PERIOD_MINUTES, 20L * 60 * BACKUP_PERIOD_MINUTES);
	}
	
	public void backupFloors() {
		getLogger().info("Backing up all floors...");
		String backupRoot = BACKUP_FOLDER + Dragons.getInstance().getServerName() + " " + new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date()) + "\\";
		for(World world : Bukkit.getWorlds()) {
			Floor floor = FloorLoader.fromWorld(world);
			if(floor == null) continue;
			File backupFolder = new File(backupRoot + floor.getWorldName());
			backupFolder.mkdirs();
			File sourceFolder = world.getWorldFolder();
			backup(sourceFolder, backupFolder);
		}
		Bukkit.broadcastMessage(ChatColor.GOLD + "[Dev Server] Automated backup completed successfully.");
 	}
	
	public void backup(File source, File dest) {
		try {
			Files.walk(source.toPath(), FileVisitOption.FOLLOW_LINKS)
				.forEach(s -> {
					try {
						Files.copy(s, dest.toPath().resolve(source.toPath().relativize(s)), StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException ignored) {}
				});
		} catch(IOException ignored) {}
	}

	
	public DiscordNotifier getBuildNotifier() {
		return buildNotifier;
	}
}
