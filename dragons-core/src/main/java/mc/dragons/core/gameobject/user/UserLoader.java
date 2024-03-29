package mc.dragons.core.gameobject.user;

import static mc.dragons.core.util.BukkitUtil.sync;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.common.base.Charsets;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectLoader;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.chat.ChatChannel;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.storage.loader.GlobalVarLoader;
import mc.dragons.core.storage.mongo.MongoStorageManager;
import mc.dragons.core.util.singletons.Singleton;
import mc.dragons.core.util.singletons.Singletons;

public class UserLoader extends GameObjectLoader<User> implements Singleton {
	private static DragonsLogger LOGGER = Dragons.getInstance().getLogger();
	private static GlobalVarLoader VAR;
	private static Set<User> users = new CopyOnWriteArraySet<>();
	private static Dragons dragons;
	
	private GameObjectRegistry masterRegistry;

	private UserLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		masterRegistry = instance.getGameObjectRegistry();
	}
	
	public static void lazyLoadGlobalVarLoader() {
		VAR = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(GlobalVarLoader.class);
	}
	
	public static UserLoader getInstance() {
		dragons = Dragons.getInstance();
		return Singletons.getInstance(UserLoader.class, () -> new UserLoader(dragons, dragons.getPersistentStorageManager()));
	}

	public static User fixUser(User user) {
		Player oldPlayer = user.getPlayer();
		Player newPlayer = Bukkit.getPlayerExact(user.getName());
		if(newPlayer == null) return user; // Offline user, so nothing to fix
		if (!newPlayer.equals(oldPlayer)) {
			LOGGER.trace("Reloading user profile of " + newPlayer.getName() + " (old=" + oldPlayer + " != " + newPlayer + ")");
			user.initialize(newPlayer);
			users.add(user);
			assign(newPlayer, user);
		}
		return user;
	}

	@Override
	public User loadObject(StorageAccess storageAccess) {
		LOGGER.trace("Loading user by storage access " + storageAccess.getIdentifier());
		for (User user : users) {
			if (user.getIdentifier().equals(storageAccess.getIdentifier())) {
				LOGGER.verbose(" - Found user in cache, fixing and returning");
				return fixUser(user);
			}
		}
		Player p = Bukkit.getPlayer(storageAccess.get("_id", UUID.class));
		if (p == null) {
			LOGGER.notice("Attempting to load offline or nonexistent user (" + storageAccess.getIdentifier() + ")");
		}
		User user = new User(p, storageManager, storageAccess);
		assign(p, user);
		users.add(user);
		masterRegistry.getRegisteredObjects().add(user);
		return user;
	}

	public User loadObject(UUID uuid) {
		LOGGER.trace("Loading user by UUID " + uuid);
		for (User user : users) {
			if (user.getUUID().equals(uuid)) {
				LOGGER.verbose(" - Found user in cache, fixing and returning");
				return fixUser(user);
			}
		}
		StorageAccess storageAccess = storageManager.getStorageAccess(GameObjectType.USER, uuid);
		if (storageAccess == null) {
			return null;
		}
		return loadObject(storageAccess);
	}

	public User loadObject(String username) {
		LOGGER.trace("Loading user by username " + username);
		for (User user : users) {
			if (user.getName().equalsIgnoreCase(username)) {
				LOGGER.verbose(" - Found user in cache, fixing and returning");
				return fixUser(user);
			}
		}
		StorageAccess storageAccess = storageManager.getStorageAccess(GameObjectType.USER, 
				new Document("username", username).append(MongoStorageManager.CASE_INSENSITIVE_SEARCH, true));
		if (storageAccess == null) {
			return null;
		}
		return loadObject(storageAccess);
	}

	public User registerNew(Player player) {
		LOGGER.trace("Registering new user " + player.getName());
		Document skills = new Document();
		Document skillProgress = new Document();
		for(SkillType skill : SkillType.values()) {
			skills.append(skill.toString(), 0);
			skillProgress.append(skill.toString(), 0.0D);
		}
		Rank rank = Rank.DEFAULT;

		if(VAR.get("autorank") == null) {
			VAR.set("autorank", new Document());
		}
		
		String autoRank = VAR.getDocument("autorank").getString(player.getUniqueId().toString());
		if(autoRank != null) {
			rank = Rank.valueOf(autoRank);
			Bukkit.getScheduler().runTaskLater(dragons, () -> {
				player.sendMessage(ChatColor.GREEN + "Your rank of " + autoRank + " was successfully applied.");
			}, 20L * 3);
			Document autoRanks = VAR.getDocument("autorank");
			autoRanks.remove(player.getUniqueId().toString());
			VAR.set("autorank", autoRanks);
		}
		Document data = new Document("_id", player.getUniqueId())
				.append("username", player.getName())
				.append("maxHealth", player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())
				.append("xp", 0)
				.append("level", 1)
				.append("rank", rank.toString())
				.append("gold", 0.0)
				.append("godMode", false)
				.append("firstJoined", System.currentTimeMillis())
				.append("lastJoined", System.currentTimeMillis())
				.append("lastSeen", System.currentTimeMillis())
				.append("skills", skills)
				.append("skillProgress", skillProgress)
				.append("inventory", new Document())
				.append("quests", new Document())
				.append("vanished", false)
				.append("punishmentHistory", new ArrayList<>())
				.append("chatChannels", new ArrayList<>(List.of(ChatChannel.LOCAL.toString(), ChatChannel.PARTY.toString(), ChatChannel.GUILD.toString())))
				.append("speakingChannel", ChatChannel.LOCAL.toString())
				.append("gamemode", GameMode.ADVENTURE.toString())
				.append("lastReadChangeLog", 0)
				.append("ip", player.getAddress().getAddress().getHostAddress())
				.append("ipHistory", new ArrayList<>(List.of(player.getAddress().getAddress().getHostAddress())))
				.append("totalOnlineTime", 0L)
				.append("currentServer", dragons.getServerName())
				.append("verified", false)
				.append("blockedUsers", new ArrayList<>());
		sync(() -> dragons.getUserHookRegistry().getHooks().forEach(h -> h.onCreateStorageAccess(data)));
		StorageAccess storageAccess = storageManager.getNewStorageAccess(GameObjectType.USER, data);
		User user = new User(player, storageManager, storageAccess);
		assign(player, user);
		users.add(user);
		masterRegistry.getRegisteredObjects().add(user);
		return user;
	}

	public static void assign(Player player, User user) {
		LOGGER.trace("Assigning player " + player + " to user " + user);
		if (player != null) {
			player.removeMetadata("handle", dragons);
			player.setMetadata("handle", new FixedMetadataValue(dragons, user));
		}
		user.setPlayer(player);
	}

	public static User fromPlayer(Player player) {
		if (player == null) {
			return null;
		}
		if (!player.hasMetadata("handle")) {
			return null;
		}
		if (player.getMetadata("handle").size() == 0) {
			return null;
		}
		Object value = player.getMetadata("handle").get(0).value();
		if (value instanceof User) {
			return (User) value;
		}
		return null;
	}

	/**
	 * Synchronously queries Mojang servers to find the UUID associated with a given username.
	 * Can be run on a separate thread to avoid blocking other operations.
	 * 
	 * @param username
	 * @return
	 */
	public static UUID uuidFromUsername(String username) {
		// https://github.com/ZerothAngel/ToHPluginUtils/blob/master/src/main/java/org/tyrannyofheaven/bukkit/util/uuid/MojangUuidResolver.java
		List<String> request = new ArrayList<>();
		request.add(username);
		String body = JSONValue.toJSONString(request);
		try {
			URL url = new URL("https://api.mojang.com/profiles/minecraft");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			connection.setUseCaches(false);
			connection.setDoOutput(true);
			connection.setConnectTimeout(15000);
			connection.setReadTimeout(15000);
			DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
			writer.write(body.getBytes(Charsets.UTF_8));
			writer.flush();
			writer.close();
			Reader reader = new InputStreamReader(connection.getInputStream());
			JSONArray profiles = (JSONArray) new JSONParser().parse(reader);
			reader.close();
			JSONObject profile = (JSONObject) profiles.get(0);
			String rawUUID = (String) profile.get("id");
			// https://stackoverflow.com/a/19399768
			String hyphenatedUUID = rawUUID.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5");
			UUID uuid = UUID.fromString(hyphenatedUUID);
			return uuid;
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Query the UUID for a given username from Mojang servers asynchronously.
	 * 
	 * @param username
	 * @param whenComplete
	 */
	public static void uuidFromUsername(String username, Consumer<UUID> whenComplete) {
		new BukkitRunnable() {
			@Override public void run() {
				whenComplete.accept(uuidFromUsername(username));
			}
		}.runTaskAsynchronously(dragons);
	}
	
	public void removeStalePlayer(Player player) {
		LOGGER.trace("Removing stale player " + player.getName());
		User user = fromPlayer(player);
		masterRegistry.getRegisteredObjects().remove(user);
		users.remove(user);
	}

	public void unregister(User user) {
		LOGGER.trace("Locally unregistering player " + user.getName());
		masterRegistry.getRegisteredObjects().remove(user);
		users.remove(user);
	}

	public static synchronized Collection<User> allUsers() {
		return users;
	}
}
