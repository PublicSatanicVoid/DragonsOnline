package mc.dragons.core.storage;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Utilities to marshal between different storage formats.
 * 
 * @author Adam
 *
 */
public class StorageUtil {
	public static Vector docToVec(Document doc) {
		return new Vector(doc.getDouble("x"), doc.getDouble("y"), doc.getDouble("z"));
	}

	public static Document vecToDoc(Vector vec) {
		return (new Document()).append("x", vec.getX()).append("y", vec.getY()).append("z", vec.getZ());
	}

	public static Document locToDoc(Location loc) {
		Document doc = vecToDoc(loc.toVector());
		return doc.append("world", loc.getWorld().getName()).append("pitch", (double) loc.getPitch()).append("yaw", (double) loc.getYaw());
	}

	public static Location docToLoc(Document doc) {
		Vector vec = docToVec(doc);
		// Casting twice to avoid type errors from boxed Double to unboxed float. 
		return vec.toLocation(Bukkit.getWorld(doc.getString("world")), (float) (double) doc.getDouble("yaw"), (float) (double) doc.get("pitch"));
	}
}
