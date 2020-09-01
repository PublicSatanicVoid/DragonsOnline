package mc.dragons.core.commands;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.storage.impl.loader.ChangeLogLoader;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class ChangeLogCommands implements CommandExecutor {
	private ChangeLogLoader changeLogLoader;

	public ChangeLogCommands(Dragons instance) {
		this.changeLogLoader = instance.getLightweightLoaderRegistry().getLoader(ChangeLogLoader.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command!");
			return true;
		}
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		if (label.equalsIgnoreCase("whatsnew") || label.equalsIgnoreCase("news")) {
			List<ChangeLogLoader.ChangeLogEntry> unread = this.changeLogLoader.getUnreadChangelogs(user.getLastReadChangeLogId());
			if (unread.size() == 0) {
				sender.sendMessage(ChatColor.RED + "You have no unread announcements or changelogs!");
				return true;
			}
			for (ChangeLogLoader.ChangeLogEntry entry : unread) {
				sender.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + entry.getTitle());
				sender.sendMessage(ChatColor.LIGHT_PURPLE + entry.getBy() + " - " + entry.getDate());
				for (String line : entry.getChangeLog())
					sender.sendMessage(ChatColor.GRAY + "- " + line);
				sender.sendMessage("");
			}
			user.markChangeLogsRead();
			return true;
		}
		if (label.equalsIgnoreCase("newsmanager")) {
			if (!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, true))
				return true;
			if (args.length == 0) {
				sender.sendMessage(ChatColor.YELLOW + "/newsmanager title <title>");
				sender.sendMessage(ChatColor.YELLOW + "/newsmanager add <changelog line>");
				sender.sendMessage(ChatColor.YELLOW + "/newsmanager remove <changelog line#>");
				sender.sendMessage(ChatColor.YELLOW + "/newsmanager preview");
				sender.sendMessage(ChatColor.YELLOW + "/newsmanager publish");
				return true;
			}
			if (args[0].equalsIgnoreCase("title")) {
				user.getLocalData().append("news.title", StringUtil.concatArgs(args, 1));
				sender.sendMessage(ChatColor.GREEN + "Set title of post successfully.");
				return true;
			}
			if (args[0].equalsIgnoreCase("add")) {
				List<String> lines = user.getLocalData().getList("news.lines", String.class);
				if (lines == null)
					lines = new ArrayList<>();
				lines.add(StringUtil.concatArgs(args, 1));
				user.getLocalData().append("news.lines", lines);
				sender.sendMessage(ChatColor.GREEN + "Added line to post successfully.");
				return true;
			}
			if (args[0].equalsIgnoreCase("remove")) {
				user.getLocalData().getList("news.lines", String.class).remove(Integer.valueOf(args[1]).intValue());
				sender.sendMessage(ChatColor.GREEN + "Removed line from post successfully.");
				return true;
			}
			if (args[0].equalsIgnoreCase("preview")) {
				sender.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + user.getLocalData().getString("news.title"));
				sender.sendMessage(ChatColor.LIGHT_PURPLE + user.getName() + " - " + Date.from(Instant.now()).toString());
				int i = 0;
				for (String line : user.getLocalData().getList("news.lines", String.class)) {
					sender.sendMessage(ChatColor.DARK_GREEN + "#" + i + ": " + ChatColor.GRAY + line);
					i++;
				}
				return true;
			}
			if (args[0].equalsIgnoreCase("publish")) {
				this.changeLogLoader.addChangeLog(user.getName(), user.getLocalData().getString("news.title"), user.getLocalData().getList("news.lines", String.class));
				sender.sendMessage(ChatColor.GREEN + "Published post successfully.");
				return true;
			}
			sender.sendMessage(ChatColor.RED + "Invalid arguments! For usage info, do /newsmanager");
			return true;
		}
		return true;
	}
}
