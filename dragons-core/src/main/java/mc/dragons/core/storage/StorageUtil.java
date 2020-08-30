 package mc.dragons.core.storage;
 
 import org.bson.Document;
 import org.bukkit.Bukkit;
 import org.bukkit.Location;
 import org.bukkit.util.Vector;
 
 public class StorageUtil {
   public static Vector docToVec(Document doc) {
     return new Vector(doc.getDouble("x").doubleValue(), doc.getDouble("y").doubleValue(), doc.getDouble("z").doubleValue());
   }
   
   public static Document vecToDoc(Vector vec) {
     return (new Document())
       .append("x", Double.valueOf(vec.getX()))
       .append("y", Double.valueOf(vec.getY()))
       .append("z", Double.valueOf(vec.getZ()));
   }
   
   public static Document locToDoc(Location loc) {
     Document doc = vecToDoc(loc.toVector());
     return doc.append("world", loc.getWorld().getName())
       .append("pitch", Double.valueOf(loc.getPitch()))
       .append("yaw", Double.valueOf(loc.getYaw()));
   }
   
   public static Location docToLoc(Document doc) {
     Vector vec = docToVec(doc);
     return vec.toLocation(Bukkit.getWorld(doc.getString("world")), (float)((Double)doc.get("yaw")).doubleValue(), (float)((Double)doc.get("pitch")).doubleValue());
   }
 }


