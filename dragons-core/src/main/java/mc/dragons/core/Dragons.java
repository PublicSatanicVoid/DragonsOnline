package mc.dragons.core;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.ProtocolLibrary;

import mc.dragons.core.addon.AddonRegistry;
import mc.dragons.core.bridge.Bridge;
import mc.dragons.core.bridge.impl.BridgeSpigot112R1;
import mc.dragons.core.commands.ChangeLogCommands;
import mc.dragons.core.commands.CorrelationCommand;
import mc.dragons.core.commands.FeedbackCommand;
import mc.dragons.core.commands.HealCommand;
import mc.dragons.core.commands.HelpCommand;
import mc.dragons.core.commands.MyQuestsCommand;
import mc.dragons.core.commands.QuestDialogueCommands;
import mc.dragons.core.commands.RankCommand;
import mc.dragons.core.commands.RespawnCommand;
import mc.dragons.core.commands.StuckQuestCommand;
import mc.dragons.core.commands.SystemLogonCommand;
import mc.dragons.core.events.EntityCombustListener;
import mc.dragons.core.events.EntityDamageByEntityEventListener;
import mc.dragons.core.events.EntityDeathEventListener;
import mc.dragons.core.events.EntityMoveListener;
import mc.dragons.core.events.EntityTargetEventListener;
import mc.dragons.core.events.InventoryEventListeners;
import mc.dragons.core.events.PlayerEventListeners;
import mc.dragons.core.events.WorldEventListeners;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCClass;
import mc.dragons.core.gameobject.npc.NPCClassLoader;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestLoader;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.region.RegionLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.SidebarManager;
import mc.dragons.core.gameobject.user.SystemProfileLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHookRegistry;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.logging.CustomLoggingProvider;
import mc.dragons.core.logging.LogFilter;
import mc.dragons.core.logging.correlation.CorrelationLogLoader;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.storage.loader.ChangeLogLoader;
import mc.dragons.core.storage.loader.FeedbackLoader;
import mc.dragons.core.storage.loader.LightweightLoaderRegistry;
import mc.dragons.core.storage.loader.WarpLoader;
import mc.dragons.core.storage.local.LocalStorageManager;
import mc.dragons.core.storage.mongo.MongoConfig;
import mc.dragons.core.storage.mongo.MongoStorageManager;
import mc.dragons.core.tasks.AutoSaveTask;
import mc.dragons.core.tasks.LagMeter;
import mc.dragons.core.tasks.LagMonitorTask;
import mc.dragons.core.tasks.SpawnEntityTask;
import mc.dragons.core.tasks.UpdateScoreboardTask;
import mc.dragons.core.tasks.VerifyGameIntegrityTask;
import mc.dragons.core.util.EntityHider;
import mc.dragons.core.util.PermissionUtil;

/**
 * The main plugin class for DragonsOnline.
 * 
 * @author Adam
 *
 */
public class Dragons extends JavaPlugin {
	private static Dragons INSTANCE;
	private Bridge bridge;

	private MongoConfig mongoConfig;
	private StorageManager persistentStorageManager;
	private LocalStorageManager localStorageManager;
	private GameObjectRegistry gameObjectRegistry;
	private AddonRegistry addonRegistry;
	private UserHookRegistry userHookRegistry;
	private LightweightLoaderRegistry lightweightLoaderRegistry;
	private SidebarManager sidebarManager;
	private EntityHider entityHider;

	private BukkitRunnable autoSaveRunnable;
	private BukkitRunnable spawnEntityRunnable;
	private VerifyGameIntegrityTask verifyGameIntegrityRunnable;
	private LagMeter lagMeter;
	private LagMonitorTask lagMonitorTask;
	private UpdateScoreboardTask updateScoreboardTask;

	private ServerOptions serverOptions;
	private boolean debug;

	private String serverName;

	private long started;

	public static final String BUKKIT_PACKAGE_NAME = Bukkit.getServer().getClass().getPackage().getName();
	public static final String BUKKIT_API_VERSION = BUKKIT_PACKAGE_NAME.substring(BUKKIT_PACKAGE_NAME.lastIndexOf(".") + 1, BUKKIT_PACKAGE_NAME.length()).substring(1);

	public static final String STAFF_DOCUMENTATION = "https://bit.ly/30FS0cW";

