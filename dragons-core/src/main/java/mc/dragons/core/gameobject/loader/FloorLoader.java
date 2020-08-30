 package mc.dragons.core.gameobject.loader;
 
 import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.World;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
 
 public class FloorLoader extends GameObjectLoader<Floor> {
   private static FloorLoader INSTANCE;
   
   private Logger LOGGER = Dragons.getInstance().getLogger();
   
   private static Map<String, Floor> worldNameToFloor;
   
   private static Map<String, Floor> floorNameToFloor;
   
   private GameObjectRegistry masterRegistry;
   
   private boolean allLoaded = false;
   
   private FloorLoader(Dragons instance, StorageManager storageManager) {
     super(instance, storageManager);
     this.masterRegistry = instance.getGameObjectRegistry();
   }
   
   public static synchronized FloorLoader getInstance(Dragons instance, StorageManager storageManager) {
     if (INSTANCE == null) {
       INSTANCE = new FloorLoader(instance, storageManager);
       worldNameToFloor = new HashMap<>();
       floorNameToFloor = new CaseInsensitiveMap<>();
     } 
     return INSTANCE;
   }
   
   public Floor loadObject(StorageAccess storageAccess) {
     lazyLoadAll();
     this.LOGGER.fine("Loading floor " + storageAccess.getIdentifier());
     Floor floor = new Floor(this.storageManager, storageAccess, false);
     this.masterRegistry.getRegisteredObjects().add(floor);
     worldNameToFloor.put(floor.getWorldName(), floor);
     floorNameToFloor.put(floor.getFloorName(), floor);
     return floor;
   }
   
   public Floor registerNew(String floorName, String worldName, String displayName, int levelMin, boolean superflat) {
     lazyLoadAll();
     this.LOGGER.fine("Registering new floor " + floorName + " (world " + worldName + ", displayName " + displayName + ", lvMin " + levelMin + ", superflat=" + superflat + ")");
     StorageAccess storageAccess = this.storageManager.getNewStorageAccess(GameObjectType.FLOOR, (new Document("floorName", floorName))
         .append("worldName", worldName)
         .append("displayName", displayName)
         .append("levelMin", Integer.valueOf(levelMin)));
     Floor floor = new Floor(this.storageManager, storageAccess, superflat);
     this.masterRegistry.getRegisteredObjects().add(floor);
     worldNameToFloor.put(worldName, floor);
     floorNameToFloor.put(floorName, floor);
     return floor;
   }
   
   public static void link(World world, Floor floor) {
     worldNameToFloor.put(world.getName(), floor);
     floorNameToFloor.put(floor.getFloorName(), floor);
   }
   
   public static Floor fromWorldName(String worldName) {
     return worldNameToFloor.get(worldName);
   }
   
   public static Floor fromWorld(World world) {
     return worldNameToFloor.get(world.getName());
   }
   
   public static Floor fromLocation(Location loc) {
     return worldNameToFloor.get(loc.getWorld().getName());
   }
   
   public static Floor fromFloorName(String floorName) {
     return floorNameToFloor.get(floorName);
   }
   
   public static void updateFloorName(String floorName, String newFloorName) {
     floorNameToFloor.put(newFloorName, floorNameToFloor.remove(floorName));
   }
   
   public void loadAll(boolean force) {
     if (this.allLoaded && !force)
       return; 
     this.LOGGER.fine("Loading all floors...");
     this.allLoaded = true;
     this.masterRegistry.removeFromRegistry(GameObjectType.FLOOR);
     this.storageManager.getAllStorageAccess(GameObjectType.FLOOR).stream().forEach(storageAccess -> this.masterRegistry.getRegisteredObjects().add(loadObject(storageAccess)));
   }
   
   public void lazyLoadAll() {
     loadAll(false);
   }
 }


