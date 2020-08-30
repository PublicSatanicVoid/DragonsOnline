 package mc.dragons.core.gameobject.loader;
 
 import mc.dragons.core.Dragons;
 import mc.dragons.core.gameobject.GameObject;
 import mc.dragons.core.storage.StorageAccess;
 import mc.dragons.core.storage.StorageManager;
 
 public abstract class GameObjectLoader<T extends GameObject> {
   protected Dragons plugin;
   
   protected StorageManager storageManager;
   
   protected GameObjectLoader(Dragons instance, StorageManager storageManager) {
     this.plugin = instance;
     this.storageManager = storageManager;
   }
   
   public abstract T loadObject(StorageAccess paramStorageAccess);
 }


