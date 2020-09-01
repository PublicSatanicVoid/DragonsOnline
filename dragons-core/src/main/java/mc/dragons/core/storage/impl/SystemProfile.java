package mc.dragons.core.storage.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;

import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;

public class SystemProfile {
	private String profileName;
	private String passwordHash;
	private PermissionLevel maxPermissionLevel;
	private SystemProfileFlags flags;
	private boolean active;
	private User currentUser;

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
				document.append(flag.toString(), Boolean.valueOf(false));
			}
			return document;
		}

		public SystemProfileFlags(Document flags) {
			this.flags = new HashMap<>();
			for (Entry<String, Object> entry : (Iterable<Entry<String, Object>>) flags.entrySet())
				this.flags.put(SystemProfileFlag.valueOf(entry.getKey()), Boolean.valueOf(((Boolean) entry.getValue()).booleanValue()));
		}

		public boolean hasFlag(SystemProfileFlag flag) {
			return this.flags.getOrDefault(flag, Boolean.valueOf(false)).booleanValue();
		}

		public void setLocalFlag(SystemProfileFlag flag, boolean value) {
			this.flags.put(flag, Boolean.valueOf(value));
		}

		@Override
		public String toString() {
			String result = "";
			for (Entry<SystemProfileFlag, Boolean> flag : this.flags.entrySet())
				result = String.valueOf(result) + flag.getKey().getName() + "(" + flagToAccess(flag.getValue().booleanValue()) + ") ";
			return result.trim();
		}
	}

	public SystemProfile(User currentUser, String profileName, String passwordHash, PermissionLevel maxPermissionLevel, SystemProfileFlags flags, boolean active) {
		this.profileName = profileName;
		this.passwordHash = passwordHash;
		this.maxPermissionLevel = maxPermissionLevel;
		this.flags = flags;
		this.active = active;
		this.currentUser = currentUser;
	}

	public String getProfileName() {
		return this.profileName;
	}

	public String getPasswordHash() {
		return this.passwordHash;
	}

	public void setLocalPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public PermissionLevel getMaxPermissionLevel() {
		return this.maxPermissionLevel;
	}

	public void setLocalMaxPermissionLevel(PermissionLevel level) {
		this.maxPermissionLevel = level;
	}

	public SystemProfileFlags getFlags() {
		return this.flags;
	}

	public boolean isActive() {
		return this.active;
	}

	public void setLocalActive(boolean active) {
		this.active = active;
	}

	public User getCurrentUser() {
		return this.currentUser;
	}

	public void setLocalCurrentUser(User user) {
		this.currentUser = user;
	}
}
