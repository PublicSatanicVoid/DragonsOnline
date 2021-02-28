package mc.dragons.core.gameobject.user.punishment;

import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;

public enum PunishmentType {
	BAN("ban", true, SystemProfileFlag.MODERATION),
	MUTE("mute", true, SystemProfileFlag.MODERATION),
	KICK("kick", false, SystemProfileFlag.HELPER),
	WARNING("warn", false, SystemProfileFlag.HELPER);

	private String dataHeader;
	private boolean hasDuration;
	private SystemProfileFlag requiredFlag;

	PunishmentType(String dataHeader, boolean hasDuration, SystemProfileFlag requiredFlagToApply) {
		this.dataHeader = dataHeader;
		this.hasDuration = hasDuration;
		requiredFlag = requiredFlagToApply;
	}

	public String getDataHeader() {
		return dataHeader;
	}

	public boolean hasDuration() {
		return hasDuration;
	}

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