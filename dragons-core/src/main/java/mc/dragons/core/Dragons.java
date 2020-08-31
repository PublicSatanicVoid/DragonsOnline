package mc.dragons.core;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketListener;

import mc.dragons.core.addon.AddonRegistry;
import mc.dragons.core.bridge.Bridge;
import mc.dragons.core.bridge.impl.BridgeSpigot112R1;
import mc.dragons.core.commands.BypassDeathCountdownCommand;
import mc.dragons.core.commands.ChangeLogCommands;
import mc.dragons.core.commands.ChannelCommand;
import mc.dragons.core.commands.FeedbackCommand;
import mc.dragons.core.commands.HelpCommand;
import mc.dragons.core.commands.ILostTheLousyStickCommand;
import mc.dragons.core.commands.MyQuestsCommand;
import mc.dragons.core.commands.PrivateMessageCommands;
import mc.dragons.core.commands.QuestDialogueCommands;
import mc.dragons.core.commands.RankCommand;
import mc.dragons.core.commands.ShoutCommand;
import mc.dragons.core.commands.SystemLogonCommand;
import mc.dragons.core.events.EntityCombustListener;
import mc.dragons.core.events.EntityDamageByEntityEventListener;
import mc.dragons.core.events.EntityDeathEventListener;
import mc.dragons.core.events.EntityMoveListener;
import mc.dragons.core.events.EntityTargetEventListener;
import mc.dragons.core.events.InventoryEventListeners;
import mc.dragons.core.events.PlayerEventListeners;
import mc.dragons.core.events.WorldEventListeners;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.loader.FloorLoader;
import mc.dragons.core.gameobject.loader.GameObjectRegistry;
import mc.dragons.core.gameobject.loader.ItemClassLoader;
import mc.dragons.core.gameobject.loader.NPCClassLoader;
import mc.dragons.core.gameobject.loader.NPCLoader;
import mc.dragons.core.gameobject.loader.QuestLoader;
import mc.dragons.core.gameobject.loader.RegionLoader;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCClass;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.user.SidebarManager;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHookRegistry;
import mc.dragons.core.logging.CustomLoggingProvider;
import mc.dragons.core.logging.LogFilter;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.storage.impl.LocalStorageManager;
import mc.dragons.core.storage.impl.MongoConfig;
import mc.dragons.core.storage.impl.MongoStorageManager;
import mc.dragons.core.storage.impl.loader.ChangeLogLoader;
import mc.dragons.core.storage.impl.loader.FeedbackLoader;
import mc.dragons.core.storage.impl.loader.LightweightLoaderRegistry;
import mc.dragons.core.storage.impl.loader.SystemProfileLoader;
import mc.dragons.core.storage.impl.loader.WarpLoader;
import mc.dragons.core.tasks.AutoSaveTask;
import mc.dragons.core.tasks.LagMeter;
import mc.dragons.core.tasks.LagMonitorTask;
import mc.dragons.core.tasks.SpawnEntityTask;
import mc.dragons.core.tasks.UpdateScoreboardTask;
import mc.dragons.core.tasks.VerifyGameIntegrityTask;

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

	public synchronized void onLoad() {
		if (INSTANCE == null) {
			INSTANCE = this;
			this.started = System.currentTimeMillis();
			getLogger().info("Searching for compatible version...");
			switch (BUKKIT_API_VERSION) {
			case "1_12_R1":
				this.bridge = (Bridge) new BridgeSpigot112R1();
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
			this.autoSaveRunnable = (BukkitRunnable) new AutoSaveTask(this);
			this.spawnEntityRunnable = (BukkitRunnable) new SpawnEntityTask(this);
			this.verifyGameIntegrityRunnable = new VerifyGameIntegrityTask(this);
			this.lagMeter = new LagMeter();
			this.lagMonitorTask = new LagMonitorTask();
			this.updateScoreboardTask = new UpdateScoreboardTask(this);
			org.apache.logging.log4j.core.Logger pluginLogger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(getLogger().getName());
			this.serverOptions = new ServerOptions(pluginLogger);
			getLogger().setLevel(this.serverOptions.getLogLevel());
			((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addFilter((Filter) new LogFilter());
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

	public void onEnable() {
		getLogger().info("Loading game objects...");
		GameObjectType.FLOOR.<Floor, FloorLoader>getLoader().lazyLoadAll();
		GameObjectType.REGION.<Region, RegionLoader>getLoader().lazyLoadAll();
		GameObjectType.ITEM_CLASS.<ItemClass, ItemClassLoader>getLoader().lazyLoadAll();
		GameObjectType.NPC_CLASS.<NPCClass, NPCClassLoader>getLoader().lazyLoadAll();
		GameObjectType.QUEST.<Quest, QuestLoader>getLoader().lazyLoadAll();
		(new BukkitRunnable() {
			public void run() {
				GameObjectType.NPC.<NPC, NPCLoader>getLoader().lazyLoadAllPermanent();
				getLogger().info("Flushing invalid game objects from initial load...");
				(new BukkitRunnable() {
					int i = 1;

					public void run() {
						verifyGameIntegrityRunnable.run(true);
						this.i++;
						if (this.i >= 5) {
							cancel();
							getLogger().info("... flush complete. Entity count: " + getEntities().size());
						}
					}
				}).runTaskTimer(Dragons.this, 20L, 20L);
			}
		}).runTaskLater(this, 20L);

		getLogger().info("Registering lightweight object loaders...");
		this.lightweightLoaderRegistry.register(new ChangeLogLoader(mongoConfig));
		this.lightweightLoaderRegistry.register(new FeedbackLoader(mongoConfig));
		this.lightweightLoaderRegistry.register(new SystemProfileLoader(this));
		this.lightweightLoaderRegistry.register(new WarpLoader(mongoConfig));

		getLogger().info("Registering events...");
		PluginManager pluginManager = getServer().getPluginManager();
		pluginManager.registerEvents(new EntityDeathEventListener(this), this);
		pluginManager.registerEvents(new EntityDamageByEntityEventListener(this), this);
		pluginManager.registerEvents(new WorldEventListeners(), this);
		pluginManager.registerEvents(new EntityTargetEventListener(this), this);
		pluginManager.registerEvents(new InventoryEventListeners(), this);
		pluginManager.registerEvents(new PlayerEventListeners(this), this);
		pluginManager.registerEvents(new EntityCombustListener(), this);

		getLogger().info("Registering packet listeners...");
		ProtocolLibrary.getProtocolManager().addPacketListener((PacketListener) new EntityMoveListener(this));

		getLogger().info("Registering commands...");
		getCommand("rank").setExecutor(new RankCommand());
		getCommand("syslogon").setExecutor(new SystemLogonCommand(this));
		getCommand("bypassdeathcountdown").setExecutor(new BypassDeathCountdownCommand());
		getCommand("ilostthelousystick").setExecutor(new ILostTheLousyStickCommand());
		getCommand("feedback").setExecutor(new FeedbackCommand(this));
		QuestDialogueCommands questDialogueCommands = new QuestDialogueCommands();
		getCommand("fastforwarddialogue").setExecutor(questDialogueCommands);
		getCommand("questchoice").setExecutor(questDialogueCommands);
		getCommand("myquests").setExecutor(new MyQuestsCommand());
		PrivateMessageCommands privateMessageCommands = new PrivateMessageCommands();
		getCommand("msg").setExecutor(privateMessageCommands);
		getCommand("reply").setExecutor(privateMessageCommands);
		getCommand("chatspy").setExecutor(privateMessageCommands);
		getCommand("shout").setExecutor(new ShoutCommand());
		getCommand("channel").setExecutor(new ChannelCommand());
		getCommand("help").setExecutor(new HelpCommand());
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

	public void onDisable() {
		((AutoSaveTask) this.autoSaveRunnable).run(true);
		for (User user : UserLoader.allUsers()) {
			if (user.getPlayer() == null || !user.getPlayer().isOnline())
				continue;
			user.handleQuit();
			user.getPlayer().kickPlayer(ChatColor.YELLOW + "This server instance has been closed. We'll be back online soon.");
		}
		for (Entity e : getEntities()) {
			if (e instanceof org.bukkit.entity.ItemFrame)
				continue;
			e.remove();
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
