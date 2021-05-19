package mc.dragons.tools.moderation.punishment;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;

public enum PunishmentCode {
	
	/* Language */
	RUDE("RU", "Rude", "Being disrespectful or rude to another server member", StandingLevelType.MUTE, SystemProfileFlag.HELPER, 1, false),
	SPAM("SP", "Spamming", "Repeatedly sending irrelevant messages to disturb the chat", StandingLevelType.MUTE, SystemProfileFlag.HELPER, 1, false),
	ADVERTISING("AD", "Advertising", "Intentionally advertising unauthorized servers, giveaways, or other media", StandingLevelType.MUTE, SystemProfileFlag.HELPER, 1, false),
	MISLEADING("MI", "Misleading", "Misleading others to disrupt gameplay", StandingLevelType.MUTE, SystemProfileFlag.HELPER, 1, false),
	INSTIGATION_1("IN1", "Instigation 1", "Promoting rule breaking to benefit yourself or others", StandingLevelType.MUTE, SystemProfileFlag.HELPER, 2, false),
	SWEARING("SW", "Swearing", "Targeted use of vulgar or explicit phrases", StandingLevelType.MUTE, SystemProfileFlag.HELPER, 2, false),
	IMPERSONATION("IM", "Impersonation", "Impersonating a staff member or another player", StandingLevelType.MUTE, SystemProfileFlag.HELPER, 2, false),
	DISCRIMINATION("DI", "Discrimination", "Discrimination against another user or group of users", StandingLevelType.MUTE, SystemProfileFlag.HELPER, 3, false),
	INAPPROPRIATE_1("IC1", "Inappropriate 1", "Using or discussing inappropriate concepts in the server chat", StandingLevelType.MUTE, SystemProfileFlag.HELPER, 3, false),
	THREAT_HARM("TH", "Threatening/Harmful", "Sending threatening or harmful messages", StandingLevelType.MUTE, SystemProfileFlag.HELPER, 4, false),
	OFFENSIVE_POLITICS("OP", "Offensive Political Speech", "Offensive political speech", StandingLevelType.MUTE, SystemProfileFlag.HELPER, 4, false),
	INAPPROPRIATE_2("IC2", "Inappropriate 2", "Using or distributing inappropriate concepts or websites", StandingLevelType.MUTE, SystemProfileFlag.HELPER, 4, false),
	INSTIGATION_2("IN2", "Instigation 2", "Distributing or advertising the use of blacklisted modifications", StandingLevelType.MUTE, SystemProfileFlag.HELPER, 4, false),
	DOXXING("DOX", "Doxxing", "Revealing or threatening to reveal sensitive information about the server or another player", StandingLevelType.BAN, SystemProfileFlag.MODERATION, 10, false),
	
