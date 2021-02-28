package mc.dragons.core.gameobject.user.permission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bson.Document;

import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.user.User;

/**
 * A named account tied to a staff member's identity. Permissions
 * are associated with a system profile and require a password to
 * authenticate in-game.
 * 
 * Multiple users can log into a single system profile, to account
 * for alternate accounts of staff members. However, only one user
 * can be signed in to each profile at a given time.
 * 
 * @author Adam
 *
 */
public class SystemProfile {
	private String profileName;
	
	// Each user can have a different password.
	private Map<UUID, String> passwordHashes;
	private PermissionLevel maxPermissionLevel;
	private SystemProfileFlags flags;
	private boolean active;
	private User currentUser;
	private List<Floor> adminFloors; // Worlds that the user has administrative rights in (creative mode)

	public static class SystemProfileFlags {
		private Map<SystemProfileFlag, Boolean> flags;

		public enum SystemProfileFlag {
			BUILD, TRIAL_BUILD_ONLY, WORLDEDIT, CMD, HELPER, MODERATION, TASK_MANAGER, GM_ITEM, GM_NPC, GM_QUEST, GM_REGION, GM_FLOOR, GM_DELETE;

			public String getName() {
				return toString().toLowerCase();
			}

			public static SystemProfileFlag parse(String str) {
				for(SystemProfileFlag flag : values()) {
					if(flag.getName().equalsIgnoreCase(str)) {
						return flag;
					}
				}
				return null;
			}
		}

		public static String flagToAccess(boolean flag) {
			return flag ? "YES" : "NO";
		}

		public static Document emptyFlagsDocument() {
			Document document = new Document();
			for(SystemProfileFlag flag : SystemProfileFlag.values()) {
				document.append(flag.toString(), false);
			}
			return document;
		}

		public SystemProfileFlags(Document flags) {
			this.flags = new HashMap<>();
			for (Entry<String, Object> entry : (Iterable<Entry<String, Object>>) flags.entrySet()) {
				this.flags.put(SystemProfileFlag.valueOf(entry.getKey()), (boolean) entry.getValue());
			}
		}

		public boolean hasFlag(SystemProfileFlag flag) {
			return flags.getOrDefault(flag, false);
		}

		public void setLocalFlag(SystemProfileFlag flag, boolean value) {
			flags.put(flag, value);
		}

		@Override
		public String toString() {
			String result = "";
			for (Entry<SystemProfileFlag, Boolean> flag : flags.entrySet()) {
				result = String.valueOf(result) + flag.getKey().getName() + "(" + flagToAccess(flag.getValue()) + ") ";
			}
			return result.trim();
		}
	}

	public SystemProfile(User currentUser, String profileName, Map<UUID, String> passwordHashes, PermissionLevel maxPermissionLevel, 
			SystemProfileFlags flags, List<Floor> adminFloors, boolean active) {
		this.profileName = profileName;
		this.passwordHashes = passwordHashes;
		this.maxPermissionLevel = maxPermissionLevel;
		this.flags = flags;
		this.adminFloors = adminFloors;
		this.active = active;
		this.currentUser = currentUser;
	}

	public String getProfileName() {
		return profileName;
	}

	public String getPasswordHash(UUID uuid) {
		return passwordHashes.getOrDefault(uuid, "");
	}

	public void setLocalPasswordHash(UUID uuid, String passwordHash) {
		passwordHashes.put(uuid, passwordHash);
	}

	public void removeLocalPasswordHash(UUID uuid) {
		passwordHashes.remove(uuid);
	}
	
	public Set<UUID> getAllowedUUIDs() {
		return passwordHashes.keySet();
	}

	public PermissionLevel getMaxPermissionLevel() {
		return maxPermissionLevel;
	}

	public void setLocalMaxPermissionLevel(PermissionLevel level) {
		maxPermissionLevel = level;
	}

	public SystemProfileFlags getFlags() {
		return flags;
	}
	
	public List<Floor> getLocalAdminFloors() {
		return adminFloors;
	}

	public boolean isActive() {
		return active;
	}

	public void setLocalActive(boolean active) {
		this.active = active;
	}

	public User getCurrentUser() {
		return currentUser;
	}

	public void setLocalCurrentUser(User user) {
		currentUser = user;
	}
}
