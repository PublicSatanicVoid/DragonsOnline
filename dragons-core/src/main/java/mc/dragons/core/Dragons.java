package mc.dragons.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.ProtocolLibrary;

import mc.dragons.core.addon.AddonRegistry;
import mc.dragons.core.bridge.Bridge;
import mc.dragons.core.bridge.impl.BridgeSpigot116R3;
import mc.dragons.core.commands.AutoRankCommand;
import mc.dragons.core.commands.ChangeLogCommands;
import mc.dragons.core.commands.DragonsCommand;
import mc.dragons.core.commands.FeedbackCommand;
import mc.dragons.core.commands.HealCommand;
import mc.dragons.core.commands.HelpCommand;
import mc.dragons.core.commands.MyQuestsCommand;
import mc.dragons.core.commands.QuestDialogueCommands;
import mc.dragons.core.commands.RankCommand;
import mc.dragons.core.commands.RespawnCommand;
import mc.dragons.core.commands.RestartInstanceCommand;
import mc.dragons.core.commands.StuckQuestCommand;
import mc.dragons.core.commands.SystemLogonCommand;
import mc.dragons.core.events.EntityCombustListener;
import mc.dragons.core.events.EntityDamageListener;
import mc.dragons.core.events.EntityDeathListener;
import mc.dragons.core.events.EntityMoveListener;
import mc.dragons.core.events.EntityTargetEventListener;
import mc.dragons.core.events.InventoryEventListeners;
import mc.dragons.core.events.PlayerEventListeners;
import mc.dragons.core.events.WorldEventListeners;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.npc.NPCClassLoader;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.quest.QuestLoader;
import mc.dragons.core.gameobject.region.RegionLoader;
import mc.dragons.core.gameobject.user.SidebarManager;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHookRegistry;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.chat.ChatMessageRegistry;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfileLoader;
import mc.dragons.core.logging.CustomLoggingProvider;
import mc.dragons.core.logging.correlation.CorrelationLogger;
import mc.dragons.core.networking.MessageDispatcher;
import mc.dragons.core.networking.RemoteAdminMessageHandler;
import mc.dragons.core.networking.StaffAlertMessageHandler;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.storage.loader.ChangeLogLoader;
import mc.dragons.core.storage.loader.FeedbackLoader;
import mc.dragons.core.storage.loader.GlobalVarLoader;
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
import mc.dragons.core.util.singletons.Singletons;

/**
 * The main plugin class for Dragons Online.
 * 
 * @author Adam
 *
 */
public class Dragons extends DragonsJavaPlugin {
	
	/**
	 * The package holding CraftBukkit.
	 */
	public static final String BUKKIT_PACKAGE_NAME = Bukkit.getServer().getClass().getPackage().getName();
	
	/**
	 * The version of CraftBukkit, as encoded in the package name.
	 * E.g. 1_16_R3
	 */
	public static final String BUKKIT_API_VERSION = BUKKIT_PACKAGE_NAME.substring(BUKKIT_PACKAGE_NAME.lastIndexOf(".") + 1, BUKKIT_PACKAGE_NAME.length()).substring(1);

	/**
	 * Link to staff command documentation.
	 */
	public static final String STAFF_DOCUMENTATION = "https://bit.ly/30FS0cW";

	/**
	 * NamespacedKey that is present on all fixed entities.
	 */
	public static NamespacedKey FIXED_ENTITY_KEY;
	
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
	private ChatMessageRegistry chatMessageRegistry;
	private MessageDispatcher messageDispatcher;

	private RemoteAdminMessageHandler remoteAdminHandler;
	private StaffAlertMessageHandler staffAlertHandler;
	
	private AutoSaveTask autoSaveRunnable;
	private BukkitRunnable spawnEntityRunnable;
	private VerifyGameIntegrityTask verifyGameIntegrityRunnable;
	private LagMeter lagMeter;
	private LagMonitorTask lagMonitorTask;
	private UpdateScoreboardTask updateScoreboardTask;

