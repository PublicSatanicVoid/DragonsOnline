package mc.dragons.core.storage;

import java.util.ArrayList;
import java.util.Map.Entry;

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
		return new Document().append("x", vec.getX()).append("y", vec.getY()).append("z", vec.getZ());
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
	
	/**
	 * Returns the document C such that applyDelta(B, C) = A.
	 * C is referred to as the delta because it contains all
	 * information required to transform B into A.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static Document getDelta(Document a, Document b) {
		
		// Goal is to associate every change to a document
		// with a document representing what was changed.
		
		// Given the most recent version of the document,
		// we can sequentially put all contents of these
		// documents until we arrive at the desired
		// past version.
		
		// getDelta({"a":1,"b":2,"c":3}, {"a":2,"b":2}) = {"+":{"c":3},"-":[]}			since {"a":2,"b":2} + {"c":3} - [] = {"a":2,"b":2,"c":3}
		// getDelta({"a":1,"b":2}, {"a":2,"b":2,"c":3}) = {"+":{"a":1},"-":["c"]}		since {"a":2,"b":2,"c":3} + {"a":1} - ["c"] = {"a":1,"b":2}
		
		Document result = new Document("+", new Document()).append("-", new ArrayList<>());
		for(Entry<String, Object> entry : a.entrySet()) {
			if(!b.containsKey(entry.getKey()) || b.get(entry.getKey()) == null || !(b.get(entry.getKey()).equals(a.get(entry.getKey())))) {
				result.get("+", Document.class).append(entry.getKey(), entry.getValue());
			}
		}
		for(Entry<String, Object> entry : b.entrySet()) {
			if(!a.containsKey(entry.getKey()) || a.get(entry.getKey()) == null) {
				result.getList("-", String.class).add(entry.getKey());
			}
		}
	
		return result;
	}
	
	public static Document applyDelta(Document base, Document delta) {
		Document result = Document.parse(base.toJson());
		result.putAll(delta.get("+", Document.class));
		for(String key : delta.getList("-", String.class)) {
			result.remove(key);
		}
		return result;
	}
}
