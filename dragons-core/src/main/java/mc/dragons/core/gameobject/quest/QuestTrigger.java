 package mc.dragons.core.gameobject.quest;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import mc.dragons.core.gameobject.GameObjectType;
 import mc.dragons.core.gameobject.item.Item;
 import mc.dragons.core.gameobject.item.ItemClass;
 import mc.dragons.core.gameobject.loader.ItemClassLoader;
 import mc.dragons.core.gameobject.loader.ItemLoader;
 import mc.dragons.core.gameobject.loader.NPCClassLoader;
 import mc.dragons.core.gameobject.loader.NPCLoader;
 import mc.dragons.core.gameobject.loader.RegionLoader;
 import mc.dragons.core.gameobject.npc.NPC;
 import mc.dragons.core.gameobject.npc.NPCClass;
 import mc.dragons.core.gameobject.region.Region;
 import mc.dragons.core.gameobject.user.User;
 import org.bson.Document;
 import org.bukkit.entity.Entity;
 import org.bukkit.event.Event;
 import org.bukkit.event.entity.EntityDeathEvent;
 import org.bukkit.event.player.PlayerInteractEntityEvent;
 import org.bukkit.inventory.ItemStack;
 
 public class QuestTrigger {
   private static RegionLoader regionLoader;
   
   private static NPCClassLoader npcClassLoader;
   
   private static ItemClassLoader itemClassLoader;
   
   private TriggerType type;
   
   private String npcClassShortName;
   
   private NPCClass npcClass;
   
   private ItemClass itemClass;
   
   private int quantity;
   
   private Region region;
   
   private Map<QuestTrigger, QuestAction> branchPoints;
   
   private Map<User, Integer> killQuantity;
   
   public enum TriggerType {
     ENTER_REGION, EXIT_REGION, CLICK_NPC, KILL_NPC, INSTANT, HAS_ITEM, NEVER, BRANCH_CONDITIONAL;
   }
   
   public static QuestTrigger fromDocument(Document trigger, Quest quest) {
     if (regionLoader == null) {
       regionLoader = (RegionLoader)GameObjectType.REGION.<Region, RegionLoader>getLoader();
       npcClassLoader = (NPCClassLoader)GameObjectType.NPC_CLASS.<NPCClass, NPCClassLoader>getLoader();
       itemClassLoader = (ItemClassLoader)GameObjectType.ITEM_CLASS.<ItemClass, ItemClassLoader>getLoader();
     } 
     QuestTrigger questTrigger = new QuestTrigger();
     questTrigger.type = TriggerType.valueOf(trigger.getString("type"));
     if (questTrigger.type == TriggerType.ENTER_REGION || questTrigger.type == TriggerType.EXIT_REGION) {
       questTrigger.region = regionLoader.getRegionByName(trigger.getString("region"));
     } else if (questTrigger.type == TriggerType.CLICK_NPC) {
       questTrigger.npcClassShortName = trigger.getString("npcClass");
     } else if (questTrigger.type == TriggerType.KILL_NPC) {
       questTrigger.npcClassShortName = trigger.getString("npcClass");
       questTrigger.quantity = trigger.getInteger("quantity").intValue();
     } else if (questTrigger.type == TriggerType.BRANCH_CONDITIONAL) {
       questTrigger.branchPoints = new LinkedHashMap<>();
       for (Document conditional : trigger.getList("branchPoints", Document.class))
         questTrigger.branchPoints.put(
             fromDocument((Document)conditional.get("trigger"), quest), 
             QuestAction.fromDocument((Document)conditional.get("action"), quest)); 
     } else if (questTrigger.type == TriggerType.HAS_ITEM) {
       questTrigger.itemClass = itemClassLoader.getItemClassByClassName(trigger.getString("itemClass"));
       questTrigger.quantity = trigger.getInteger("quantity").intValue();
     } 
     return questTrigger;
   }
   
   private QuestTrigger() {
     this.killQuantity = new HashMap<>();
   }
   
   public static QuestTrigger onEnterRegion(Region region) {
     QuestTrigger trigger = new QuestTrigger();
     trigger.type = TriggerType.ENTER_REGION;
     trigger.region = region;
     return trigger;
   }
   
   public static QuestTrigger onExitRegion(Region region) {
     QuestTrigger trigger = new QuestTrigger();
     trigger.type = TriggerType.EXIT_REGION;
     trigger.region = region;
     return trigger;
   }
   
   public static QuestTrigger onClickNPC(NPCClass npcClass) {
     QuestTrigger trigger = new QuestTrigger();
     trigger.type = TriggerType.CLICK_NPC;
     trigger.npcClass = npcClass;
     return trigger;
   }
   
   public static QuestTrigger onKillNPC(NPCClass npcClass, int quantity) {
     QuestTrigger trigger = new QuestTrigger();
     trigger.type = TriggerType.KILL_NPC;
     trigger.npcClass = npcClass;
     trigger.quantity = quantity;
     return trigger;
   }
   
   public static QuestTrigger instant() {
     QuestTrigger trigger = new QuestTrigger();
     trigger.type = TriggerType.INSTANT;
     return trigger;
   }
   
   public static QuestTrigger hasItem(ItemClass itemClass, int quantity) {
     QuestTrigger trigger = new QuestTrigger();
     trigger.type = TriggerType.HAS_ITEM;
     trigger.itemClass = itemClass;
     trigger.quantity = quantity;
     return trigger;
   }
   
   public static QuestTrigger never() {
     QuestTrigger trigger = new QuestTrigger();
     trigger.type = TriggerType.NEVER;
     return trigger;
   }
   
   public static QuestTrigger branchConditional(Map<QuestTrigger, QuestAction> branchPoints) {
     QuestTrigger trigger = new QuestTrigger();
     trigger.type = TriggerType.BRANCH_CONDITIONAL;
     trigger.branchPoints = branchPoints;
     return trigger;
   }
   
   public TriggerType getTriggerType() {
     return this.type;
   }
   
   public NPCClass getNPCClass() {
     npcClassDeferredLoad();
     return this.npcClass;
   }
   
   public Region getRegion() {
     return this.region;
   }
   
   public ItemClass getItemClass() {
     return this.itemClass;
   }
   
   public int getQuantity() {
     return this.quantity;
   }
   
   public Map<QuestTrigger, QuestAction> getBranchPoints() {
     return this.branchPoints;
   }
   
   private void npcClassDeferredLoad() {
     if (this.npcClass == null)
       this.npcClass = npcClassLoader.getNPCClassByClassName(this.npcClassShortName); 
   }
   
   public Document toDocument() {
     List<Document> conditions;
     Document document = new Document("type", this.type.toString());
     switch (this.type) {
       case ENTER_REGION:
       case EXIT_REGION:
         document.append("region", this.region.getName());
         break;
       case CLICK_NPC:
         npcClassDeferredLoad();
         document.append("npcClass", this.npcClass.getClassName());
         break;
       case KILL_NPC:
         npcClassDeferredLoad();
         document.append("npcClass", this.npcClass.getClassName());
         document.append("quantity", Integer.valueOf(this.quantity));
         break;
       case HAS_ITEM:
         document.append("itemClass", this.itemClass.getClassName()).append("quantity", Integer.valueOf(this.quantity));
         break;
       case BRANCH_CONDITIONAL:
         conditions = new ArrayList<>();
         for (Map.Entry<QuestTrigger, QuestAction> entry : this.branchPoints.entrySet())
           conditions.add((new Document("trigger", ((QuestTrigger)entry.getKey()).toDocument())).append("action", ((QuestAction)entry.getValue()).toDocument())); 
         document.append("branchPoints", conditions);
         break;

case INSTANT:
	break;
case NEVER:
	break;
default:
	break;     } 
     return document;
   }
   
   public boolean test(User user, Event event) {
     if (this.type == TriggerType.INSTANT)
       return true; 
     if (this.type == TriggerType.NEVER)
       return false; 
     if (this.type == TriggerType.HAS_ITEM) {
       user.debug(" [ - Testing if has item " + this.itemClass.getClassName());
       int has = 0;
       byte b;
       int i;
       ItemStack[] arrayOfItemStack;
       for (i = (arrayOfItemStack = user.getPlayer().getInventory().getContents()).length, b = 0; b < i; ) {
         ItemStack itemStack = arrayOfItemStack[b];
         Item item = ItemLoader.fromBukkit(itemStack);
         if (item != null && 
           item.getClassName().equals(this.itemClass.getClassName()))
           has += itemStack.getAmount(); 
         b++;
       } 
       user.debug("    [ - has " + has + " vs. needs " + this.quantity);
       return (has >= this.quantity);
     } 
     if (this.type == TriggerType.ENTER_REGION) {
       user.updateState(false, false);
       if (user.getRegions().contains(this.region))
         return true; 
     } 
     if (this.type == TriggerType.EXIT_REGION) {
       user.updateState(false, false);
       if (!user.getRegions().contains(this.region))
         return true; 
     } 
     if (this.type == TriggerType.CLICK_NPC) {
       npcClassDeferredLoad();
       if (event == null)
         return false; 
       if (event instanceof PlayerInteractEntityEvent) {
         user.debug("    [ - it's an interact entity event");
         PlayerInteractEntityEvent interactEvent = (PlayerInteractEntityEvent)event;
         NPC npc = NPCLoader.fromBukkit(interactEvent.getRightClicked());
         if (npc == null)
           return false; 
         user.debug("    [ - clicked class: " + npc.getNPCClass().getClassName() + "; want: " + this.npcClass.getClassName());
         if (npc.getNPCClass().equals(this.npcClass))
           return true; 
       } 
     } 
     if (this.type == TriggerType.KILL_NPC) {
       npcClassDeferredLoad();
       if (event == null)
         return false; 
       if (event instanceof EntityDeathEvent) {
         EntityDeathEvent deathEvent = (EntityDeathEvent)event;
         NPC npc = NPCLoader.fromBukkit((Entity)deathEvent.getEntity());
         if (npc == null)
           return false; 
         if (npc.getNPCClass().equals(this.npcClass)) {
           this.killQuantity.put(user, Integer.valueOf(((Integer)this.killQuantity.getOrDefault(user, Integer.valueOf(0))).intValue() + 1));
           if (((Integer)this.killQuantity.getOrDefault(user, Integer.valueOf(0))).intValue() >= this.quantity)
             return true; 
         } 
       } 
     } 
     if (this.type == TriggerType.BRANCH_CONDITIONAL)
       for (Map.Entry<QuestTrigger, QuestAction> conditional : this.branchPoints.entrySet()) {
         if (((QuestTrigger)conditional.getKey()).test(user, event)) {
           ((QuestAction)conditional.getValue()).execute(user);
           return true;
         } 
       }  
     return false;
   }
 }