	private ServerOptions serverOptions;
	private boolean debug;

	private String serverName;

	private long started;

	/**
	 * Only intended for use by the Bukkit API plugin loader.
	 * 
	 * @apiNote This needs to be public for Bukkit to load it.
	 * 
	 */
	public Dragons() {
		super();
	}
	
	/**
	 * 
	 * @return The singleton instance of Dragons
	 */
	public static Dragons getInstance() {
		return Singletons.getInstance(Dragons.class, () -> new Dragons());
	}
	
	@Override
	public synchronized void onLoad() {
		FIXED_ENTITY_KEY = new NamespacedKey(this, "fixed");
		started = System.currentTimeMillis();
		
		getLogger().info("Searching for compatible version...");
		switch (BUKKIT_API_VERSION) {
		case "1_16_R3":
			bridge = new BridgeSpigot116R3();
			break;
		default:
			getLogger().severe("Incompatible server version (" + BUKKIT_API_VERSION + ")");
			getLogger().severe("Cannot run DragonsOnline.");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		serverName = getConfig().getString("serverName");
		getLogger().info("Server instance name is " + serverName);
		
		getLogger().info("Initializing storage and registries...");
		saveDefaultConfig();
		mongoConfig = new MongoConfig(this);
		persistentStorageManager = new MongoStorageManager(this);
		localStorageManager = new LocalStorageManager();
		gameObjectRegistry = new GameObjectRegistry(this, persistentStorageManager);
		addonRegistry = new AddonRegistry(this);
		userHookRegistry = new UserHookRegistry();
		lightweightLoaderRegistry = new LightweightLoaderRegistry();
		sidebarManager = new SidebarManager(this);
		chatMessageRegistry = new ChatMessageRegistry();
		messageDispatcher = new MessageDispatcher(this);
		setCustomLoggingProvider(new CustomLoggingProvider(this));
		
		autoSaveRunnable = new AutoSaveTask(this);
		spawnEntityRunnable = new SpawnEntityTask(this);
		verifyGameIntegrityRunnable = new VerifyGameIntegrityTask(this);
		lagMeter = new LagMeter();
		lagMonitorTask = new LagMonitorTask();
		updateScoreboardTask = new UpdateScoreboardTask(this);
		
		serverOptions = new ServerOptions(this);
		serverOptions.setLogLevel(Level.parse(getConfig().getString("loglevel")));
		
		debug = getConfig().getBoolean("debug");
		if (debug) {
			if (serverOptions.getLogLevel().intValue() > Level.CONFIG.intValue()) {
				serverOptions.setLogLevel(Level.CONFIG);
			}
			serverOptions.setVerifyIntegrityEnabled(false);
			getLogger().warning("==========================================================");
			getLogger().warning("THIS SERVER IS IN DEVELOPMENT MODE.");
			getLogger().warning("GAME INTEGRITY WILL NOT BE VERIFIED AFTER INITIAL LOAD.");
			getLogger().warning("==========================================================");
		}
		
		customLoggingProvider.enableCustomLogging();
		enableDebugLogging();
		getLogger().info("Log token is " + customLoggingProvider.getCustomLogFilter().getLogEntryUUID());
	}

	@Override
	public void onEnable() {
		getLogger().info("Removing stale entities...");
		boolean hasFixed = false;
		for (Entity e : getEntities()) {
			if(e.getPersistentDataContainer().has(FIXED_ENTITY_KEY, PersistentDataType.SHORT)) {
				getLogger().verbose("-Skipping fixed entity #" + e.getEntityId());
				hasFixed = true;
			}
			if (e instanceof ItemFrame) {
				getLogger().verbose("-Skipping item frame #" + e.getEntityId());
				continue;
			}
			e.remove();
		}
		
		if(hasFixed) {
			getLogger().notice("The use of fixed entities is not automatically synced across servers the same way that persistent NPCs are.\n"
					+ "Instead, the appropriate world files must be copied.");
		}
		
		// Game objects must be loaded from database in a particular sequence, to ensure
		// all dependencies are ready.
		// For example, items cannot be loaded before their item classes have been loaded,
		// and regions cannot be loaded before their associated floors have been loaded.
		
		getLogger().info("Loading game objects...");
		GameObjectType.getLoader(FloorLoader.class).lazyLoadAll();
		GameObjectType.getLoader(RegionLoader.class).lazyLoadAll();
		GameObjectType.getLoader(ItemClassLoader.class).lazyLoadAll();
		GameObjectType.getLoader(NPCClassLoader.class).lazyLoadAll();
		GameObjectType.getLoader(QuestLoader.class).lazyLoadAll();
		
		// If the server did not shut down gracefully (and sometimes if it did) there may be
		// entities remaining from the previous instance which are no longer linked to a
		// live game object. These entities need to be purged as they will not be responsive
		// to new game events.
		
		new BukkitRunnable() {
			@Override
			public void run() {
				GameObjectType.getLoader(NPCLoader.class).lazyLoadAllPermanent();
				getLogger().info("Flushing invalid game objects from initial load...");
				new BukkitRunnable() {
					int i = 1;

					@Override
					public void run() {
						verifyGameIntegrityRunnable.run(true);
						i++;
						if (i >= 5) {
							cancel();
							getLogger().info("... flush complete. Entity count: " + getEntities().size());
							System.gc();
						}
					}
				}.runTaskTimer(Dragons.this, 20L, 20L);
			}
		}.runTaskLater(this, 20L);

		getLogger().info("Registering lightweight object loaders...");
		lightweightLoaderRegistry.register(new ChangeLogLoader(mongoConfig));
		lightweightLoaderRegistry.register(new FeedbackLoader(mongoConfig));
		lightweightLoaderRegistry.register(new WarpLoader(mongoConfig));
		lightweightLoaderRegistry.register(new CorrelationLogger(mongoConfig));
		lightweightLoaderRegistry.register(new SystemProfileLoader(this));
		lightweightLoaderRegistry.register(new GlobalVarLoader(mongoConfig));
		
		UserLoader.lazyLoadGlobalVarLoader();
		
		getLogger().info("Registering events...");
		PluginManager pluginManager = getServer().getPluginManager();
		pluginManager.registerEvents(new EntityDeathListener(this), this);
		pluginManager.registerEvents(new EntityDamageListener(this), this);
		pluginManager.registerEvents(new WorldEventListeners(this), this);
		pluginManager.registerEvents(new EntityTargetEventListener(this), this);
		pluginManager.registerEvents(new InventoryEventListeners(), this);
		pluginManager.registerEvents(new PlayerEventListeners(this), this);
		pluginManager.registerEvents(new EntityCombustListener(), this);

		getLogger().info("Registering packet listeners...");
		ProtocolLibrary.getProtocolManager().addPacketListener(new EntityMoveListener(this));
		entityHider = new EntityHider(Dragons.getInstance(), EntityHider.Policy.BLACKLIST);
		
		getLogger().info("Registering commands...");
		getCommand("dragons").setExecutor(new DragonsCommand());
		getCommand("rank").setExecutor(new RankCommand());
		getCommand("autorank").setExecutor(new AutoRankCommand());
		getCommand("syslogon").setExecutor(new SystemLogonCommand());
		getCommand("respawn").setExecutor(new RespawnCommand());
		getCommand("heal").setExecutor(new HealCommand());
		getCommand("feedback").setExecutor(new FeedbackCommand());
		getCommand("myquests").setExecutor(new MyQuestsCommand());
		getCommand("help").setExecutor(new HelpCommand());
		getCommand("stuckquest").setExecutor(new StuckQuestCommand());
		getCommand("restartinstance").setExecutor(new RestartInstanceCommand());
		QuestDialogueCommands questDialogueCommands = new QuestDialogueCommands();
		getCommand("fastforwarddialogue").setExecutor(questDialogueCommands);
		getCommand("questchoice").setExecutor(questDialogueCommands);
		ChangeLogCommands changeLogCommandsExecutor = new ChangeLogCommands();
		getCommand("news").setExecutor(changeLogCommandsExecutor);
		getCommand("newsmanager").setExecutor(changeLogCommandsExecutor);

		getLogger().info("Scheduling tasks...");
		autoSaveRunnable.runTaskTimer(this, 0L, serverOptions.getAutoSavePeriodTicks());
		spawnEntityRunnable.runTaskTimer(this, 0L, serverOptions.getCustomSpawnRate());
		verifyGameIntegrityRunnable.runTaskTimer(this, 0L, serverOptions.getVerifyIntegritySweepRate());
		lagMeter.runTaskTimer(this, 100L, 1L);
		lagMonitorTask.runTaskAsynchronously(this);
		updateScoreboardTask.runTaskTimer(this, 100L, 20L);

		getLogger().info("Registering message handlers...");
		remoteAdminHandler = new RemoteAdminMessageHandler();
		staffAlertHandler = new StaffAlertMessageHandler();
		
		getLogger().info("Enabling addons...");
		addonRegistry.enableAll();
	}

	@Override
	public void onDisable() {
		autoSaveRunnable.run(true);
		User.getConnectionMessageHandler().clearServerEntries();
		UUID logToken = customLoggingProvider.getCustomLogFilter().getLogEntryUUID();
		getLogger().info("As a reminder, this session's log token is " + logToken);
		String kickMessage = ChatColor.YELLOW + "This server instance has been closed. We'll be back online soon.";
		String kickMessageDev = kickMessage + "\nLog Token: " + logToken;
		for (User user : UserLoader.allUsers()) { 
			if (user.getPlayer() == null || !user.getPlayer().isOnline()) {
				continue;
			}
			user.handleQuit(false);
			if(PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.DEVELOPER, false)) {
				user.getPlayer().kickPlayer(kickMessageDev);
			}
			else {
				user.getPlayer().kickPlayer(kickMessage);
			}
		}
	}
	
