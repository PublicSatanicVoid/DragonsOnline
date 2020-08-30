 package mc.dragons.core.gameobject.loader;
 
 import java.util.HashSet;
 import java.util.Set;
 import java.util.stream.Collectors;
 import mc.dragons.core.Dragons;
 import mc.dragons.core.gameobject.GameObject;
 import mc.dragons.core.gameobject.GameObjectType;
 import mc.dragons.core.storage.StorageManager;
 
 public class GameObjectRegistry {
   protected Dragons plugin;
   
   protected StorageManager storageManager;
   
   protected Set<GameObject> registeredObjects;
   
   public GameObjectRegistry(Dragons instance, StorageManager storageManager) {
     this.plugin = instance;
     this.storageManager = storageManager;
     this.registeredObjects = new HashSet<>();
   }
   
   public GameObject registerNew() {
     return null;
   }
   
   public Set<GameObject> getRegisteredObjects() {
     return this.registeredObjects;
   }
   
   public Set<GameObject> getRegisteredObjects(GameObjectType... types) {
     return (Set<GameObject>)this.registeredObjects.stream()
       .filter(obj -> {
           GameObjectType[] arrayOfGameObjectType;
           int i = (arrayOfGameObjectType = types).length;
           for (byte b = 0; b < i; b++) {
             GameObjectType type = arrayOfGameObjectType[b];
             if (type == obj.getType())
               return true; 
           } 
           return false;
         }).collect(Collectors.toSet());
   }
   
   public void removeFromDatabase(GameObject gameObject) {
     this.storageManager.removeObject(gameObject);
     this.registeredObjects.remove(gameObject);
   }
   
   public void removeFromRegistry(GameObjectType type) {
     this.registeredObjects.removeIf(obj -> (obj.getType() == type));
   }
 }


