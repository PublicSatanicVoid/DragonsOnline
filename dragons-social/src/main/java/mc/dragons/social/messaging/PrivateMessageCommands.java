package mc.dragons.social.messaging;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.gameobject.user.punishment.PunishmentData;
import mc.dragons.core.gameobject.user.punishment.PunishmentType;
import mc.dragons.core.util.StringUtil;
import mc.dragons.social.DragonsSocial;

public class PrivateMessageCommands extends DragonsCommandExecutor {
	private PrivateMessageHandler handler;
	
	public PrivateMessageCommands(DragonsSocial instance) {
		handler = new PrivateMessageHandler(instance);
	}
	
	private void privateMessageCommand(User user, String to, String message) {
		User target = lookupUser(user.getPlayer(), to);
		if(target == null) return;
		if (user.equals(target) && !user.getLocalData().getBoolean("canSelfMessage", false)) {
			user.getPlayer().sendMessage(ChatColor.RED + "You can't message yourself!");
		}
		else if (target.hasActiveDialogue()) {
			user.getPlayer().sendMessage(ChatColor.RED + "That player is in active dialogue and cannot hear you.");
		}
		else {
			handler.send(user, target, message);
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		User user = user(sender);
		
		if (!user.hasJoined()) {
			sender.sendMessage(ChatColor.RED + "You are not joined yet!");
			return true;
		}
		PunishmentData mute = user.getActivePunishmentData(PunishmentType.MUTE);
		if (mute != null) {
			sender.sendMessage(ChatColor.RED + "You are muted!" + (mute.getReason().equals("") ? "" : " (" + mute.getReason() + ")"));
			if(!mute.isPermanent()) {
				sender.sendMessage(ChatColor.RED + "Expires " + mute.getExpiry().toString());
			}
			return true;
		}
		if (user.hasActiveDialogue()) {
			sender.sendMessage(ChatColor.RED + "Chat is unavailable while in active dialogue!");
			return true;
		}
		
		if (label.equalsIgnoreCase("msg") || label.equalsIgnoreCase("tell") || label.equalsIgnoreCase("whisper") || label.equalsIgnoreCase("m") || label.equalsIgnoreCase("t")
				|| label.equalsIgnoreCase("w")) {
			if (args.length < 2) {
				if (!label.equalsIgnoreCase("msg"))
					sender.sendMessage(ChatColor.RED + "Alias for /msg.");
				sender.sendMessage(ChatColor.RED + "/msg <player> <message>");
				return true;
			}
			String message = StringUtil.concatArgs(args, 1);
			privateMessageCommand(user, args[0], message);
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
			String target = user.getLastReceivedMessageFrom();
			if (target == null) {
				sender.sendMessage(ChatColor.RED + "You don't have anyone to reply to!");
				return true;
			}
			String message = StringUtil.concatArgs(args, 0);
			privateMessageCommand(user, target, message);
			return true;
		}
		
		else if (label.equalsIgnoreCase("chatspy")) {
			if(!requirePlayer(sender) || !hasPermission(sender, SystemProfileFlag.MODERATION)) return true;
			user.setChatSpy(!user.hasChatSpy());
			sender.sendMessage(ChatColor.GREEN + "Chat spy " + (user.hasChatSpy() ? "enabled" : "disabled"));
			return true;
		}
		
		else if (label.equalsIgnoreCase("toggleselfmessage")) {
			if(!requirePermission(sender, PermissionLevel.ADMIN)) return true;
			user.getLocalData().append("canSelfMessage", !user.getLocalData().getBoolean("canSelfMessage", false));
			sender.sendMessage(ChatColor.GREEN + "Toggled self-messaging ability.");
		}
		return true;
	}
}
