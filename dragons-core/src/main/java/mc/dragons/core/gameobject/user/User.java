package mc.dragons.core.gameobject.user;

import static mc.dragons.core.util.BukkitUtil.rollingAsync;
import static mc.dragons.core.util.BukkitUtil.rollingSync;
import static mc.dragons.core.util.BukkitUtil.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.Quest.QuestPauseState;
import mc.dragons.core.gameobject.quest.QuestLoader;
import mc.dragons.core.gameobject.quest.QuestStep;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.region.RegionLoader;
import mc.dragons.core.gameobject.user.chat.ChatChannel;
import mc.dragons.core.gameobject.user.chat.ChatMessageHandler;
import mc.dragons.core.gameobject.user.chat.MessageData;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.gameobject.user.permission.SystemProfileLoader;
import mc.dragons.core.gui.GUI;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.storage.loader.ChangeLogLoader;
import mc.dragons.core.storage.loader.ChangeLogLoader.ChangeLogEntry;
import mc.dragons.core.util.MathUtil;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

/**
 * Represents a player in the RPG.
 * 
 * <p>Like all {@link mc.dragons.core.gameobject.GameObject}s,
 * users are backed by the database.
 * 
 * <p>In addition to the standard properties of players, users
 * have specific skills which can be leveled up, as well as friends,
 * guilds, parties, quest logs, and expanded inventories.
 * 
 * @author Adam
 *
 */
public class User extends GameObject {
	
	// Increasing this will theoretically decrease server load
	public static final double MIN_DISTANCE_TO_UPDATE_STATE = 2.0D;
	
	// Avoid re-allocating the same object a gazillion times
	private static final String DASH_PATTERN_QUOTED = Pattern.quote("-");
	
	private static Dragons instance = Dragons.getInstance();
	private static DragonsLogger LOGGER = instance.getLogger();

	private static RegionLoader regionLoader = GameObjectType.REGION.getLoader();
	private static QuestLoader questLoader = GameObjectType.QUEST.getLoader();
	private static ItemLoader itemLoader = GameObjectType.ITEM.getLoader();
	private static UserLoader userLoader = GameObjectType.USER.getLoader();

	private static UserHookRegistry userHookRegistry = instance.getUserHookRegistry();
	private static ChatMessageHandler chatMessageHandler = new ChatMessageHandler(instance);
	private static ConnectionMessageHandler connectionMessageHandler = new ConnectionMessageHandler();
	private static ChangeLogLoader changeLogLoader = instance.getLightweightLoaderRegistry().getLoader(ChangeLogLoader.class);
	private static SystemProfileLoader systemProfileLoader = instance.getLightweightLoaderRegistry().getLoader(SystemProfileLoader.class);
	private static StateLoader stateLoader = instance.getLightweightLoaderRegistry().getLoader(StateLoader.class);
	private static FileConfiguration config = instance.getConfig();
	
	private Player player; // The underlying Bukkit player associated with this User, or null if the user is offline.
	private Set<Region> cachedRegions; // Last-known occupied regions.
	private Location cachedLocation; // Last-known location.
	private PermissionLevel activePermissionLevel = PermissionLevel.USER;
	private SystemProfile profile; // System profile the user is logged into, or null if none.
	private Map<Quest, QuestStep> questProgress;
	private Map<Quest, Integer> questActionIndices;
	private Map<Quest, QuestPauseState> questPauseStates;
	private Map<Quest, UUID> questCorrelationIDs; // correlation IDs for logging related to quests
	private List<CommandSender> currentlyDebugging; // List of users for which this user is currently receiving debug information.
//	private boolean debuggingErrors; // Whether the user will receive errors from the console in the game chat.
	private Level streamConsoleLevel = Level.OFF; // Minimum level of log messages for the user to receive in the game chat.
	private List<String> currentDialogueBatch; // Current NPC dialogue the player is reading.
	private String currentDialogueSpeaker;
	private int currentDialogueIndex;
	private long whenBeganDialogue;
	private List<Consumer<User>> currentDialogueCompletionHandlers; // Handlers that fire when the current dialogue batch is completed
	private boolean isOverridingWalkSpeed;
	private String lastReceivedMessageFrom;
	private boolean chatSpy; // Whether the user can see others' private messages.
	private GUI currentGUI;
	private boolean joined; // If the user has joined and authenticated yet.
	private boolean initErrorOccurred;
	private boolean initialized = false;
	private List<MessageData> seenMessages;
	
	public static ConnectionMessageHandler getConnectionMessageHandler() {
		return connectionMessageHandler;
	}
	
	public static List<User> asUsers(List<UUID> uuids) {
		return uuids.stream().map(uuid -> userLoader.loadObject(uuid)).collect(Collectors.toList());
	}
	
	/**
	 * Calculates the user's global level based on their current XP.
	 * 
	 * @param xp
	 * @return
	 */
	public static int calculateLevel(int xp) {
		return (int) Math.floor(calculateLevelDecimal(xp));
	}

	public static double calculateLevelDecimal(double xp) {
		return 0.8D * (xp / 1000000 + Math.sqrt(xp / 100)) + 1;
	}
	
	/**
	 * Calculates the maximum XP required for the given global level.
	 * Inverse function of calculateLevel.
	 * 
	 * @param level
	 * @return
	 */
	public static int calculateMaxXP(int level) {
		return (int) Math.floor(calculateMaxXPDecimal(level));
	}

	public static double calculateMaxXPDecimal(int level) {
		return 1250000.0D * Math.pow(Math.sqrt(level + 1999) - 44.721359549996D, 2.0D);
	}
	
	public static int calculateSkillLevel(double progress) {
		return (int) Math.floor(Math.sqrt(progress / 17.0D));
	}

	public static int calculateMaxHealth(int level) {
		return 20 + (level - 1) * 2;
	}

	public User(Player player, StorageManager storageManager, StorageAccess storageAccess) {
		super(storageManager, storageAccess);
		LOGGER.trace("Constructing user (" + player + ", " + storageManager + ", " + storageAccess + ")");
		currentlyDebugging = new ArrayList<>();
		joined = false;
		initialize(player);
	}

	public User initialize(Player player) {
		UUID initCorrelationID = LOGGER.newCID();
		LOGGER.trace(initCorrelationID, "Initializing user " + this + " on player " + player);
		
		if(!Thread.currentThread().getName().contains("Rolling Async") && !Thread.currentThread().getName().contains("Server thread")) {
			LOGGER.warning("User " + getName() + " is being initialized on an unexpected thread (" + Thread.currentThread().getName() + ")");
			LOGGER.warning("Stack trace follows:");
			Thread.dumpStack();
		}
		
		this.player = player;
		sendActionBar(ChatColor.GRAY + "Loading your profile...");
		if (player != null) {
			LOGGER.trace(initCorrelationID, "Bukkit player exists");
			setData("lastLocation", StorageUtil.locToDoc(player.getLocation()));
			setData("health", player.getHealth());
			player.getInventory().clear();
			player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(calculateMaxHealth(getLevel()));
			if (getData("health") != null) {
				player.setHealth((double) getData("health"));
			}
		}
		
		rollingAsync(() -> {
			loadInventory(initCorrelationID, (Document) getData("inventory"));
			questProgress = new HashMap<>();
			questActionIndices = new HashMap<>();
			questPauseStates = new HashMap<>();
			questCorrelationIDs = new HashMap<>();
			seenMessages = Collections.synchronizedList(new ArrayList<>());
			loadQuests(initCorrelationID, (Document) getData("quests"));
			cachedRegions = new HashSet<>();
			activePermissionLevel = PermissionLevel.USER;
			rollingSync(() -> {
				instance.getSidebarManager().createScoreboard(player);
				userHookRegistry.getHooks().forEach(h -> h.onInitialize(this)); // Hooks should be able to assume they're running in the main thread
			});
			if(initErrorOccurred) {
				LOGGER.warning(initCorrelationID, "An error occurred during initialization of user " + getIdentifier());
			}
			else {
				LOGGER.discardCID(initCorrelationID);
			}
			initialized = true;
			LOGGER.trace("Finished initializing user " + this);
		});
		
		return this;
	}

