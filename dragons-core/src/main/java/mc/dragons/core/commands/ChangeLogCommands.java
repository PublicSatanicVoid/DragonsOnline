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
	
	private void viewNews(CommandSender sender, User user) {
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
			managerShowHelp(sender);
		}
		else if(args[0].equalsIgnoreCase("title")) {
			managerSetTitle(sender, user, args);
		}
		else if(args[0].equalsIgnoreCase("add")) {
			managerAddLine(sender, user, args);
		}
		else if(args[0].equalsIgnoreCase("remove")) {
			managerRemoveLine(sender, user, args);
		}
		else if(args[0].equalsIgnoreCase("preview")) {
			managerPreview(sender, user);
		}
		else if(args[0].equalsIgnoreCase("publish")) {
			managerPublish(sender, user);
		}
		else {
			managerShowHelp(sender);
		}
	}
	
	private void managerShowHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.YELLOW + "/newsmanager title <title>");
		sender.sendMessage(ChatColor.YELLOW + "/newsmanager add <changelog line>");
		sender.sendMessage(ChatColor.YELLOW + "/newsmanager remove <changelog line#>");
		sender.sendMessage(ChatColor.YELLOW + "/newsmanager preview");
		sender.sendMessage(ChatColor.YELLOW + "/newsmanager publish");
	}
	
	private void managerSetTitle(CommandSender sender, User user, String[] args) {
		user.getLocalData().append("news.title", StringUtil.concatArgs(args, 1));
		sender.sendMessage(ChatColor.GREEN + "Set title of post successfully.");
	}
	
	private void managerAddLine(CommandSender sender, User user, String[] args) {
		List<String> lines = user.getLocalData().getList("news.lines", String.class);
		if (lines == null) {
			lines = new ArrayList<>();
		}
		lines.add(ChatColor.translateAlternateColorCodes('&',StringUtil.concatArgs(args, 1)));
		user.getLocalData().append("news.lines", lines);
		sender.sendMessage(ChatColor.GREEN + "Added line to post successfully.");	
	}
	
	private void managerRemoveLine(CommandSender sender, User user, String[] args) {
		Integer lineNo = parseIntType(sender, args[1]);
		if(lineNo == null) return;
		
		user.getLocalData().getList("news.lines", String.class).remove((int) lineNo);
		sender.sendMessage(ChatColor.GREEN + "Removed line from post successfully.");
	}
	
	private void managerPreview(CommandSender sender, User user) {
		sender.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + user.getLocalData().getString("news.title"));
		sender.sendMessage(ChatColor.LIGHT_PURPLE + user.getName() + " - " + Date.from(Instant.now()).toString());
		int i = 0;
		for (String line : user.getLocalData().getList("news.lines", String.class)) {
			sender.sendMessage(ChatColor.DARK_GREEN + "#" + i + ": " + ChatColor.GRAY + line);
			i++;
		}
	}
	
	private void managerPublish(CommandSender sender, User user) {
		changeLogLoader.addChangeLog(user.getName(), user.getLocalData().getString("news.title"), user.getLocalData().getList("news.lines", String.class));
		sender.sendMessage(ChatColor.GREEN + "Published post successfully.");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		requirePlayer(sender);
		User user = user(sender);
		
		if (label.equalsIgnoreCase("whatsnew") || label.equalsIgnoreCase("news")) {
			viewNews(sender, user);
		}
		else if (label.equalsIgnoreCase("newsmanager")) {
			manageNews(sender, user, args);
		}
		return true;
	}
}
