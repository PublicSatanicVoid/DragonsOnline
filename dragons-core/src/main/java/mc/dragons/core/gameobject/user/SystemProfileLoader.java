package mc.dragons.core.gameobject.user;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.storage.loader.AbstractLightweightLoader;

public class SystemProfileLoader extends AbstractLightweightLoader<SystemProfile> {
	private static String saltString;

	private Set<SystemProfile> profiles;
	private Logger LOGGER;
	
	static {
		saltString = Dragons.getInstance().getConfig().getString("db.mongo.password-salt-string");
	}

	public SystemProfileLoader(Dragons instance) {
		super(instance.getMongoConfig(), "#unused#", "sysprofiles");
		this.LOGGER = instance.getLogger();
		this.profiles = new HashSet<>();
	}
	
	public static String passwordHash(String password) {
		try {
			return (new BigInteger(1, MessageDigest.getInstance("SHA-256").digest((saltString + password).getBytes(StandardCharsets.UTF_8)))).toString(16);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return "SHA256HashFailedNoSuchAlgorithmException";
		}
	}
	
	public SystemProfile authenticateProfile(User user, String profileName, String profilePassword) {
		if (!isAvailable(profileName, user.getName()))
			return null;
		SystemProfile systemProfile = loadProfile(profileName);
		if (systemProfile == null)
			return null;
		if (!systemProfile.isActive())
			return null;
		if (!systemProfile.getPasswordHash(user.getUUID()).equals(passwordHash(profilePassword)))
			return null;
		systemProfile.setLocalCurrentUser(user);
		this.LOGGER.info(String.valueOf(user.getName()) + " logged into system profile " + profileName);
		return systemProfile;
	}

	public SystemProfile loadProfile(String profileName) {
		LOGGER.fine("Loading profile " + profileName);
		for (SystemProfile systemProfile : this.profiles) {
			if (systemProfile.getProfileName().equalsIgnoreCase(profileName))
				return systemProfile;
		}
		LOGGER.fine("-Profile was not found in cache, fetching from mongo");
		Document profile = this.collection.find(new Document("profileName", profileName)).first();
		if (profile == null)
			return null;
		Document flags = profile.get("flags", Document.class);
		@SuppressWarnings("unchecked")
		Map<String, String> hashesRaw = (Map<String, String>) profile.get("profilePasswordHashes");
		Map<UUID, String> hashes = new HashMap<>();
		hashesRaw.forEach((k, v) -> hashes.put(UUID.fromString(k), v));
		SystemProfile systemProfile = new SystemProfile(null, profileName, hashes, PermissionLevel.valueOf(profile.getString("maxPermissionLevel")),
				new SystemProfile.SystemProfileFlags(flags), profile.getBoolean("active").booleanValue());
		this.profiles.add(systemProfile);
		return systemProfile;
	}

	public void migrateProfile(String profileName, String ownerUsername) {
		Document profile = this.collection.find(new Document("profileName", profileName)).first();
		String hash = profile.getString("profilePasswordHash");
		profile.remove("profilePasswordHash");
		UserLoader.uuidFromUsername(ownerUsername, ownerUUID -> {
			profile.append("profilePasswordHashes", new Document(ownerUUID.toString(), hash));
			this.collection.updateOne(new Document("profileName", profileName), new Document("$set", profile));
			profiles.removeIf(p -> p.getProfileName().equalsIgnoreCase(profileName));
			loadProfile(profileName);	
		});
	}
	
	public void registerAlt(String profileName, String username, String password) {
		Document profile = this.collection.find(new Document("profileName", profileName)).first();
		String hash = passwordHash(password);
		UserLoader.uuidFromUsername(username, uuid -> {
			profile.get("profilePasswordHashes", Document.class).append(uuid.toString(), hash);
			loadProfile(profileName).setLocalPasswordHash(uuid, hash);
			this.collection.updateOne(new Document("profileName", profileName), new Document("$set", profile));
		});
	}
	
	public void unregisterAlt(String profileName, String username) {
		Document profile = this.collection.find(new Document("profileName", profileName)).first();
		UserLoader.uuidFromUsername(username, uuid -> {
			profile.get("profilePasswordHashes", Document.class).remove(uuid.toString());
			loadProfile(profileName).removeLocalPasswordHash(uuid);
			this.collection.updateOne(new Document("profileName", profileName), new Document("$set", profile));
		});
	}
	
