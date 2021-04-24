package mc.dragons.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.loader.FeedbackLoader;
import mc.dragons.core.storage.loader.FeedbackLoader.FeedbackEntry;
import mc.dragons.core.storage.mongo.pagination.PaginatedResult;
import mc.dragons.core.util.StringUtil;

public class FeedbackCommand extends DragonsCommandExecutor {
	private FeedbackLoader feedbackLoader = dragons.getLightweightLoaderRegistry().getLoader(FeedbackLoader.class);

	private void showHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.YELLOW + "/feedback <Your feedback here>");
		if (hasPermission(sender, PermissionLevel.GM)) {
			sender.sendMessage(ChatColor.YELLOW + "/feedback -list [Page#]");
			sender.sendMessage(ChatColor.YELLOW + "/feedback -unread <Feedback#>");
		}
	}
	
	private void displayFeedback(CommandSender sender, FeedbackEntry entry) {
		sender.sendMessage(ChatColor.YELLOW + "[#" + entry.getId() + "] " + ChatColor.GREEN + "[" + entry.getFrom() + "] " + ChatColor.RESET + entry.getFeedback());
		feedbackLoader.markRead(entry.getId(), true);
	}
	
	/* 0=-list, 1=[page#] */
	private void listUnread(CommandSender sender, String[] args) {
		Integer page = parseInt(sender, args[1]);
		if(page == null) return;
		
		PaginatedResult<FeedbackEntry> results = feedbackLoader.getUnreadFeedback(page);
		if(results.getPage().size() == 0) {
			sender.sendMessage(ERR_NO_RESULTS);
			return;
		}
		
		sender.sendMessage(ChatColor.GREEN + "Listing unread feedback (Page " + page + " of " + results.getPages() + ", " + results.getTotal() + " total)");
		results.getPage().forEach(entry -> displayFeedback(sender, entry));
	}
	
	/* 0=-unread, 1=<feedbackID> */
	private void markUnread(CommandSender sender, String[] args) {
		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /feedback -unread <Feedback#>");
			return;
		}
		Integer id = parseInt(sender, args[1]);
		if(id == null) return;
		
		feedbackLoader.markRead(id, false);
		sender.sendMessage(ChatColor.GREEN + "Marked feedback entry " + args[1] + " as unread.");
	}
	
	/* 0->n=<feedback> */
	private void submitFeedback(CommandSender sender, String[] args) {
		String feedback = StringUtil.concatArgs(args, 0);
		feedbackLoader.addFeedback(user(sender).getName(), feedback);
		sender.sendMessage(ChatColor.GREEN + "Your feedback has been recorded. Thank you for submitting it!");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		
		if (args.length == 0) {
			showHelp(sender);
			return true;
		}
		if (args[0].equalsIgnoreCase("-list")) {
			if (!requirePermission(sender, PermissionLevel.GM)) return true;
			listUnread(sender, args);
			return true;
		}
		if (args[0].equalsIgnoreCase("-unread")) {
			if (!requirePermission(sender, PermissionLevel.GM)) return true;
			markUnread(sender, args);
			return true;
		}
		submitFeedback(sender, args);
		return true;
	}
}