	/**
	 * 
	 * @return All loaded chunks across all worlds
	 */
	public List<Chunk> getLoadedChunks() {
		List<Chunk> chunks = new ArrayList<>();
		for (World w : Bukkit.getWorlds()) {
			for (Chunk c : w.getLoadedChunks()) {
				chunks.add(c);
			}
		}
		return chunks;
	}

	/**
	 * 
	 * @return All entities across all worlds
	 */
	public List<Entity> getEntities() {
		List<Entity> entities = new ArrayList<>();
		for (World w : Bukkit.getWorlds()) {
			entities.addAll(w.getEntities());
		}
		return entities;
	}
	
	/**
	 * 
	 * @return Configuration data for our MongoDB connection
	 */
	public MongoConfig getMongoConfig() {
		return mongoConfig;
	}
	
	/**
	 * 
	 * @return The storage manager responsible for persistent storage
	 */
	public StorageManager getPersistentStorageManager() {
		return persistentStorageManager;
	}

	/**
	 * 
	 * @return The storage manager responsible for non-persistent storage
	 */
	public LocalStorageManager getLocalStorageManager() {
		return localStorageManager;
	}

	/**
	 * 
	 * @return The central registry for all game objects
	 */
	public GameObjectRegistry getGameObjectRegistry() {
		return gameObjectRegistry;
	}

