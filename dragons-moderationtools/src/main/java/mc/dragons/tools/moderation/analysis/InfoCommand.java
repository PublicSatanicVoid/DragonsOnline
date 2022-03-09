package mc.dragons.tools.moderation.analysis;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.user.SkillType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.MathUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.hold.HoldLoader;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldEntry;
import mc.dragons.tools.moderation.punishment.PunishmentData;
import mc.dragons.tools.moderation.punishment.PunishmentType;
import mc.dragons.tools.moderation.punishment.StandingLevelType;
import net.md_5.bungee.api.chat.TextComponent;

public class InfoCommand extends DragonsCommandExecutor {
	private HoldLoader holdLoader = dragons.getLightweightLoaderRegistry().getLoader(HoldLoader.class);
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.HELPER)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a player! /userinfo <player>");
			return true;
		}
		
		Player targetPlayer = Bukkit.getPlayerExact(args[0]);
		User targetUser = lookupUser(sender, args[0]);
		if(targetUser == null) return true;
		
		boolean helper = hasPermission(sender, SystemProfileFlag.HELPER);
		boolean mod = hasPermission(sender, SystemProfileFlag.MODERATION);
		
		targetUser.safeResyncData();
		WrappedUser wrapped = WrappedUser.of(targetUser);
		
		boolean goodStanding = true;
		
		String skills = Arrays.stream(SkillType.values()).map(s -> s.toString() + " (" + targetUser.getSkillLevel(s) + ")").reduce((a,b) -> a + ", " + b).get();
		
		PunishmentData banData = wrapped.getActivePunishmentData(PunishmentType.BAN);
		PunishmentData muteData = wrapped.getActivePunishmentData(PunishmentType.MUTE);
		
		TextComponent report = StringUtil.clickableHoverableText(ChatColor.GRAY + "[Report] ", "/report " + targetUser.getName() + " ", true, "Click to report this user");
		TextComponent hold = !helper ? StringUtil.plainText("") : StringUtil.clickableHoverableText(ChatColor.GRAY + "[Place Hold] ", "/hold " + targetUser.getName() + " ", true, 
			"Click to suspend this account for " + HoldLoader.HOLD_DURATION_HOURS + " hours pending review");
		TextComponent punish = !helper ? StringUtil.plainText("") : StringUtil.clickableHoverableText(ChatColor.GRAY + "[Punish] ", "/punish " + targetUser.getName(), "Click to punish this user");
		TextComponent escalate = StringUtil.clickableHoverableText(ChatColor.GRAY + "[Escalate] ", "/escalate " + targetUser.getName() + " ", 
			"Click to escalate an issue with this user", "for review by a senior staff member");
		TextComponent watchlist = !mod ? StringUtil.plainText("") : StringUtil.clickableHoverableText(ChatColor.GRAY + "[Watchlist] ", "/watchlist add " + targetUser.getName() + " ", true, 
			"Click to add this user to the watch list.", "You will be prompted to enter a reason.");
		TextComponent reports = !mod ? StringUtil.plainText("") : StringUtil.clickableHoverableText(ChatColor.GRAY + "[View Reports] ", "/reports on " + targetUser.getName(), 
			"Click to view reports filed against this user");
		TextComponent phistory = !helper ? StringUtil.plainText("") : StringUtil.clickableHoverableText(ChatColor.GRAY + "[View Punishments]", 
			"/viewpunishments " + targetUser.getName(), "Click to view this user's full punishment history");
		
		sender.sendMessage(ChatColor.GOLD + "Report for User " + targetUser.getName());
		if(targetPlayer == null) {
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.ITALIC + "This player is offline. Showing cached data.");
		}
		sender.spigot().sendMessage(report, hold, punish, escalate, watchlist, reports, phistory);
		sender.sendMessage(ChatColor.YELLOW + "UUID: " + ChatColor.RESET + targetUser.getIdentifier().getUUID().toString());
		if(targetUser.getServerName() != null) {
			sender.sendMessage(ChatColor.YELLOW + "Current Server: " + ChatColor.RESET + targetUser.getServerName());
		}
		if(mod) {
			sender.sendMessage(ChatColor.YELLOW + "Active Punishments:");
			if(banData == null) {
				sender.sendMessage(ChatColor.WHITE + "- Not banned");
			}
			else {
				sender.sendMessage(ChatColor.WHITE + "- Banned: " + banData.getReason() + " (" + (banData.isPermanent() ? "Permanent" : "Until " + StringUtil.DATE_FORMAT.format(banData.getExpiry())) + ")");
				goodStanding = false;
			}
			
			if(muteData == null) {
				sender.sendMessage(ChatColor.WHITE + "- Not muted");
			}
			else {
				sender.sendMessage(ChatColor.WHITE + "- Muted: " + muteData.getReason() + " (" + (muteData.isPermanent() ? "Permanent" : "Until " + StringUtil.DATE_FORMAT.format(muteData.getExpiry())) + ")");
				goodStanding = false;
			}
			List<HoldEntry> holds = holdLoader.getActiveHoldsByUser(targetUser);
			if(holds.size() > 0) {
				sender.spigot().sendMessage(StringUtil.plainText(ChatColor.WHITE + "- " + holds.size() + " holds have been placed on this account. "),
					StringUtil.clickableHoverableText(ChatColor.GRAY + "[View Holds]", "/viewholds " + targetUser.getName(), "Click to view active holds on this user"));
				goodStanding = false;
			}
			wrapped.updateStandingLevels();
			sender.sendMessage(ChatColor.YELLOW + "Standing Levels: " + ChatColor.RESET + Arrays.stream(StandingLevelType.values())
				.map(t -> t.toString() + " (" + wrapped.getStandingLevel(t) + ")").reduce((a,b)-> a + ", " + b).get());
			for(StandingLevelType level : StandingLevelType.values()) {
				if(wrapped.getStandingLevel(level) > 1) {
					goodStanding = false;
				}
			}
		}
		sender.sendMessage(ChatColor.YELLOW + "XP: " + ChatColor.RESET + targetUser.getXP() + " [Level " + targetUser.getLevel() + "] (" + MathUtil.round(targetUser.getLevelProgress() * 100) + "%)");
		sender.sendMessage(ChatColor.YELLOW + "Rank: " + ChatColor.RESET + targetUser.getRank().getRankName());
		sender.sendMessage(ChatColor.YELLOW + "Gold Balance: " + ChatColor.RESET + targetUser.getGold());
		sender.sendMessage(ChatColor.YELLOW + "Total Time Online: " + ChatColor.RESET + StringUtil.parseSecondsToTimespan(targetUser.getTotalOnlineTime()));
		sender.sendMessage(ChatColor.YELLOW + "Speaking Channel: " + ChatColor.RESET + targetUser.getSpeakingChannel());
		sender.sendMessage(ChatColor.YELLOW + "Listening Channels: " + ChatColor.RESET + StringUtil.parseList(targetUser.getActiveChatChannels()));
		if(targetPlayer == null) {
			sender.sendMessage(ChatColor.YELLOW + "Cached Location: " + ChatColor.RESET + StringUtil.locToString(targetUser.getSavedLocation()) + " in " + targetUser.getSavedLocation().getWorld().getName());
			sender.sendMessage(ChatColor.YELLOW + "Cached Floor: " + ChatColor.RESET + FloorLoader.fromLocation(targetUser.getSavedLocation()).getDisplayName());
			sender.sendMessage(ChatColor.YELLOW + "Cached Regions: " + ChatColor.RESET + regionLoader.getRegionsByLocation(targetUser.getSavedLocation()).stream().map(r -> r.getFlags().getString("fullname")).collect(Collectors.joining(", ")));
			sender.sendMessage(ChatColor.YELLOW + "Health: " + ChatColor.RESET + targetUser.getSavedHealth() + " / " + targetUser.getSavedMaxHealth());
		}
		else if(mod) {
			if(targetUser.getSystemProfile() != null) {
				sender.sendMessage(ChatColor.YELLOW + "System Profile: " + ChatColor.RESET + targetUser.getSystemProfile().getProfileName());
			}
			sender.sendMessage(ChatColor.YELLOW + "Active Permission Level: " + ChatColor.RESET + targetUser.getActivePermissionLevel().toString());
			sender.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.RESET + StringUtil.locToString(targetPlayer.getLocation()) + " in " + targetPlayer.getWorld().getName());
			sender.sendMessage(ChatColor.YELLOW + "Floor: " + ChatColor.RESET + FloorLoader.fromLocation(targetPlayer.getLocation()).getDisplayName());
			sender.sendMessage(ChatColor.YELLOW + "Regions: " + ChatColor.RESET + targetUser.getRegions().stream().map(r -> r.getFlags().getString("fullname")).collect(Collectors.joining(", ")));
			sender.sendMessage(ChatColor.YELLOW + "Health: " + ChatColor.RESET + targetPlayer.getHealth() + " / " + targetPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		}
		sender.sendMessage(ChatColor.YELLOW + "Skills: " + ChatColor.RESET + skills);
		sender.sendMessage(ChatColor.YELLOW + "First Join: " + ChatColor.RESET + StringUtil.formatDate(targetUser.getFirstJoined()));
		sender.sendMessage(ChatColor.YELLOW + "Last Join: " + ChatColor.RESET + StringUtil.formatDate(targetUser.getLastJoined()));
		sender.sendMessage(ChatColor.YELLOW + "Last Seen: " + ChatColor.RESET + StringUtil.formatDate(targetUser.getLastSeen()));
		
		if(mod) {
			if(goodStanding) {
				sender.sendMessage(ChatColor.GREEN + "User is in good standing");
			}
			else {
				sender.sendMessage(ChatColor.RED + "User is not in good standing");
			}
		}
		
		if(targetPlayer == null) {
			// User was only constructed for querying purposes. Since they're not really online, remove them from local registry
			userLoader.unregister(targetUser);
		}
		
		return true;
	}
}