	private synchronized void initErrorOccurred() {
		initErrorOccurred = true;
	}
	
	public boolean isInitialized() {
		return initialized;
	}
	
	public void addDebugTarget(CommandSender debugger) {
		currentlyDebugging.add(debugger);
	}

	public void removeDebugTarget(CommandSender debugger) {
		currentlyDebugging.remove(currentlyDebugging.indexOf(debugger));
	}

	public void debug(String message) {
		boolean console = false;
		for (CommandSender debugger : currentlyDebugging) {
			debugger.sendMessage(ChatColor.YELLOW + "DBG:" + getName() + " " + ChatColor.RESET + message);
			if(debugger instanceof ConsoleCommandSender) {
				console = true;
			}
		}
		if(!console) {
			LOGGER.debug("DBG:" + getName() + " " + message);
		}
	}

	@Deprecated
	public void setDebuggingErrors(boolean on) {
//		debuggingErrors = on;
		setStreamConsoleLevel(Level.WARNING);
	}
	
	@Deprecated
	public boolean isDebuggingErrors() {
//		return debuggingErrors;
		return getStreamConsoleLevel().intValue() <= Level.WARNING.intValue();
	}
	
	public void setStreamConsoleLevel(Level level) {
		streamConsoleLevel = level;
	}
	
	public Level getStreamConsoleLevel() {
		return streamConsoleLevel;
	}
	
	public void updateState() {
		updateState(true, true);
	}

	/**
	 * Called periodically to update the user's cached data and dispatch context-based game events.
	 * 
	 * @param applyQuestTriggers
	 * @param notify
	 */
	public void updateState(boolean applyQuestTriggers, boolean notify) {
		LOGGER.verbose("Update user state: " + getName() + " (applyQuestTriggers=" + applyQuestTriggers + ", notify=" + notify + ")");
		String worldName = player.getWorld().getName();
		boolean privilegedWorld = !worldName.equals("staff_verification") && !worldName.equals("trials") && !worldName.equalsIgnoreCase("trial-" + player.getName());
		if (PermissionUtil.verifyActiveProfileFlag(this, SystemProfileFlag.TRIAL_BUILD_ONLY, false) && privilegedWorld) {
			player.sendMessage(ChatColor.RED + "Trial builders can only access the trial world!");
			if (cachedLocation.getWorld().getName().equals("trials") || cachedLocation.getWorld().getName().equalsIgnoreCase("trial-" + player.getName())) {
				player.teleport(cachedLocation);
			} else if (Bukkit.getWorld("trial-" + player.getName()) != null) {
				player.teleport(Bukkit.getWorld("trial-" + player.getName()).getSpawnLocation());
			} else {
				player.teleport(Bukkit.getWorld("trials").getSpawnLocation());
			}
		}
		Set<Region> regions = regionLoader.getRegionsByLocation(player.getLocation());
		if (cachedLocation != null && cachedLocation.getWorld() != player.getLocation().getWorld()) {
			Floor floor = FloorLoader.fromWorldName(player.getLocation().getWorld().getName());
			cachedLocation = player.getLocation();
			cachedRegions = regions;
			if (notify) {
				if (floor == null) {
					sendActionBar(ChatColor.DARK_RED + "- Unofficial World -");
					player.sendMessage(ChatColor.RED + "WARNING: This is an unofficial world and is not associated with a floor.");
				} else {
					player.sendMessage(ChatColor.GRAY + "Floor " + floor.getLevelMin() + ": " + floor.getDisplayName());
					player.sendTitle(ChatColor.DARK_GRAY + "Floor " + floor.getLevelMin(), ChatColor.GRAY + floor.getDisplayName(), 20, 40, 20);
				}
			}
			return;
		}
		for (Region region : cachedRegions) {
			if (regions.contains(region) || Boolean.valueOf(region.getFlags().getString(Region.FLAG_HIDDEN)).booleanValue()) {
				continue;
			}
			if (notify) {
				player.sendMessage(ChatColor.GRAY + "Leaving " + region.getFlags().getString(Region.FLAG_FULLNAME));
			}
		}
		for (Region region : regions) {
			if (!cachedRegions.contains(region)) {
				int lvMin = Integer.parseInt(region.getFlags().getString(Region.FLAG_LVMIN));
				if (getLevel() < lvMin) {
					player.setVelocity(cachedLocation.toVector().subtract(player.getLocation().toVector()).multiply(2.0D));
					if (notify) {
						player.sendMessage(ChatColor.RED + "This region requires level " + lvMin + " to enter");
					}
				}
				if (Boolean.valueOf(region.getFlags().getString(Region.FLAG_HIDDEN))) {
					continue;
				}
				if (notify) {
					if (Boolean.parseBoolean(region.getFlags().getString("showtitle"))) {
						player.sendTitle("", ChatColor.GRAY + "Entering " + region.getFlags().getString(Region.FLAG_FULLNAME), 20, 40, 20);
					}
					player.sendMessage(ChatColor.GRAY + "Entering " + region.getFlags().getString(Region.FLAG_FULLNAME));
					if (!region.getFlags().getString(Region.FLAG_DESC).equals("")) {
						player.sendMessage(ChatColor.DARK_GRAY + "   " + ChatColor.ITALIC + region.getFlags().getString(Region.FLAG_DESC));
					}
				}
				int lvRec = Integer.parseInt(region.getFlags().getString(Region.FLAG_LVREC));
				if (getLevel() < lvRec && notify) {
					player.sendMessage(ChatColor.YELLOW + " Caution: The recommended level for this region is " + lvRec);
				}
			}
		}
		if (applyQuestTriggers) {
			updateQuests(null);
		}
		userHookRegistry.getHooks().forEach(h -> h.onUpdateState(this, cachedLocation));
		cachedLocation = player.getLocation();
		cachedRegions = regions;
		updateEffectiveWalkSpeed();
	}
	
	
	/*
	 * Quest management
	 */
	
	public void loadQuests(UUID cid, Document questProgressDoc) {
		LOGGER.verbose(cid, "stored quest data: " + questProgressDoc.toJson());
		questProgress.clear();
		questActionIndices.clear();
		questPauseStates.clear();
		questCorrelationIDs.clear();
		for (Entry<String, Object> entry : (Iterable<Entry<String, Object>>) questProgressDoc.entrySet()) {
			Quest quest = questLoader.getQuestByName(entry.getKey());
			if (quest == null) {
				continue;
			}
			questProgress.put(quest, quest.getSteps().get((int) entry.getValue()));
			questActionIndices.put(quest, 0);
			questPauseStates.put(quest, QuestPauseState.NORMAL);
			questCorrelationIDs.put(quest, LOGGER.newCID());
		}
	}

	public void logQuestEvent(Quest quest, Level level, String message) {
		LOGGER.log(questCorrelationIDs.computeIfAbsent(quest, q -> LOGGER.newCID()), level, quest.getName() + " | " + message);
	}
	
