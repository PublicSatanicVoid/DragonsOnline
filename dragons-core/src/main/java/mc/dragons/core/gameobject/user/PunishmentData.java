package mc.dragons.core.gameobject.user;

import java.util.Date;

public class PunishmentData {
	private PunishmentType type;
	private String reason;
	private Date expiry;
	public boolean permanent;

	public PunishmentData(PunishmentType type, String reason, Date expiry, boolean permanent) {
		this.type = type;
		this.reason = reason;
		this.expiry = expiry;
		this.permanent = permanent;
	}

	public PunishmentType getType() {
		return type;
	}

	public String getReason() {
		return reason;
	}

	public Date getExpiry() {
		return expiry;
	}

	public boolean isPermanent() {
		return permanent;
	}
}