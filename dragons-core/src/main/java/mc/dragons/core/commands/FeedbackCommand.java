package mc.dragons.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.loader.FeedbackLoader;
import mc.dragons.core.storage.loader.FeedbackLoader.FeedbackEntry;
import mc.dragons.core.storage.mongo.pagination.PaginatedResult;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class FeedbackCommand implements CommandExecutor {
	private FeedbackLoader feedbackLoader;

	public FeedbackCommand(Dragons instance) {
		feedbackLoader = instance.getLightweightLoaderRegistry().getLoader(FeedbackLoader.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
		} else {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command.");
			return true;
		}
		if (args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/feedback <Your feedback here>");
			if (PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, false)) {
				sender.sendMessage(ChatColor.YELLOW + "/feedback -list [Page#]");
				sender.sendMessage(ChatColor.YELLOW + "/feedback -unread <Feedback#>");
			}
			return true;
		}
		if (args[0].equalsIgnoreCase("-list")) {
			if (!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, true)) {
				return true;
			}
			int page = 1;
			if(args.length > 1) {
				page = Integer.valueOf(args[1]);
			}
			PaginatedResult<FeedbackEntry> results = feedbackLoader.getUnreadFeedback(page);
			sender.sendMessage(ChatColor.GREEN + "Listing unread feedback (Page " + page + " of " + results.getPages() + ", " + results.getTotal() + " total)");
			for (FeedbackEntry entry : results.getPage()) {
				sender.sendMessage(ChatColor.YELLOW + "[#" + entry.getId() + "] " + ChatColor.GREEN + "[" + entry.getFrom() + "] " + ChatColor.RESET + entry.getFeedback());
				feedbackLoader.markRead(entry.getId(), true);
			}
			return true;
		}
		if (args[0].equalsIgnoreCase("-unread")) {
			if (!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, true)) {
				return true;
			}
			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "Insufficient arguments! /feedback -unread <FeedbackUUID>");
				return true;
			}
			try {
				int uuid = Integer.valueOf(args[1]).intValue();
				feedbackLoader.markRead(uuid, false);
				sender.sendMessage(ChatColor.GREEN + "Marked feedback entry " + args[1] + " as unread.");
			} catch (IllegalArgumentException e) {
				sender.sendMessage(ChatColor.RED + "Invalid ID!");
			}
			return true;
		}
		String feedback = StringUtil.concatArgs(args, 0);
		feedbackLoader.addFeedback(user.getName(), feedback);
		sender.sendMessage(ChatColor.GREEN + "Your feedback has been recorded. Thank you for submitting it!");
		return true;
	}
}
