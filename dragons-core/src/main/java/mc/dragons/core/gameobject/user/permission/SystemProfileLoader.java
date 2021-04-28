package mc.dragons.core.gameobject.user.permission;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.user.Rank;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.storage.loader.AbstractLightweightLoader;

/**
 * Loads system profiles from the database and locally authenticates users.
 * 
 * TODO: Replace with a remote authentication API so that login implementation
 * is abstracted away from server view.
 * 
 * @author Adam
 *
 */
public class SystemProfileLoader extends AbstractLightweightLoader<SystemProfile> {
	private static String saltString;

	private Set<SystemProfile> profileCache;
	private DragonsLogger LOGGER;
	
	static {
		saltString = Dragons.getInstance().getConfig().getString("db.mongo.password-salt-string");
	}

	public SystemProfileLoader(Dragons instance) {
		super(instance.getMongoConfig(), "#unused#", "sysprofiles");
		LOGGER = instance.getLogger();
		profileCache = new HashSet<>();
	}
	
	public static String passwordHash(String password) {
		try {
			return new BigInteger(1, MessageDigest.getInstance("SHA-256").digest((saltString + password).getBytes(StandardCharsets.UTF_8))).toString(16);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public SystemProfile authenticateProfile(User user, String profileName, String profilePassword, UUID cid) {
		LOGGER.debug(cid, "authenticating profile " + profileName + " for user " + user.getName() + " (" + user.getUUID() + ")");
		if (!isAvailable(profileName, user.getName())) {
			LOGGER.trace(cid, "profile is already logged in, returning null");
			return null;
		}
		SystemProfile systemProfile = loadProfile(profileName);
		if (systemProfile == null) {
			LOGGER.trace(cid, "profile does not exist, returning null");
			return null;
		}
		if (!systemProfile.isActive()) {
			LOGGER.trace(cid, "profile is inactive, returning null");
			return null;
		}
		if(systemProfile.getPasswordHash(user.getUUID()) == null) {
			LOGGER.trace(cid, "user is not authorized for this profile, returning null");
			return null;
		}
		if (!systemProfile.getPasswordHash(user.getUUID()).equals(passwordHash(profilePassword))) {
			LOGGER.trace(cid, "invalid password, returning null");
			return null;
		}
		LOGGER.trace(cid, "authentication passed, logging in.");
		systemProfile.setLocalCurrentUser(user);
		LOGGER.info(user.getName() + " logged into system profile " + profileName);
		return systemProfile;
	}

	public SystemProfile loadProfile(String profileName) {
		LOGGER.trace("Loading profile " + profileName);
		for (SystemProfile systemProfile : profileCache) {
			if (systemProfile.getProfileName().equalsIgnoreCase(profileName)) {
				return systemProfile;
			}
		}
		
		LOGGER.verbose("-Profile was not found in cache, fetching from database");
		Document profile = collection.find(new Document("profileName", profileName)).first();
		if (profile == null) {
			return null;
		}
		Document flags = profile.get("flags", Document.class);
		
		@SuppressWarnings("unchecked")
		Map<String, String> hashesRaw = (Map<String, String>) profile.get("profilePasswordHashes");
		Map<UUID, String> hashes = new HashMap<>();
		hashesRaw.forEach((k, v) -> hashes.put(UUID.fromString(k), v));
		
		List<Floor> adminFloors = profile.getList("adminFloors", String.class).stream().map(name -> FloorLoader.fromFloorName(name)).collect(Collectors.toList());
		
		SystemProfile systemProfile = new SystemProfile(null, profileName, hashes, PermissionLevel.valueOf(profile.getString("maxPermissionLevel")),
				new SystemProfileFlags(flags), adminFloors, profile.getBoolean("active").booleanValue());
		profileCache.add(systemProfile);
		return systemProfile;
	}

	public List<SystemProfile> getAllProfiles() {
		return collection.find().map(d -> loadProfile(d.getString("profileName"))).into(new ArrayList<>());
	}

	public void migrateProfile(String profileName, String ownerUsername) {
		Document profile = collection.find(new Document("profileName", profileName)).first();
		String hash = profile.getString("profilePasswordHash");
		profile.remove("profilePasswordHash");
		UserLoader.uuidFromUsername(ownerUsername, ownerUUID -> {
			profile.append("profilePasswordHashes", new Document(ownerUUID.toString(), hash));
			collection.updateOne(new Document("profileName", profileName), new Document("$set", profile));
			profileCache.removeIf(p -> p.getProfileName().equalsIgnoreCase(profileName));
			loadProfile(profileName);	
		});
	}
	
	public void registerAlt(String profileName, String username, String password) {
		Document profile = collection.find(new Document("profileName", profileName)).first();
		String hash = passwordHash(password);
		UserLoader.uuidFromUsername(username, uuid -> {
			profile.get("profilePasswordHashes", Document.class).append(uuid.toString(), hash);
			loadProfile(profileName).setLocalPasswordHash(uuid, hash);
			collection.updateOne(new Document("profileName", profileName), new Document("$set", profile));
		});
	}
	
	public void unregisterAlt(String profileName, String username) {
		Document profile = collection.find(new Document("profileName", profileName)).first();
		UserLoader.uuidFromUsername(username, uuid -> {
			profile.get("profilePasswordHashes", Document.class).remove(uuid.toString());
			loadProfile(profileName).removeLocalPasswordHash(uuid);
			collection.updateOne(new Document("profileName", profileName), new Document("$set", profile));
		});
	}
	
	public void addAdminFloor(String profileName, Floor floor) {
		Document profile = collection.find(new Document("profileName", profileName)).first();
		profile.getList("adminFloors", String.class).add(floor.getFloorName());
		loadProfile(profileName).getLocalAdminFloors().add(floor);
		collection.updateOne(new Document("profileName", profileName), new Document("$set", profile));
	}
	
	public void removeAdminFloor(String profileName, Floor floor) {
		Document profile = collection.find(new Document("profileName", profileName)).first();
		profile.getList("adminFloors", String.class).remove(floor.getFloorName());
		loadProfile(profileName).getLocalAdminFloors().remove(floor);
		collection.updateOne(new Document("profileName", profileName), new Document("$set", profile));
	}
	
	private void kickProfileLocally(String profileName) {
		String currentUser = getCurrentUser(profileName);
		if (currentUser.equals("")) {
			return;
		}
		Player player = Bukkit.getPlayerExact(currentUser);
		player.kickPlayer("Your system profile changed, relog for updated permissions.");
		logoutProfile(profileName);
	}

	public String getCurrentUser(String profileName) {
		Document profile = collection.find(new Document("profileName", profileName)).first();
		if (profile == null) {
			return "";
		}
		return profile.getString("currentUser");
	}

	public boolean isAvailable(String profileName, String testUser) {
		String currentUser = getCurrentUser(profileName);
		if (!currentUser.equals(testUser) && !currentUser.equals("")) {
			return false;
		}
		return true;
	}

	public void setActive(String profileName, boolean active) {
		collection.updateOne(new Document("profileName", profileName), new Document("$set", new Document("active", active)));
		loadProfile(profileName).setLocalActive(active);
		if (!active) {
			kickProfileLocally(profileName);
		}
	}

	public void createProfileUUID(String profileName, UUID owner, String profilePassword, PermissionLevel permissionLevel) {
		Map<String, String> hashes = new HashMap<>();
		hashes.put(owner.toString(), passwordHash(profilePassword));
		collection.insertOne(new Document("profileName", profileName).append("profilePasswordHashes", hashes).append("maxPermissionLevel", permissionLevel.toString())
				.append("flags", SystemProfileFlags.emptyFlagsDocument()).append("currentUser", "").append("adminFloors", new ArrayList<>()).append("active", true)
				.append("rank", Rank.UNSPECIFIED_STAFF.toString()));
		LOGGER.info("Created new system profile " + profileName + " with max permission level " + permissionLevel);
	}
	
	public void createProfile(String profileName, String owner, String profilePassword, PermissionLevel permissionLevel) {
		UserLoader.uuidFromUsername(owner, uuid -> {
			createProfileUUID(profileName, uuid, profilePassword, permissionLevel);
		});
	}

	public void setProfileMaxPermissionLevel(String profileName, PermissionLevel newMaxPermissionLevel) {
		collection.updateOne(new Document("profileName", profileName), new Document("$set", new Document("maxPermissionLevel", newMaxPermissionLevel.toString())));
		loadProfile(profileName).setLocalMaxPermissionLevel(newMaxPermissionLevel);
		kickProfileLocally(profileName);
		LOGGER.info("Max permission level of " + profileName + " changed to " + newMaxPermissionLevel);
	}

	public void setProfileFlag(String profileName, String flagName, boolean flagValue) {
		collection.updateOne(new Document("profileName", profileName), new Document("$set", new Document("flags." + flagName, Boolean.valueOf(flagValue))));
		SystemProfileFlags flags = loadProfile(profileName).getFlags();
		flags.setLocalFlag(SystemProfileFlags.SystemProfileFlag.valueOf(flagName), flagValue);
		kickProfileLocally(profileName);
		LOGGER.info("Profile flag " + flagName + " of " + profileName + " set to " + flagValue);
	}

	public void setProfilePassword(String profileName, UUID uuid, String newPassword) {
		String hash = passwordHash(newPassword);
		
		@SuppressWarnings("unchecked")
		Map<String, String> hashesRaw = (Map<String, String>) collection.find(new Document("profileName", profileName)).first().get("profilePasswordHashes");
		hashesRaw.put(uuid.toString(), hash);
		
		collection.updateOne(new Document("profileName", profileName), new Document("$set", new Document("profilePasswordHashes", hashesRaw)));
		loadProfile(profileName).setLocalPasswordHash(uuid, hash);
		kickProfileLocally(profileName);
		
		LOGGER.info("Profile " + profileName + " password has changed");
	}

	public void logoutProfile(String profileName) {
		collection.updateOne(new Document("profileName", profileName), new Document("$set", new Document("currentUser", "")));
		loadProfile(profileName).setLocalCurrentUser(null);
		LOGGER.info("Profile " + profileName + " was logged out");
	}
}