	public void logAllQuestData(Quest quest) {
		logQuestEvent(quest, Level.CONFIG, "Dumping all quest data");
		logQuestEvent(quest, Level.CONFIG, "Current Step: " + getQuestProgress().get(quest).getStepName());
		logQuestEvent(quest, Level.CONFIG, "Current Action Index: " + getQuestActionIndex(quest));
		logQuestEvent(quest, Level.CONFIG, "Current Pause State: " + getQuestPauseState(quest));
		logQuestEvent(quest, Level.CONFIG, "Has Active Dialogue: " + hasActiveDialogue());
		if(hasActiveDialogue()) {
			logQuestEvent(quest, Level.CONFIG, "Number of dialogue callbacks: " + currentDialogueCompletionHandlers.size());
		}
	}
	
	public UUID getQuestCorrelationID(Quest quest) {
		return questCorrelationIDs.computeIfAbsent(quest, q -> LOGGER.newCID());
	}

	public void setDialogueBatch(Quest quest, String speaker, List<String> dialogue) {
		currentDialogueSpeaker = speaker;
		currentDialogueBatch = dialogue;
		currentDialogueIndex = 0;
		whenBeganDialogue = System.currentTimeMillis();
		currentDialogueCompletionHandlers = new CopyOnWriteArrayList<>();
	}

	public boolean hasActiveDialogue() {
		return currentDialogueBatch != null;
	}

	public long getWhenBeganDialogue() {
		return whenBeganDialogue;
	}

	public void onDialogueComplete(Consumer<User> handler) {
		if (!hasActiveDialogue()) {
			return;
		}
		currentDialogueCompletionHandlers.add(handler);
	}

	public void resetDialogueAndHandleCompletion() {
		if (currentDialogueBatch == null) {
			return;
		}
		if (currentDialogueIndex >= currentDialogueBatch.size()) {
			debug("Handling dialogue completion...");
			currentDialogueSpeaker = null;
			currentDialogueBatch = null;
			currentDialogueIndex = 0;
			for (Consumer<User> handler : currentDialogueCompletionHandlers) {
				handler.accept(this);
			}
		}
	}

	public void fastForwardDialogue() {
		while (hasActiveDialogue()) {
			nextDialogue();
		}
	}

