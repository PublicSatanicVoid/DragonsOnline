package mc.dragons.core.gameobject.user.punishment;

import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;

/**
 * Types of punishments that can be applied to a user.
 * 
 * @author Adam
 *
 */
public enum PunishmentType {
	
	/**
	 * The user will be blocked from joining until
	 * the punishment expires.
	 */
	BAN("ban", true, SystemProfileFlag.MODERATION),
	
	/**
	 * The user will be blocked from sending messages
	 * until the punishment expires.
	 */
	MUTE("mute", true, SystemProfileFlag.MODERATION),
	
	/**
	 * The user will be kicked from the server but will
	 * be able to rejoin.
	 */
	KICK("kick", false, SystemProfileFlag.HELPER),
	
	/**
	 * The user will be issued a warning privately.
	 */
	WARNING("warn", false, SystemProfileFlag.HELPER);

	private String dataHeader;
	private boolean hasDuration;
	private SystemProfileFlag requiredFlag;

	PunishmentType(String dataHeader, boolean hasDuration, SystemProfileFlag requiredFlagToApply) {
		this.dataHeader = dataHeader;
		this.hasDuration = hasDuration;
		requiredFlag = requiredFlagToApply;
	}

	/**
	 * 
	 * @return The field for this active punishment information in user data.
	 */
	public String getDataHeader() {
		return dataHeader;
	}

	/**
	 * 
	 * @return Whether this punishment can be applied for a duration of time.
	 */
	public boolean hasDuration() {
		return hasDuration;
	}

	/**
	 * 
	 * @return The system profile flag a staff member must have to apply
	 * this punishment.
	 */
	public SystemProfileFlag getRequiredFlagToApply() {
		return requiredFlag;
	}

	public static PunishmentType fromDataHeader(String header) {
		for(PunishmentType type : values()) {
			if(type.getDataHeader().equalsIgnoreCase(header)) {
				return type;
			}
		}
		return null;
	}
}