package mc.dragons.tools.moderation;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.User.PunishmentData;
import mc.dragons.core.util.PermissionUtil;

public class RemovePunishmentCommand implements CommandExecutor {
	private UserLoader userLoader;
	
	public RemovePunishmentCommand() {
		userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true)) return true;
		}
		
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Specify a player and the punishment number! /removepunishment <player> <#>");
			sender.sendMessage(ChatColor.RED + "You can view the list of a player's punishments and numbers with /viewpunishments <player>");
			return true;
		}
		
		String username = args[0];
		User targetUser = userLoader.loadObject(username);
		
		if(targetUser == null) {
			sender.sendMessage(ChatColor.RED + "That player does not exist!");
			return true;
		}
		
		int punishmentNo = Integer.valueOf(args[1]) - 1;
		
		if(punishmentNo < 0 || punishmentNo >= targetUser.getPunishmentHistory().size()) {
			sender.sendMessage(ChatColor.RED + "Invalid punishment number! Use /viewpunishments " + targetUser.getName() + " to see their punishment records.");
			return true;
		}
		
		PunishmentData record = targetUser.getPunishmentHistory().get(punishmentNo);
		
		@SuppressWarnings("unchecked")
		List<Document> rawHistory = (List<Document>) targetUser.getStorageAccess().get("punishmentHistory");
		rawHistory.remove(punishmentNo);
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
