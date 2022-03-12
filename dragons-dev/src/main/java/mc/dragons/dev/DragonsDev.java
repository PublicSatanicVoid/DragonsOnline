package mc.dragons.dev;

import static mc.dragons.core.util.BukkitUtil.syncPeriodic;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;

import mc.dragons.core.Dragons;
import mc.dragons.core.DragonsJavaPlugin;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gui.GUI;
import mc.dragons.core.gui.GUIElement;
import mc.dragons.core.util.FileUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.dev.build.BackupCommand;
import mc.dragons.dev.build.StartTrialCommand;
import mc.dragons.dev.notifier.AdviceBroadcaster;
import mc.dragons.dev.notifier.DiscordNotifier;
import mc.dragons.dev.notifier.TipsCommand;
import mc.dragons.dev.tasks.TaskCommands;
import mc.dragons.dev.tasks.TaskLoader;

public class DragonsDev extends DragonsJavaPlugin {
	
	private static int ADVICE_BROADCASTER_PERIOD_SECONDS;
	private static String BACKUP_FOLDER;
	private static int BACKUP_PERIOD_MINUTES;
	private static int BACKUP_RETENTION_DAYS;

	public static int MAX_STARS;
	public static int MAX_ASSIGN_STARS;
	public static GUI STAFF_SHOP;
	
	private Dragons dragons;
	private DiscordNotifier buildNotifier;
	
	public void onEnable() {
		enableDebugLogging();
		saveDefaultConfig();
		
		dragons = getDragonsInstance();
		
		buildNotifier = new DiscordNotifier(getConfig().getString("discord-notifier-webhook-url"));
		buildNotifier.setEnabled(getConfig().getBoolean("discord-notifier-enabled"));
		
		ADVICE_BROADCASTER_PERIOD_SECONDS = getConfig().getInt("staff-advice.period-seconds", 60 * 2);
		BACKUP_FOLDER = getConfig().getString("backup.folder", "C:\\DragonsBackups\\");
		BACKUP_PERIOD_MINUTES = getConfig().getInt("backup.period-minutes", 60);
		BACKUP_RETENTION_DAYS = getConfig().getInt("backup.retention-days", 30);
		MAX_STARS = getConfig().getInt("stars.max-stars", 5);
		MAX_ASSIGN_STARS = getConfig().getInt("stars.max-assign-stars", 10);
		
		Map<String, Object> shop = getConfig().getConfigurationSection("stars.staff-shop").getValues(false);
		ItemClassLoader itemClassLoader = GameObjectType.ITEM_CLASS.getLoader();
		ItemLoader itemLoader = GameObjectType.ITEM.getLoader();
		int rows = (int) Math.ceil(2 + (double) shop.size() / 7);
		int slot = 10;
		int run = 0;
		STAFF_SHOP = new GUI(rows, "Staff Shop");
		for(Entry<String, Object> entry : shop.entrySet()) {
			String className = entry.getKey();
			int price = (int) entry.getValue();
			ItemClass itemClass = itemClassLoader.getItemClassByClassName(className);
			if(itemClass == null) {
				getLogger().warning("Could not add item to staff shop: No item class \"" + className + "\" exists");
				continue;
			}
			List<String> lore = new ArrayList<>();
			lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + price + TaskLoader.STAR);
			lore.addAll(Item.getCompleteLore(itemClass.getData(), itemClass.getLore().<String>toArray(new String[itemClass.getLore().size()]), null, false, itemClass));
			GUIElement elem = new GUIElement(slot, itemClass.getMaterial(), itemClass.getDecoratedName(), lore, 1, user -> {
				int stars = DevUserHook.getStars(user);
				if(stars < price) {
					user.getPlayer().sendMessage(ChatColor.RED + "This item costs " + price + TaskLoader.STAR + "! (Need " + (price - stars) + " more)");
				}
				else {
					user.giveItem(itemLoader.registerNew(itemClass));
					dragons.getStaffAlertHandler().sendGenericMessage(PermissionLevel.TESTER, ChatColor.GOLD + user.getName() + " redeemed " + price + TaskLoader.STAR + " for a " + itemClass.getDecoratedName() + "!");
					DevUserHook.addStars(user, -price);
				}
			});
			STAFF_SHOP.add(elem);
			slot++;
			run++;
			if(run == 7) {
				run = 0;
				slot += 2;
			}
		}
		
