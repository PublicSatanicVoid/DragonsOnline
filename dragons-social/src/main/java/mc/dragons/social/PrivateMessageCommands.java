package mc.dragons.social;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.gameobject.user.punishment.PunishmentData;
import mc.dragons.core.gameobject.user.punishment.PunishmentType;
import mc.dragons.core.util.StringUtil;

public class PrivateMessageCommands extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if (sender instanceof Player) {
			player = player(sender);
			user = user(sender);
			if (!user.hasJoined()) {
				sender.sendMessage(ChatColor.RED + "You are not joined yet!");
				return true;
			}
			if (user.hasActiveDialogue()) {
				sender.sendMessage(ChatColor.RED + "Chat is unavailable while in active dialogue!");
				return true;
			}
			PunishmentData mute = user.getActivePunishmentData(PunishmentType.MUTE);
			if (mute != null) {
				player.sendMessage(ChatColor.RED + "You are muted!" + (mute.getReason().equals("") ? "" : " (" + mute.getReason() + ")"));
				player.sendMessage(ChatColor.RED + "Expires " + mute.getExpiry().toString());
				return true;
			}
		}
		
		if (label.equalsIgnoreCase("msg") || label.equalsIgnoreCase("tell") || label.equalsIgnoreCase("whisper") || label.equalsIgnoreCase("m") || label.equalsIgnoreCase("t")
				|| label.equalsIgnoreCase("w")) {
			if (args.length < 2) {
				if (!label.equalsIgnoreCase("msg"))
					sender.sendMessage(ChatColor.RED + "Alias for /msg.");
				sender.sendMessage(ChatColor.RED + "/msg <player> <message>");
				return true;
			}
			Player target = lookupPlayer(sender, args[0]);
			if(target == null) return true;
			if (target.equals(player)) {
				sender.sendMessage(ChatColor.RED + "You can't message yourself!");
				return true;
			}
			User targetUser = UserLoader.fromPlayer(target);
			if (targetUser.hasActiveDialogue()) {
				sender.sendMessage(ChatColor.RED + "That player is in active dialogue and cannot hear you.");
				return true;
			}
			String message = StringUtil.concatArgs(args, 1);
			target.sendMessage(ChatColor.GOLD + "[" + sender.getName() + " -> You] " + ChatColor.GRAY + message);
			sender.sendMessage(ChatColor.GOLD + "[You -> " + target.getName() + "] " + ChatColor.GRAY + message);
			for (User test : UserLoader.allUsers()) {
				if (test.hasChatSpy() && test.getPlayer() != target && test.getPlayer() != sender)
					test.getPlayer().sendMessage(ChatColor.DARK_AQUA + "[" + sender.getName() + " -> " + target.getName() + "] " + ChatColor.GRAY + message);
			}
			targetUser.setLastReceivedMessageFrom(sender);
			return true;
		}
		
		else if (label.equalsIgnoreCase("reply") || label.equalsIgnoreCase("re") || label.equalsIgnoreCase("r")) {
			if(!requirePlayer(sender)) return true;
			if (args.length == 0) {
				if (!label.equalsIgnoreCase("reply"))
					sender.sendMessage(ChatColor.RED + "Alias for /reply.");
				sender.sendMessage(ChatColor.RED + "/reply <message>");
				return true;
			}
			CommandSender target = user.getLastReceivedMessageFrom();
			if (target == null) {
				sender.sendMessage(ChatColor.RED + "You don't have anyone to reply to!");
				return true;
			}
			String message = StringUtil.concatArgs(args, 0);
			target.sendMessage(ChatColor.GOLD + "[" + sender.getName() + " -> You] " + ChatColor.GRAY + message);
			sender.sendMessage(ChatColor.GOLD + "[You -> " + target.getName() + "] " + ChatColor.GRAY + message);
			if (target instanceof Player) {
				User targetUser = UserLoader.fromPlayer((Player) target);
				if (targetUser.hasActiveDialogue()) {
					sender.sendMessage(ChatColor.RED + "That player is in active dialogue and cannot hear you.");
					return true;
				}
				targetUser.setLastReceivedMessageFrom(sender);
			}
			for (User test : UserLoader.allUsers()) {
				if (test.hasChatSpy() && test.getPlayer() != target && test.getPlayer() != sender)
					test.getPlayer().sendMessage(ChatColor.DARK_AQUA + "[" + sender.getName() + " -> " + target.getName() + "] " + ChatColor.GRAY + message);
			}
			return true;
		}
		
		else if (label.equalsIgnoreCase("chatspy")) {
			if(!requirePlayer(sender) || !hasPermission(sender, SystemProfileFlag.MODERATION)) return true;
			user.setChatSpy(!user.hasChatSpy());
			sender.sendMessage(ChatColor.GREEN + "Chat spy " + (user.hasChatSpy() ? "enabled" : "disabled"));
			return true;
		}
		return true;
	}
}
