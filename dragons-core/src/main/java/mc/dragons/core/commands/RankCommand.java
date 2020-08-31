package mc.dragons.core.commands;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.Rank;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankCommand implements CommandExecutor {
	private UserLoader userLoader = GameObjectType.USER.<User, UserLoader>getLoader();

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if (!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true))
				return true;
		}
		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /rank <player> <rank>");
			return true;
		}
		String username = args[0];
		Player targetPlayer = Bukkit.getPlayerExact(username);
		User targetUser = this.userLoader.loadObject(username);
		Rank rank = null;
		try {
			rank = Rank.valueOf(args[1]);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Invalid rank! Valid ranks are " + StringUtil.parseList((Object[]) Rank.values()));
		}
		if (rank == null)
			return true;
		targetUser.setRank(rank);
		if (targetPlayer == null) {
			sender.sendMessage(ChatColor.YELLOW + "Player is not online on this server! They will have to rejoin for the rank update to be fully applied.");
		} else {
			targetPlayer.sendTitle(ChatColor.DARK_GRAY + "Rank Update", rank.getNameColor() + rank.getRankName(), 20, 40, 20);
			targetPlayer.sendMessage(ChatColor.GRAY + "Your rank was updated to " + rank.getNameColor() + rank.getRankName());
		}
		sender.sendMessage(ChatColor.GREEN + "Rank updated successfully.");
		return true;
	}
}
