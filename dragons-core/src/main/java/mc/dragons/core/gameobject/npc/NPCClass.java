 package mc.dragons.core.gameobject.npc;
 
 import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.NPCAddon;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
 
 public class NPCClass extends GameObject {
   private LootTable lootTable;
   
   private NPCConditionalActions[] conditionals = new NPCConditionalActions[(NPCConditionalActions.NPCTrigger.values()).length];
   
   private List<NPCAddon> addons;
   
   @SuppressWarnings("unchecked")
   public NPCClass(StorageManager storageManager, StorageAccess storageAccess) {
     super(storageManager, storageAccess);
     LOGGER.fine("Constructing NPC Class (" + storageManager + ", " + storageAccess + ")");
     this.lootTable = new LootTable(this);
     int i = 0;
     byte b;
     int j;
     NPCConditionalActions.NPCTrigger[] arrayOfNPCTrigger;
     for (j = (arrayOfNPCTrigger = NPCConditionalActions.NPCTrigger.values()).length, b = 0; b < j; ) {
       NPCConditionalActions.NPCTrigger trigger = arrayOfNPCTrigger[b];
       this.conditionals[i] = new NPCConditionalActions(trigger, this);
       i++;
       b++;
     } 
     this.addons = (List<NPCAddon>)((List<String>)getData("addons"))
       .stream()
       .map(addonName -> (NPCAddon)Dragons.getInstance().getAddonRegistry().getAddonByName(addonName))
       .collect(Collectors.toList());
   }
   
   private void saveAddons() {
     setData("addons", this.addons.stream().map(a -> a.getName()).collect(Collectors.toList()));
   }
   
   public void addAddon(NPCAddon addon) {
     this.addons.add(addon);
     saveAddons();
   }
   
   public void removeAddon(NPCAddon addon) {
     this.addons.remove(addon);
     saveAddons();
   }
   
   public List<NPCAddon> getAddons() {
     return this.addons;
   }
   
   public void handleMove(NPC npc, Location loc) {
     this.addons.forEach(addon -> addon.onMove(npc, loc));
   }
   
   public void handleTakeDamage(NPC on, GameObject from, double amt) {
     this.addons.forEach(addon -> addon.onTakeDamage(on, from, amt));
   }
   
   public void handleDealDamage(NPC from, GameObject to, double amt) {
     this.addons.forEach(addon -> addon.onDealDamage(from, to, amt));
   }
   
   public void handleDeath(NPC npc) {
     this.addons.forEach(addon -> addon.onDeath(npc));
   }
   
   public void executeConditionals(NPCConditionalActions.NPCTrigger trigger, User user, NPC npc) {
     user.debug("Executing conditionals");
     byte b;
     int i;
     NPCConditionalActions[] arrayOfNPCConditionalActions;
     for (i = (arrayOfNPCConditionalActions = this.conditionals).length, b = 0; b < i; ) {
       NPCConditionalActions conditionalAction = arrayOfNPCConditionalActions[b];
       if (conditionalAction.getTrigger() == trigger)
         conditionalAction.executeConditionals(user, npc); 
       b++;
     } 
   }
   
   public NPCConditionalActions getConditionalActions(NPCConditionalActions.NPCTrigger trigger) {
     byte b;
     int i;
     NPCConditionalActions[] arrayOfNPCConditionalActions;
     for (i = (arrayOfNPCConditionalActions = this.conditionals).length, b = 0; b < i; ) {
       NPCConditionalActions conditionalAction = arrayOfNPCConditionalActions[b];
       if (conditionalAction.getTrigger() == trigger)
         return conditionalAction; 
       b++;
     } 
     return null;
   }
   
   public LootTable getLootTable() {
     return this.lootTable;
   }
   
   public void updateLootTable(String regionName, String itemName, double lootChancePercent) {
     Document lootTableData = (Document)this.storageAccess.get("lootTable");
     Document regionLoot = (Document)lootTableData.get(regionName);
     if (regionLoot == null) {
       lootTableData.append(regionName, new Document(itemName, Double.valueOf(lootChancePercent)));
       return;
     } 
     regionLoot.append(itemName, Double.valueOf(lootChancePercent));
     update(new Document("lootTable", lootTableData));
   }
   
   public void deleteFromLootTable(String regionName, String itemName) {
     Document lootTableData = (Document)this.storageAccess.get("lootTable");
     Document regionLoot = (Document)lootTableData.get(regionName);
     if (regionLoot == null)
       return; 
     regionLoot.remove(itemName);
     update(new Document("lootTable", lootTableData));
   }
   
   public String getClassName() {
     return (String)getData("className");
   }
   
   public String getName() {
     return (String)getData("name");
   }
   
   public void setName(String displayName) {
     setData("name", displayName);
   }
   
   public EntityType getEntityType() {
     return EntityType.valueOf((String)getData("entityType"));
   }
   
   public void setEntityType(EntityType type) {
     setData("entityType", type.toString());
   }
   
   public Material getHeldItemType() {
     Object holding = getData("holding");
     if (holding == null)
       return null; 
     return Material.valueOf((String)holding);
   }
   
   public void setHeldItemType(Material type) {
     setData("holding", type.toString());
   }
   
   public boolean isImmortal() {
     return ((Boolean)getData("immortal")).booleanValue();
   }
   
   public void setImmortal(boolean immortal) {
     setData("immortal", Boolean.valueOf(immortal));
   }
   
   public boolean hasAI() {
     return ((Boolean)getData("ai")).booleanValue();
   }
   
   public void setAI(boolean hasAI) {
     setData("ai", Boolean.valueOf(hasAI));
   }
   
   public double getMaxHealth() {
     return ((Double)getData("maxHealth")).doubleValue();
   }
   
   public void setMaxHealth(double maxHealth) {
     setData("maxHealth", Double.valueOf(maxHealth));
   }
   
   public int getLevel() {
     return ((Integer)getData("level")).intValue();
   }
   
   public void setLevel(int level) {
     setData("level", Integer.valueOf(level));
   }
   
   public NPC.NPCType getNPCType() {
     return NPC.NPCType.valueOf((String)getData("npcType"));
   }
   
   public void setNPCType(NPC.NPCType npcType) {
     setData("npcType", npcType);
   }
   
   public Map<Attribute, Double> getCustomAttributes() {
     Map<Attribute, Double> result = new HashMap<>();
     for (Map.Entry<String, Object> attribute : (Iterable<Map.Entry<String, Object>>)((Document)getData("attributes")).entrySet())
       result.put(Attribute.valueOf(attribute.getKey()), Double.valueOf(((Double)attribute.getValue()).doubleValue())); 
     return result;
   }
   
   public void setCustomAttribute(Attribute attribute, double base) {
     Document attributes = (Document)getData("attributes");
     attributes.append(attribute.toString(), Double.valueOf(base));
     this.storageAccess.update(new Document("attributes", attributes));
   }
   
   public void removeCustomAttribute(Attribute attribute) {
     Document attributes = (Document)getData("attributes");
     attributes.remove(attribute.toString());
     this.storageAccess.update(new Document("attributes", attributes));
   }
 }


