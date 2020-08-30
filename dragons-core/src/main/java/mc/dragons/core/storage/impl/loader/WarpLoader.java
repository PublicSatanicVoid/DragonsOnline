 package mc.dragons.core.storage.impl.loader;
 
 import java.util.Collection;
 import java.util.LinkedHashMap;
 import java.util.Map;
 import mc.dragons.core.storage.StorageUtil;
 import org.bson.Document;
 import org.bson.conversions.Bson;
 import org.bukkit.Location;
 
 public class WarpLoader extends AbstractLightweightLoader<WarpLoader.WarpEntry> {
   private Map<String, WarpEntry> warps;
   
   public static class WarpEntry {
     private String warpName;
     
     private Location location;
     
     public WarpEntry(String warpName, Location location) {
       this.warpName = warpName;
       this.location = location;
     }
     
     public WarpEntry(Document document) {
       this(document.getString("name"), StorageUtil.docToLoc((Document)document.get("location", Document.class)));
     }
     
     public String getWarpName() {
       return this.warpName;
     }
     
     public Location getLocation() {
       return this.location;
     }
     
     public Document toDocument() {
       return (new Document("name", this.warpName)).append("location", StorageUtil.locToDoc(this.location));
     }
   }
   
   public WarpLoader() {
     super("#unused#", "warps");
     this.warps = new LinkedHashMap<>();
     for (Document doc : this.collection.find()) {
       WarpEntry entry = new WarpEntry(doc);
       this.warps.put(entry.getWarpName().toLowerCase(), entry);
     } 
   }
   
   public Location getWarp(String warpName) {
     warpName = warpName.toLowerCase();
     if (!this.warps.containsKey(warpName))
       return null; 
     return ((WarpEntry)this.warps.get(warpName)).getLocation();
   }
   
   public Collection<WarpEntry> getWarps() {
     return this.warps.values();
   }
   
   public void addWarp(String warpName, Location location) {
     warpName = warpName.toLowerCase();
     WarpEntry entry = new WarpEntry(warpName, location);
     this.warps.put(entry.getWarpName(), entry);
     this.collection.insertOne(entry.toDocument());
   }
   
   public void delWarp(String warpName) {
     warpName = warpName.toLowerCase();
     this.collection.deleteOne((Bson)new Document("name", warpName));
     this.warps.remove(warpName);
   }
 }


