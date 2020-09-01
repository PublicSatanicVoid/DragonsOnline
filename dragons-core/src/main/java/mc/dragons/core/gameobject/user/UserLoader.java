package mc.dragons.core.gameobject.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectLoader;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import net.md_5.bungee.api.ChatColor;

public class UserLoader extends GameObjectLoader<User> {
	private static UserLoader INSTANCE;

	private static Logger LOGGER = Dragons.getInstance().getLogger();

	private static Set<User> users;

	private GameObjectRegistry masterRegistry;

	private UserLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		users = new HashSet<>();
		this.masterRegistry = instance.getGameObjectRegistry();
	}

	public static synchronized UserLoader getInstance(Dragons instance, StorageManager storageManager) {
		if (INSTANCE == null)
			INSTANCE = new UserLoader(instance, storageManager);
		return INSTANCE;
	}

	public static User fixUser(User user) {
		Player oldPlayer = user.getPlayer();
		Player newPlayer = Bukkit.getPlayerExact(user.getName());
		if (oldPlayer != newPlayer) {
			newPlayer.sendMessage(ChatColor.YELLOW + "" + ChatColor.ITALIC + "Reloading your user profile...");

			user.initialize(newPlayer);
			users.add(user);
			assign(newPlayer, user);
		}
		return user;
	}

	@Override
	public User loadObject(StorageAccess storageAccess) {
		LOGGER.finest("Loading user by storage access " + storageAccess.getIdentifier());
		for (User user1 : users) {
			if (user1.getIdentifier().equals(storageAccess.getIdentifier())) {
				LOGGER.finest(" - Found user in cache, fixing and returning");
				return fixUser(user1);
			}
		}
		Player p = this.plugin.getServer().getPlayer((UUID) storageAccess.get("_id"));
		if (p == null)
			LOGGER.warning("Attempting to load user with an offline or nonexistent player (" + storageAccess.getIdentifier() + ")");
		User user = new User(p, this.storageManager, storageAccess);
		assign(p, user);
		users.add(user);
		this.masterRegistry.getRegisteredObjects().add(user);
		return user;
	}

	public User loadObject(UUID uuid) {
		LOGGER.finest("Loading user by UUID " + uuid);
		for (User user : users) {
			LOGGER.finest("-Checking user " + user + ": " + user.getUUID() + " vs " + uuid + " (eq: " + user.getUUID().equals(uuid) + ")");
			if (user.getUUID().equals(uuid)) {
				LOGGER.finest(" - Found user in cache, fixing and returning");
				return fixUser(user);
			}
		}
		StorageAccess storageAccess = this.storageManager.getStorageAccess(GameObjectType.USER, uuid);
		if (storageAccess == null)
			return null;
		return loadObject(storageAccess);
	}

	public User loadObject(String username) {
		LOGGER.fine("Loading user by username " + username);
		for (User user : users) {
			if (user.getName().equalsIgnoreCase(username)) {
				LOGGER.finer(" - Found user in cache, fixing and returning");
				return fixUser(user);
			}
		}
		StorageAccess storageAccess = this.storageManager.getStorageAccess(GameObjectType.USER, new Document("username", username));
		if (storageAccess == null)
			return null;
		return loadObject(storageAccess);
	}

	public User registerNew(Player player) {
		LOGGER.fine("Registering new user " + player.getName());
		Document skills = new Document();
		Document skillProgress = new Document();
		for(SkillType skill : SkillType.values()) {
			skills.append(skill.toString(), Integer.valueOf(0));
			skillProgress.append(skill.toString(), Double.valueOf(0.0D));
		}
		Document data = (new Document("_id", player.getUniqueId())).append("username", player.getName())
				.append("maxHealth", Double.valueOf(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())).append("xp", Integer.valueOf(0)).append("level", Integer.valueOf(1))
				.append("rank", Rank.DEFAULT.toString()).append("gold", Double.valueOf(0.0D)).append("godMode", Boolean.valueOf(false)).append("firstJoined", Long.valueOf(System.currentTimeMillis()))
				.append("lastJoined", Long.valueOf(System.currentTimeMillis())).append("lastSeen", Long.valueOf(System.currentTimeMillis())).append("skills", skills)
				.append("skillProgress", skillProgress).append("inventory", new Document()).append("quests", new Document()).append("vanished", Boolean.valueOf(false))
				.append("punishmentHistory", new ArrayList<>()).append("chatChannels", new ArrayList<>(Arrays.asList(new String[] { ChatChannel.LOCAL.toString() })))
				.append("speakingChannel", ChatChannel.LOCAL.toString()).append("gamemode", GameMode.ADVENTURE.toString()).append("lastReadChangeLog", Integer.valueOf(0))
				.append("ip", player.getAddress().getAddress().getHostAddress()).append("totalOnlineTime", Long.valueOf(0L));
		StorageAccess storageAccess = this.storageManager.getNewStorageAccess(GameObjectType.USER, data);
		User user = new User(player, this.storageManager, storageAccess);
		assign(player, user);
		users.add(user);
		this.masterRegistry.getRegisteredObjects().add(user);
		return user;
	}

	public static void assign(Player player, User user) {
		LOGGER.fine("Assigning player " + player + " to user " + user);
		if (player != null) {
			player.removeMetadata("handle", Dragons.getInstance());
			player.setMetadata("handle", new FixedMetadataValue(Dragons.getInstance(), user));
		}
		user.setPlayer(player);
	}

	public static User fromPlayer(Player player) {
		if (player == null)
			return null;
		if (!player.hasMetadata("handle"))
			return null;
		if (player.getMetadata("handle").size() == 0)
			return null;
		Object value = player.getMetadata("handle").get(0).value();
		if (value instanceof User)
			return (User) value;
		return null;
	}

	public void removeStalePlayer(Player player) {
		LOGGER.fine("Removing stale player " + player.getName());
		User user = fromPlayer(player);
		this.masterRegistry.getRegisteredObjects().remove(user);
		users.remove(user);
	}

	public void unregister(User user) {
		LOGGER.fine("Locally unregistering player " + user.getName());
		this.masterRegistry.getRegisteredObjects().remove(user);
		users.remove(user);
	}

	public static Collection<User> allUsers() {
		return users;
	}
}
