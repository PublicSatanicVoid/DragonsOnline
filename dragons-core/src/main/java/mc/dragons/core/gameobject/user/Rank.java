package mc.dragons.core.gameobject.user;

import org.bukkit.ChatColor;

/**
 * Cosmetic tags indicating a user's role or contribution to the game.
 * Only non-staff ranks are tied to actual permissions; staff permissions
 * require a secondary layer of authentication via system profiles.
 * 
 * @author Adam
 *
 */
public enum Rank {
	DEFAULT("Default", "None", "", ChatColor.GRAY, ChatColor.GRAY, false), 
	
	BETA_TESTER("Beta Tester", "Beta Tester", ChatColor.WHITE + "[TESTER]", ChatColor.GRAY, ChatColor.WHITE, false),
	
	FRIEND("Friend", "Friend", ChatColor.AQUA + "[FRIEND]", ChatColor.AQUA, ChatColor.WHITE, false),
	
	PATRON("Patron", "Patron", ChatColor.DARK_AQUA + "[PATRON]", ChatColor.AQUA, ChatColor.WHITE, false),
	PATRON_PLUS("Patron+", "Patron+", ChatColor.DARK_AQUA + "[PATRON" + ChatColor.YELLOW + "+" + ChatColor.DARK_AQUA + "]", ChatColor.AQUA, ChatColor.WHITE, false),
	INVESTOR("Investor", "Investor", ChatColor.GOLD + "[INVESTOR]", ChatColor.YELLOW, ChatColor.WHITE, false),
	
	YOUTUBE("YouTuber", "YouTuber", ChatColor.RED + "[YOU" + ChatColor.WHITE + "TUBE" + ChatColor.RED + "]", ChatColor.RED, ChatColor.WHITE, false),
	MEDIA("Media", "Media", ChatColor.DARK_PURPLE + "[MEDIA]", ChatColor.LIGHT_PURPLE, ChatColor.WHITE, false),
	
	UNSPECIFIED_STAFF("Staff", "Staff", ChatColor.YELLOW + "[STAFF]", ChatColor.YELLOW, ChatColor.WHITE, true),
	
	CONTENT_TEAM("Content Team", "Content Team", ChatColor.BLUE + "[CONTENT TEAM]", ChatColor.BLUE, ChatColor.WHITE, true),
	TRIAL_BUILDER("Trial Builder", "Trial Builder", ChatColor.WHITE + "[TRIAL BUILDER]", ChatColor.GRAY, ChatColor.WHITE, false),
	NEW_BUILDER("New Builder", "New Builder", ChatColor.BLUE + "[NEW BUILDER]", ChatColor.BLUE, ChatColor.WHITE, true),
	BUILDER("Builder", "Builder", ChatColor.BLUE + "[BUILDER]", ChatColor.BLUE, ChatColor.WHITE, true),
	CMD("Command Block Editor", "CMD", ChatColor.DARK_PURPLE + "[CMD]", ChatColor.DARK_PURPLE, ChatColor.WHITE, true),
	BUILDER_CMD("Builder + CMD", "Builder + CMD", ChatColor.BLUE + "[BUILD" + ChatColor.DARK_GRAY + "+" + ChatColor.DARK_PURPLE + "CMD" + ChatColor.BLUE + "]", ChatColor.BLUE, ChatColor.WHITE, true),
	BUILD_MANAGER("Build Manager", "Build Manager", ChatColor.BLUE + "[BUILD MGR]", ChatColor.BLUE, ChatColor.WHITE, true),
	HEAD_BUILDER("Head Builder", "Head Builder", ChatColor.BLUE + "[HEAD BUILDER]", ChatColor.BLUE, ChatColor.WHITE, true),
	
	HELPER("Helper", "Helper", ChatColor.DARK_GREEN + "[HELPER]", ChatColor.DARK_GREEN, ChatColor.WHITE, true),
	MODERATOR("Moderator", "Moderator", ChatColor.DARK_GREEN + "[MOD]", ChatColor.DARK_GREEN, ChatColor.WHITE, true),
	COMMUNITY_MANAGER("Community Manager", "Community Mgr", ChatColor.DARK_GREEN + "[CM]", ChatColor.DARK_GREEN, ChatColor.WHITE, true),
	
	NEW_GM("New GM", "New GM", ChatColor.GOLD + "[NEW GM]", ChatColor.GOLD, ChatColor.WHITE, true), 
	GM("Game Master", "Game Master", ChatColor.GOLD + "[GM]", ChatColor.GOLD, ChatColor.WHITE, true),
	HEAD_GM("Head GM", "Head GM", ChatColor.GOLD + "[HEAD GM]", ChatColor.GOLD, ChatColor.WHITE, true),
	
	DEVELOPER("Developer", "Developer", ChatColor.DARK_RED + "[DEV]", ChatColor.RED, ChatColor.WHITE, true),
	LEAD_DEVELOPER("Lead Developer", "Lead Developer", ChatColor.DARK_RED + "[LEAD DEV]", ChatColor.RED, ChatColor.WHITE, true),
	
	ADMIN("Administrator", "Admin", ChatColor.DARK_RED + "[ADMIN]", ChatColor.RED, ChatColor.WHITE, true);

	private String rankName;
	private String shortName;
	private String chatPrefix;
	private ChatColor nameColor;
	private ChatColor chatColor;
	private boolean staff;

	public static String OFF_DUTY_STAFF_PREFIX = "[Off Duty]";
	
	Rank(String rankName, String shortName, String chatPrefix, ChatColor nameColor, ChatColor chatColor, boolean staff) {
		this.rankName = rankName;
		this.shortName = shortName;
		this.chatPrefix = (ChatColor.BLACK + "").repeat(this.ordinal()) + chatPrefix; // Will this ensure correct ordering???
		this.nameColor = nameColor;
		this.chatColor = chatColor;
		this.staff = staff;
	}

	public String getRankName() {
		return rankName;
	}

	public String getShortName() {
		return shortName;
	}

	public boolean hasChatPrefix() {
		return this != DEFAULT;
	}

	public String getChatPrefix() {
		return chatPrefix;
	}

	public ChatColor getNameColor() {
		return nameColor;
	}

	public ChatColor getChatColor() {
		return chatColor;
	}
	
	public boolean isStaff() {
		return staff;
	}
}
