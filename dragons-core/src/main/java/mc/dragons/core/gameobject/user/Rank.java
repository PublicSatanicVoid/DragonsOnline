 package mc.dragons.core.gameobject.user;
 
 import org.bukkit.ChatColor;
 
 public enum Rank {
   DEFAULT(
     "Default", "None", "", ChatColor.GRAY, ChatColor.GRAY),
   BETA_TESTER("Beta Tester", "Beta Tester", ChatColor.WHITE + "[Beta]", ChatColor.GRAY, ChatColor.WHITE),
   PATRON("Patron", "Patron", ChatColor.DARK_AQUA + "[Patron]", ChatColor.AQUA, ChatColor.WHITE),
   PATRON_PLUS("Patron+", "Patron+", ChatColor.DARK_AQUA + "[Patron" + ChatColor.YELLOW + "+" + ChatColor.DARK_AQUA + "]", ChatColor.AQUA, ChatColor.WHITE),
   INVESTOR("Investor", "Investor", ChatColor.GOLD + "[Investor]", ChatColor.YELLOW, ChatColor.WHITE),
   YOUTUBE("YouTuber", "YouTuber", ChatColor.RED + "[You" + ChatColor.WHITE + "Tube" + ChatColor.RED + "]", ChatColor.RED, ChatColor.WHITE),
   MEDIA(
     "Media", "Media", ChatColor.DARK_PURPLE + "[Media]", ChatColor.LIGHT_PURPLE, ChatColor.WHITE),
   CONTENT_TEAM("Content Team", "Content Team", ChatColor.BLUE + "[Content Team]", ChatColor.BLUE, ChatColor.WHITE),
   TRIAL_BUILDER(
     "Trial Builder", "Trial Builder", ChatColor.WHITE + "[Trial Builder]", ChatColor.GRAY, ChatColor.WHITE),
   NEW_BUILDER("New Builder", "New Builder", ChatColor.BLUE + "[New Builder]", ChatColor.BLUE, ChatColor.WHITE),
   BUILDER("Builder", "Builder", ChatColor.BLUE + "[Builder]", ChatColor.BLUE, ChatColor.WHITE),
   BUILDER_CMD("Builder + CMD", "Builder + CMD", ChatColor.BLUE + "[Build" + ChatColor.DARK_GRAY + "+" + ChatColor.DARK_PURPLE + "CMD" + ChatColor.BLUE + "]", ChatColor.BLUE, ChatColor.WHITE),
   HEAD_BUILDER("Head Builder", "Head Builder", ChatColor.BLUE + "[Head Builder]", ChatColor.BLUE, ChatColor.WHITE),
   HELPER("Helper", "Helper", ChatColor.GREEN + "[Helper]", ChatColor.GREEN, ChatColor.WHITE),
   MODERATOR("Moderator", "Moderator", ChatColor.DARK_GREEN + "[Moderator]", ChatColor.GREEN, ChatColor.WHITE),
   NEW_GM("New GM", "New GM", ChatColor.GOLD + "[New GM]", ChatColor.GOLD, ChatColor.WHITE),
   GM("Game Master", "Game Master", ChatColor.GOLD + "[GM]", ChatColor.GOLD, ChatColor.WHITE),
   HEAD_GM("Head GM", "Head GM", ChatColor.GOLD + "[Head GM]", ChatColor.GOLD, ChatColor.WHITE),
   DEVELOPER("Developer", "Developer", ChatColor.DARK_RED + "[Developer]", ChatColor.RED, ChatColor.WHITE),
   LEAD_DEVELOPER("Lead Developer", "Lead Developer", ChatColor.DARK_RED + "[Lead Dev]", ChatColor.RED, ChatColor.WHITE),
   ADMIN("Administrator", "Admin", ChatColor.DARK_RED + "[Admin]", ChatColor.RED, ChatColor.WHITE);
   
   private String rankName;
   
   private String shortName;
   
   private String chatPrefix;
   
   private ChatColor nameColor;
   
   private ChatColor chatColor;
   
   Rank(String rankName, String shortName, String chatPrefix, ChatColor nameColor, ChatColor chatColor) {
     this.rankName = rankName;
     this.shortName = shortName;
     this.chatPrefix = chatPrefix;
     this.nameColor = nameColor;
     this.chatColor = chatColor;
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
 }


