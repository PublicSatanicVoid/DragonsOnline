package mc.dragons.core.commands;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.loader.ChangeLogLoader;
import mc.dragons.core.storage.loader.ChangeLogLoader.ChangeLogEntry;
import mc.dragons.core.util.StringUtil;

public class ChangeLogCommands extends DragonsCommandExecutor {
	private ChangeLogLoader changeLogLoader;

	public ChangeLogCommands() {
		changeLogLoader = instance.getLightweightLoaderRegistry().getLoader(ChangeLogLoader.class);
	}
	
	private void viewNews(CommandSender sender, User user, String[] args) {
		if(!requirePlayer(sender)) return;
		List<ChangeLogEntry> unread = changeLogLoader.getUnreadChangelogs(user.getLastReadChangeLogId());
		if (unread.size() == 0) {
			sender.sendMessage(ChatColor.RED + "You have no unread announcements or changelogs!");
			return;
		}
		for (ChangeLogEntry entry : unread) {
			sender.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + entry.getTitle());
			sender.sendMessage(ChatColor.LIGHT_PURPLE + entry.getBy() + " - " + entry.getDate());
			for (String line : entry.getChangeLog()) {
				sender.sendMessage(ChatColor.GRAY + "- " + line);
			}
			sender.sendMessage("");
		}
		user.markChangeLogsRead();
	}

	private void manageNews(CommandSender sender, User user, String[] args) {
		if(!requirePermission(sender, PermissionLevel.GM)) return;
		if(args.length == 0) {
			manageNews_showHelp(sender);
		}
		else if(args[0].equalsIgnoreCase("title")) {
			manageNews_setTitle(sender, user, args);
		}
		else if(args[0].equalsIgnoreCase("add")) {
			manageNews_addLine(sender, user, args);
		}
		else if(args[0].equalsIgnoreCase("remove")) {
			manageNews_removeLine(sender, user, args);
		}
		else if(args[0].equalsIgnoreCase("preview")) {
			manageNews_preview(sender, user, args);
		}
		else if(args[0].equalsIgnoreCase("publish")) {
			manageNews_publish(sender, user, args);
		}
		else {
			manageNews_showHelp(sender);
		}
	}
	
	private void manageNews_showHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.YELLOW + "/newsmanager title <title>");
		sender.sendMessage(ChatColor.YELLOW + "/newsmanager add <changelog line>");
		sender.sendMessage(ChatColor.YELLOW + "/newsmanager remove <changelog line#>");
		sender.sendMessage(ChatColor.YELLOW + "/newsmanager preview");
		sender.sendMessage(ChatColor.YELLOW + "/newsmanager publish");
	}
	
	/* 0=title, 1->n=<title> */
	private void manageNews_setTitle(CommandSender sender, User user, String[] args) {
		user.getLocalData().append("news.title", StringUtil.concatArgs(args, 1));
		sender.sendMessage(ChatColor.GREEN + "Set title of post successfully.");
	}
	
	/* 0=add, 1->n=<line> */
	private void manageNews_addLine(CommandSender sender, User user, String[] args) {
		List<String> lines = user.getLocalData().getList("news.lines", String.class);
		if (lines == null) {
			lines = new ArrayList<>();
		}
		lines.add(ChatColor.translateAlternateColorCodes('&',StringUtil.concatArgs(args, 1)));
		user.getLocalData().append("news.lines", lines);
		sender.sendMessage(ChatColor.GREEN + "Added line to post successfully.");	
	}
	
	/* 0=remove, 1=<line#> */
	private void manageNews_removeLine(CommandSender sender, User user, String[] args) {
		Integer lineNo = parseIntType(sender, args[1]);
		if(lineNo == null) return;
		
		user.getLocalData().getList("news.lines", String.class).remove((int) lineNo);
		sender.sendMessage(ChatColor.GREEN + "Removed line from post successfully.");
	}
	
	/* 0=preview */
	private void manageNews_preview(CommandSender sender, User user, String[] args) {
		sender.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + user.getLocalData().getString("news.title"));
		sender.sendMessage(ChatColor.LIGHT_PURPLE + user.getName() + " - " + Date.from(Instant.now()).toString());
		int i = 0;
		for (String line : user.getLocalData().getList("news.lines", String.class)) {
			sender.sendMessage(ChatColor.DARK_GREEN + "#" + i + ": " + ChatColor.GRAY + line);
			i++;
		}
	}
	
	/* 0=publish */
	private void manageNews_publish(CommandSender sender, User user, String[] args) {
		changeLogLoader.addChangeLog(user.getName(), user.getLocalData().getString("news.title"), user.getLocalData().getList("news.lines", String.class));
		sender.sendMessage(ChatColor.GREEN + "Published post successfully.");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		requirePlayer(sender);
		User user = user(sender);
		
		if (label.equalsIgnoreCase("whatsnew") || label.equalsIgnoreCase("news")) {
			viewNews(sender, user, args);
		}
		else if (label.equalsIgnoreCase("newsmanager")) {
			manageNews(sender, user, args);
		}
		return true;
	}
}
