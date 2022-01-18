package mc.dragons.tools.moderation.punishment;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.bson.Document;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.StringUtil;

/**
 * Record about a punishment applied to a user.
 * 
 * @author Adam
 *
 */
public class PunishmentData {
	private static UserLoader userLoader = GameObjectType.USER.getLoader();
	
	private Document data;
	
	public PunishmentData(Document data) {
		this.data = data;
	}

	public PunishmentType getType() {
		return PunishmentType.valueOf(data.getString("type"));
	}

	public PunishmentCode getCode() {
		return PunishmentCode.valueOf(data.getString("code"));
	}
	
	public int getStandingLevelChange() {
		return data.getInteger("standingLevelChange");
	}
	
	public String getExtra() {
		return data.getString("extra");
	}
	
	public String getReason() {
		return PunishmentCode.formatReason(getCode(), getExtra());
	}

	public Date getExpiry() {
		return new Date(1000L * (data.getLong("issuedOn") + getDuration()));
	}
	
	public String getTimeToExpiry() {
		return StringUtil.parseSecondsToTimespan(getExpiry().toInstant().getEpochSecond() - Instant.now().getEpochSecond());
	}
	
	public Date getIssuedDate() {
		return new Date(1000L * data.getLong("issuedOn"));
	}

	public User getIssuedBy() {
		return userLoader.loadObject(data.get("issuedBy", UUID.class));
	}
	
	public long getDuration() {
		return data.getLong("duration");
	}
	
	public boolean isRevoked() {
		return data.getBoolean("revoked", false);
	}

	public RevocationCode getRevocationCode() {
		return RevocationCode.valueOf(data.getString("revokedCode"));
	}
	
	public User getRevokedBy() {
		return userLoader.loadObject(data.get("revokedBy", UUID.class));
	}
	
	public boolean hasExpired() {
		if(isPermanent()) return false;
		return getExpiry().before(new Date());
	}
	
	public boolean isPermanent() {
		return getDuration() == -1L;
	}
	
	public boolean isWarningAcknowledged() {
		return data.getBoolean("acknowledged", false);
	}
	
	public static PunishmentData fromDocument(Document data) {
		if(data == null) return null;
		PunishmentData pdata = new PunishmentData(data);
		return pdata;
	}
}