package mc.dragons.tools.moderation.analysis;

import java.util.Arrays;
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
import mc.dragons.core.util.MathUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.punishment.PunishmentData;
import mc.dragons.tools.moderation.punishment.PunishmentType;
import mc.dragons.tools.moderation.punishment.StandingLevelType;

public class InfoCommand extends DragonsCommandExecutor {
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.MODERATOR)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a player! /userinfo <player>");
			return true;
		}
		
		Player targetPlayer = Bukkit.getPlayerExact(args[0]);
		User targetUser = lookupUser(sender, args[0]);
		if(targetUser == null) return true;
		
		WrappedUser wrapped = WrappedUser.of(targetUser);
		
		boolean goodStanding = true;
		
		String skills = Arrays.stream(SkillType.values()).map(s -> s.toString() + " (" + targetUser.getSkillLevel(s) + ")").reduce((a,b) -> a + ", " + b).get();
		
		/*String skills = "";
		for(SkillType skill : SkillType.values()) {
			skills += skill.toString() + " (" + targetUser.getSkillLevel(skill) + "), ";
		}
		skills = skills.substring(0, skills.length() - 2);*/
		
		PunishmentData banData = wrapped.getActivePunishmentData(PunishmentType.BAN);
		PunishmentData muteData = wrapped.getActivePunishmentData(PunishmentType.MUTE);
		
		sender.sendMessage(ChatColor.GOLD + "Report for User " + targetUser.getName());
		if(targetPlayer == null) {
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.ITALIC + "This player is offline. Showing cached data.");
		}
		sender.sendMessage(ChatColor.YELLOW + "UUID: " + ChatColor.RESET + targetUser.getIdentifier().getUUID().toString());
		if(hasPermission(sender, PermissionLevel.ADMIN)) {
			sender.sendMessage(ChatColor.YELLOW + "Last IP: " + ChatColor.RESET + targetUser.getLastIP());
		}
		sender.sendMessage(ChatColor.YELLOW + "Active Punishments:");
		if(banData == null) {
			sender.sendMessage(ChatColor.WHITE + "- Not banned");
		}
		else {
			sender.sendMessage(ChatColor.WHITE + "- Banned: " + banData.getReason() + " (" + (banData.isPermanent() ? "Permanent" : "Until " + banData.getExpiry().toString()) + ")");
			goodStanding = false;
		}
		
		if(muteData == null) {
			sender.sendMessage(ChatColor.WHITE + "- Not muted");
		}
		else {
			sender.sendMessage(ChatColor.WHITE + "- Muted: " + muteData.getReason() + " (" + (muteData.isPermanent() ? "Permanent" : "Until " + muteData.getExpiry().toString()) + ")");
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
		else {
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
		sender.sendMessage(ChatColor.YELLOW + "First Join: " + ChatColor.RESET + targetUser.getFirstJoined().toString());
		sender.sendMessage(ChatColor.YELLOW + "Last Join: " + ChatColor.RESET + targetUser.getLastJoined().toString());
		sender.sendMessage(ChatColor.YELLOW + "Last Seen: " + ChatColor.RESET + targetUser.getLastSeen().toString());
		
		if(goodStanding) {
			sender.sendMessage(ChatColor.GREEN + "User is in good standing");
		}
		else {
			sender.sendMessage(ChatColor.RED + "User is not in good standing");
		}
		
		if(targetPlayer == null) {
			// User was only constructed for querying purposes. Since they're not really online, remove them from local registry
			userLoader.unregister(targetUser);
		}
		
		return true;
	}
}
