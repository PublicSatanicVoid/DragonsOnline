package mc.dragons.core.storage.loader;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Location;

import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.storage.mongo.MongoConfig;

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
			this(document.getString("name"), StorageUtil.docToLoc(document.get("location", Document.class)));
		}

		public String getWarpName() {
			return warpName;
		}

		public Location getLocation() {
			return location;
		}

		public Document toDocument() {
			return new Document("name", warpName).append("location", StorageUtil.locToDoc(location));
		}
	}

	public WarpLoader(MongoConfig config) {
		super(config, "#unused#", "warps");
		warps = new LinkedHashMap<>();
		for (Document doc : collection.find()) {
			WarpEntry entry = new WarpEntry(doc);
			warps.put(entry.getWarpName().toLowerCase(), entry);
		}
	}

	public Location getWarp(String warpName) {
		String storedWarpName = warpName.toLowerCase();
		if (!warps.containsKey(storedWarpName)) {
			return null;
		}
		return warps.get(storedWarpName).getLocation();
	}

	public Collection<WarpEntry> getWarps() {
		return warps.values();
	}

	public void addWarp(String warpName, Location location) {
		String storedWarpName = warpName.toLowerCase();
		WarpEntry entry = new WarpEntry(storedWarpName, location);
		warps.put(entry.getWarpName(), entry);
		collection.insertOne(entry.toDocument());
	}

	public void delWarp(String warpName) {
		String storedWarpName = warpName.toLowerCase();
		collection.deleteOne(new Document("name", storedWarpName));
		warps.remove(warpName);
	}
}