	public boolean nextDialogue() {
		if (!hasActiveDialogue()) {
			return false;
		}
		debug("nextDialogue");
		debug(" - idx=" + currentDialogueIndex);
		TextComponent message = new TextComponent(
				TextComponent.fromLegacyText(ChatColor.GRAY + "[" + (currentDialogueIndex + 1) + "/" + currentDialogueBatch.size() + "] " + ChatColor.DARK_GREEN + currentDialogueSpeaker
						+ ": " + ChatColor.GREEN + currentDialogueBatch.get(currentDialogueIndex++).replaceAll(Pattern.quote("%PLAYER%"), getName())));
		message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/fastforwarddialogue"));
		message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + "Click to fast-forward through the dialogue")));
		player.spigot().sendMessage(message);
		if (currentDialogueIndex >= currentDialogueBatch.size()) {
			resetDialogueAndHandleCompletion();
			return false;
		}
		return true;
	}

	public void setQuestPaused(Quest quest, boolean paused) {
		questPauseStates.put(quest, paused ? QuestPauseState.PAUSED : QuestPauseState.RESUMED);
		debug(String.valueOf(paused ? "Paused" : "Unpaused") + " quest " + quest.getName());
		logQuestEvent(quest, Level.FINE, "Set quest pause state to " + paused);
	}

	public void resetQuestPauseState(Quest quest) {
		questPauseStates.put(quest, QuestPauseState.NORMAL);
		debug("Reset pause state for quest " + quest.getName());
	}

	public QuestPauseState getQuestPauseState(Quest quest) {
		return questPauseStates.getOrDefault(quest, QuestPauseState.NORMAL);
	}

	/**
	 * Called whenever a quest trigger has been potentially updated.
	 * 
	 * @param event
	 */
	public void updateQuests(Event event) {
		if (currentDialogueBatch != null && currentDialogueIndex < currentDialogueBatch.size()) {
			debug("updateQuests(): Cancelled quest update because of active dialogue");
			return;
		}
		for (Entry<Quest, QuestStep> questStep : questProgress.entrySet()) {			
			if (questStep.getValue().getStepName().equalsIgnoreCase("Complete")) {
				continue;
			}
			debug("updateQuests(): Step " + questStep.getValue().getStepName() + " of " + questStep.getKey().getName());
			QuestPauseState pauseState = getQuestPauseState(questStep.getKey());
			if (pauseState == QuestPauseState.PAUSED) {
				continue;
			}
			Quest quest = questStep.getKey();
			int actionIndex = getQuestActionIndex(quest);
			debug("updateQuests():   - Trigger = " + questStep.getValue().getTrigger().getTriggerType());
			debug("updateQuests():   - Action# = " + actionIndex);
			
			/*
			 * If they meet the trigger, great
			 * If they were resumed from a pause state, great
			 * If they already met the trigger and were further along in the stage, great
			 * Any of these conditions are sufficient
			 */
			if (questStep.getValue().getTrigger().test(this, event) || pauseState == QuestPauseState.RESUMED || actionIndex > 0) {
				debug("updateQuests():     - Triggered (starting @ action #" + actionIndex + ")");
				if (questStep.getValue().executeActions(this, actionIndex)) {
					debug("updateQuests():      - Normal progression to next step");
					int nextIndex = quest.getSteps().indexOf(questStep.getValue()) + 1;
					if (nextIndex != quest.getSteps().size()) {
						QuestStep nextStep = quest.getSteps().get(nextIndex);
						logQuestEvent(quest, Level.FINE, "update quest progress step " + questStep.getValue().getStepName() + " -> " + nextStep.getStepName());
						updateQuestProgress(quest, nextStep, true);
					}
				}
			}
		}
	}

	public Map<Quest, QuestStep> getQuestProgress() {
		return questProgress;
	}

	public void updateQuestProgress(Quest quest, QuestStep questStep, boolean notify) {
		if(quest.isLocked() && !PermissionUtil.verifyActivePermissionLevel(this, PermissionLevel.GM, false)) {
			sendActionBar(ChatColor.RED + "Quest \"" + quest.getQuestName() + "\" is currently locked! Try again later.");
		}
		Document updatedQuestProgress = (Document) getData("quests");
		if (questStep == null) {
			questProgress.remove(quest);
			updatedQuestProgress.remove(quest.getName());
			storageAccess.update(new Document("quests", updatedQuestProgress));
			return;
		}
		debug("updateQuestProgress(" + quest.getName() + ", " + questStep.getStepName() + ", notify=" + notify + ")");
		questProgress.put(quest, questStep);
		resetQuestPauseState(quest);
		questActionIndices.put(quest, Integer.valueOf(0));
		updatedQuestProgress.append(quest.getName(), Integer.valueOf(quest.getSteps().indexOf(questStep)));
		storageAccess.update(new Document("quests", updatedQuestProgress));
		if (notify) {
			if (questStep.getStepName().equals("Complete")) {
				logQuestEvent(quest, Level.FINE, "completed quest");
				player.sendMessage(ChatColor.GRAY + "Completed quest " + quest.getQuestName());
			} else {
				player.sendMessage(ChatColor.GRAY + "New Objective: " + questStep.getStepName());
			}
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				User.this.updateQuests((Event) null);
			}
		}.runTaskLater(instance, 1L);
	}

	public void updateQuestAction(Quest quest, int actionIndex) {
		logQuestEvent(quest, Level.FINE, "set action index to " + actionIndex);
		questActionIndices.put(quest, actionIndex);
	}

	public int getQuestActionIndex(Quest quest) {
		return questActionIndices.getOrDefault(quest, 0);
	}

	public void updateQuestProgress(Quest quest, QuestStep questStep) {
		updateQuestProgress(quest, questStep, true);
	}

	
	/*
	 * GUI management
	 */
	
	public void openGUI(GUI gui, Inventory inventory) {
		player.closeInventory();
		debug("opening gui " + gui.getMenuName());
		player.openInventory(inventory);
		currentGUI = gui;
	}

	public void closeGUI(boolean forceClose) {
		debug("closing gui");
		if (currentGUI == null) {
			return;
		}
		currentGUI = null;
		if (forceClose) {
			player.closeInventory();
		}
	}

	public boolean hasOpenGUI() {
		return currentGUI != null;
	}

	public GUI getCurrentGUI() {
		return currentGUI;
	}
	
	/*
	 * Chat management
	 */

	public synchronized List<MessageData> getSeenMessages() {
		return seenMessages;
	}
	
	public List<ChatChannel> getActiveChatChannels() {
		return getData().getList("chatChannels", String.class).stream().map(ch -> ChatChannel.valueOf(ch)).collect(Collectors.toList());
	}

	public void addActiveChatChannel(ChatChannel channel) {
		List<String> channels = getData().getList("chatChannels", String.class);
		channels.add(channel.toString());
		setData("chatChannels", channels);
	}

	public void removeActiveChatChannel(ChatChannel channel) {
		List<String> channels = getData().getList("chatChannels", String.class);
		channels.remove(channel.toString());
		setData("chatChannels", channels);
	}

	public ChatChannel getSpeakingChannel() {
		return ChatChannel.valueOf((String) getData("speakingChannel"));
	}

	public void setSpeakingChannel(ChatChannel channel) {
		setData("speakingChannel", channel.toString());
	}

	/**
	 * Send the user a message if they are listening on the specified channel.
	 * 
	 * @param channel
	 * @param message
	 */
	public void sendMessage(ChatChannel channel, User source, String message) {
		sendMessage(channel, source, TextComponent.fromLegacyText(message));
	}

	/**
	 * Send the user a message if they are listening on the specified channel.
	 * 
	 * @param channel
	 * @param message
	 */
	public void sendMessage(ChatChannel channel, User source, BaseComponent... message) {
		if (getActiveChatChannels().contains(channel) && channel.canHear(this, source)) {
			player.spigot().sendMessage(new ComponentBuilder(channel.getPrefix()).append(" ").append(message).create());
		}
	}

	public void chat(String message) {
		if (!hasJoined()) {
			player.sendMessage(ChatColor.RED + "You are not joined yet!");
			return;
		}
		if (hasActiveDialogue()) {
			player.sendMessage(ChatColor.RED + "Chat is unavailable while in NPC dialogue!");
			return;
		}
		for(UserHook hook : instance.getUserHookRegistry().getHooks()) {
			if(!hook.checkAllowChat(this, message)) {
				LOGGER.debug("Blocked " + getName() + " from sending chat message \"" + message + "\"" + " due to policy from user hook " + hook);
				return;
			}
		}
		if (!getSpeakingChannel().canHear(this, this)) {
			player.sendMessage(ChatColor.RED + "Could not deliver message: You can't hear yourself! "
				+ "Make sure you're listening to the channel you're speaking on (/cl" + getSpeakingChannel().getAbbreviation().toLowerCase() + ")");
			return;
		}
		
		chatMessageHandler.send(this, getSpeakingChannel(), message);
		userHookRegistry.getHooks().forEach(hook -> hook.onChat(getSpeakingChannel(), message));
	}

	public String getLastReceivedMessageFrom() {
		return lastReceivedMessageFrom;
	}

	public void setLastReceivedMessageFrom(String lastReceivedMessageFrom) {
		this.lastReceivedMessageFrom = lastReceivedMessageFrom;
	}

	public void setChatSpy(boolean enabled) {
		chatSpy = enabled;
	}

	public boolean hasChatSpy() {
		return chatSpy;
	}
	
	public List<User> getBlockedUsers() {
		return asUsers(getData().getList("blockedUsers", UUID.class));
	}
	
	public void blockUser(User user) {
		List<UUID> blocked = getData().getList("blockedUsers", UUID.class);
		blocked.add(user.getUUID());
		storageAccess.set("blockedUsers", blocked);
	}
	
	public void unblockUser(User user) {
		List<UUID> blocked = getData().getList("blockedUsers", UUID.class);
		blocked.remove(user.getUUID());
		storageAccess.set("blockedUsers", blocked);
	}
	
	/*
	 * Item management
	 */
	

	/**
	 * Give the player an RPG item.
	 * 
	 * This process is non-trivial, as we have to allow stacking
	 * in certain cases despite conflicting metadata.
	 * 
	 * Thus, we need to rewrite practically the whole item stacking
	 * algorithm, as well as correctly synchronize with the player's
	 * stored inventory data.
	 * 
	 * @param item
	 * @param updateDB
	 * @param dbOnly
	 * @param silent
	 */
	public void giveItem(Item item, boolean updateDB, boolean dbOnly, boolean silent) {
		int giveQuantity = item.getQuantity();
		int maxStackSize = item.getMaxStackSize();
		if (!dbOnly) {
			int remaining = giveQuantity;
			for (int i = 0; i < player.getInventory().getContents().length; i++) {
				ItemStack itemStack = player.getInventory().getContents()[i];
				if (itemStack != null) {
					Item testItem = ItemLoader.fromBukkit(itemStack);
					if (testItem != null && item.getClassName().equals(testItem.getClassName()) && !item.isCustom() && !testItem.isCustom()) {
						int quantity = Math.min(maxStackSize, testItem.getQuantity() + remaining);
						int added = quantity - testItem.getQuantity();
						debug("Adding to existing stack: " + testItem.getUUID().toString() + " (curr=" + testItem.getQuantity() + "=" + testItem.getItemStack().getAmount() + ", add=" + added + ", tot=" + quantity + ")");
						remaining -= added;
						testItem.setQuantity(quantity);
						player.getInventory().setItem(i, testItem.getItemStack());
						item.setQuantity(item.getQuantity() - added);
						debug("-Quantity: " + testItem.getQuantity() + "=" + testItem.getItemStack().getAmount());
						if (remaining == 0) {
							break;
						}
						debug(" - " + remaining + " remaining to dispense");
					}
				}
			}
			if (remaining > 0) {
				debug("Adding remaining items as new item stack");
				player.getInventory().addItem(new ItemStack[] { item.getItemStack() });
			}
		}
		if (updateDB) {
			storageAccess.update(new Document("inventory", getInventoryAsDocument()));
		}
		if (!silent) {
			player.sendMessage(ChatColor.GRAY + "+ " + item.getDecoratedName() + (item.getQuantity() > 1 ? ChatColor.GRAY + " (x" + giveQuantity + ")" : ""));
		}
	}

	public void giveItem(Item item) {
		giveItem(item, true, false, false);
	}

	public void takeItem(Item item, int amount, boolean updateDB, boolean updateInventory, boolean notify) {
		debug("Removing " + amount + " of " + item.getName() + " (has " + item.getQuantity() + "=" + item.getItemStack().getAmount() + ")");
		if (amount < item.getQuantity()) {
			debug("-Current quantity: " + item.getQuantity() + "=" + item.getItemStack().getAmount());
			debug("-New quantity: " + (item.getQuantity() - amount));
			item.setQuantity(item.getQuantity() - amount);
			debug("-Saved quantity: " + item.getQuantity() + "=" + item.getItemStack().getAmount());
		}
		if (updateInventory) {
			ItemStack removal = item.getItemStack().clone();
			removal.setAmount(amount);
			player.getInventory().removeItem(new ItemStack[] { removal });
		}
		if (updateDB) {
			storageAccess.update(new Document("inventory", getInventoryAsDocument()));
		}
		if (notify) {
			player.sendMessage(ChatColor.RED + "- " + item.getDecoratedName() + (amount > 1 ? ChatColor.GRAY + " (x" + amount + ")" : ""));
		}
		debug("-Final quantity: " + item.getQuantity() + "=" + item.getItemStack().getAmount());
	}

	public void takeItem(Item item) {
		takeItem(item, 1, true, true, true);
	}

	public void buyItem(ItemClass itemClass, int quantity, double costPer) {
		debug("Attempting to buy " + quantity + " of " + itemClass.getClassName() + " at " + costPer + "g ea");
		double price = costPer * quantity;
		double balance = getGold();
		if (balance < price) {
			player.sendMessage(ChatColor.RED + "Cannot buy this item! Costs " + price + "g, you have " + balance + " (need " + (price - balance) + "g more)");
			return;
		}
		takeGold(price, false);
		Item item = itemLoader.registerNew(itemClass);
		item.setQuantity(quantity);
		giveItem(item, true, false, true);
		player.sendMessage(ChatColor.GREEN + "Purchased " + item.getDecoratedName() + (quantity > 1 ? ChatColor.GRAY + " (x" + quantity + ")" : "") + ChatColor.GREEN + " for "
				+ ChatColor.GOLD + price + "g");
	}

	
	/*
	 * Inventory management
	 */
	
	/**
	 * 
	 * @param cid
	 * @return whether an error occurred.
	 */
	public boolean loadInventory(UUID cid, Document inventory) {
		LOGGER.verbose(cid, "Stored inventory data: " + inventory);
		List<UUID> usedItems = new ArrayList<>();
		int dups = 0;
		int broken = 0;
		Set<UUID> uuids = inventory.entrySet().stream().map(e -> (UUID) e.getValue()).collect(Collectors.toSet());
		Map<UUID, Item> items = itemLoader.loadObjects(uuids);
		
		Map<Integer, ItemStack> bukkitInventory = new HashMap<>();
		
		for (Entry<String, Object> entry : (Iterable<Entry<String, Object>>) inventory.entrySet()) {
			String[] labels = entry.getKey().split(DASH_PATTERN_QUOTED);
			String part = labels[0];
			int slot = Integer.valueOf(labels[1]);
			UUID id = (UUID) entry.getValue();
			if(usedItems.contains(id)) {
				dups++;
				LOGGER.warning(cid, "Duplicated item: " + id);
				continue;
			}
			Item item = items.get(id);
			if (item == null) {
				broken++;
				LOGGER.warning(cid, "Could not load item: " + id);
				continue;
			}
			ItemStack itemStack = item.getItemStack();
			if (part.equals("I")) {
				bukkitInventory.put(slot, itemStack);
				continue;
			}
		}
		syncSetInventory(bukkitInventory);
		boolean error = false;
		if (broken > 0) {
			player.sendMessage(ChatColor.RED + "" + broken + " items in your saved inventory could not be loaded.");
			error = true;
		}
		if(dups > 0) {
			player.sendMessage(ChatColor.RED + "" + dups + " duplicated items were found in your saved inventory.");
			error = true;
		}
		if(error) {
			player.sendMessage(ChatColor.RED + "Use this error code in any communications with staff: " + StringUtil.toHdFont("Correlation ID: " + cid));
			initErrorOccurred();
		}
		return error;
	}
	
	private void syncSetInventory(Map<Integer, ItemStack> inventory) {
		if(player == null) return;
		sync(() -> {
			inventory.forEach((slot, itemStack) -> {
				player.getInventory().setItem(slot, itemStack);
			});
		});
	}
	public void clearInventory() {
		player.getInventory().clear();
		setData("inventory", new ArrayList<>());
		sendActionBar(ChatColor.DARK_RED + "- All items have been lost! -");
	}
	
	public Document getInventoryAsDocument() {
		Document inventory = new Document();
		for (int i = 0; i < player.getInventory().getContents().length; i++) {
			ItemStack is = player.getInventory().getContents()[i];
			if (is != null) {
				Item item = ItemLoader.fromBukkit(is);
				if (item != null) {
					inventory.append("I-" + i, item.getUUID());
				}
			}
		}
		return inventory;
	}
	
	
	/*
	 * Event handlers
	 */

	public void handleJoin(boolean firstJoin) {
		joined = true;
		setData("username", getPlayer().getName());
		setData("lastJoined", System.currentTimeMillis());
		setData("currentServer", instance.getServerName());
		if (PermissionUtil.verifyActivePermissionLevel(this, PermissionLevel.TESTER, false)) {
			player.setGameMode(getSavedGameMode());
		} else {
			player.setGameMode(GameMode.ADVENTURE);
		}
		if (isVanished()) {
			player.sendMessage(ChatColor.DARK_GREEN + "You are currently vanished.");
		} else if (getRank().ordinal() >= Rank.PATRON.ordinal()) {
			Bukkit.broadcastMessage(getRank().getNameColor() + "" + ChatColor.BOLD + getRank().getRankName() + " " + player.getName() + " joined!");
		} else {
			Bukkit.broadcastMessage(ChatColor.GRAY + player.getName() + " joined!");
		}
		player.sendMessage(ChatColor.GOLD + "Hello " + getName() + " and welcome to DragonsOnline.");
		String spacer = ChatColor.GRAY + "    -    ";
		player.sendMessage(ChatColor.LIGHT_PURPLE + "Level: " + getLevel() + spacer + ChatColor.GREEN + "XP: " + getXP() + " (" + MathUtil.round(getLevelProgress() * 100.0F) + "%)" + spacer
				+ ChatColor.YELLOW + "Gold: " + getGold());
		TextComponent component = new TextComponent(ChatColor.AQUA + "Speaking in ");
		TextComponent speaking = getSpeakingChannel().format();
		speaking.setColor(net.md_5.bungee.api.ChatColor.DARK_AQUA);
		component.addExtra(speaking);
		component.addExtra(ChatColor.AQUA + " and listening to ");
		List<ChatChannel> channels = getActiveChatChannels();
		for (int i = 0; i < channels.size(); i++) {
			TextComponent listening = channels.get(i).format();
			listening.setColor(net.md_5.bungee.api.ChatColor.DARK_AQUA);
			component.addExtra(listening);
			if (i < channels.size() - 1) {
				component.addExtra(", ");
			}
		}
		player.spigot().sendMessage(component);
		if (firstJoin) {
			player.sendMessage(ChatColor.AQUA + "Use " + ChatColor.DARK_AQUA + "/channel" + ChatColor.AQUA + " to change channels.");
		}
		if (getUnreadChangeLogs().size() > 0) {
			player.sendMessage(ChatColor.DARK_GREEN + "You have unread changelogs! Do " + ChatColor.GREEN + "/whatsnew" + ChatColor.DARK_GREEN + " to read them!");
		}
		userHookRegistry.getHooks().forEach(h -> h.onVerifiedJoin(this));
		player.sendMessage("");
		updateState();
		updateVanishState();
		updateVanishStatesOnSelf();
		updateVanillaLeveling();
		updateTablistHeaders();
		String ip = player.getAddress().getAddress().getHostAddress();
		setData("ip", ip);
		
		List<String> ipHistory = getData().getList("ipHistory", String.class);
		if(!ipHistory.contains(ip)) {
			ipHistory.add(ip);
		}
		setData("ipHistory", ipHistory);
		connectionMessageHandler.logConnect(this);
		LOGGER.exiting("User", "handleJoin");
	}

	public void handleQuit() {
		handleQuit(true);
	}
	
	public void handleQuit(boolean removeFromLocalRegistry) {
		autoSave();
		setData("totalOnlineTime", getTotalOnlineTime() + getLocalOnlineTime());
		setData("currentServer", null);
		if (!isVanished() && joined) {
			Bukkit.broadcastMessage(ChatColor.GRAY + player.getName() + " left!");
		}
		if (profile != null && instance.isEnabled()) {
			systemProfileLoader.logoutProfile(profile.getProfileName());
			setActivePermissionLevel(PermissionLevel.USER);
			setSystemProfile(null);
		}
		player.getInventory().clear();
		player.getInventory().setArmorContents(new ItemStack[4]);
		userHookRegistry.getHooks().forEach(h -> h.onQuit(this));
		connectionMessageHandler.logDisconnect(this);
		if(removeFromLocalRegistry) {
			userLoader.removeStalePlayer(player);
		}
		player = null;
	}

	public void handleMove() {
		boolean update = false;
		if (cachedLocation == null) {
			cachedLocation = player.getLocation();
		} else if (player.getLocation().getWorld() != cachedLocation.getWorld()) {
			update = true;
		} else if (player.getLocation().distanceSquared(cachedLocation) >= 4.0D) {
			update = true;
		}
		if (update) {
			updateState();
		}
	}
	
	
	/*
	 * Miscellaneous setters and getters
	 */

	public int getLastReadChangeLogId() {
		return (int) getData("lastReadChangeLog");
	}

	public List<ChangeLogEntry> getUnreadChangeLogs() {
		return changeLogLoader.getUnreadChangelogs(getLastReadChangeLogId());
	}

	public void markChangeLogsRead() {
		setData("lastReadChangeLog", Integer.valueOf(changeLogLoader.getCurrentMaxId()));
	}

	public String getLastIP() {
		return (String) getData("ip");
	}
	
	public List<String> getIPHistory() {
		return getData().getList("ipHistory", String.class);
	}

	public Rank getRank() {
		return Rank.valueOf((String) getData("rank"));
	}

	public void setRank(Rank rank) {
		setData("rank", rank.toString());
	}

	public Set<Region> getRegions() {
		return cachedRegions;
	}

	public Date getFirstJoined() {
		return new Date((long) getData("firstJoined"));
	}

	public Date getLastJoined() {
		return new Date((long) getData("lastJoined"));
	}

	public Date getLastSeen() {
		return new Date((long) getData("lastSeen"));
	}

	public boolean hasJoined() {
		return joined;
	}
	
	public long getTotalOnlineTime() {
		return (long) getData("totalOnlineTime");
	}

	public long getLocalOnlineTime() {
		return (long) Math.floor((System.currentTimeMillis() - ((long) getData("lastJoined"))) / 1000L);
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public String getName() {
		return (String) getData("username");
	}

	public Location getSavedLocation() {
		return StorageUtil.docToLoc((Document) getData("lastLocation"));
	}
	
	public String getServer() {
		return (String) getData("currentServer");
	}

	/**
	 * Location as staff is saved separately so that compromised staff
	 * accounts will not be logged in at potentially sensitive areas.
	 * 
	 * @return
	 */
	public Location getSavedStaffLocation() {
		if (getData("lastStaffLocation") == null) {
			setData("lastStaffLocation", getData("lastLocation"));
		}
		return StorageUtil.docToLoc((Document) getData("lastStaffLocation"));
	}

	public void setSavedLocation(Location loc) {
		setData("lastLocation", StorageUtil.locToDoc(loc));
	}

	public void setSavedStaffLocation(Location loc) {
		setData("lastStaffLocation", StorageUtil.locToDoc(loc));
	}
	
	public GameMode getSavedGameMode() {
		return GameMode.valueOf((String) getData("gamemode"));
	}

	public void setGameMode(GameMode gameMode, boolean updateBukkit) {
		setData("gamemode", gameMode.toString());
		if (updateBukkit) {
			player.setGameMode(gameMode);
		}
	}
	
	public double getSavedHealth() {
		return (double) getData("health");
	}

	public double getSavedMaxHealth() {
		return (double) getData("maxHealth");
	}

	public double getGold() {
		return (double) getData("gold");
	}

	public void setGold(double gold, boolean notify) {
		setData("gold", Double.valueOf(gold));
		if (notify) {
			player.sendMessage(ChatColor.GRAY + "Your gold balance is now " + ChatColor.GOLD + gold);
		}
	}

	public void setGold(double gold) {
		setGold(gold, true);
	}

	public void giveGold(double gold, boolean notify) {
		setData("gold", Double.valueOf(getGold() + gold));
		if (notify) {
			player.sendMessage(ChatColor.GRAY + "+ " + ChatColor.GOLD + gold + " Gold");
		}
	}

	public void giveGold(double gold) {
		giveGold(gold, true);
	}

	public void takeGold(double gold, boolean notify) {
		setData("gold", Double.valueOf(getGold() - gold));
		if (notify) {
			player.sendMessage(ChatColor.RED + "- " + ChatColor.GOLD + gold + " Gold");
		}
	}

	public void takeGold(double gold) {
		takeGold(gold, true);
	}

	public void sendActionBar(String message) {
		if(player == null) return;
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
	}

	@Deprecated
	public void sendTitle(ChatColor titleColor, String title, ChatColor subtitleColor, String subtitle) {
		sendTitle(titleColor, title, subtitleColor, subtitle, 20, 40, 20);
	}

	@Deprecated
	public void sendTitle(ChatColor titleColor, String title, ChatColor subtitleColor, String subtitle, int fadeInTime, int showTime, int fadeOutTime) {
		instance.getBridge().sendTitle(player, titleColor, title, subtitleColor, subtitle, fadeInTime, showTime, fadeOutTime);
	}

	public void updateTablistHeaders() {
		player.setPlayerListHeaderFooter(tablistText(config.getString("game.tablist.header")), 
				tablistText(config.getString("game.tablist.footer")));
	}
	
	public String tablistText(String raw) {
		return StringUtil.colorize(raw).replaceAll("%SERVER%", instance.getServerName());
	}
	
	public void overrideWalkSpeed(float speed) {
		player.setWalkSpeed(speed);
		isOverridingWalkSpeed = true;
	}

	public void removeWalkSpeedOverride() {
		isOverridingWalkSpeed = false;
		player.setWalkSpeed((float) getEffectiveWalkSpeed());
	}

	public double getEffectiveWalkSpeed() {
		if (isOverridingWalkSpeed) {
			return player.getWalkSpeed();
		}
		double speed = instance.getServerOptions().getDefaultWalkSpeed();
		for(ItemStack itemStack : player.getInventory().getArmorContents()) {
			if (itemStack != null) {
				Item item = ItemLoader.fromBukkit(itemStack);
				if (item != null) {
					speed += item.getSpeedBoost();
				}
			}
		}
		ItemStack held = player.getInventory().getItemInMainHand();
		Item item = ItemLoader.fromBukkit(held);
		if (item != null) {
			speed += item.getSpeedBoost();
		}
		return Math.min(1.0D, Math.max(0.05D, speed));
	}

	public void updateEffectiveWalkSpeed() {
		player.setWalkSpeed((float) getEffectiveWalkSpeed());
	}

	/**
	 * When the player dies, they are frozen for a given period of time
	 * until they can respawn.
	 * 
	 * @param seconds
	 */
	public void setDeathCountdown(int seconds) {
		setData("deathCountdown", Integer.valueOf(seconds));
		setData("deathTime", Long.valueOf(System.currentTimeMillis()));
		player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * seconds, 10, false, false));
		player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * seconds, 10, false, false));
		player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * seconds, 10, false, false));
		player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * seconds, 0, false, false));
		new BukkitRunnable() {
			int counter = seconds;

			@Override
			public void run() {
				if (User.this.hasDeathCountdown()) {
					User.this.sendActionBar(ChatColor.DARK_RED + "Respawning in " + counter + "s");
					counter--;
				} else {
					User.this.sendActionBar(ChatColor.YELLOW + "Respawning...");
					cancel();
				}
			}
		}.runTaskTimer(instance, 0L, 20L);
	}

	public boolean hasDeathCountdown() {
		Long deathTime = (Long) getData("deathTime");
		if (deathTime == null) {
			return false;
		}
		int deathCountdown = (int) getData("deathCountdown");
		long now = System.currentTimeMillis();
		return deathTime.longValue() + 1000 * deathCountdown > now;
	}

	public int getDeathCountdownRemaining() {
		Long deathTime = (Long) getData("deathTime");
		if (deathTime == null) {
			return 0;
		}
		int deathCountdown = (int) getData("deathCountdown");
		long now = System.currentTimeMillis();
		long remaining = deathTime + deathCountdown - now;
		if(remaining < 0L) return 0;
		return (int) remaining;
	}
	
	public void respawn() {
		player.spigot().respawn();
	}

	public void sendToFloor(String floorName, boolean overrideLevelRequirement) {
		Floor floor = FloorLoader.fromFloorName(floorName);
		if (!overrideLevelRequirement && getLevel() < floor.getLevelMin()) {
			return;
		}
		player.teleport(floor.getWorld().getSpawnLocation());
	}

	public void sendToFloor(String floorName) {
		sendToFloor(floorName, false);
	}

	public String getListName() {
		return (getRank().getChatPrefix() + getRank().getNameColor() + " " + getName() + " " +
				StringUtil.parseList(userHookRegistry.getHooks().stream().map(h -> h.getListNameSuffix(this)).collect(Collectors.toList()), " ")).trim();
	}
	
	public void updateListName() {
		if(player == null) return;
		player.setPlayerListName(getListName());
	}
	
	
	/*
	 * Leveling management
	 */
	
	public void addXP(int xp) {
		setXP(getXP() + xp);
	}

	public void setXP(int xp) {
		int level = calculateLevel(xp);
		if (level > getLevel()) {
			player.sendTitle(ChatColor.DARK_AQUA + "Level Up!", ChatColor.AQUA + "" + getLevel() + " >>> " + level, 20, 40, 20);
			Bukkit.broadcastMessage(ChatColor.AQUA + getName() + " is now level " + level + "!");
		}
		update(new Document("xp", xp).append("level", level));
		updateVanillaLeveling();
	}

	public void updateVanillaLeveling() {
		player.setLevel(calculateLevel(getXP()));
		player.setExp(getLevelProgress());
		player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(calculateMaxHealth(getLevel()));
	}

	public int getXP() {
		return (int) getData("xp");
	}

	public int getLevel() {
		return (int) getData("level");
	}

	public float getLevelProgress() {
		int prevMax = calculateMaxXP(getLevel());
		return (float) Math.min(1.0D, (double) (getXP() - prevMax) / (calculateMaxXP(getLevel() + 1) - prevMax));
	}

	public ChatColor getLevelColor() {
		int level = getLevel();
		if (level < 10) {
			return ChatColor.GRAY;
		}
		if (level < 20) {
			return ChatColor.YELLOW;
		}
		if (level < 30) {
			return ChatColor.GREEN;
		}
		if (level < 40) {
			return ChatColor.AQUA;
		}
		if (level < 50) {
			return ChatColor.DARK_AQUA;
		}
		if (level < 60) {
			return ChatColor.GOLD;
		}
		if (level < 70) {
			return ChatColor.DARK_GREEN;
		}
		if (level < 80) {
			return ChatColor.LIGHT_PURPLE;
		}
		if (level < 90) {
			return ChatColor.DARK_PURPLE;
		}
		if (level < 100) {
			return ChatColor.RED;
		}
		return ChatColor.WHITE;
	}
	
	
	/*
	 * Vanish management (only applies to staff)
	 */

	public static void updateVanishStateBetween(User userOf, User userFor) {
		if (userOf == null || userFor == null) {
			return;
		}
		if (userOf.player == null || userFor.player == null) {
			return;
		}
		if (userOf.isVanished() && userFor.getActivePermissionLevel().ordinal() < userOf.getActivePermissionLevel().ordinal()) {
			userFor.player.hidePlayer(instance, userOf.player);
		} else if (!userFor.player.canSee(userOf.player)) {
			userFor.player.showPlayer(instance, userOf.player);
		}
	}

	public void updateVanishStatesOnSelf() {
		for (Player test : Bukkit.getOnlinePlayers()) {
			User user = UserLoader.fromPlayer(test);
			updateVanishStateBetween(user, this);
		}
	}

	public void updateVanishState() {
		player.setCollidable(!isVanished());
		player.setAllowFlight(!(!isVanished() && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR));
		if (isVanished()) {
			player.setPlayerListName(" ");
			
			// This instantly stops all entities from targeting the player.
			// Entities are prevented in future with relevant event handlers,
			// but those alone are not sufficient to stop pre-existing targeting
			// behaviors.
			final GameMode restore = player.getGameMode();
			if(restore != GameMode.CREATIVE && restore != GameMode.SPECTATOR) {
				player.setGameMode(GameMode.SPECTATOR);
				Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> {
					player.setGameMode(restore);
				}, 1L);
			}
			
		} else {
			updateListName();
		}
		for (Player test : Bukkit.getOnlinePlayers()) {
			updateVanishStateBetween(this, UserLoader.fromPlayer(test));
		}
	}

	public void setVanished(boolean vanished) {
		setData("vanished", vanished);
		updateVanishState();
	}

	public boolean isVanished() {
		return (boolean) getData("vanished");
	}

	/**
	 * God mode makes the player invincible to world or entity events
	 * and allows them to instantly kill non-immortal entities.
	 * 
	 * @param enabled
	 */
	public void setGodMode(boolean enabled) {
		setData("godMode", enabled);
	}

	public boolean isGodMode() {
		return (boolean) getData("godMode");
	}
	
	
	/*
	 * Authentication management
	 */

	public void setSystemProfile(SystemProfile profile) {
		this.profile = profile;
		LOGGER.trace("User " + getName() + " system profile set to " + (profile == null ? "null" : profile.getProfileName()));
	}

	public SystemProfile getSystemProfile() {
		return profile;
	}

	public PermissionLevel getActivePermissionLevel() {
		return activePermissionLevel;
	}

	public boolean setActivePermissionLevel(PermissionLevel permissionLevel) {
		if (permissionLevel.ordinal() > getSystemProfile().getMaxPermissionLevel().ordinal()) {
			LOGGER.warning("ACCESS ERROR: Blocked attempt to raise active permission level of " + getName() + " above profile maximum ("
				+ getSystemProfile().getProfileName() + "=" + getSystemProfile().getMaxPermissionLevel() + "<" + permissionLevel);
			return false;
		}
		
		LOGGER.debug("User " + getName() + " active permission level set to " + permissionLevel);
		activePermissionLevel = permissionLevel;
		SystemProfileFlags flags = getSystemProfile().getFlags();
		
		// Permissions for non-RPG plugins need to be added separately.
		
		// WorldEdit/FAWE
		player.addAttachment(instance, "worldedit.*", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "fawe.*", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "fawe.voxelbrush", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		
		// VoxelSniper/FAVS
		player.addAttachment(instance, "voxelsniper.sniper", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "voxelsniper.ignorelimitations", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "voxelsniper.goto", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "voxelsniper.brush.*", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		
		// Builders-Utilities
		player.addAttachment(instance, "builders.util.trapdoor", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "builders.util.secretblocks", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "builders.util.banner", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "builders.util.color", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "builders.util.tpgm3", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "builders.util.slabs", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "builders.util.terracotta", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "builders.util.nightvision", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "builders.util.noclip", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "builders.util.advancedfly", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "builders.util.gui", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "builders.util.aliases", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		
		// goBrush and goPaint
		player.addAttachment(instance, "gobrush.use", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		player.addAttachment(instance, "gopaint.use", flags.hasFlag(SystemProfileFlag.WORLDEDIT));
		
		// Native Commands
		player.addAttachment(instance, "minecraft.command.teleport", permissionLevel.ordinal() >= PermissionLevel.BUILDER.ordinal() || flags.hasFlag(SystemProfileFlag.CMD));
		player.addAttachment(instance, "minecraft.command.tp", permissionLevel.ordinal() >= PermissionLevel.BUILDER.ordinal() || flags.hasFlag(SystemProfileFlag.CMD));
		player.addAttachment(instance, "minecraft.command.give", flags.hasFlag(SystemProfileFlag.BUILD) || flags.hasFlag(SystemProfileFlag.CMD));
		player.addAttachment(instance, "minecraft.command.summon", flags.hasFlag(SystemProfileFlag.BUILD) || flags.hasFlag(SystemProfileFlag.CMD));
		player.addAttachment(instance, "minecraft.command.setworldspawn", permissionLevel.ordinal() >= PermissionLevel.GM.ordinal());
		
		boolean wasOp = player.isOp();
		player.setOp(flags.hasFlag(SystemProfileFlag.CMD));
		boolean isOp = player.isOp();
		if(isOp && !wasOp) {
			player.sendMessage(ChatColor.GOLD + "Operator status is now ACTIVE.");
		}
		if(!isOp && wasOp) {
			player.sendMessage(ChatColor.GOLD + "Operator status is now INACTIVE.");
		}
		
		sendActionBar(ChatColor.GRAY + "Active permission level changed to " + permissionLevel.toString());
		updateVanishStatesOnSelf();
		return true;
	}
	
	public boolean isVerified() {
		return (boolean) getData("verified");
	}
	
	public void setVerified(boolean verified) {
		setData("verified", verified);
		if(player != null) {
			if(verified) {
				player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "You are now verified!");
				player.sendMessage(ChatColor.GRAY + "The staff have determined that you are a trusted member of the community.");
			}
			else {
				player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Your verification status was revoked.");
			}
		}
	}
	
	
	/*
	 * Skill management
	 */

	public int getSkillLevel(SkillType type) {
		return ((Document) getData("skills")).getInteger(type.toString()).intValue();
	}

	public void setSkillLevel(SkillType type, int level) {
		Document skillLevels = (Document) getData("skills");
		skillLevels.append(type.toString(), level);
		update(new Document("skills", skillLevels));
	}

	public void incrementSkillProgress(SkillType type, double increment) {
		setSkillProgress(type, getSkillProgress(type) + increment);
	}

	public void setSkillProgress(SkillType type, double progress) {
		Document skillProgress = (Document) getData("skillProgress");
		skillProgress.append(type.toString(), progress);
		int currentLevel = getSkillLevel(type);
		int level = calculateSkillLevel(progress);
		if (level != currentLevel) {
			setSkillLevel(type, level);
			player.sendTitle(ChatColor.DARK_GREEN + type.getFriendlyName() + (level > currentLevel ? " Increased!" : " Changed"),
					ChatColor.GREEN + "" + currentLevel + " >>> " + level, 20, 40, 20);
		}
		update(new Document("skillProgress", skillProgress));
	}

	public double getSkillProgress(SkillType type) {
		return ((Document) getData("skillProgress")).getDouble(type.toString());
	}

	/*
	 * State Tokens
	 */
	
	public UUID getState() {
		Document state = new Document("loc", StorageUtil.locToDoc(player.getLocation()))
				.append("health", player.getHealth())
				.append("gamemode", player.getGameMode().toString())
				.append("quests", getData().get("quests", Document.class))
				.append("deathCountdown", getDeathCountdownRemaining())
				.append("speaking", getSpeakingChannel().toString())
				.append("listening", getActiveChatChannels().stream().map(ch -> ch.toString()).collect(Collectors.toList()))
				.append("inventory", getInventoryAsDocument())
				.append("xp", getXP())
				.append("gold", getGold())
				.append("godMode", isGodMode())
				.append("skills", getData().get("skills", Document.class))
				.append("skillProgress", getData().get("skillProgress", Document.class))
				.append("lastRes", getData().getInteger("lastResId"))
				.append("resExitTo", getData().get("resExitTo", Document.class))
				.append("originalUser", getUUID().toString())
				.append("originalTime", StringUtil.dateFormatNow());
		return stateLoader.registerStateToken(state);
	}
	
	public UUID setState(UUID token) {
		UUID backup = getState();
		Document state = stateLoader.getState(token);
		if(state == null || state.isEmpty()) return backup;
		player.teleport(StorageUtil.docToLoc(state.get("loc", Document.class)));
		setXP(state.getInteger("xp"));
		player.setHealth(state.getDouble("health"));
		player.setGameMode(GameMode.valueOf(state.getString("gamemode")));
		loadQuests(null, state.get("quests", Document.class));
		if(state.getInteger("deathCountdown", 0) != 0) {
			setDeathCountdown(state.getInteger("deathCountdown"));
		}
		setSpeakingChannel(ChatChannel.valueOf(state.getString("speaking")));
		List<ChatChannel> ch = new ArrayList<>(getActiveChatChannels());
		for(ChatChannel c : ch) {
			removeActiveChatChannel(c);
		}
		for(String c : state.getList("listening", String.class)) {
			addActiveChatChannel(ChatChannel.valueOf(c));
		}
		clearInventory();
		loadInventory(null, state.get("inventory", Document.class));
		setGold(state.getDouble("gold"), false);
		getData().append("skills", state.get("skills", Document.class));
		getData().append("skillProgress", state.get("skillProgress", Document.class));
		getData().append("lastResId", state.getInteger("lastRes"));
		if(state.getBoolean("resExitTo") != null) {
			getData().append("resExitTo", StorageUtil.docToLoc(state.get("resExitTo", Document.class)));
		}
		Bukkit.getScheduler().runTaskLater(instance, () -> {
			sendActionBar(ChatColor.GRAY + "Your state was updated (" + token + ")");
		}, 5L);
		return backup;
	}
	
	/*
	 * Saving and Syncing
	 */

	public void resyncData() {
		if(player != null) {
			LOGGER.warning("Resyncing data while user is online: " + getName() + " - this may overwrite local changes.");
		}
		storageAccess = storageManager.getStorageAccess(GameObjectType.USER, getUUID());
	}
	
	public void safeResyncData() {
		if(player == null) {
			resyncData();
		}
	}
	
	@Override
	public void autoSave() {
		super.autoSave();
		if (player == null) {
			return;
		}
		sendActionBar(ChatColor.GREEN + "Autosaving...");
		Document autoSaveData = new Document("lastSeen", System.currentTimeMillis())
				.append("maxHealth", player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())
				.append("health", player.getHealth())
				.append("gamemode", joined ? player.getGameMode().toString() : getSavedGameMode().toString())
				.append("inventory", getInventoryAsDocument());
		if (joined) {
			String key = PermissionUtil.verifyActivePermissionLevel(this, PermissionLevel.TESTER, false) ? "lastStaffLocation" : "lastLocation";
			Floor floor = FloorLoader.fromLocation(player.getLocation());
			if(!floor.isVolatile()) {
				autoSaveData.append(key, StorageUtil.locToDoc(player.getLocation()));
			}
		}
		for (ItemStack itemStack : player.getInventory().getContents()) {
			if (itemStack != null) {
				Item item = ItemLoader.fromBukkit(itemStack);
				if (item != null) {
					item.autoSave();
				}
			}
		}
		userHookRegistry.getHooks().forEach(h -> h.onAutoSave(this, autoSaveData));
		update(autoSaveData);
	}
}
