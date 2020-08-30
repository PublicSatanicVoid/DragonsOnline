 package mc.dragons.core.gameobject.user;
 
 import java.time.Instant;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.UUID;
 import java.util.concurrent.CopyOnWriteArrayList;
 import java.util.function.Consumer;
 import java.util.regex.Pattern;
 import java.util.stream.Collectors;
 import mc.dragons.core.Dragons;
 import mc.dragons.core.gameobject.GameObject;
 import mc.dragons.core.gameobject.GameObjectType;
 import mc.dragons.core.gameobject.floor.Floor;
 import mc.dragons.core.gameobject.item.Item;
 import mc.dragons.core.gameobject.item.ItemClass;
 import mc.dragons.core.gameobject.loader.FloorLoader;
 import mc.dragons.core.gameobject.loader.ItemLoader;
 import mc.dragons.core.gameobject.loader.QuestLoader;
 import mc.dragons.core.gameobject.loader.RegionLoader;
 import mc.dragons.core.gameobject.loader.UserLoader;
 import mc.dragons.core.gameobject.quest.Quest;
 import mc.dragons.core.gameobject.quest.QuestStep;
 import mc.dragons.core.gameobject.region.Region;
 import mc.dragons.core.gui.GUI;
 import mc.dragons.core.storage.StorageAccess;
 import mc.dragons.core.storage.StorageManager;
 import mc.dragons.core.storage.StorageUtil;
 import mc.dragons.core.storage.impl.SystemProfile;
 import mc.dragons.core.storage.impl.loader.ChangeLogLoader;
 import mc.dragons.core.storage.impl.loader.SystemProfileLoader;
 import mc.dragons.core.util.MathUtil;
 import mc.dragons.core.util.PermissionUtil;
 import mc.dragons.core.util.StringUtil;
 import net.md_5.bungee.api.chat.BaseComponent;
 import net.md_5.bungee.api.chat.ClickEvent;
 import net.md_5.bungee.api.chat.ComponentBuilder;
 import net.md_5.bungee.api.chat.HoverEvent;
 import net.md_5.bungee.api.chat.TextComponent;
 import org.bson.Document;
 import org.bukkit.Bukkit;
 import org.bukkit.ChatColor;
 import org.bukkit.GameMode;
 import org.bukkit.Location;
 import org.bukkit.attribute.Attribute;
 import org.bukkit.command.CommandSender;
 import org.bukkit.entity.Player;
 import org.bukkit.event.Event;
 import org.bukkit.inventory.Inventory;
 import org.bukkit.inventory.ItemStack;
 import org.bukkit.plugin.Plugin;
 import org.bukkit.potion.PotionEffect;
 import org.bukkit.potion.PotionEffectType;
 import org.bukkit.scheduler.BukkitRunnable;
 
 public class User extends GameObject {
   public static final double MIN_DISTANCE_TO_UPDATE_STATE = 2.0D;
   
   private static Dragons instance;
   
   private static RegionLoader regionLoader;
   
   private static FloorLoader floorLoader;
   
   private static QuestLoader questLoader;
   
   private static ItemLoader itemLoader;
   
   private static UserLoader userLoader;
   
   private static UserHookRegistry userHookRegistry;
   
   private static ChangeLogLoader changeLogLoader;
   
   private static SystemProfileLoader systemProfileLoader;
   
   private Player player;
   
   private Set<Region> cachedRegions;
   
   private Location cachedLocation;
   
   private PermissionLevel activePermissionLevel;
   
   private SystemProfile profile;
   
   private Map<Quest, QuestStep> questProgress;
   
   private Map<Quest, Integer> questActionIndices;
   
   private Map<Quest, QuestPauseState> questPauseStates;
   
   private List<CommandSender> currentlyDebugging;
   
   private List<String> currentDialogueBatch;
   
   private String currentDialogueSpeaker;
   
   private int currentDialogueIndex;
   
   private long whenBeganDialogue;
   
   private List<Consumer<User>> currentDialogueCompletionHandlers;
   
   private boolean isOverridingWalkSpeed;
   
   private CommandSender lastReceivedMessageFrom;
   
   private boolean chatSpy;
   
   private GUI currentGUI;
   
   private List<String> guiHotfixOpenedBefore;
   
   private boolean joined;
   
   public enum PunishmentType {
     BAN("ban", true, SystemProfile.SystemProfileFlags.SystemProfileFlag.MODERATION),
     MUTE("mute", true, SystemProfile.SystemProfileFlags.SystemProfileFlag.MODERATION),
     KICK("kick", false, SystemProfile.SystemProfileFlags.SystemProfileFlag.HELPER),
     WARNING("warn", false, SystemProfile.SystemProfileFlags.SystemProfileFlag.HELPER);
     
     private String dataHeader;
     
     private boolean hasDuration;
     
     private SystemProfile.SystemProfileFlags.SystemProfileFlag requiredFlag;
     
     PunishmentType(String dataHeader, boolean hasDuration, SystemProfile.SystemProfileFlags.SystemProfileFlag requiredFlagToApply) {
       this.dataHeader = dataHeader;
       this.hasDuration = hasDuration;
       this.requiredFlag = requiredFlagToApply;
     }
     
     public String getDataHeader() {
       return this.dataHeader;
     }
     
     public boolean hasDuration() {
       return this.hasDuration;
     }
     
     public SystemProfile.SystemProfileFlags.SystemProfileFlag getRequiredFlagToApply() {
       return this.requiredFlag;
     }
     
     public static PunishmentType fromDataHeader(String header) {
       byte b;
       int i;
       PunishmentType[] arrayOfPunishmentType;
       for (i = (arrayOfPunishmentType = values()).length, b = 0; b < i; ) {
         PunishmentType type = arrayOfPunishmentType[b];
         if (type.getDataHeader().equalsIgnoreCase(header))
           return type; 
         b++;
       } 
       return null;
     }
   }
   
   public class PunishmentData {
     private User.PunishmentType type;
     
     private String reason;
     
     private Date expiry;
     
     public boolean permanent;
     
     public PunishmentData(User.PunishmentType type, String reason, Date expiry, boolean permanent) {
       this.type = type;
       this.reason = reason;
       this.expiry = expiry;
       this.permanent = permanent;
     }
     
     public User.PunishmentType getType() {
       return this.type;
     }
     
     public String getReason() {
       return this.reason;
     }
     
     public Date getExpiry() {
       return this.expiry;
     }
     
     public boolean isPermanent() {
       return this.permanent;
     }
   }
   
   public enum QuestPauseState {
     NORMAL, PAUSED, RESUMED;
   }
   
   public static int calculateLevel(int xp) {
     return (int)Math.floor(0.8D * ((xp / 1000000) + Math.sqrt((xp / 100)))) + 1;
   }
   
   public static int calculateMaxXP(int level) {
     return (int)Math.floor(1250000.0D * Math.pow(Math.sqrt((level + 1999)) - 44.721359549996D, 2.0D));
   }
   
   public static int calculateSkillLevel(double progress) {
     return (int)Math.floor(Math.sqrt(progress / 17.0D));
   }
   
   public static int calculateMaxHealth(int level) {
     return 20 + (level - 1) * 2;
   }
   
   public User(Player player, StorageManager storageManager, StorageAccess storageAccess) {
     super(storageManager, storageAccess);
     LOGGER.fine("Constructing user (" + player + ", " + storageManager + ", " + storageAccess + ")");
     this.currentlyDebugging = new ArrayList<>();
     if (instance == null)
       instance = Dragons.getInstance(); 
     if (regionLoader == null)
       regionLoader = (RegionLoader)GameObjectType.REGION.<Region, RegionLoader>getLoader(); 
     if (floorLoader == null)
       floorLoader = (FloorLoader)GameObjectType.FLOOR.<Floor, FloorLoader>getLoader(); 
     if (questLoader == null)
       questLoader = (QuestLoader)GameObjectType.QUEST.<Quest, QuestLoader>getLoader(); 
     if (itemLoader == null)
       itemLoader = (ItemLoader)GameObjectType.ITEM.<Item, ItemLoader>getLoader(); 
     if (userLoader == null)
       userLoader = (UserLoader)GameObjectType.USER.<User, UserLoader>getLoader(); 
     if (userHookRegistry == null)
       userHookRegistry = instance.getUserHookRegistry(); 
     if (changeLogLoader == null)
       changeLogLoader = (ChangeLogLoader)instance.getLightweightLoaderRegistry().getLoader(ChangeLogLoader.class); 
     if (systemProfileLoader == null)
       systemProfileLoader = (SystemProfileLoader)instance.getLightweightLoaderRegistry().getLoader(SystemProfileLoader.class); 
     this.joined = false;
     initialize(player);
   }
   
   public User initialize(Player player) {
     LOGGER.fine("Initializing user " + this + " on player " + player);
     this.player = player;
     if (player != null) {
       setData("lastLocation", StorageUtil.locToDoc(player.getLocation()));
       setData("health", Double.valueOf(player.getHealth()));
       player.getInventory().clear();
       player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(calculateMaxHealth(getLevel()));
       if (getData("health") != null)
         player.setHealth(((Double)getData("health")).doubleValue()); 
       Document inventory = (Document)getData("inventory");
       List<String> brokenItems = new ArrayList<>();
       for (Map.Entry<String, Object> entry : (Iterable<Map.Entry<String, Object>>)inventory.entrySet()) {
         String[] labels = ((String)entry.getKey()).split(Pattern.quote("-"));
         String part = labels[0];
         int slot = Integer.valueOf(labels[1]).intValue();
         Item item = itemLoader.loadObject((UUID)entry.getValue());
         if (item == null) {
           brokenItems.add(entry.getValue().toString());
           continue;
         } 
         ItemStack itemStack = item.getItemStack();
         if (part.equals("I")) {
           player.getInventory().setItem(slot, itemStack);
           continue;
         } 
         if (part.equals("Helmet")) {
           player.getInventory().setHelmet(itemStack);
           continue;
         } 
         if (part.equals("Chestplate")) {
           player.getInventory().setChestplate(itemStack);
           continue;
         } 
         if (part.equals("Leggings")) {
           player.getInventory().setLeggings(itemStack);
           continue;
         } 
         if (part.equals("Boots"))
           player.getInventory().setBoots(itemStack); 
       } 
       if (brokenItems.size() > 0) {
         player.sendMessage(ChatColor.RED + "" + brokenItems.size() + " items in your saved inventory could not be loaded:");
         brokenItems.forEach(uuid -> player.sendMessage(ChatColor.RED + " - " + uuid));
       } 
     } 
     this.questProgress = new HashMap<>();
     this.questActionIndices = new HashMap<>();
     this.questPauseStates = new HashMap<>();
     Document questProgressDoc = (Document)getData("quests");
     for (Map.Entry<String, Object> entry : (Iterable<Map.Entry<String, Object>>)questProgressDoc.entrySet()) {
       Quest quest = questLoader.getQuestByName(entry.getKey());
       if (quest == null)
         continue; 
       this.questProgress.put(quest, quest.getSteps().get(((Integer)entry.getValue()).intValue()));
       this.questActionIndices.put(quest, Integer.valueOf(0));
       this.questPauseStates.put(quest, QuestPauseState.NORMAL);
     } 
     this.cachedRegions = new HashSet<>();
     this.activePermissionLevel = PermissionLevel.USER;
     this.guiHotfixOpenedBefore = new ArrayList<>();
     userHookRegistry.getHooks().forEach(h -> h.onInitialize(this));
     instance.getSidebarManager().createScoreboard(player);
     LOGGER.fine("Finished initializing user " + this);
     return this;
   }
   
   public void addDebugTarget(CommandSender debugger) {
     this.currentlyDebugging.add(debugger);
   }
   
   public void removeDebugTarget(CommandSender debugger) {
     this.currentlyDebugging.remove(this.currentlyDebugging.indexOf(debugger));
   }
   
   public void debug(String message) {
     for (CommandSender debugger : this.currentlyDebugging)
       debugger.sendMessage("[DEBUG:" + getName() + "] " + message); 
   }
   
   public void updateState() {
     updateState(true, true);
   }
   
   public void updateState(boolean applyQuestTriggers, boolean notify) {
     LOGGER.finest("Update user state: " + getName() + " (applyQuestTriggers=" + applyQuestTriggers + ", notify=" + notify + ")");
     String worldName = this.player.getWorld().getName();
     boolean privilegedWorld = (!worldName.equals("staff_verification") && !worldName.equals("trials") && !worldName.equalsIgnoreCase("trial-" + this.player.getName()));
     if (PermissionUtil.verifyActiveProfileFlag(this, SystemProfile.SystemProfileFlags.SystemProfileFlag.TRIAL_BUILD_ONLY, false) && privilegedWorld) {
       this.player.sendMessage(ChatColor.RED + "Trial builders can only access the trial world!");
       if (this.cachedLocation.getWorld().getName().equals("trials") || this.cachedLocation.getWorld().getName().equalsIgnoreCase("trial-" + this.player.getName())) {
         this.player.teleport(this.cachedLocation);
       } else if (Bukkit.getWorld("trial-" + this.player.getName()) != null) {
         this.player.teleport(Bukkit.getWorld("trial-" + this.player.getName()).getSpawnLocation());
       } else {
         this.player.teleport(Bukkit.getWorld("trials").getSpawnLocation());
       } 
     } 
     Set<Region> regions = regionLoader.getRegionsByLocation(this.player.getLocation());
     if (this.cachedLocation != null && 
       this.cachedLocation.getWorld() != this.player.getLocation().getWorld()) {
       Floor floor = FloorLoader.fromWorldName(this.player.getLocation().getWorld().getName());
       this.cachedLocation = this.player.getLocation();
       this.cachedRegions = regions;
       if (notify)
         if (floor == null) {
           sendActionBar(ChatColor.DARK_RED + "- Unofficial World -");
           this.player.sendMessage(ChatColor.RED + "WARNING: This is an unofficial world and is not associated with a floor.");
         } else {
           this.player.sendMessage(ChatColor.GRAY + "Floor " + floor.getLevelMin() + ": " + floor.getDisplayName());
           this.player.sendTitle(ChatColor.DARK_GRAY + "Floor " + floor.getLevelMin(), ChatColor.GRAY + floor.getDisplayName(), 20, 40, 20);
         }  
       return;
     } 
     for (Region region : this.cachedRegions) {
       if (regions.contains(region) || 
         Boolean.valueOf(region.getFlags().getString("hidden")).booleanValue())
         continue; 
       if (notify)
         this.player.sendMessage(ChatColor.GRAY + "Leaving " + region.getFlags().getString("fullname")); 
     } 
     for (Region region : regions) {
       if (!this.cachedRegions.contains(region)) {
         int lvMin = Integer.parseInt(region.getFlags().getString("lvmin"));
         if (getLevel() < lvMin) {
           this.player.setVelocity(this.cachedLocation.toVector().subtract(this.player.getLocation().toVector()).multiply(2.0D));
           if (notify)
             this.player.sendMessage(ChatColor.RED + "This region requires level " + lvMin + " to enter"); 
         } 
         if (Boolean.valueOf(region.getFlags().getString("hidden")).booleanValue())
           continue; 
         if (notify) {
           if (Boolean.parseBoolean(region.getFlags().getString("showtitle")))
             this.player.sendTitle("", ChatColor.GRAY + "Entering " + region.getFlags().getString("fullname"), 20, 40, 20); 
           this.player.sendMessage(ChatColor.GRAY + "Entering " + region.getFlags().getString("fullname"));
           if (!region.getFlags().getString("desc").equals(""))
             this.player.sendMessage(ChatColor.DARK_GRAY + "   " + ChatColor.ITALIC + region.getFlags().getString("desc")); 
         } 
         int lvRec = Integer.parseInt(region.getFlags().getString("lvrec"));
         if (getLevel() < lvRec && notify)
           this.player.sendMessage(ChatColor.YELLOW + "Caution: The recommended level for this region is " + lvRec); 
       } 
     } 
     if (applyQuestTriggers)
       updateQuests((Event)null); 
     userHookRegistry.getHooks().forEach(h -> h.onUpdateState(this, this.cachedLocation));
     this.cachedLocation = this.player.getLocation();
     this.cachedRegions = regions;
     updateEffectiveWalkSpeed();
   }
   
   public void setDialogueBatch(Quest quest, String speaker, List<String> dialogue) {
     this.currentDialogueSpeaker = speaker;
     this.currentDialogueBatch = dialogue;
     this.currentDialogueIndex = 0;
     this.whenBeganDialogue = System.currentTimeMillis();
     this.currentDialogueCompletionHandlers = new CopyOnWriteArrayList<>();
   }
   
   public boolean hasActiveDialogue() {
     return (this.currentDialogueBatch != null);
   }
   
   public long getWhenBeganDialogue() {
     return this.whenBeganDialogue;
   }
   
   public void onDialogueComplete(Consumer<User> handler) {
     if (!hasActiveDialogue())
       return; 
     this.currentDialogueCompletionHandlers.add(handler);
   }
   
   public void resetDialogueAndHandleCompletion() {
     if (this.currentDialogueBatch == null)
       return; 
     if (this.currentDialogueIndex >= this.currentDialogueBatch.size()) {
       debug("Handling dialogue completion...");
       this.currentDialogueSpeaker = null;
       this.currentDialogueBatch = null;
       this.currentDialogueIndex = 0;
       for (Consumer<User> handler : this.currentDialogueCompletionHandlers)
         handler.accept(this); 
     } 
   }
   
   public void fastForwardDialogue() {
     while (hasActiveDialogue())
       nextDialogue(); 
   }
   
   public boolean nextDialogue() {
     if (!hasActiveDialogue())
       return false; 
     debug("nextDialogue");
     debug(" - idx=" + this.currentDialogueIndex);
     TextComponent message = new TextComponent(TextComponent.fromLegacyText(
           ChatColor.GRAY + "[" + (this.currentDialogueIndex + 1) + "/" + this.currentDialogueBatch.size() + "] " + 
           ChatColor.DARK_GREEN + this.currentDialogueSpeaker + ": " + 
           ChatColor.GREEN + ((String)this.currentDialogueBatch.get(this.currentDialogueIndex++)).replaceAll(Pattern.quote("%PLAYER%"), getName())));
     message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/fastforwarddialogue"));
     message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder(ChatColor.YELLOW + "Click to fast-forward through the dialogue")).create()));
     this.player.spigot().sendMessage((BaseComponent)message);
     if (this.currentDialogueIndex >= this.currentDialogueBatch.size()) {
       resetDialogueAndHandleCompletion();
       return false;
     } 
     return true;
   }
   
   public void setQuestPaused(Quest quest, boolean paused) {
     this.questPauseStates.put(quest, paused ? QuestPauseState.PAUSED : QuestPauseState.RESUMED);
     debug(String.valueOf(paused ? "Paused" : "Unpaused") + " quest " + quest.getName());
   }
   
   public void resetQuestPauseState(Quest quest) {
     this.questPauseStates.put(quest, QuestPauseState.NORMAL);
     debug("Reset pause state for quest " + quest.getName());
   }
   
   public QuestPauseState getQuestPauseState(Quest quest) {
     return this.questPauseStates.getOrDefault(quest, QuestPauseState.NORMAL);
   }
   
   public void updateQuests(Event event) {
     debug("Updating quests...");
     if (this.currentDialogueBatch != null && 
       this.currentDialogueIndex < this.currentDialogueBatch.size()) {
       debug("- Cancelled quest update because of active dialogue");
       return;
     } 
     for (Map.Entry<Quest, QuestStep> questStep : this.questProgress.entrySet()) {
       debug("- Step " + ((QuestStep)questStep.getValue()).getStepName() + " of " + ((Quest)questStep.getKey()).getName());
       if (((QuestStep)questStep.getValue()).getStepName().equalsIgnoreCase("Complete"))
         continue; 
       QuestPauseState pauseState = getQuestPauseState(questStep.getKey());
       if (pauseState == QuestPauseState.PAUSED)
         continue; 
       debug("  - Trigger: " + ((QuestStep)questStep.getValue()).getTrigger().getTriggerType());
       if (((QuestStep)questStep.getValue()).getTrigger().test(this, event) || pauseState == QuestPauseState.RESUMED) {
         Quest quest = questStep.getKey();
         debug("   - Triggered (starting @ action #" + getQuestActionIndex(quest) + ")");
         if (((QuestStep)questStep.getValue()).executeActions(this, getQuestActionIndex(quest))) {
           debug("      - Normal progression to next step");
           int nextIndex = quest.getSteps().indexOf(questStep.getValue()) + 1;
           if (nextIndex != quest.getSteps().size()) {
             QuestStep nextStep = quest.getSteps().get(nextIndex);
             updateQuestProgress(quest, nextStep, true);
           } 
         } 
       } 
     } 
   }
   
   public Map<Quest, QuestStep> getQuestProgress() {
     return this.questProgress;
   }
   
   public void updateQuestProgress(Quest quest, QuestStep questStep, boolean notify) {
     Document updatedQuestProgress = (Document)getData("quests");
     if (questStep == null) {
       this.questProgress.remove(quest);
       updatedQuestProgress.remove(quest.getName());
       this.storageAccess.update(new Document("quests", updatedQuestProgress));
       return;
     } 
     debug("==UPDATING QUEST PROGRESS: " + quest.getName() + " step " + questStep.getStepName());
     this.questProgress.put(quest, questStep);
     resetQuestPauseState(quest);
     this.questActionIndices.put(quest, Integer.valueOf(0));
     updatedQuestProgress.append(quest.getName(), Integer.valueOf(quest.getSteps().indexOf(questStep)));
     this.storageAccess.update(new Document("quests", updatedQuestProgress));
     if (notify)
       if (questStep.getStepName().equals("Complete")) {
         this.player.sendMessage(ChatColor.GRAY + "Completed quest " + quest.getQuestName());
       } else {
         this.player.sendMessage(ChatColor.GRAY + "New Objective: " + questStep.getStepName());
       }  
     (new BukkitRunnable() {
         public void run() {
           User.this.updateQuests((Event)null);
         }
       }).runTaskLater((Plugin)instance, 1L);
   }
   
   public void updateQuestAction(Quest quest, int actionIndex) {
     this.questActionIndices.put(quest, Integer.valueOf(actionIndex));
   }
   
   public int getQuestActionIndex(Quest quest) {
     return ((Integer)this.questActionIndices.getOrDefault(quest, Integer.valueOf(0))).intValue();
   }
   
   public void updateQuestProgress(Quest quest, QuestStep questStep) {
     updateQuestProgress(quest, questStep, true);
   }
   
   public void openGUI(GUI gui, Inventory inventory) {
     this.player.closeInventory();
     debug("opening gui " + gui.getMenuName());
     this.player.openInventory(inventory);
     this.currentGUI = gui;
   }
   
   public void closeGUI(boolean forceClose) {
     debug("closing gui");
     if (this.currentGUI == null)
       return; 
     this.currentGUI = null;
     if (forceClose)
       this.player.closeInventory(); 
   }
   
   public boolean hasHotfixedGUI(GUI gui) {
     return this.guiHotfixOpenedBefore.contains(gui.getMenuName());
   }
   
   public void hotfixGUI() {
     if (this.currentGUI == null)
       return; 
     this.guiHotfixOpenedBefore.add(this.currentGUI.getMenuName());
     (new BukkitRunnable() {
         public void run() {
           User.this.currentGUI.open(User.this);
         }
       }).runTaskLater((Plugin)instance, 1L);
   }
   
   public boolean hasOpenGUI() {
     return (this.currentGUI != null);
   }
   
   public GUI getCurrentGUI() {
     return this.currentGUI;
   }
   
   @SuppressWarnings("unchecked")
   public List<ChatChannel> getActiveChatChannels() {
     return (List<ChatChannel>)((List<String>)getData("chatChannels")).stream().map(ch -> ChatChannel.valueOf(ch)).collect(Collectors.toList());
   }
   
   @SuppressWarnings("unchecked")
   public void addActiveChatChannel(ChatChannel channel) {
     List<String> channels = (List<String>)getData("chatChannels");
     channels.add(channel.toString());
     setData("chatChannels", channels);
   }
   
   @SuppressWarnings("unchecked")
   public void removeActiveChatChannel(ChatChannel channel) {
     List<String> channels = (List<String>)getData("chatChannels");
     channels.remove(channel.toString());
     setData("chatChannels", channels);
   }
   
   public ChatChannel getSpeakingChannel() {
     return ChatChannel.valueOf((String)getData("speakingChannel"));
   }
   
   public void setSpeakingChannel(ChatChannel channel) {
     setData("speakingChannel", channel.toString());
   }
   
   public void sendMessage(ChatChannel channel, String message) {
     sendMessage(channel, TextComponent.fromLegacyText(message));
   }
   
   public void sendMessage(ChatChannel channel, Location source, String message) {
     sendMessage(channel, source, TextComponent.fromLegacyText(message));
   }
   
   public void sendMessage(ChatChannel channel, Location source, BaseComponent... message) {
     if (channel == ChatChannel.LOCAL && !hasChatSpy() && !FloorLoader.fromWorld(source.getWorld()).equals(FloorLoader.fromWorld(this.player.getWorld())))
       return; 
     sendMessage(channel, message);
   }
   
   public void sendMessage(ChatChannel channel, BaseComponent... message) {
     if (getActiveChatChannels().contains(channel))
       this.player.spigot().sendMessage((new ComponentBuilder((BaseComponent)channel.getPrefix())).append(" ").append(message).create()); 
   }
   
   public void chat(String message) {
     LOGGER.finer("Chat message from " + getName());
     if (!this.joined) {
       this.player.sendMessage(ChatColor.RED + "You are not joined yet!");
       return;
     } 
     if (hasActiveDialogue()) {
       this.player.sendMessage(ChatColor.RED + "Chat is unavailable while in active dialogue!");
       return;
     } 
     PunishmentData muteData = getActivePunishmentData(PunishmentType.MUTE);
     if (muteData != null) {
       this.player.sendMessage(ChatColor.RED + "You are muted!" + (muteData.getReason().equals("") ? "" : (" (" + muteData.getReason() + ")")));
       this.player.sendMessage(ChatColor.RED + "Expires " + muteData.getExpiry().toString());
       return;
     } 
     LOGGER.finer("-Creating message text component");
     String messageSenderInfo = "";
     if (getRank().hasChatPrefix())
       messageSenderInfo = String.valueOf(messageSenderInfo) + getRank().getChatPrefix() + " "; 
     messageSenderInfo = String.valueOf(messageSenderInfo) + getRank().getNameColor() + getName();
     TextComponent messageComponent = new TextComponent(messageSenderInfo);
     messageComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder(ChatColor.YELLOW + "" + ChatColor.BOLD + getName() + "\n"))
           .append(ChatColor.GRAY + "Rank: " + ChatColor.RESET + getRank().getNameColor() + getRank().getRankName() + "\n")
           .append(ChatColor.GRAY + "Level: " + getLevelColor() + getLevel() + "\n")
           .append(ChatColor.GRAY + "XP: " + ChatColor.RESET + getXP() + "\n")
           .append(ChatColor.GRAY + "Gold: " + ChatColor.RESET + getGold() + "\n")
           .append(ChatColor.GRAY + "Location: " + ChatColor.RESET + StringUtil.locToString(this.player.getLocation()) + 
             ChatColor.DARK_GRAY + ChatColor.ITALIC + " (when message sent)\n")
           .append(ChatColor.GRAY + "Floor: " + ChatColor.RESET + FloorLoader.fromWorld(this.player.getWorld()).getDisplayName() + 
             ChatColor.DARK_GRAY + ChatColor.ITALIC + " (when message sent)\n")
           .append(ChatColor.GRAY + "First Joined: " + ChatColor.RESET + getFirstJoined().toString())
           .create()));
     messageComponent.addExtra(ChatColor.GRAY + " » " + getRank().getChatColor() + message);
     ChatChannel channel = getSpeakingChannel();
     if (!channel.canHear(this, this))
       this.player.sendMessage(ChatColor.RED + "It looks like you can't hear yourself! Make sure you are listening to the channel you're speaking on. (/c l " + channel.getAbbreviation().toLowerCase() + ")"); 
     Location location = this.player.getLocation();
     int rec = 0;
     int tot = 0;
     for (User user : UserLoader.allUsers()) {
       tot++;
       LOGGER.finer("-Checking if " + user.getName() + " can receive");
       if (!channel.canHear(user, this) && !user.hasChatSpy())
         continue; 
       LOGGER.finer("  -Yes!");
       user.sendMessage(channel, location, new BaseComponent[] { (BaseComponent)messageComponent });
       rec++;
     } 
     if (rec <= 1 && tot > 1)
       this.player.sendMessage(ChatColor.RED + "There's currently nobody else online in that channel!"); 
     LOGGER.info("[" + channel.getAbbreviation() + "/" + this.player.getWorld().getName() + "] [" + getName() + "] " + message);
   }
   
   public CommandSender getLastReceivedMessageFrom() {
     return this.lastReceivedMessageFrom;
   }
   
   public void setLastReceivedMessageFrom(CommandSender lastReceivedMessageFrom) {
     this.lastReceivedMessageFrom = lastReceivedMessageFrom;
   }
   
   public void setChatSpy(boolean enabled) {
     this.chatSpy = enabled;
   }
   
   public boolean hasChatSpy() {
     return this.chatSpy;
   }
   
   public void giveItem(Item item, boolean updateDB, boolean dbOnly, boolean silent) {
     int giveQuantity = item.getQuantity();
     int maxStackSize = item.getMaxStackSize();
     if (!dbOnly) {
       int remaining = giveQuantity;
       for (int i = 0; i < (this.player.getInventory().getContents()).length; i++) {
         ItemStack itemStack = this.player.getInventory().getContents()[i];
         if (itemStack != null) {
           Item testItem = ItemLoader.fromBukkit(itemStack);
           if (testItem != null && 
             item.getClassName().equals(testItem.getClassName()) && !item.isCustom() && !testItem.isCustom()) {
             int quantity = Math.min(maxStackSize, testItem.getQuantity() + remaining);
             int added = quantity - testItem.getQuantity();
             debug("Adding to existing stack: " + testItem.getUUID().toString() + " (curr=" + testItem.getQuantity() + ", add=" + added + ", tot=" + quantity + ")");
             remaining -= added;
             testItem.setQuantity(quantity);
             this.player.getInventory().setItem(i, testItem.getItemStack());
             item.setQuantity(item.getQuantity() - added);
             if (remaining == 0)
               break; 
             debug(" - " + remaining + " remaining to dispense");
           } 
         } 
       } 
       if (remaining > 0) {
         debug("Adding remaining items as new item stack");
         this.player.getInventory().addItem(new ItemStack[] { item.getItemStack() });
       } 
     } 
     if (updateDB)
       this.storageAccess.update(new Document("inventory", getInventoryAsDocument())); 
     if (!silent)
       this.player.sendMessage(ChatColor.GRAY + "+ " + item.getDecoratedName() + ((item.getQuantity() > 1) ? (ChatColor.GRAY + " (x" + giveQuantity + ")") : "")); 
   }
   
   public void giveItem(Item item) {
     giveItem(item, true, false, false);
   }
   
   public void takeItem(Item item, int amount, boolean updateDB, boolean updateInventory, boolean notify) {
     debug("Removing " + amount + " of " + item.getName() + " (has " + item.getQuantity() + ")");
     if (amount < item.getQuantity()) {
       debug("-New quantity: " + item.getQuantity());
       item.setQuantity(item.getQuantity() - amount);
     } 
     if (updateInventory) {
       ItemStack removal = item.getItemStack().clone();
       removal.setAmount(amount);
       this.player.getInventory().removeItem(new ItemStack[] { removal });
     } 
     if (updateDB)
       this.storageAccess.update(new Document("inventory", getInventoryAsDocument())); 
     if (notify)
       this.player.sendMessage(ChatColor.RED + "- " + item.getDecoratedName() + ((amount > 1) ? (ChatColor.GRAY + " (x" + amount + ")") : "")); 
   }
   
   public void takeItem(Item item) {
     takeItem(item, 1, true, true, true);
   }
   
   public void buyItem(ItemClass itemClass, int quantity, double costPer) {
     debug("Attempting to buy " + quantity + " of " + itemClass.getClassName() + " at " + costPer + "g ea");
     double price = costPer * quantity;
     double balance = getGold();
     if (balance < price) {
       this.player.sendMessage(ChatColor.RED + "Cannot buy this item! Costs " + price + "g, you have " + balance + " (need " + (price - balance) + "g more)");
       return;
     } 
     takeGold(price, false);
     Item item = itemLoader.registerNew(itemClass);
     item.setQuantity(quantity);
     giveItem(item, true, false, true);
     this.player.sendMessage(ChatColor.GREEN + "Purchased " + item.getDecoratedName() + ((quantity > 1) ? (ChatColor.GRAY + " (x" + quantity + ")") : "") + ChatColor.GREEN + " for " + ChatColor.GOLD + price + "g");
   }
   
   public Document getInventoryAsDocument() {
     Document inventory = new Document();
     for (int i = 0; i < (this.player.getInventory().getContents()).length; i++) {
       ItemStack is = this.player.getInventory().getContents()[i];
       if (is != null) {
         Item item = ItemLoader.fromBukkit(is);
         if (item != null)
           inventory.append("I-" + i, item.getUUID()); 
       } 
     } 
     ItemStack helmetStack = this.player.getInventory().getHelmet();
     Item helmet = ItemLoader.fromBukkit(helmetStack);
     if (helmet != null)
       inventory.append("Helmet-0", helmet.getUUID()); 
     ItemStack chestplateStack = this.player.getInventory().getChestplate();
     Item chestplate = ItemLoader.fromBukkit(chestplateStack);
     if (chestplate != null)
       inventory.append("Chestplate-0", chestplate.getUUID()); 
     ItemStack leggingsStack = this.player.getInventory().getLeggings();
     Item leggings = ItemLoader.fromBukkit(leggingsStack);
     if (leggings != null)
       inventory.append("Leggings-0", leggings.getUUID()); 
     ItemStack bootsStack = this.player.getInventory().getBoots();
     Item boots = ItemLoader.fromBukkit(bootsStack);
     if (boots != null)
       inventory.append("Boots-0", boots.getUUID()); 
     return inventory;
   }
   
   public void handleJoin(boolean firstJoin) {
     this.joined = true;
     setData("lastJoined", Long.valueOf(System.currentTimeMillis()));
     if (PermissionUtil.verifyActivePermissionLevel(this, PermissionLevel.TESTER, false)) {
       this.player.setGameMode(getSavedGameMode());
     } else {
       this.player.setGameMode(GameMode.ADVENTURE);
     } 
     if (isVanished()) {
       this.player.sendMessage(ChatColor.DARK_GREEN + "You are currently vanished.");
     } else if (getRank().ordinal() >= Rank.PATRON.ordinal()) {
       Bukkit.broadcastMessage(getRank().getNameColor() + "" + ChatColor.BOLD + getRank().getRankName() + " " + this.player.getName() + " joined!");
     } else {
       Bukkit.broadcastMessage(ChatColor.GRAY + this.player.getName() + " joined!");
     } 
     this.player.sendMessage(ChatColor.GOLD + "Hello " + getName() + " and welcome to DragonsOnline.");
     String spacer = ChatColor.GRAY + "    -    ";
     this.player.sendMessage(ChatColor.LIGHT_PURPLE + "Level: " + getLevel() + spacer + ChatColor.GREEN + "XP: " + getXP() + " (" + MathUtil.round((getLevelProgress() * 100.0F)) + "%)" + 
         spacer + ChatColor.YELLOW + "Gold: " + getGold());
     TextComponent component = new TextComponent(ChatColor.AQUA + "Speaking in ");
     TextComponent speaking = getSpeakingChannel().format();
     speaking.setColor(net.md_5.bungee.api.ChatColor.DARK_AQUA);
     component.addExtra((BaseComponent)speaking);
     component.addExtra(ChatColor.AQUA + " and listening to ");
     List<ChatChannel> channels = getActiveChatChannels();
     for (int i = 0; i < channels.size(); i++) {
       TextComponent listening = ((ChatChannel)channels.get(i)).format();
       listening.setColor(net.md_5.bungee.api.ChatColor.DARK_AQUA);
       component.addExtra((BaseComponent)listening);
       if (i < channels.size() - 1)
         component.addExtra(", "); 
     } 
     this.player.spigot().sendMessage((BaseComponent)component);
     if (firstJoin)
       this.player.sendMessage(ChatColor.AQUA + "Use " + ChatColor.DARK_AQUA + "/channel" + ChatColor.AQUA + " to change channels."); 
     if (getUnreadChangeLogs().size() > 0)
       this.player.sendMessage(ChatColor.DARK_GREEN + "You have unread changelogs! Do " + ChatColor.GREEN + "/whatsnew" + ChatColor.DARK_GREEN + " to read them!"); 
     userHookRegistry.getHooks().forEach(h -> h.onVerifiedJoin(this));
     this.player.sendMessage("");
     updateState();
     updateVanishState();
     updateVanishStatesOnSelf();
     updateVanillaLeveling();
     setData("ip", this.player.getAddress().getAddress().getHostAddress());
     LOGGER.exiting("User", "handleJoin");
   }
   
   public void handleQuit() {
     autoSave();
     setData("totalOnlineTime", Long.valueOf(getTotalOnlineTime() + getLocalOnlineTime()));
     if (!isVanished() && this.joined)
       Bukkit.broadcastMessage(ChatColor.GRAY + this.player.getName() + " left!"); 
     if (this.profile != null && instance.isEnabled()) {
       systemProfileLoader.logoutProfile(this.profile.getProfileName());
       setActivePermissionLevel(PermissionLevel.USER);
       setSystemProfile((SystemProfile)null);
     } 
     this.player.getInventory().clear();
     this.player.getInventory().setArmorContents(new ItemStack[4]);
     userHookRegistry.getHooks().forEach(h -> h.onQuit(this));
     userLoader.removeStalePlayer(this.player);
   }
   
   public long getTotalOnlineTime() {
     return ((Long)getData("totalOnlineTime")).longValue();
   }
   
   public long getLocalOnlineTime() {
     return (long)Math.floor(((System.currentTimeMillis() - ((Long)getData("lastJoined")).longValue()) / 1000L));
   }
   
   public void handleMove() {
     boolean update = false;
     if (this.cachedLocation == null) {
       this.cachedLocation = this.player.getLocation();
     } else if (this.player.getLocation().getWorld() != this.cachedLocation.getWorld()) {
       update = true;
     } else if (this.player.getLocation().distanceSquared(this.cachedLocation) >= 4.0D) {
       update = true;
     } 
     if (update)
       updateState(); 
   }
   
   public Player getPlayer() {
     return this.player;
   }
   
   public void setPlayer(Player player) {
     this.player = player;
   }
   
   public String getName() {
     return (String)getData("username");
   }
   
   public Location getSavedLocation() {
     return StorageUtil.docToLoc((Document)getData("lastLocation"));
   }
   
   public Location getSavedStaffLocation() {
     if (getData("lastStaffLocation") == null)
       setData("lastStaffLocation", getData("lastLocation")); 
     return StorageUtil.docToLoc((Document)getData("lastStaffLocation"));
   }
   
   public double getSavedHealth() {
     return ((Double)getData("health")).doubleValue();
   }
   
   public double getSavedMaxHealth() {
     return ((Double)getData("maxHealth")).doubleValue();
   }
   
   public double getGold() {
     return ((Double)getData("gold")).doubleValue();
   }
   
   public void setGold(double gold, boolean notify) {
     setData("gold", Double.valueOf(gold));
     if (notify)
       this.player.sendMessage(ChatColor.GRAY + "Your gold balance is now " + ChatColor.GOLD + gold); 
   }
   
   public void setGold(double gold) {
     setGold(gold, true);
   }
   
   public void giveGold(double gold, boolean notify) {
     setData("gold", Double.valueOf(getGold() + gold));
     if (notify)
       this.player.sendMessage(ChatColor.GRAY + "+ " + ChatColor.GOLD + gold + " Gold"); 
   }
   
   public void giveGold(double gold) {
     giveGold(gold, true);
   }
   
   public void takeGold(double gold, boolean notify) {
     setData("gold", Double.valueOf(getGold() - gold));
     if (notify)
       this.player.sendMessage(ChatColor.RED + "- " + ChatColor.GOLD + gold + " Gold"); 
   }
   
   public void takeGold(double gold) {
     takeGold(gold, true);
   }
   
   public void sendActionBar(String message) {
     instance.getBridge().sendActionBar(this.player, message);
   }
   
   @Deprecated
   public void sendTitle(ChatColor titleColor, String title, ChatColor subtitleColor, String subtitle) {
     sendTitle(titleColor, title, subtitleColor, subtitle, 20, 40, 20);
   }
   
   @Deprecated
   public void sendTitle(ChatColor titleColor, String title, ChatColor subtitleColor, String subtitle, int fadeInTime, int showTime, int fadeOutTime) {
     instance.getBridge().sendTitle(this.player, titleColor, title, subtitleColor, subtitle, fadeInTime, showTime, fadeOutTime);
   }
   
   public void overrideWalkSpeed(float speed) {
     this.player.setWalkSpeed(speed);
     this.isOverridingWalkSpeed = true;
   }
   
   public void removeWalkSpeedOverride() {
     this.isOverridingWalkSpeed = false;
     this.player.setWalkSpeed((float)getEffectiveWalkSpeed());
   }
   
   public double getEffectiveWalkSpeed() {
     if (this.isOverridingWalkSpeed)
       return this.player.getWalkSpeed(); 
     double speed = instance.getServerOptions().getDefaultWalkSpeed();
     byte b;
     int i;
     ItemStack[] arrayOfItemStack;
     for (i = (arrayOfItemStack = this.player.getInventory().getArmorContents()).length, b = 0; b < i; ) {
       ItemStack itemStack = arrayOfItemStack[b];
       if (itemStack != null) {
         Item item1 = ItemLoader.fromBukkit(itemStack);
         if (item1 != null)
           speed += item1.getSpeedBoost(); 
       } 
       b++;
     } 
     ItemStack held = this.player.getInventory().getItemInMainHand();
     Item item = ItemLoader.fromBukkit(held);
     if (item != null)
       speed += item.getSpeedBoost(); 
     return Math.min(1.0D, Math.max(0.05D, speed));
   }
   
   public void updateEffectiveWalkSpeed() {
     this.player.setWalkSpeed((float)getEffectiveWalkSpeed());
   }
   
   public void clearInventory() {
     this.player.getInventory().clear();
     setData("inventory", new ArrayList<>());
     sendActionBar(ChatColor.DARK_RED + "- All items have been lost! -");
   }
   
   public void setDeathCountdown(int seconds) {
     setData("deathCountdown", Integer.valueOf(seconds));
     setData("deathTime", Long.valueOf(System.currentTimeMillis()));
     this.player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * seconds, 10, false, false), true);
     this.player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * seconds, 10, false, false), true);
     this.player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * seconds, 10, false, false), true);
     this.player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * seconds, 0, false, false), true);
     (new BukkitRunnable() {
         int counter = seconds;
         
         public void run() {
           if (User.this.hasDeathCountdown()) {
             User.this.sendActionBar(ChatColor.DARK_RED + "Respawning in " + this.counter + "s");
             this.counter--;
           } else {
             User.this.sendActionBar(ChatColor.YELLOW + "Respawning...");
             cancel();
           } 
         }
       }).runTaskTimer((Plugin)instance, 0L, 20L);
   }
   
   public boolean hasDeathCountdown() {
     Long deathTime = (Long)getData("deathTime");
     if (deathTime == null)
       return false; 
     int deathCountdown = ((Integer)getData("deathCountdown")).intValue();
     long now = System.currentTimeMillis();
     return (deathTime.longValue() + (1000 * deathCountdown) > now);
   }
   
   public void respawn() {
     instance.getBridge().respawnPlayer(this.player);
   }
   
   public void sendToFloor(String floorName, boolean overrideLevelRequirement) {
     Floor floor = FloorLoader.fromFloorName(floorName);
     if (!overrideLevelRequirement && getLevel() < floor.getLevelMin())
       return; 
     this.player.teleport(floor.getWorld().getSpawnLocation());
   }
   
   public void sendToFloor(String floorName) {
     sendToFloor(floorName, false);
   }
   
   public void addXP(int xp) {
     setXP(getXP() + xp);
   }
   
   public void setXP(int xp) {
     int level = calculateLevel(xp);
     if (level > getLevel()) {
       this.player.sendTitle(ChatColor.DARK_AQUA + "Level Up!", ChatColor.AQUA + "" + getLevel() + " >>> " + level, 20, 40, 20);
       Bukkit.broadcastMessage(ChatColor.AQUA + getName() + " is now level " + level + "!");
     } 
     update((new Document("xp", Integer.valueOf(xp))).append("level", Integer.valueOf(level)));
     updateVanillaLeveling();
   }
   
   public void updateVanillaLeveling() {
     this.player.setLevel(calculateLevel(getXP()));
     this.player.setExp(getLevelProgress());
     this.player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(calculateMaxHealth(getLevel()));
   }
   
   public int getXP() {
     return ((Integer)getData("xp")).intValue();
   }
   
   public int getLevel() {
     return ((Integer)getData("level")).intValue();
   }
   
   public float getLevelProgress() {
     int prevMax = calculateMaxXP(getLevel());
     return (float)Math.min(1.0D, ((getXP() - prevMax) / (calculateMaxXP(getLevel() + 1) - prevMax)));
   }
   
   public ChatColor getLevelColor() {
     int level = getLevel();
     if (level < 10)
       return ChatColor.GRAY; 
     if (level < 20)
       return ChatColor.YELLOW; 
     if (level < 30)
       return ChatColor.GREEN; 
     if (level < 40)
       return ChatColor.AQUA; 
     if (level < 50)
       return ChatColor.DARK_AQUA; 
     if (level < 60)
       return ChatColor.GOLD; 
     if (level < 70)
       return ChatColor.DARK_GREEN; 
     if (level < 80)
       return ChatColor.LIGHT_PURPLE; 
     if (level < 90)
       return ChatColor.DARK_PURPLE; 
     if (level < 100)
       return ChatColor.RED; 
     return ChatColor.WHITE;
   }
   
   public static void updateVanishStateBetween(User userOf, User userFor) {
     if (userOf == null || userFor == null)
       return; 
     if (userOf.player == null || userFor.player == null)
       return; 
     if (userOf.isVanished() && userFor.getActivePermissionLevel().ordinal() < userOf.getActivePermissionLevel().ordinal()) {
       userFor.player.hidePlayer((Plugin)instance, userOf.player);
     } else if (!userFor.player.canSee(userOf.player)) {
       userFor.player.showPlayer((Plugin)instance, userOf.player);
     } 
   }
   
   public void updateVanishStatesOnSelf() {
     for (Player test : Bukkit.getOnlinePlayers()) {
       User user = UserLoader.fromPlayer(test);
       updateVanishStateBetween(user, this);
     } 
   }
   
   public void updateVanishState() {
     this.player.setCollidable(!isVanished());
     this.player.setAllowFlight(!(!isVanished() && this.player.getGameMode() != GameMode.CREATIVE && this.player.getGameMode() != GameMode.SPECTATOR));
     if (isVanished()) {
       this.player.setPlayerListName(ChatColor.DARK_GRAY + "" +  ChatColor.MAGIC + "[Staff Member Joining]");
     } else {
       this.player.setPlayerListName(getRank().getNameColor() + this.player.getName());
     } 
     for (Player test : Bukkit.getOnlinePlayers())
       updateVanishStateBetween(this, UserLoader.fromPlayer(test)); 
   }
   
   public void setVanished(boolean vanished) {
     setData("vanished", Boolean.valueOf(vanished));
     updateVanishState();
   }
   
   public boolean isVanished() {
     return ((Boolean)getData("vanished")).booleanValue();
   }
   
   public void setGodMode(boolean enabled) {
     setData("godMode", Boolean.valueOf(enabled));
   }
   
   public boolean isGodMode() {
     return ((Boolean)getData("godMode")).booleanValue();
   }
   
   public void setSystemProfile(SystemProfile profile) {
     this.profile = profile;
     LOGGER.fine("User " + getName() + " system profile set to " + ((profile == null) ? "null" : profile.getProfileName()));
   }
   
   public SystemProfile getSystemProfile() {
     return this.profile;
   }
   
   public PermissionLevel getActivePermissionLevel() {
     return this.activePermissionLevel;
   }
   
   public boolean setActivePermissionLevel(PermissionLevel permissionLevel) {
     if (permissionLevel.ordinal() > getSystemProfile().getMaxPermissionLevel().ordinal())
       return false; 
     LOGGER.fine("User " + getName() + " active permission level set to " + permissionLevel);
     this.activePermissionLevel = permissionLevel;
     SystemProfile.SystemProfileFlags flags = getSystemProfile().getFlags();
     this.player.addAttachment((Plugin)instance, "worldedit.*", flags.hasFlag(SystemProfile.SystemProfileFlags.SystemProfileFlag.WORLDEDIT));
     this.player.addAttachment((Plugin)instance, "minecraft.command.teleport", !(permissionLevel.ordinal() < PermissionLevel.BUILDER.ordinal() && !flags.hasFlag(SystemProfile.SystemProfileFlags.SystemProfileFlag.CMD)));
     this.player.addAttachment((Plugin)instance, "minecraft.command.tp", !(permissionLevel.ordinal() < PermissionLevel.BUILDER.ordinal() && !flags.hasFlag(SystemProfile.SystemProfileFlags.SystemProfileFlag.CMD)));
     this.player.addAttachment((Plugin)instance, "minecraft.command.give", !(permissionLevel.ordinal() < PermissionLevel.GM.ordinal() && !flags.hasFlag(SystemProfile.SystemProfileFlags.SystemProfileFlag.CMD)));
     this.player.addAttachment((Plugin)instance, "minecraft.command.summon", !(permissionLevel.ordinal() < PermissionLevel.GM.ordinal() && !flags.hasFlag(SystemProfile.SystemProfileFlags.SystemProfileFlag.CMD)));
     this.player.addAttachment((Plugin)instance, "minecraft.command.setworldspawn", (permissionLevel.ordinal() >= PermissionLevel.GM.ordinal()));
     this.player.setOp(flags.hasFlag(SystemProfile.SystemProfileFlags.SystemProfileFlag.CMD));
     sendActionBar(ChatColor.GRAY + "Active permission level changed to " + permissionLevel.toString());
     updateVanishStatesOnSelf();
     return true;
   }
   
   public int getLastReadChangeLogId() {
     return ((Integer)getData("lastReadChangeLog")).intValue();
   }
   
   public List<ChangeLogLoader.ChangeLogEntry> getUnreadChangeLogs() {
     return changeLogLoader.getUnreadChangelogs(getLastReadChangeLogId());
   }
   
   public void markChangeLogsRead() {
     setData("lastReadChangeLog", Integer.valueOf(changeLogLoader.getCurrentMaxId()));
   }
   
   public String getLastIP() {
     return (String)getData("ip");
   }
   
   public Rank getRank() {
     return Rank.valueOf((String)getData("rank"));
   }
   
   public void setRank(Rank rank) {
     setData("rank", rank.toString());
   }
   
   public Set<Region> getRegions() {
     return this.cachedRegions;
   }
   
   public Date getFirstJoined() {
     return new Date(((Long)getData("firstJoined")).longValue());
   }
   
   public Date getLastJoined() {
     return new Date(((Long)getData("lastJoined")).longValue());
   }
   
   public Date getLastSeen() {
     return new Date(((Long)getData("lastSeen")).longValue());
   }
   
   public boolean hasJoined() {
     return this.joined;
   }
   
   public int getSkillLevel(SkillType type) {
     return ((Document)getData("skills")).getInteger(type.toString()).intValue();
   }
   
   public void setSkillLevel(SkillType type, int level) {
     Document skillLevels = (Document)getData("skills");
     skillLevels.append(type.toString(), Integer.valueOf(level));
     update(new Document("skills", skillLevels));
   }
   
   public void incrementSkillProgress(SkillType type, double increment) {
     setSkillProgress(type, getSkillProgress(type) + increment);
   }
   
   public void setSkillProgress(SkillType type, double progress) {
     Document skillProgress = (Document)getData("skillProgress");
     skillProgress.append(type.toString(), Double.valueOf(progress));
     int currentLevel = getSkillLevel(type);
     int level = calculateSkillLevel(progress);
     if (level != currentLevel) {
       setSkillLevel(type, level);
       this.player.sendTitle(ChatColor.DARK_GREEN + type.getFriendlyName() + ((level > currentLevel) ? " Increased!" : " Changed"), 
           ChatColor.GREEN + "" + currentLevel + " >>> " + level, 20, 40, 20);
     } 
     update(new Document("skillProgress", skillProgress));
   }
   
   public double getSkillProgress(SkillType type) {
     return ((Document)getData("skillProgress")).getDouble(type.toString()).doubleValue();
   }
   
   public GameMode getSavedGameMode() {
     return GameMode.valueOf((String)getData("gamemode"));
   }
   
   public void setGameMode(GameMode gameMode, boolean updateBukkit) {
     setData("gamemode", gameMode.toString());
     if (updateBukkit)
       this.player.setGameMode(gameMode); 
   }
   
   @SuppressWarnings("unchecked")
   public List<PunishmentData> getPunishmentHistory() {
     List<PunishmentData> history = new ArrayList<>();
     List<Document> results = (List<Document>)getData("punishmentHistory");
     for (Document entry : results) {
       Date expiry = new Date(1000L * (entry.getLong("banDate").longValue() + entry.getLong("duration").longValue()));
       history.add(new PunishmentData(PunishmentType.valueOf(entry.getString("type")), entry.getString("reason"), expiry, (entry.getLong("duration").longValue() == -1L)));
     } 
     return history;
   }
   
   public void punish(PunishmentType punishmentType, String reason) {
     punish(punishmentType, reason, -1L);
   }
   
   public void punish(PunishmentType punishmentType, String reason, long durationSeconds) {
     long now = Instant.now().getEpochSecond();
     Document punishment = (new Document("type", punishmentType.toString()))
       .append("reason", reason)
       .append("duration", Long.valueOf(durationSeconds))
       .append("banDate", Long.valueOf(now));
     setData(punishmentType.getDataHeader(), punishment);
     @SuppressWarnings("unchecked")
	 List<Document> punishmentHistory = (List<Document>)getData("punishmentHistory");
     punishmentHistory.add(punishment);
     setData("punishmentHistory", punishmentHistory);
     String expiry = (durationSeconds == -1L) ? "Never" : (new Date(1000L * (now + durationSeconds))).toString();
     if (this.player != null)
       if (punishmentType == PunishmentType.BAN) {
         this.player.kickPlayer(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You have been banned.\n\n" + (
             reason.equals("") ? "" : (ChatColor.GRAY + "Reason: " + ChatColor.WHITE + reason + ChatColor.WHITE + "\n")) + 
             ChatColor.GRAY + "Expires: " + ChatColor.WHITE + expiry);
       } else if (punishmentType == PunishmentType.KICK) {
         this.player.kickPlayer(ChatColor.DARK_RED + "You were kicked!\n\n" + (
             reason.equals("") ? "" : (ChatColor.GRAY + "Reason: " + ChatColor.WHITE + reason + "\n\n")) + 
             ChatColor.YELLOW + "Repeated kicks may result in a ban.");
       } else if (punishmentType == PunishmentType.WARNING) {
         this.player.sendMessage(" ");
         this.player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You have received a warning.");
         if (!reason.equals(" "))
           this.player.sendMessage(ChatColor.RED + "Reason: " + reason); 
         this.player.sendMessage(ChatColor.GRAY + "Repeated warnings may result in a ban.");
         this.player.sendMessage("");
       } else if (punishmentType == PunishmentType.MUTE) {
         this.player.sendMessage(" ");
         this.player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You have been muted.");
         if (!reason.equals(""))
           this.player.sendMessage(ChatColor.RED + "Reason: " + reason); 
         this.player.sendMessage(ChatColor.RED + "Expires: " + expiry);
         this.player.sendMessage(" ");
       }  
   }
   
   public void unpunish(PunishmentType punishmentType) {
     setData(punishmentType.getDataHeader(), null);
     if (this.player != null && 
       punishmentType == PunishmentType.MUTE) {
       this.player.sendMessage("");
       this.player.sendMessage(ChatColor.DARK_GREEN + "Your mute has been revoked.");
       this.player.sendMessage("");
     } 
   }
   
   public PunishmentData getActivePunishmentData(PunishmentType punishmentType) {
     Document banData = (Document)getData(punishmentType.getDataHeader());
     if (banData == null)
       return null; 
     PunishmentType type = PunishmentType.valueOf(banData.getString("type"));
     String reason = banData.getString("reason");
     long duration = banData.getLong("duration").longValue();
     long banDate = banData.getLong("banDate").longValue();
     long now = Instant.now().getEpochSecond();
     Date expiry = new Date(1000L * (banDate + duration));
     if (duration == -1L)
       return new PunishmentData(type, reason, expiry, true); 
     if (now > banDate + duration)
       return null; 
     return new PunishmentData(type, reason, expiry, false);
   }
   
   public void setSavedLocation(Location loc) {
     setData("lastLocation", StorageUtil.locToDoc(loc));
   }
   
   public void setSavedStaffLocation(Location loc) {
     setData("lastStaffLocation", StorageUtil.locToDoc(loc));
   }
   
   public void autoSave() {
     super.autoSave();
     if (this.player == null)
       return; 
     sendActionBar(ChatColor.GREEN + "Autosaving...");
     Document autoSaveData = (new Document("lastSeen", Long.valueOf(System.currentTimeMillis())))
       .append("maxHealth", Double.valueOf(this.player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()))
       .append("health", Double.valueOf(this.player.getHealth()))
       .append("gamemode", this.joined ? this.player.getGameMode().toString() : getSavedGameMode().toString())
       .append("inventory", getInventoryAsDocument());
     if (this.joined) {
       String key = PermissionUtil.verifyActivePermissionLevel(this, PermissionLevel.TESTER, false) ? "lastStaffLocation" : "lastLocation";
       autoSaveData.append(key, StorageUtil.locToDoc(this.player.getLocation()));
     }
     for (ItemStack itemStack : this.player.getInventory().getContents()) {
       if (itemStack != null) {
         Item item = ItemLoader.fromBukkit(itemStack);
         if (item != null)
           item.autoSave(); 
       } 
     } 
     userHookRegistry.getHooks().forEach(h -> h.onAutoSave(this, autoSaveData));
     update(autoSaveData);
   }
 }


