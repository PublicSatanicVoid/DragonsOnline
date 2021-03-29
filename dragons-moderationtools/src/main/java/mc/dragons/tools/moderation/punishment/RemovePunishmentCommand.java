package mc.dragons.tools.moderation.punishment;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.gameobject.user.punishment.PunishmentData;

public class RemovePunishmentCommand extends DragonsCommandExecutor {

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN) || !requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Specify a player and the punishment number! /removepunishment <player> <#>");
			sender.sendMessage(ChatColor.RED + "You can view the list of a player's punishments and numbers with /viewpunishments <player>");
			return true;
		}
		
		User targetUser = lookupUser(sender, args[0]);
		Integer punishmentNo = parseIntType(sender, args[1]);
		if(targetUser == null || punishmentNo == null) return true;
		punishmentNo--;
		
		if(punishmentNo < 0 || punishmentNo >= targetUser.getPunishmentHistory().size()) {
			sender.sendMessage(ChatColor.RED + "Invalid punishment number! Use /viewpunishments " + targetUser.getName() + " to see their punishment records.");
			return true;
		}
		
		PunishmentData record = targetUser.getPunishmentHistory().get(punishmentNo);
		
		@SuppressWarnings("unchecked")
		List<Document> rawHistory = (List<Document>) targetUser.getStorageAccess().get("punishmentHistory");
		rawHistory.remove((int) punishmentNo);
		targetUser.getStorageAccess().set("punishmentHistory", rawHistory);
		
		sender.sendMessage(ChatColor.GREEN + "Removed punishment #" + args[1] + " from " + targetUser.getName());
		if(record.getExpiry().after(Date.from(Instant.now())) || record.isPermanent()) {
			targetUser.unpunish(record.getType());
		}
		
		if(targetUser.getPlayer() == null) {
			// User was only constructed for querying purposes. Since they're not really online, remove them from local registry
			userLoader.unregister(targetUser);
		}
		
		return true;
	}
}
