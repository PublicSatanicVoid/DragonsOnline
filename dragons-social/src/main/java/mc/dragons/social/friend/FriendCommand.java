package mc.dragons.social.friend;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.mongo.pagination.PaginationUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.social.DragonsSocial;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;

public class FriendCommand extends DragonsCommandExecutor {
	private static int PAGE_SIZE = 5;
	public static String SUCCESS_PREFIX = ChatColor.DARK_PURPLE + "" + ChatColor.MAGIC + "FF" + ChatColor.RESET + " ";
	
	private FriendMessageHandler friendHandler;
	
	private void showHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.LIGHT_PURPLE + "/friend <Player>" + ChatColor.AQUA + " sends or accepts a friend request.");
		sender.sendMessage(ChatColor.LIGHT_PURPLE + "/friend list [Page#]" + ChatColor.AQUA + " lists your friends.");
		sender.sendMessage(ChatColor.LIGHT_PURPLE + "/friend remove <Player>" + ChatColor.AQUA + " removes a friend.");
		sender.sendMessage(ChatColor.LIGHT_PURPLE + "/friend requests [Page#]" + ChatColor.AQUA + " lists your friend requests.");
		sender.sendMessage(ChatColor.LIGHT_PURPLE + "/friend toggle" + ChatColor.AQUA + " toggle whether you can receive friend requests.");
	}
	
	private void toggleSelfFriend(CommandSender sender) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return;
		User user = user(sender);
		user.getLocalData().append("canSelfFriend", !user.getLocalData().getBoolean("canSelfFriend", false));
		sender.sendMessage(ChatColor.GREEN + "Toggled self-friend ability.");
	}
	
	private void dumpFriendData(CommandSender sender, String[] args) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return;
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/dumpFriends <Player>");
			return;
		}
		User target = lookupUser(sender, args[0]);
		if(target == null) return;
		for(User friend : FriendUtil.getFriends(target)) {
			sender.sendMessage("Friend: " + friend.getName() + " - " + friend.getServer());
		}
		for(User out : FriendUtil.getOutgoing(target)) {
			sender.sendMessage("Out: " + out.getName() + " - " + out.getServer());
		}
		for(User in : FriendUtil.getIncoming(target)) {
			sender.sendMessage("In: " + in.getName() + " - " + in.getServer());
		}
	}
	
	private void listFriends(CommandSender sender, String[] args) {
		Integer page = 1;
		if(args.length > 1) {
			page = parseInt(sender, args[1]);
			if(page == null) return;
		}
		User user = user(sender);
		List<User> all = FriendUtil.getFriends(user);
		List<User> result = PaginationUtil.paginateList(all, page, PAGE_SIZE);
		sender.sendMessage(ChatColor.DARK_PURPLE + "You have " + all.size() + " friends. Showing page " + page + " of " + (int) Math.ceil((double) all.size() / PAGE_SIZE)); 
		for(User friend : result) {
			if(friend.getPlayer() == null) friend.resyncData(); // make sure we have up-to-date information about location
			String status = user.getServer() == null ? ChatColor.DARK_GRAY + "OFFLINE" : ChatColor.GRAY + "ONLINE: " + user.getServer();
			sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.LIGHT_PURPLE + friend.getName() + ChatColor.GRAY + " - " + status, "/friend remove " + friend.getName(), 
					ChatColor.RED + "Click to remove " + friend.getName() + " from your friends list."));
		}
	}
	
	private void removeFriend(CommandSender sender, String[] args) {
		if(args.length == 1) {
			sender.sendMessage(ChatColor.RED + "Specify a friend to remove! /friend remove <Player>");
			return;
		}
		User user = user(sender);
		User target = lookupUser(sender, args[1]);
		if(target == null) return;
		if(!FriendUtil.getFriends(user).contains(target)) {
			sender.sendMessage(ChatColor.RED + "You're not friends with " + target.getName() + "!");
			return;
		}
		boolean success = FriendUtil.removeFriend(user, target);
		if(!success) {
			sender.sendMessage(ChatColor.RED + "Something went wrong. Please try again later.");
			return;
		}
		sender.sendMessage(ChatColor.LIGHT_PURPLE + "Removed " + target.getName() + " from your friends list.");
		friendHandler.pushRemove(user, target);
	}
	
	private void ignoreRequest(CommandSender sender, String[] args) {
		if(args.length == 1) {
			sender.sendMessage(ChatColor.RED + "Specify a friend request to ignore! /friend ignore <Player>");
			return;
		}
		User user = user(sender);
		User target = lookupUser(sender, args[1]);
		if(target == null) return;
		if(!FriendUtil.getIncoming(user).contains(target)) {
			sender.sendMessage(ChatColor.RED + "You don't have a friend request from " + target.getName() + "!");
			return;
		}
		boolean success = FriendUtil.denyRequest(user, target);
		if(!success) {
			sender.sendMessage(ChatColor.RED + "Something went wrong. Please try again later.");
			return;
		}
		sender.sendMessage(ChatColor.LIGHT_PURPLE + "Ignored friend request from " + target.getName());
		friendHandler.pushIgnore(user, target);
	}
	
	private void listRequests(CommandSender sender, String[] args) {
		Integer page = 1;
		if(args.length > 1) {
			page = parseInt(sender, args[1]);
			if(page == null) return;
		}
		User user = user(sender);
		List<User> all = FriendUtil.getIncoming(user);
		List<User> result = PaginationUtil.paginateList(all, page, PAGE_SIZE);
		sender.sendMessage(ChatColor.DARK_PURPLE + "You have " + all.size() + " incoming friend requests. Showing page " + page + " of " + (int) Math.ceil((double) all.size() / PAGE_SIZE)); 
		for(User friend : result) {
			TextComponent accept = StringUtil.clickableHoverableText(ChatColor.GREEN + "" + ChatColor.BOLD + "[ACCEPT] ", "/friend " + friend.getName(), ChatColor.GREEN + "Accept this request");
			TextComponent reject = StringUtil.clickableHoverableText(ChatColor.GRAY + "" + ChatColor.BOLD + "[IGNORE]   ", "/friend ignore " + friend.getName(), ChatColor.GRAY + "Ignore this request");
			sender.spigot().sendMessage(accept, reject, new TextComponent(ChatColor.LIGHT_PURPLE + friend.getName()));
		}
	}
	
	private void toggleRequests(CommandSender sender) {
		User user = user(sender);
		FriendUtil.toggleRequests(user);
		sender.sendMessage(ChatColor.LIGHT_PURPLE + "Toggled incoming friend requests " + (FriendUtil.allowsRequests(user) ? "ON" : "OFF"));
	}
	
	private void sendOrConfirm(CommandSender sender, String[] args) {
		User user = user(sender);
		User target = lookupUser(sender, args[0]);
		if(target == null) return;
		else if(target.equals(user) && !user.getLocalData().getBoolean("canSelfFriend", false)) {
			sender.sendMessage(ChatColor.RED + "You can't friend yourself!");
			return;
		}
		else if(FriendUtil.getIncoming(user).contains(target)) {
			boolean success = FriendUtil.acceptRequest(user, target);
			if(!success) {
				sender.sendMessage(ChatColor.RED + "Something went wrong. Please try again later.");
				return;
			}
			sender.sendMessage(SUCCESS_PREFIX + ChatColor.LIGHT_PURPLE + "You are now friends with " + target.getName() + "!");
			friendHandler.pushAccept(user, target);
		}
		else if(FriendUtil.getOutgoing(user).contains(target)) {
			sender.sendMessage(ChatColor.RED + "You've already sent a friend request to " + target.getName() + "!");
			return;
		}
		else if(!FriendUtil.allowsRequests(target)) {
			sender.sendMessage(ChatColor.RED + target.getName() + " does not allow friend requests!");
		}
		else if(target.getBlockedUsers().contains(user)) {
			sender.sendMessage(ChatColor.RED + target.getName() + " has blocked you!");
		}
		else if(user.getBlockedUsers().contains(target)) {
			sender.sendMessage(ChatColor.RED + "You can't send friend requests to a player you've blocked!");
		}
		else {
			if(target.getPlayer() == null) {
				target.resyncData();
				if(target.getServer() == null) {
					sender.sendMessage(ChatColor.RED + "Could not send request: " + target.getName() + " is not online on any servers!");
					return;
				}
			}
			boolean success = FriendUtil.sendRequest(user, target);
			if(!success) {
				sender.sendMessage(ChatColor.RED + "Something went wrong. Please try again later.");
				return;
			}
			sender.sendMessage(SUCCESS_PREFIX + ChatColor.LIGHT_PURPLE + "Sent a friend request to " + target.getName());
			friendHandler.pushRequest(user, target);
		}
	}
	
	public FriendCommand(DragonsSocial instance) {
		friendHandler = new FriendMessageHandler(instance);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		
		if(label.equalsIgnoreCase("toggleselffriend")) {
			toggleSelfFriend(sender);
		}
		
		else if(label.equalsIgnoreCase("dumpFriends")) {
			dumpFriendData(sender, args);
		}
		
		else if(args.length == 0) {
			showHelp(sender);
		}
		
		else if(args[0].equalsIgnoreCase("list")) {
			listFriends(sender, args);
		}
		
		else if(args[0].equalsIgnoreCase("remove")) {
			removeFriend(sender, args);
		}
		
		else if(args[0].equalsIgnoreCase("ignore")) {
			ignoreRequest(sender, args);
		}
		
		else if(args[0].equalsIgnoreCase("requests")) {
			listRequests(sender, args);
		}
		
		else if(args[0].equalsIgnoreCase("toggle")) {
			toggleRequests(sender);
		}
		
		else {
			sendOrConfirm(sender, args);
		}
		
		return true;
	}

}
