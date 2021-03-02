package mc.dragons.core.gameobject.user.punishment;

import java.time.Instant;
import java.util.Date;

import org.bson.Document;

public class PunishmentData {
	private PunishmentType type;
	private String reason;
	private Date expiry;
	private boolean permanent;

	public PunishmentData(PunishmentType type, String reason, Date expiry, boolean permanent) {
		this.type = type;
		this.reason = reason;
		this.expiry = expiry;
		this.permanent = permanent;
	}
	
	private PunishmentData(Document data) {
		type = PunishmentType.valueOf(data.getString("type"));
		reason = data.getString("reason");
		long duration = data.getLong("duration");
		long banDate = data.getLong("banDate");
		expiry = new Date(1000L * (banDate + duration));
		permanent = duration == -1L;
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
	
	public static PunishmentData fromDocument(Document data) {
		if(data == null) return null;
		PunishmentData pdata = new PunishmentData(data);
		if(!pdata.isPermanent() && pdata.expiry.before(Date.from(Instant.now()))) return null;
		return pdata;
	}
}