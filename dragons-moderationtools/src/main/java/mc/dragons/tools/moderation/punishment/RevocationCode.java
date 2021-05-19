package mc.dragons.tools.moderation.punishment;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;

public enum RevocationCode {
	WRONG_PLAYER("WP", "Wrong player", SystemProfileFlag.HELPER),
	WRONG_REASON("WR", "Wrong reason", SystemProfileFlag.HELPER),
	ISSUE_RESOLVED("IR", "Issue resolved", SystemProfileFlag.APPEALS_TEAM),
	SECOND_CHANCE("SC", "Second chance", SystemProfileFlag.APPEALS_TEAM),
	INSUFFICIENT_EVIDENCE("IE", "Insufficient evidence", SystemProfileFlag.APPEALS_TEAM);
	
	private String code;
	private String description;
	private SystemProfileFlag permission;
	
	RevocationCode(String code, String description, SystemProfileFlag permission) {
		this.code = code;
		this.description = description;
		this.permission = permission;
	}
	
	public String getCode() {
		return code;
	}
	
	public String getDescription() {
		return description;
	}
	
	public SystemProfileFlag getPermissionToApply() {
		return permission;
	}
	
	public static RevocationCode getByCode(String code) {
		for(RevocationCode pc : values()) {
			if(pc.getCode().equalsIgnoreCase(code)) {
				return pc;
			}
		}
		return null;
	}
	
	public static RevocationCode parseCode(CommandSender sender, String code) {
		RevocationCode result = getByCode(code);
		if(result == null) {
			sender.sendMessage(ChatColor.RED + "Invalid punishment revocation code! Valid codes are:");
			for(RevocationCode pc : values()) {
				sender.sendMessage(" " + pc.getCode() + ChatColor.GRAY + " - " + pc.getDescription());
			}
		}
		return result;
	}
}
