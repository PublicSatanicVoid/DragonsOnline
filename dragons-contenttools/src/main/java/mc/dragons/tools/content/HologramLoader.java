package mc.dragons.tools.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.storage.loader.AbstractLightweightLoader;
import mc.dragons.core.storage.mongo.MongoConfig;
import mc.dragons.core.util.HologramUtil;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.content.HologramLoader.Hologram;

public class HologramLoader extends AbstractLightweightLoader<Hologram> {
	public static final double LINE_HEIGHT = 0.3;
	public static final double Y_OFFSET = 2;
	public static final String PLAYER_QUOTE = Pattern.quote("%PLAYER%");
	
	private Map<Integer, Hologram> holograms = new HashMap<>();
	
	public class Hologram {
		private Document data;
		private List<ArmorStand> parts;
		
		public Hologram(Document data) {
			this.data = data;
			this.parts = new ArrayList<>();
			holograms.put(getId(), this);
		}
		
		public Hologram(Location loc, String[] lines) {
			int id = HologramLoader.this.counter.reserveNextId(HologramLoader.this.counterName);
			Document data = new Document("_id", id).append("loc", StorageUtil.locToDoc(loc)).append("lines", List.of(lines))
					.append("action", "none");
			HologramLoader.this.collection.insertOne(data);
			this.data = data;
			this.parts = new ArrayList<>();
			holograms.put(id, this);
		}
		
		public void runActions(User user) {
			String action = data.getString("action");
			if(action.equals("cmd")) {
				Bukkit.dispatchCommand(user.getPlayer(), data.getString("cmd").replaceAll(PLAYER_QUOTE, user.getName()));
			}
			else if(action.equals("ccmd")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), data.getString("cmd").replaceAll(PLAYER_QUOTE, user.getName()));
			}
		}
		
		public void setNoAction() {
			data.append("action", "none");
			save();
		}
		
		public void setCmdAction(String playerCmd) {
			data.append("action", "cmd");
			data.append("cmd", playerCmd);
			save();
		}
		
		public void setCcmdAction(String consoleCmd) {
			data.append("action",  "ccmd");
			data.append("cmd", consoleCmd);
			save();
		}
		
		public String describeActions() {
			String action = data.getString("action");
			if(action.equals("none")) {
				return "None";
			}
			else if(action.equals("cmd")) {
				return "Player command: " + data.getString("cmd");
			}
			else if(action.equals("ccmd")) {
				return "Console command: " + data.getString("cmd");
			}
			else {
				return "Unknown";
			}
		}
		
		public int getId() {
			return data.getInteger("_id");
		}
		
		public Location getLocation() {
			return StorageUtil.docToLoc(data.get("loc", Document.class));
		}
		
		public String[] getText() {
			return data.getList("lines", String.class).toArray(new String[] {});
		}
		
		public void spawn() {
			String[] lines = getText();
			for(int i = 0; i < lines.length; i++) {
				parts.add(HologramUtil.clickableHologram(StringUtil.colorize(lines[i]), getLocation().clone().subtract(0, LINE_HEIGHT * i, 0), u -> {
					boolean gm = PermissionUtil.verifyActivePermissionLevel(u, PermissionLevel.GM, false);
					if(gm) {
						Bukkit.dispatchCommand(u.getPlayer(), "hologram " + getId() + " info");
						u.getPlayer().spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GREEN + "[Run Actions]", "/holo " + getId() + " runactions", 
								"Click to run associated actions for this hologram"));
					}
					else {
						runActions(u);
					}
				}));
			}
		}
		
		public void destroy() {
			for(ArmorStand part : parts) {
				part.remove();
			}
			parts.clear();
		}
		
		private void refresh() {
			destroy();
			spawn();
		}
		
		private void save() {
			HologramLoader.this.collection.updateOne(new Document("_id", data.getInteger("_id")), new Document("$set", data));
		}
		
		public void setText(String[] lines) {
			data.append("lines", List.of(lines));
			refresh();
			save();
		}
		
		public void setLocation(Location location) {
			data.append("loc", StorageUtil.locToDoc(location));
			refresh();
			save();
		}
	}
	
	public HologramLoader(MongoConfig config) {
		super(config, "holograms", "holograms");
		for(Document data : collection.find()) {
			Hologram h = new Hologram(data);
			h.spawn();
		}
	}
	
	public void spawnAll() {
		for(Hologram hologram : holograms.values()) {
			hologram.spawn();
		}
	}
	
	public void destroyAll() {
		for(Hologram hologram : holograms.values()) {
			hologram.destroy();
		}
	}
	
	public Collection<Hologram> getAllHolograms() {
		return holograms.values();
	}

	public Hologram getHologram(int id) {
		return holograms.get(id);
	}
	
	public void deleteHologram(Hologram hologram) {
		hologram.destroy();
		holograms.remove(hologram.getId());
		collection.deleteOne(new Document("_id", hologram.getId()));
	}
	
}
