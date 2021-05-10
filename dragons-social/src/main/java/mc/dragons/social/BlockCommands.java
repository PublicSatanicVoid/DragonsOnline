package mc.dragons.social;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;
import mc.dragons.social.friend.FriendUtil;

public class BlockCommands extends DragonsCommandExecutor {

	private void toggleSelfBlock(CommandSender sender) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return;
		User user = user(sender);
		user.getLocalData().append("canSelfBlock", !user.getLocalData().getBoolean("canSelfBlock", false));
		sender.sendMessage(ChatColor.GREEN + "Toggled self-block ability.");
	}
	
	private void doBlock(CommandSender sender, User user, User target, boolean block) {
		if(block) {
			user.blockUser(target);
			sender.sendMessage(ChatColor.GREEN + "Blocked " + target.getName() + " successfully. You will no longer see their messages and they cannot send you friend requests.");
			if(FriendUtil.getOutgoing(user).contains(target)) {
				FriendUtil.denyRequest(user, target);
			}
			if(FriendUtil.getIncoming(user).contains(target)) {
				FriendUtil.denyRequest(target, user);
			}
		}
		else {
			user.unblockUser(target);
			sender.sendMessage(ChatColor.GREEN + "Un-blocked " + target.getName() + " successfully.");
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		boolean block = label.equalsIgnoreCase("block");
		String action = block ? "block" : "unblock";

		User user = user(sender);
		
		if(label.equalsIgnoreCase("toggleselfblock")) {
			toggleSelfBlock(sender);
			return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a player to " + action + "! /" + label + " <Player>");
			List<User> blockedUsers = user.getBlockedUsers();
			if(!block && blockedUsers.size() > 0) {
				sender.sendMessage(ChatColor.GRAY + "Click a user below to unblock them:");
				for(User blocked : blockedUsers) {
					sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + "- " + ChatColor.RESET + blocked.getName(),
							"/unblock " + blocked.getName(), "Click to un-block " + blocked.getName()));
				}
			}
			return true;
		}
		
		User target = lookupUser(sender, args[0]);
		if(target == null) return true;
		if(target.equals(user) && !user.getLocalData().getBoolean("canSelfBlock", false)) {
			sender.sendMessage(ChatColor.RED + "You can't block yourself!");
			return true;
		}
		
		if(user.getBlockedUsers().contains(target) == block) {
			sender.sendMessage(ChatColor.RED + (block ? "You have already blocked " : "You have not blocked ") + target.getName() + "!");
			return true;
		}
		
		doBlock(sender, user, target, block);
		
		return true;
	}

}
