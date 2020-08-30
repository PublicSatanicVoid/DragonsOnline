 package mc.dragons.core.gameobject.npc;
 
 import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.loader.GameObjectRegistry;
import mc.dragons.core.gameobject.loader.NPCClassLoader;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.EntityHider;
import mc.dragons.core.util.ProgressBarUtil;
import mc.dragons.core.util.StringUtil;
 
 public class NPC extends GameObject {
   protected Entity entity;
   
   protected Entity healthIndicator;
   
   protected GameObjectRegistry registry;
   
   public enum NPCType {
     HOSTILE(ChatColor.RED, "", false, false, true, false, false),
     NEUTRAL(ChatColor.YELLOW, "", false, false, true, false, false),
     QUEST(ChatColor.DARK_GREEN, ChatColor.DARK_GREEN + "[NPC] ", true, true, false, true, true),
     SHOP(ChatColor.DARK_AQUA, ChatColor.DARK_AQUA + "[NPC] ", true, true, false, true, true),
     PERSISTENT(ChatColor.YELLOW, "", true, true, true, true, true),
     COMPANION(ChatColor.GOLD, ChatColor.GOLD + "[COMPANION] ", true, false, true, false, true);
     
     private ChatColor nameColor;
     
     private String prefix;
     
     private boolean persistent;
     
     private boolean immortalByDefault;
     
     private boolean aiByDefault;
     
     private boolean loadImmediately;
     
     private boolean respawnOnDeath;
     
     NPCType(ChatColor nameColor, String prefix, boolean persistent, boolean immortalByDefault, boolean aiByDefault, boolean loadImmediately, boolean respawnOnDeath) {
       this.nameColor = nameColor;
       this.prefix = prefix;
       this.persistent = persistent;
       this.immortalByDefault = immortalByDefault;
       this.aiByDefault = aiByDefault;
       this.loadImmediately = loadImmediately;
       this.respawnOnDeath = respawnOnDeath;
     }
     
     public ChatColor getNameColor() {
       return this.nameColor;
     }
     
     public String getPrefix() {
       return this.prefix;
     }
     
     public boolean isPersistent() {
       return this.persistent;
     }
     
     public boolean isImmortalByDefault() {
       return this.immortalByDefault;
     }
     
     public boolean hasAIByDefault() {
       return this.aiByDefault;
     }
     
     public boolean isLoadedImmediately() {
       return this.loadImmediately;
     }
     
     public boolean canRespawnOnDeath() {
       return this.respawnOnDeath;
     }
   }
   
   protected boolean isDamageExternalized = false;
   
   protected static NPCClassLoader npcClassLoader;
   
   protected static EntityHider entityHider;
   
   public NPC(Entity entity, StorageManager storageManager, StorageAccess storageAccess) {
     super(storageManager, storageAccess);
     LOGGER.fine("Constructing NPC (" + StringUtil.entityToString(entity) + ", " + storageManager + ", " + storageAccess + ")");
     if (npcClassLoader == null)
       npcClassLoader = (NPCClassLoader)GameObjectType.NPC_CLASS.<NPCClass, NPCClassLoader>getLoader(); 
     if (entityHider == null)
       entityHider = new EntityHider((Plugin)Dragons.getInstance(), EntityHider.Policy.BLACKLIST); 
     this.entity = entity;
     this.registry = Dragons.getInstance().getGameObjectRegistry();
     initializeEntity();
     getNPCClass().getAddons().forEach(addon -> addon.initialize(this));
   }
   
   public void initializeEntity() {
     this.entity.setCustomName(getDecoratedName());
     this.entity.setCustomNameVisible(true);
     this.healthIndicator = this.entity;
     Dragons.getInstance().getBridge().setEntityAI(this.entity, getNPCClass().hasAI());
     Dragons.getInstance().getBridge().setEntityInvulnerable(this.entity, isImmortal());
     if (this.entity instanceof Zombie)
       ((Zombie)this.entity).setBaby(false); 
     if (this.entity.isInsideVehicle())
       this.entity.getVehicle().eject(); 
     if (this.entity instanceof Attributable) {
       Attributable att = (Attributable)this.entity;
       for (Map.Entry<Attribute, Double> a : getNPCClass().getCustomAttributes().entrySet())
         att.getAttribute(a.getKey()).setBaseValue(((Double)a.getValue()).doubleValue()); 
     } 
     Material heldItemType = getNPCClass().getHeldItemType();
     if (heldItemType != null)
       setHeldItem(new ItemStack(heldItemType)); 
     this.entity.setMetadata("handle", (MetadataValue)new FixedMetadataValue((Plugin)Dragons.getInstance(), this));
   }
   
   public boolean isDamageExternalized() {
     return this.isDamageExternalized;
   }
   
   public void setDamageExternalized(boolean externalized) {
     this.isDamageExternalized = externalized;
   }
   
   public NPCClass getNPCClass() {
     String className = (String)getData("className");
     if (className == null)
       return null; 
     return npcClassLoader.getNPCClassByClassName(className);
   }
   
   public void setMaxHealth(double maxHealth) {
     if (this.entity instanceof Attributable) {
       Attributable attributable = (Attributable)this.entity;
       attributable.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
       setData("maxHealth", Double.valueOf(maxHealth));
     } 
   }
   
   public double getMaxHealth() {
     if (this.entity instanceof Attributable) {
       Attributable attributable = (Attributable)this.entity;
       return attributable.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
     } 
     return 0.0D;
   }
   
   public void setHealth(double health) {
     if (this.entity instanceof Damageable) {
       Damageable damageable = (Damageable)this.entity;
       damageable.setHealth(health);
     } 
   }
   
   public void damage(double damage, Entity source) {
     if (this.entity instanceof Damageable) {
       Damageable damageable = (Damageable)this.entity;
       damageable.damage(damage, source);
     } 
   }
   
   public void damage(double damage) {
     if (this.entity instanceof Damageable) {
       Damageable damageable = (Damageable)this.entity;
       damageable.damage(damage);
     } 
   }
   
   public double getHealth() {
     if (this.entity instanceof Damageable) {
       Damageable damageable = (Damageable)this.entity;
       return damageable.getHealth();
     } 
     return 0.0D;
   }
   
   public ItemStack getHeldItem() {
     if (this.entity instanceof LivingEntity)
       return ((LivingEntity)this.entity).getEquipment().getItemInMainHand(); 
     return null;
   }
   
   public void setHeldItem(ItemStack itemStack) {
     if (this.entity instanceof LivingEntity)
       ((LivingEntity)this.entity).getEquipment().setItemInMainHand(itemStack); 
     setData("holding", itemStack.getType().toString());
   }
   
   public boolean isImmortal() {
     return ((Boolean)getData("immortal")).booleanValue();
   }
   
   public void setExternalHealthIndicator(ArmorStand indicator) {
     this.healthIndicator.setCustomNameVisible(false);
     this.healthIndicator = (Entity)indicator;
     indicator.setCustomNameVisible(true);
     updateHealthBar(0.0D);
   }
   
   public void updateHealthBar() {
     updateHealthBar(0.0D);
   }
   
   public void updateHealthBar(double additionalDamage) {
     this.healthIndicator.setCustomName(String.valueOf(getDecoratedName()) + (
         isImmortal() ? (
         ChatColor.LIGHT_PURPLE + " [Immortal]") : (
         ChatColor.DARK_GRAY + " [" + ProgressBarUtil.getHealthBar(getHealth() - additionalDamage, getMaxHealth()) + ChatColor.DARK_GRAY + "]")));
   }
   
   public String getName() {
     return (String)getData("name");
   }
   
   public String getDecoratedName() {
     return String.valueOf(getNPCType().getPrefix()) + getNPCType().getNameColor() + getName() + ChatColor.GRAY + " Lv " + getLevel();
   }
   
   public NPCType getNPCType() {
     return NPCType.valueOf((String)getData("npcType"));
   }
   
   public void setNPCType(NPCType npcType) {
     setData("npcType", npcType.toString());
   }
   
   public int getLevel() {
     return ((Integer)getData("level")).intValue();
   }
   
   public void setTarget(LivingEntity target) {
     if (this.entity instanceof Creature) {
       if (target != null) {
         Creature c = (Creature)this.entity;
         c.setTarget(target);
       } 
       this.entity.setMetadata("target", (MetadataValue)new FixedMetadataValue((Plugin)Dragons.getInstance(), target));
     } 
   }
   
   public LivingEntity getDeclaredTarget() {
     if (this.entity instanceof Creature && this.entity.getMetadata("target").size() > 0)
       return (LivingEntity)((MetadataValue)this.entity.getMetadata("target").get(0)).value(); 
     return null;
   }
   
   public void remove() {
     this.entity.remove();
     this.registry.removeFromDatabase(this);
   }
   
   public void phase(Player playerFor) {
     LOGGER.finer("Phasing NPC " + getIdentifier() + " for " + playerFor.getName());
     for (Player p : Bukkit.getOnlinePlayers()) {
       if (!p.equals(playerFor))
         entityHider.hideEntity(p, this.entity); 
     } 
     entityHider.showEntity(playerFor, this.entity);
   }
   
   public void unphase(Player playerFor) {
     entityHider.hideEntity(playerFor, this.entity);
   }
   
   public void setEntity(Entity entity) {
     if (this.entity != null)
       this.entity.removeMetadata("handle", (Plugin)Dragons.getInstance()); 
     LOGGER.finer("Replacing entity backing NPC " + getIdentifier() + ": " + StringUtil.entityToString(this.entity) + " -> " + StringUtil.entityToString(entity));
     this.entity = entity;
     this.entity.setMetadata("handle", (MetadataValue)new FixedMetadataValue((Plugin)Dragons.getInstance(), this));
   }
   
   public Entity getEntity() {
     return this.entity;
   }
   
   public void regenerate(Location spawn) {
     LOGGER.fine("Regenerating NPC " + getIdentifier() + " at " + StringUtil.locToString(spawn));
     if (this.entity != null) {
       this.entity.remove();
       LOGGER.fine("-Removed old entity");
     } 
     setEntity(spawn.getWorld().spawnEntity(spawn, getNPCClass().getEntityType()));
     this.healthIndicator = this.entity;
     initializeEntity();
   }
 }


