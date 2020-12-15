package mc.dragons.core.gameobject.user;

import org.bukkit.ChatColor;

public enum Rank {
	DEFAULT("Default", "None", "", ChatColor.GRAY, ChatColor.GRAY, false), 
	BETA_TESTER("Beta Tester", "Beta Tester", ChatColor.WHITE + "[Beta]", ChatColor.GRAY, ChatColor.WHITE, false),
	PATRON("Patron", "Patron", ChatColor.DARK_AQUA + "[Patron]", ChatColor.AQUA, ChatColor.WHITE, false),
	PATRON_PLUS("Patron+", "Patron+", ChatColor.DARK_AQUA + "[Patron" + ChatColor.YELLOW + "+" + ChatColor.DARK_AQUA + "]", ChatColor.AQUA, ChatColor.WHITE, false),
	INVESTOR("Investor", "Investor", ChatColor.GOLD + "[Investor]", ChatColor.YELLOW, ChatColor.WHITE, false),
	YOUTUBE("YouTuber", "YouTuber", ChatColor.RED + "[You" + ChatColor.WHITE + "Tube" + ChatColor.RED + "]", ChatColor.RED, ChatColor.WHITE, false),
	MEDIA("Media", "Media", ChatColor.DARK_PURPLE + "[Media]", ChatColor.LIGHT_PURPLE, ChatColor.WHITE, false),
	CONTENT_TEAM("Content Team", "Content Team", ChatColor.BLUE + "[Content Team]", ChatColor.BLUE, ChatColor.WHITE, true),
	TRIAL_BUILDER("Trial Builder", "Trial Builder", ChatColor.WHITE + "[Trial Builder]", ChatColor.GRAY, ChatColor.WHITE, false),
	NEW_BUILDER("New Builder", "New Builder", ChatColor.BLUE + "[New Builder]", ChatColor.BLUE, ChatColor.WHITE, true),
	BUILDER("Builder", "Builder", ChatColor.BLUE + "[Builder]", ChatColor.BLUE, ChatColor.WHITE, true),
	BUILDER_CMD("Builder + CMD", "Builder + CMD", ChatColor.BLUE + "[Build" + ChatColor.DARK_GRAY + "+" + ChatColor.DARK_PURPLE + "CMD" + ChatColor.BLUE + "]", ChatColor.BLUE, ChatColor.WHITE, true),
	BUILD_MANAGER("Build Manager", "Build Manager", ChatColor.BLUE + "[Build Mgr]", ChatColor.BLUE, ChatColor.WHITE, true),
	HEAD_BUILDER("Head Builder", "Head Builder", ChatColor.BLUE + "[Head Builder]", ChatColor.BLUE, ChatColor.WHITE, true),
	HELPER("Helper", "Helper", ChatColor.GREEN + "[Helper]", ChatColor.GREEN, ChatColor.WHITE, true),
	MODERATOR("Moderator", "Moderator", ChatColor.DARK_GREEN + "[Moderator]", ChatColor.GREEN, ChatColor.WHITE, true),
	NEW_GM("New GM", "New GM", ChatColor.GOLD + "[New GM]", ChatColor.GOLD, ChatColor.WHITE, true), 
	GM("Game Master", "Game Master", ChatColor.GOLD + "[GM]", ChatColor.GOLD, ChatColor.WHITE, true),
	HEAD_GM("Head GM", "Head GM", ChatColor.GOLD + "[Head GM]", ChatColor.GOLD, ChatColor.WHITE, true),
	DEVELOPER("Developer", "Developer", ChatColor.DARK_RED + "[Developer]", ChatColor.RED, ChatColor.WHITE, true),
	LEAD_DEVELOPER("Lead Developer", "Lead Developer", ChatColor.DARK_RED + "[Lead Dev]", ChatColor.RED, ChatColor.WHITE, true),
	ADMIN("Administrator", "Admin", ChatColor.DARK_RED + "[Admin]", ChatColor.RED, ChatColor.WHITE, true);

	private String rankName;
	private String shortName;
	private String chatPrefix;
	private ChatColor nameColor;
	private ChatColor chatColor;
	private boolean staff;

	public static String OFF_DUTY_STAFF_PREFIX = ChatColor.BLUE + "[OD]";
	
	Rank(String rankName, String shortName, String chatPrefix, ChatColor nameColor, ChatColor chatColor, boolean staff) {
		this.rankName = rankName;
		this.shortName = shortName;
		this.chatPrefix = chatPrefix;
		this.nameColor = nameColor;
		this.chatColor = chatColor;
		this.staff = staff;
	}

	public String getRankName() {
		return this.rankName;
	}

	public String getShortName() {
		return this.shortName;
	}

	public boolean hasChatPrefix() {
		return (this != DEFAULT);
	}

	public String getChatPrefix() {
		return this.chatPrefix;
	}

	public ChatColor getNameColor() {
		return this.nameColor;
	}

	public ChatColor getChatColor() {
		return this.chatColor;
	}
	
	public boolean isStaff() {
		return this.staff;
	}
}