	/**
	 * 
	 * @return The central registry for all game object add-ons
	 */
	public AddonRegistry getAddonRegistry() {
		return addonRegistry;
	}

	/**
	 * 
	 * @return The central registry for all user hooks
	 */
	public UserHookRegistry getUserHookRegistry() {
		return userHookRegistry;
	}

	/**
	 * 
	 * @return The central registry of all lightweight object loaders
	 */
	public LightweightLoaderRegistry getLightweightLoaderRegistry() {
		return lightweightLoaderRegistry;
	}

	/**
	 * 
	 * @return The custom sidebar (scoreboard) manager
	 */
	public SidebarManager getSidebarManager() {
		return sidebarManager;
	}

	/**
	 * 
	 * @return The entity hider
	 */
	public EntityHider getEntityHider() {
		return entityHider;
	}
	
	/**
	 * 
	 * @return The central registry of all chat messages sent
	 */
	public ChatMessageRegistry getChatMessageRegistry() {
		return chatMessageRegistry;
	}
	
	/**
	 * 
	 * @return The custom logging provider
	 */
	public CustomLoggingProvider getCustomLoggingProvider() {
		return customLoggingProvider;
	}
	
	/**
	 * 
	 * @return The inter-server network message dispatcher
	 */
	public MessageDispatcher getMessageDispatcher() {
		return messageDispatcher;
	}
	
