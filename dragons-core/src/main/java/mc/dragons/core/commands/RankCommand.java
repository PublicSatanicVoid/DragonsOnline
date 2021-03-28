package mc.dragons.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.user.Rank;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;

public class RankCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return true;
		
		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /rank <player> <rank>");
			sender.sendMessage(ChatColor.RED + "Valid ranks are: " + StringUtil.parseList(Rank.values()));
			return true;
		}

		User targetUser = lookupUser(sender, args[0]);
		if(targetUser == null) return true;
		Player targetPlayer = targetUser.getPlayer();
		
		Rank rank = StringUtil.parseEnum(sender, Rank.class, args[1]);
		if (rank == null) {
			return true;
		}
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
