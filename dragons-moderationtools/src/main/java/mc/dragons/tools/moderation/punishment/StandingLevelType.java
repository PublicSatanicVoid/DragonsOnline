package mc.dragons.tools.moderation.punishment;

public enum StandingLevelType {
	BAN(PunishmentType.BAN),
	MUTE(PunishmentType.MUTE);
	
	private PunishmentType punishmentType;
	
	StandingLevelType(PunishmentType punishmentType) {
		this.punishmentType = punishmentType;
	}
	
	public PunishmentType getPunishmentType() {
		return punishmentType;
	}
}
