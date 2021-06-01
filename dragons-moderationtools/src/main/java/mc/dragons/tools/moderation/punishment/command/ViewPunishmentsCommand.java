package mc.dragons.tools.moderation.punishment.command;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.hold.HoldLoader;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldEntry;
import mc.dragons.tools.moderation.punishment.PunishmentData;
import mc.dragons.tools.moderation.punishment.PunishmentType;

public class ViewPunishmentsCommand extends DragonsCommandExecutor {
	private HoldLoader holdLoader = dragons.getLightweightLoaderRegistry().getLoader(HoldLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.HELPER)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a player! /viewpunishments <player>");
			return true;
		}
		
		boolean mod = hasPermission(sender, SystemProfileFlag.MODERATION);
		Player targetPlayer = Bukkit.getPlayerExact(args[0]);
		User targetUser = lookupUser(sender, args[0]);
		
		if(targetUser == null) return true;

		WrappedUser wrapped = WrappedUser.of(targetUser);
		
		PunishmentData banData = wrapped.getActivePunishmentData(PunishmentType.BAN);
		PunishmentData muteData = wrapped.getActivePunishmentData(PunishmentType.MUTE);
		
		sender.sendMessage(ChatColor.GOLD + "Punishment History for User " + targetUser.getName());
		if(targetPlayer == null) {
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.ITALIC + "This player is offline. Showing cached data.");
		}
		if(!mod) {
			sender.sendMessage(ChatColor.GRAY + "- You only have access to see punishments and holds that you have applied -");
		}
		if(mod) {
			sender.sendMessage(ChatColor.YELLOW + "Active Punishments:");
			if(banData == null || banData.hasExpired() || banData.isRevoked()) {
				sender.sendMessage(ChatColor.WHITE + "- Not banned");
			}
			else {
				sender.sendMessage(ChatColor.WHITE + "- Banned: " + banData.getReason() + " (" + (banData.isPermanent() ? "Permanent" : "Until " + StringUtil.DATE_FORMAT.format(banData.getExpiry()) + ")"));
			}
			if(muteData == null || muteData.hasExpired() || muteData.isRevoked()) {
				sender.sendMessage(ChatColor.WHITE + "- Not muted");
			}
			else {
				sender.sendMessage(ChatColor.WHITE + "- Muted: " + muteData.getReason() + " (" + (muteData.isPermanent() ? "Permanent" : "Until " + StringUtil.DATE_FORMAT.format(muteData.getExpiry()) + ")"));
			}
		}
		
		sender.sendMessage(ChatColor.YELLOW + "Past Punishments:");
		
		int i = 1;
		for(PunishmentData entry : wrapped.getPunishmentHistory()) {
			if((entry.getIssuedBy() == null || !entry.getIssuedBy().equals(user(sender))) && !mod) {
				i++;
				continue;
			}
			String duration = "";
			if(entry.isPermanent()) duration = "(Permanent)";
			else if(entry.getExpiry() != null) duration = "(Until " + StringUtil.DATE_FORMAT.format(entry.getExpiry()) + ")";
			String removed = "";
			if(entry.hasExpired()) {
				removed = ChatColor.GREEN + " (Expired)";
			}
			else if(entry.isRevoked()) {
				removed = ChatColor.AQUA + " (Revoked: " + entry.getRevocationCode().getDescription() + ")";
			}
			sender.sendMessage(ChatColor.DARK_GREEN + "#" + i + ": " + ChatColor.RED + entry.getType() + ": " + ChatColor.WHITE + entry.getReason() + " " + duration + removed);
			i++;
		}

		List<HoldEntry> holds = holdLoader.getActiveHoldsByUser(targetUser).stream().filter(h -> h.getBy().equals(user(sender)) || mod).collect(Collectors.toList());
		if(holds.size() > 0) {
			sender.spigot().sendMessage(StringUtil.plainText(ChatColor.YELLOW + "Pending Holds: " + ChatColor.RESET + holds.size()),
					StringUtil.clickableHoverableText(ChatColor.GRAY + " [View Holds]", "/viewholds " + targetUser.getName(), "Click to view active holds on this user"));
		}
		
		if(targetPlayer == null) {
			// User was only constructed for querying purposes. Since they're not really online, remove them from local registry
			userLoader.unregister(targetUser);
		}
		
		return true;
	}
}