	/**
	 * 
	 * @return The message handler for remote administration and telemetry services
	 */
	public RemoteAdminMessageHandler getRemoteAdminHandler() {
		return remoteAdminHandler;
	}
	
	/**
	 * 
	 * @return The message handler for staff alerts
	 */
	public StaffAlertMessageHandler getStaffAlertHandler() {
		return staffAlertHandler;
	}
	
	/**
	 * 
	 * @return The local, non-persistent server options manager
	 */
	public ServerOptions getServerOptions() {
		return serverOptions;
	}

	/**
	 * 
	 * @return The abstraction layer for highly version-dependent functionality
	 */
	public Bridge getBridge() {
		return bridge;
	}

	/**
	 * 
	 * @return The record of recent recorded Ticks Per Second readings
	 */
	public List<Double> getTPSRecord() {
		return lagMonitorTask.getTPSRecord();
	}

	/**
	 * 
	 * @return The runnable for auto-saving
	 */
	public BukkitRunnable getAutoSaveRunnable() {
		return autoSaveRunnable;
	}

	/**
	 * Replaces the auto-save runnable. Does not cancel
	 * the existing runnable.
	 * 
	 * @param runnable
	 */
	public void setAutoSaveRunnable(AutoSaveTask runnable) {
		autoSaveRunnable = runnable;
	}

	/**
	 * 
	 * @return The mob spawning runnable
	 */
	public BukkitRunnable getSpawnEntityRunnable() {
		return spawnEntityRunnable;
	}

	/**
	 * Replaces the mob spawning runnable. Does not cancel
	 * the existing runnable.
	 * 
	 * @param runnable
	 */
	public void setSpawnEntityRunnable(BukkitRunnable runnable) {
		spawnEntityRunnable = runnable;
	}

	/**
	 * 
	 * @return The game integrity verification runnable
	 */
	public BukkitRunnable getVerifyGameIntegrityRunnable() {
		return verifyGameIntegrityRunnable;
	}

	/**
	 * Replaces the game integrity verification runnable. Does not cancel
	 * the existing runnable.
	 * @param runnable
	 */
	public void setVerifyGameIntegrityRunnable(VerifyGameIntegrityTask runnable) {
		verifyGameIntegrityRunnable = runnable;
	}

	/**
	 * 
	 * @return The unique identifier for this server on the network
	 */
	public String getServerName() {
		return serverName;
	}

	/**
	 * 
	 * @return The uptime in milliseconds of this server since it was started
	 */
	public long getUptime() {
		return System.currentTimeMillis() - started;
	}

	/**
	 * 
	 * @return Whether this server is configured to be in debug mode 
	 * (no periodic integrity verification and lower log level)
	 */
	public boolean isDebug() {
		return debug;
	}
}
