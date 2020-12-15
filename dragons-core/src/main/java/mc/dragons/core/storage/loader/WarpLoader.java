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
			return this.warpName;
		}

		public Location getLocation() {
			return this.location;
		}

		public Document toDocument() {
			return (new Document("name", this.warpName)).append("location", StorageUtil.locToDoc(this.location));
		}
	}

	public WarpLoader(MongoConfig config) {
		super(config, "#unused#", "warps");
		this.warps = new LinkedHashMap<>();
		for (Document doc : this.collection.find()) {
			WarpEntry entry = new WarpEntry(doc);
			this.warps.put(entry.getWarpName().toLowerCase(), entry);
		}
	}

	public Location getWarp(String warpName) {
		String storedWarpName = warpName.toLowerCase();
		if (!this.warps.containsKey(storedWarpName))
			return null;
		return this.warps.get(storedWarpName).getLocation();
	}

	public Collection<WarpEntry> getWarps() {
		return this.warps.values();
	}

	public void addWarp(String warpName, Location location) {
		String storedWarpName = warpName.toLowerCase();
		WarpEntry entry = new WarpEntry(storedWarpName, location);
		this.warps.put(entry.getWarpName(), entry);
		this.collection.insertOne(entry.toDocument());
	}

	public void delWarp(String warpName) {
		String storedWarpName = warpName.toLowerCase();
		this.collection.deleteOne(new Document("name", storedWarpName));
		this.warps.remove(warpName);
	}
}
