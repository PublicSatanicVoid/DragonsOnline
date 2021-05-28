package mc.dragons.social.messaging;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.social.DragonsSocial;
import mc.dragons.social.friend.FriendUtil;

public class PrivateMessageCommands extends DragonsCommandExecutor {
	private PrivateMessageHandler handler;
	
	public PrivateMessageCommands(DragonsSocial instance) {
		handler = new PrivateMessageHandler(instance);
	}
	
	private void privateMessageCommand(User user, String to, String message) {
		User target = lookupUser(user.getPlayer(), to);
		if(target == null) return;
		target.safeResyncData();
		if (user.equals(target) && !user.getLocalData().getBoolean("canSelfMessage", false)) {
			user.getPlayer().sendMessage(ChatColor.RED + "You can't message yourself!");
		}
		else if (target.hasActiveDialogue()) {
			user.getPlayer().sendMessage(ChatColor.RED + "That player is in active dialogue and cannot hear you.");
		}
		else if (target.getBlockedUsers().contains(user)) {
			user.getPlayer().sendMessage(ChatColor.RED + "That player has blocked you.");
		}
		else if (user.getBlockedUsers().contains(target)) {
			user.getPlayer().sendMessage(ChatColor.RED + "You can't send messages to players you've blocked.");
		}
		else if (!FriendUtil.getFriends(user).contains(target) && !user.getData().getBoolean("messageFriendsOnly", false) 
			&& !hasPermission(user, PermissionLevel.HELPER) && !hasPermission(target, PermissionLevel.HELPER)) {
				user.getPlayer().sendMessage(ChatColor.RED + "You can't send or receive messages from non-friends. Use /togglemsg to change this.");
		}
		else if (!FriendUtil.getFriends(user).contains(target) && !target.getData().getBoolean("messageFriendsOnly", false) 
				&& !hasPermission(user, PermissionLevel.HELPER) && !hasPermission(target, PermissionLevel.HELPER)) {
					user.getPlayer().sendMessage(ChatColor.RED + "This user has blocked private messages from non-friends.");
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
		
		String message = StringUtil.concatArgs(args, 1);
		for(UserHook hook : dragons.getUserHookRegistry().getHooks()) {
			if(!hook.checkAllowChat(user, message)) {
				return true;
			}
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
			message = StringUtil.concatArgs(args, 0);
			if (target == null) {
				sender.sendMessage(ChatColor.RED + "You don't have anyone to reply to!");
				return true;
			}
			privateMessageCommand(user, target, message);
			return true;
		}
		
		else if (label.equalsIgnoreCase("togglemsg")) {
			boolean all = user.getData().getBoolean("messageFriendsOnly", false);
			user.getStorageAccess().set("messageFriendsOnly", !all);
			sender.sendMessage(ChatColor.GREEN + "Set messaging capability to " + (all ? "EVERYONE" : "FRIENDS ONLY"));
			if(!all) {
				sender.sendMessage(ChatColor.GRAY + " You'll still be able to send and receive messages from staff members.");
			}
		}
		
		else if (label.equalsIgnoreCase("chatspy")) {
			if(!requirePlayer(sender)|| !requirePermission(sender, PermissionLevel.ADMIN) || !requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
			user.setChatSpy(!user.hasChatSpy());
			sender.sendMessage(ChatColor.GREEN + "Chat spy " + (user.hasChatSpy() ? "enabled" : "disabled"));
			return true;
		}
		
		else if (label.equalsIgnoreCase("toggleselfmessage")) {
			if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.DEVELOPER)) return true;
			user.getLocalData().append("canSelfMessage", !user.getLocalData().getBoolean("canSelfMessage", false));
			sender.sendMessage(ChatColor.GREEN + "Toggled self-messaging ability.");
		}
		return true;
	}
}