	@Override
	public synchronized void onLoad() {
		if (INSTANCE == null) {
			INSTANCE = this;
			this.started = System.currentTimeMillis();
			getLogger().info("Searching for compatible version...");
			switch (BUKKIT_API_VERSION) {
			case "1_12_R1":
				this.bridge = new BridgeSpigot112R1();
				break;
			default:
				getLogger().severe("Incompatible server version (" + BUKKIT_API_VERSION + ")");
				getLogger().severe("Cannot run Dragons.");
				getServer().getPluginManager().disablePlugin(this);
				return;
			}
			getLogger().info("Initializing storage and registries...");
			saveDefaultConfig();
			this.mongoConfig = new MongoConfig(this);
			this.persistentStorageManager = new MongoStorageManager(this);
			this.localStorageManager = new LocalStorageManager();
			this.gameObjectRegistry = new GameObjectRegistry(this, this.persistentStorageManager);
			this.addonRegistry = new AddonRegistry(this);
			this.userHookRegistry = new UserHookRegistry();
			this.lightweightLoaderRegistry = new LightweightLoaderRegistry();
			this.sidebarManager = new SidebarManager(this);
			
			this.autoSaveRunnable = new AutoSaveTask(this);
			this.spawnEntityRunnable = new SpawnEntityTask(this);
			this.verifyGameIntegrityRunnable = new VerifyGameIntegrityTask(this);
			this.lagMeter = new LagMeter();
			this.lagMonitorTask = new LagMonitorTask();
			this.updateScoreboardTask = new UpdateScoreboardTask(this);
			org.apache.logging.log4j.core.Logger pluginLogger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(getLogger().getName());
			this.serverOptions = new ServerOptions(pluginLogger);
			getLogger().setLevel(this.serverOptions.getLogLevel());
			((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addFilter(new LogFilter());
			this.serverOptions.setLogLevel(Level.parse(getConfig().getString("loglevel")));
			this.debug = getConfig().getBoolean("debug");
			if (this.debug) {
				if (this.serverOptions.getLogLevel().intValue() > Level.CONFIG.intValue())
					this.serverOptions.setLogLevel(Level.CONFIG);
				this.serverOptions.setVerifyIntegrityEnabled(false);
				getLogger().config("===========================================================================================");
				getLogger().config("THIS SERVER IS IN DEVELOPMENT MODE. GAME INTEGRITY WILL NOT BE VERIFIED AFTER INITIAL LOAD.");
				getLogger().config("===========================================================================================");
			}
			this.serverName = getConfig().getString("serverName");
			getLogger().info("Server instance name is " + this.serverName);
			CustomLoggingProvider.enableCustomLogging();
		}
	}

	@Override
	public void onEnable() {
		getLogger().info("Removing unwanted entities...");
		for (Entity e : getEntities()) {
			if (e instanceof ItemFrame)
				continue;
			e.remove();
		}
		
		// Game objects must be loaded from database in a particular sequence, to ensure
		// all dependencies are ready.
		// For example, items cannot be loaded before their item classes have been loaded,
		// and regions cannot be loaded before their associated floors have been loaded.
		
		getLogger().info("Loading game objects...");
		GameObjectType.FLOOR.<Floor, FloorLoader>getLoader().lazyLoadAll();
		GameObjectType.REGION.<Region, RegionLoader>getLoader().lazyLoadAll();
		GameObjectType.ITEM_CLASS.<ItemClass, ItemClassLoader>getLoader().lazyLoadAll();
		GameObjectType.NPC_CLASS.<NPCClass, NPCClassLoader>getLoader().lazyLoadAll();
		GameObjectType.QUEST.<Quest, QuestLoader>getLoader().lazyLoadAll();
		
		// If the server did not shut down gracefully (and sometimes if it did) there may be
		// entities remaining from the previous instance which are no longer linked to a
		// live game object. These entities need to be purged as they will not be responsive
		// to new game events.
		
		(new BukkitRunnable() {
			@Override
			public void run() {
				GameObjectType.NPC.<NPC, NPCLoader>getLoader().lazyLoadAllPermanent();
				getLogger().info("Flushing invalid game objects from initial load...");
				(new BukkitRunnable() {
					int i = 1;

					@Override
					public void run() {
						verifyGameIntegrityRunnable.run(true);
						this.i++;
						if (this.i >= 5) {
							cancel();
							getLogger().info("... flush complete. Entity count: " + getEntities().size());
							System.gc();
						}
					}
				}).runTaskTimer(Dragons.this, 20L, 20L);
			}
		}).runTaskLater(this, 20L);

		getLogger().info("Registering lightweight object loaders...");
		this.lightweightLoaderRegistry.register(new ChangeLogLoader(mongoConfig));
		this.lightweightLoaderRegistry.register(new FeedbackLoader(mongoConfig));
		this.lightweightLoaderRegistry.register(new WarpLoader(mongoConfig));
		this.lightweightLoaderRegistry.register(new CorrelationLogLoader(mongoConfig));
		this.lightweightLoaderRegistry.register(new SystemProfileLoader(this));

		getLogger().info("Registering events...");
		PluginManager pluginManager = getServer().getPluginManager();
		pluginManager.registerEvents(new EntityDeathEventListener(this), this);
		pluginManager.registerEvents(new EntityDamageByEntityEventListener(this), this);
		pluginManager.registerEvents(new WorldEventListeners(this), this);
		pluginManager.registerEvents(new EntityTargetEventListener(this), this);
		pluginManager.registerEvents(new InventoryEventListeners(), this);
		pluginManager.registerEvents(new PlayerEventListeners(this), this);
		pluginManager.registerEvents(new EntityCombustListener(), this);

		getLogger().info("Registering packet listeners...");
		ProtocolLibrary.getProtocolManager().addPacketListener(new EntityMoveListener(this));
		this.entityHider = new EntityHider(Dragons.getInstance(), EntityHider.Policy.BLACKLIST);
		
		getLogger().info("Registering commands...");
		getCommand("rank").setExecutor(new RankCommand());
		getCommand("syslogon").setExecutor(new SystemLogonCommand(this));
		getCommand("respawn").setExecutor(new RespawnCommand());
		getCommand("heal").setExecutor(new HealCommand());
		getCommand("feedback").setExecutor(new FeedbackCommand(this));
		getCommand("myquests").setExecutor(new MyQuestsCommand());
		getCommand("help").setExecutor(new HelpCommand());
		getCommand("correlation").setExecutor(new CorrelationCommand(this));
		getCommand("stuckquest").setExecutor(new StuckQuestCommand(this));
		QuestDialogueCommands questDialogueCommands = new QuestDialogueCommands();
		getCommand("fastforwarddialogue").setExecutor(questDialogueCommands);
		getCommand("questchoice").setExecutor(questDialogueCommands);
		ChangeLogCommands changeLogCommandsExecutor = new ChangeLogCommands(this);
		getCommand("news").setExecutor(changeLogCommandsExecutor);
		getCommand("newsmanager").setExecutor(changeLogCommandsExecutor);

		getLogger().info("Scheduling tasks...");
		this.autoSaveRunnable.runTaskTimer(this, 0L, this.serverOptions.getAutoSavePeriodTicks());
		this.spawnEntityRunnable.runTaskTimer(this, 0L, this.serverOptions.getCustomSpawnRate());
		this.verifyGameIntegrityRunnable.runTaskTimer(this, 0L, this.serverOptions.getVerifyIntegritySweepRate());
		this.lagMeter.runTaskTimer(this, 100L, 1L);
		this.lagMonitorTask.runTaskAsynchronously(this);
		this.updateScoreboardTask.runTaskTimer(this, 100L, 20L);

		getLogger().info("Enabling addons...");
		this.addonRegistry.enableAll();
	}

	@Override
	public void onDisable() {
		((AutoSaveTask) this.autoSaveRunnable).run(true);
		for (User user : UserLoader.allUsers()) {
			if (user.getPlayer() == null || !user.getPlayer().isOnline())
				continue;
			if(PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.TESTER, false))
				continue;
			user.handleQuit();
			user.getPlayer().kickPlayer(ChatColor.YELLOW + "This server instance has been closed. We'll be back online soon.");
		}
	}

	public List<Chunk> getLoadedChunks() {
		List<Chunk> chunks = new ArrayList<>();
		for (World w : Bukkit.getWorlds()) {
			for (Chunk c : w.getLoadedChunks()) {
				chunks.add(c);
			}
		}
		return chunks;
	}

	public List<Entity> getEntities() {
		List<Entity> entities = new ArrayList<>();
		for (World w : Bukkit.getWorlds())
			entities.addAll(w.getEntities());
		return entities;
	}

	public static Dragons getInstance() {
		return INSTANCE;
	}
	
	public MongoConfig getMongoConfig() {
		return this.mongoConfig;
	}
	
	public StorageManager getPersistentStorageManager() {
		return this.persistentStorageManager;
	}

	public LocalStorageManager getLocalStorageManager() {
		return this.localStorageManager;
	}

	public GameObjectRegistry getGameObjectRegistry() {
		return this.gameObjectRegistry;
	}

	public AddonRegistry getAddonRegistry() {
		return this.addonRegistry;
	}

	public UserHookRegistry getUserHookRegistry() {
		return this.userHookRegistry;
	}

	public LightweightLoaderRegistry getLightweightLoaderRegistry() {
		return this.lightweightLoaderRegistry;
	}

	public SidebarManager getSidebarManager() {
		return this.sidebarManager;
	}

	public EntityHider getEntityHider() {
		return this.entityHider;
	}
	
	public ServerOptions getServerOptions() {
		return this.serverOptions;
	}

	public Bridge getBridge() {
		return this.bridge;
	}

	public List<Double> getTPSRecord() {
		return this.lagMonitorTask.getTPSRecord();
	}

	public BukkitRunnable getAutoSaveRunnable() {
		return this.autoSaveRunnable;
	}

	public void setAutoSaveRunnable(BukkitRunnable runnable) {
		this.autoSaveRunnable = runnable;
	}

	public BukkitRunnable getSpawnEntityRunnable() {
		return this.spawnEntityRunnable;
	}

	public void setSpawnEntityRunnable(BukkitRunnable runnable) {
		this.spawnEntityRunnable = runnable;
	}

	public BukkitRunnable getVerifyGameIntegrityRunnable() {
		return this.verifyGameIntegrityRunnable;
	}

	public void setVerifyGameIntegrityRunnable(VerifyGameIntegrityTask runnable) {
		this.verifyGameIntegrityRunnable = runnable;
	}

	public String getServerName() {
		return this.serverName;
	}

	public long getUptime() {
		return System.currentTimeMillis() - this.started;
	}

	public boolean isDebug() {
		return this.debug;
	}
}