	/* Content */
	INAPPROPRIATE_AESTHETICS("IA", "Inappropriate Aesthetics", "Using companions, pets, or items in an inappropriate way", StandingLevelType.BAN, SystemProfileFlag.MODERATION, 1, false),
	DISRUPTIVE_GAMEPLAY("DG", "Disruptive Gameplay", "Using game mechanics to disrupt other players' experience", StandingLevelType.BAN, SystemProfileFlag.MODERATION, 1, false),
	BAD_SKIN("BS", "Bad Skin", "Using inappropriate capes or skins on the server", StandingLevelType.BAN, SystemProfileFlag.MODERATION, 10, false),
	BAD_NAME("BN", "Bad Name", "Your account name is not allowed on the server.", StandingLevelType.BAN, SystemProfileFlag.MODERATION, 10, false),
	STAT_BOOST("SB", "Stat Boost", "Boosting your account to improve your stats", StandingLevelType.BAN, SystemProfileFlag.MODERATION, 2, false),
	EXPLOITING_1("EX1", "Exploiting 1", "Exploiting gameplay features for personal advantage", StandingLevelType.BAN, SystemProfileFlag.MODERATION, 2, false),
	EXPLOITING_2("EX2", "Exploiting 2", "Exploiting gameplay features to affect other players", StandingLevelType.BAN, SystemProfileFlag.MODERATION, 3, false),
	CHEATING("CH", "Cheating", "Cheating through the use of blacklisted client modifications", StandingLevelType.BAN, SystemProfileFlag.MODERATION, 4, false),
	CHEATING_WARNING("CHW", "Cheating Warning", "Detected use of blacklisted client modifications. Continued use will result in a ban", StandingLevelType.BAN, SystemProfileFlag.MODERATION, 0, true),
	COMPROMISED("BOT", "Compromised", "Your account is compromised or stolen. Please create an appeal after taking the necessary steps to secure your account", StandingLevelType.BAN, SystemProfileFlag.MODERATION, 10, false),
	SPECIAL_COMPROMISED("SBOT", "Special Compromised", "Please contact a staff manager for further information", StandingLevelType.BAN, SystemProfileFlag.MODERATION, 10, false),
	AC_BAN("ACB", "Anticheat Ban", "Cheating through the use of blacklisted client modifications [DAC]", StandingLevelType.BAN, PermissionLevel.ADMIN, 4, true),
	STAFF_ABUSE("SA", "Staff Abuse", "Abuse of staff privileges", StandingLevelType.BAN, PermissionLevel.ADMIN, 10, false),
	BAD_DEV("BD", "Bad Developer", "You are a terrible developer", StandingLevelType.BAN, PermissionLevel.ADMIN, 0, true), // This is a self-own
	SUSPEND("SUS", "Suspended", "Your account has been suspended", StandingLevelType.BAN, SystemProfileFlag.MODERATION, 10, false);
	
	private String code;
	private String name;
	private String description;
	private StandingLevelType type;
	private SystemProfileFlag flag;
	private PermissionLevel permission;
	private int standingLevel;
	private boolean hidden;
	
	PunishmentCode(String code, String name, String description, StandingLevelType type, SystemProfileFlag flag, int standingLevel, boolean hidden) {
		this.code = code;
		this.name = name;
		this.description = description;
		this.type = type;
		this.flag = flag;
		this.standingLevel = standingLevel;
		this.hidden = hidden;
	}
	
	PunishmentCode(String code, String name, String description, StandingLevelType type, PermissionLevel permission, int standingLevel, boolean hidden) {
		this.code = code;
		this.name = name;
		this.description = description;
		this.type = type;
		this.permission = permission;
		this.standingLevel = standingLevel;
		this.hidden = hidden;
	}
	
	public String getCode() {
		return code;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public boolean isHidden() {
		return hidden;
	}
	
	public StandingLevelType getType() {
		return type;
	}
	
	public SystemProfileFlag getRequiredFlagToApply() {
		if(flag == null) {
			return SystemProfileFlag.HELPER;
		}
		return flag;
	}
	
	public PermissionLevel getRequiredPermissionToApply() {
		if(permission == null) {
			return PermissionLevel.HELPER;
		}
		return permission;
	}
	
	public int getStandingLevel() {
		return standingLevel;
	}
	
	public static PunishmentCode getByCode(String code) {
		for(PunishmentCode pc : values()) {
			if(pc.getCode().equalsIgnoreCase(code)) {
				return pc;
			}
		}
		return null;
	}
	
	public static PunishmentCode parseCode(CommandSender sender, String code) {
		PunishmentCode result = getByCode(code);
		if(result == null) {
			sender.sendMessage(ChatColor.RED + "Invalid punishment code! Valid codes are:");
			for(PunishmentCode pc : values()) {
				if(pc.isHidden()) continue;
				sender.sendMessage(" " + pc.getCode() + ChatColor.GRAY + " - " + pc.getName() + " (Level " + pc.getStandingLevel() + ")");
			}
		}
		return result;
	}
	
	public static String formatReason(PunishmentCode code, String extra) {
		return code.getDescription() + (extra == null || extra.isBlank() ? "" : " (" + extra + ")");
	}
}
