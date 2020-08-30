 package mc.dragons.core.gameobject.loader;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.UUID;
 import java.util.logging.Logger;
 import mc.dragons.core.Dragons;
 import mc.dragons.core.gameobject.GameObject;
 import mc.dragons.core.gameobject.GameObjectType;
 import mc.dragons.core.gameobject.npc.NPC;
 import mc.dragons.core.gameobject.npc.NPCClass;
 import mc.dragons.core.gameobject.npc.NPCConditionalActions;
 import mc.dragons.core.storage.StorageAccess;
 import mc.dragons.core.storage.StorageManager;
 import org.bson.Document;
 import org.bukkit.entity.EntityType;
 
 public class NPCClassLoader extends GameObjectLoader<NPCClass> {
   private static NPCClassLoader INSTANCE;
   
   private Logger LOGGER = Dragons.getInstance().getLogger();
   
   private GameObjectRegistry masterRegistry;
   
   private boolean allLoaded = false;
   
   private Map<String, NPCClass> cachedNPCClasses;
   
   private NPCClassLoader(Dragons instance, StorageManager storageManager) {
     super(instance, storageManager);
     this.masterRegistry = instance.getGameObjectRegistry();
     this.cachedNPCClasses = new HashMap<>();
   }
   
   public static synchronized NPCClassLoader getInstance(Dragons instance, StorageManager storageManager) {
     if (INSTANCE == null)
       INSTANCE = new NPCClassLoader(instance, storageManager); 
     return INSTANCE;
   }
   
   public NPCClass loadObject(StorageAccess storageAccess) {
     lazyLoadAll();
     this.LOGGER.fine("Loading NPC class " + storageAccess.getIdentifier());
     NPCClass npcClass = new NPCClass(this.storageManager, storageAccess);
     this.masterRegistry.getRegisteredObjects().add(npcClass);
     this.cachedNPCClasses.put(npcClass.getClassName(), npcClass);
     return npcClass;
   }
   
   public NPCClass getNPCClassByClassName(String npcClassName) {
     lazyLoadAll();
     return this.cachedNPCClasses.computeIfAbsent(npcClassName, name -> {
           for (GameObject gameObject : this.masterRegistry.getRegisteredObjects(new GameObjectType[] { GameObjectType.NPC_CLASS })) {
             NPCClass npcClass = (NPCClass)gameObject;
             if (npcClass.getClassName().equalsIgnoreCase(name))
               return npcClass; 
           } 
           return null;
         });
   }
   
   public NPCClass registerNew(String className, String name, EntityType entityType, double maxHealth, int level, NPC.NPCType npcType) {
     lazyLoadAll();
     this.LOGGER.fine("Registering new NPC class (" + className + ")");
     Document emptyConditionals = new Document();
     byte b;
     int i;
     NPCConditionalActions.NPCTrigger[] arrayOfNPCTrigger;
     for (i = (arrayOfNPCTrigger = NPCConditionalActions.NPCTrigger.values()).length, b = 0; b < i; ) {
       NPCConditionalActions.NPCTrigger trigger = arrayOfNPCTrigger[b];
       emptyConditionals.append(trigger.toString(), new ArrayList<>());
       b++;
     } 
     Document data = (new Document("_id", UUID.randomUUID()))
       .append("className", className)
       .append("name", name)
       .append("entityType", entityType.toString())
       .append("maxHealth", Double.valueOf(maxHealth))
       .append("level", Integer.valueOf(level))
       .append("ai", Boolean.valueOf(npcType.hasAIByDefault()))
       .append("immortal", Boolean.valueOf(npcType.isImmortalByDefault()))
       .append("attributes", new Document())
       .append("npcType", npcType.toString())
       .append("lootTable", new Document())
       .append("conditionals", emptyConditionals)
       .append("addons", new ArrayList<String>());
     StorageAccess storageAccess = this.storageManager.getNewStorageAccess(GameObjectType.NPC_CLASS, data);
     NPCClass npcClass = new NPCClass(this.storageManager, storageAccess);
     this.masterRegistry.getRegisteredObjects().add(npcClass);
     this.cachedNPCClasses.put(npcClass.getClassName(), npcClass);
     return npcClass;
   }
   
   public void loadAll(boolean force) {
     if (this.allLoaded && !force)
       return; 
     this.LOGGER.fine("Loading all NPC classes...");
     this.allLoaded = true;
     this.masterRegistry.removeFromRegistry(GameObjectType.NPC_CLASS);
     this.storageManager.getAllStorageAccess(GameObjectType.NPC_CLASS).stream().forEach(storageAccess -> this.masterRegistry.getRegisteredObjects().add(loadObject(storageAccess)));
   }
   
   public void lazyLoadAll() {
     loadAll(false);
   }
 }