		TaskLoader taskLoader = new TaskLoader(dragons);
		dragons.getLightweightLoaderRegistry().register(taskLoader);
		
		DevUserHook hook = new DevUserHook();
		dragons.getUserHookRegistry().registerHook(hook);
		Bukkit.getPluginManager().registerEvents(hook, this);
		
		CommandExecutor taskCommands = new TaskCommands(this);
		getCommand("task").setExecutor(taskCommands);
		getCommand("tasks").setExecutor(taskCommands);
		getCommand("taskinfo").setExecutor(taskCommands);
		getCommand("assign").setExecutor(taskCommands);
		getCommand("unassign").setExecutor(taskCommands);
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
		getCommand("reopen").setExecutor(taskCommands);
		getCommand("reloadtasks").setExecutor(taskCommands);
		getCommand("stars").setExecutor(taskCommands);
		getCommand("takestars").setExecutor(taskCommands);
		getCommand("givestars").setExecutor(taskCommands);
		getCommand("getstars").setExecutor(taskCommands);
		getCommand("taskrename").setExecutor(taskCommands);
		getCommand("findtasks").setExecutor(taskCommands);
		getCommand("staffshop").setExecutor(new StaffShopCommand());
		getCommand("backup").setExecutor(new BackupCommand(this));
		getCommand("starttrial").setExecutor(new StartTrialCommand());
		getCommand("tips").setExecutor(new TipsCommand());
		
		new AdviceBroadcaster().runTaskTimer(this, 20L * ADVICE_BROADCASTER_PERIOD_SECONDS, 20L * ADVICE_BROADCASTER_PERIOD_SECONDS);
		syncPeriodic(() -> {
			if(Bukkit.getOnlinePlayers().size() > 0) {
				backupFloors();
			}
		}, 20 * 60 * BACKUP_PERIOD_MINUTES, 20 * 60 * BACKUP_PERIOD_MINUTES);
	}
	
	public void backupFloors() {
		getLogger().info("Backing up all floors...");
		String backupRoot = BACKUP_FOLDER + dragons.getServerName() + " " + StringUtil.dateFormatNow().replaceAll(Pattern.quote(":"), ".") + "\\";
		for(World world : Bukkit.getWorlds()) {
			Floor floor = FloorLoader.fromWorld(world);
			if(floor == null) continue;
			File backupFolder = new File(backupRoot + floor.getWorldName());
			backupFolder.mkdirs();
			File sourceFolder = world.getWorldFolder();
			FileUtil.copyFolder(sourceFolder, backupFolder);
		}
		File folder = new File(BACKUP_FOLDER);
		if(!folder.exists()) {
			getLogger().warning("Configured backup folder (" + BACKUP_FOLDER + ") does not exist! Cannot inspect past backups to enforce retention policy.");
		}
		if(!folder.isDirectory()) {
			getLogger().warning("Configured backup folder (" + BACKUP_FOLDER + ") is not a directory! Cannot inspect past backups to enforce retention policy.");
		}
		List<Path> purge = new ArrayList<>();
		if(BACKUP_RETENTION_DAYS != -1) {
			long now = System.currentTimeMillis();
			try {
				Files.walk(folder.toPath(), 1, FileVisitOption.FOLLOW_LINKS).forEach(backup -> {
					int age = 0;
					try {
						age = (int) Math.floor((double) (now - Files.getLastModifiedTime(backup).toMillis()) / (1000 * 60 * 60 * 24));
					} catch (IOException e) {
						e.printStackTrace();
					}
					if(age > BACKUP_RETENTION_DAYS) {
						purge.add(backup);
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
			for(Path path : purge) {
				FileUtil.deleteFolder(path.toFile());
			}
		}
		Bukkit.broadcastMessage(ChatColor.GOLD + "[Dev Server] Automated backup completed successfully.");
		getLogger().info(purge.size() + " old backups were purged.");
 	}
	
	public DiscordNotifier getBuildNotifier() {
		return buildNotifier;
	}
}