	private void kickProfileLocally(String profileName) {
		String currentUser = getCurrentUser(profileName);
		if (currentUser.equals(""))
			return;
		Player player = Bukkit.getPlayerExact(currentUser);
		player.kickPlayer("Your system profile changed, relog for updated permissions.");
		logoutProfile(profileName);
	}

	public String getCurrentUser(String profileName) {
		Document profile = this.collection.find(new Document("profileName", profileName)).first();
		if (profile == null)
			return "";
		return profile.getString("currentUser");
	}

	public boolean isAvailable(String profileName, String testUser) {
		String currentUser = getCurrentUser(profileName);
		if (!currentUser.equals(testUser) && !currentUser.equals(""))
			return false;
		return true;
	}

	public void setActive(String profileName, boolean active) {
		this.collection.updateOne(new Document("profileName", profileName), new Document("$set", new Document("active", active)));
		loadProfile(profileName).setLocalActive(active);
		if (!active)
			kickProfileLocally(profileName);
	}

	public void createProfileUUID(String profileName, UUID owner, String profilePassword, PermissionLevel permissionLevel) {
		LOGGER.fine("-createProfileUUID("+profileName+","+owner+",***,"+permissionLevel);
		Map<String, String> hashes = new HashMap<>();
		LOGGER.fine("--hashes="+hashes);
		hashes.put(owner.toString(), passwordHash(profilePassword));
		LOGGER.fine("--hashes.size="+hashes.size());
		this.collection.insertOne((new Document("profileName", profileName)).append("profilePasswordHashes", hashes).append("maxPermissionLevel", permissionLevel.toString())
				.append("flags", SystemProfile.SystemProfileFlags.emptyFlagsDocument()).append("currentUser", "").append("active", true));
		LOGGER.fine("--done");
	}
	
	public void createProfile(String profileName, String owner, String profilePassword, PermissionLevel permissionLevel) {
		UserLoader.uuidFromUsername(owner, uuid -> {
			createProfileUUID(profileName, uuid, profilePassword, permissionLevel);
		});
	}

	public void setProfileMaxPermissionLevel(String profileName, PermissionLevel newMaxPermissionLevel) {
		this.collection.updateOne(new Document("profileName", profileName), new Document("$set", new Document("maxPermissionLevel", newMaxPermissionLevel.toString())));
		loadProfile(profileName).setLocalMaxPermissionLevel(newMaxPermissionLevel);
		kickProfileLocally(profileName);
		this.LOGGER.info("Max permission level of " + profileName + " changed to " + newMaxPermissionLevel);
	}

	public void setProfileFlag(String profileName, String flagName, boolean flagValue) {
		this.collection.updateOne(new Document("profileName", profileName), new Document("$set", new Document("flags." + flagName, Boolean.valueOf(flagValue))));
		SystemProfile.SystemProfileFlags flags = loadProfile(profileName).getFlags();
		flags.setLocalFlag(SystemProfile.SystemProfileFlags.SystemProfileFlag.valueOf(flagName), flagValue);
		kickProfileLocally(profileName);
		this.LOGGER.info("Profile flag " + flagName + " of " + profileName + " set to " + flagValue);
	}

	public void setProfilePassword(String profileName, UUID uuid, String newPassword) {
		String hash = passwordHash(newPassword);
		@SuppressWarnings("unchecked")
		Map<UUID, String> hashes = (Map<UUID, String>) this.collection.find(new Document("profileName", profileName)).first().get("profilePasswordHashes");
		hashes.put(uuid, hash);
		this.collection.updateOne(new Document("profileName", profileName), new Document("$set", new Document("profilePasswordHashes", hashes)));
		loadProfile(profileName).setLocalPasswordHash(uuid, hash);
		kickProfileLocally(profileName);
		this.LOGGER.info("Profile " + profileName + " password has changed");
	}

	public void logoutProfile(String profileName) {
		this.collection.updateOne(new Document("profileName", profileName), new Document("$set", new Document("currentUser", "")));
		loadProfile(profileName).setLocalCurrentUser(null);
		this.LOGGER.info("Profile " + profileName + " was logged out");
	}
}
