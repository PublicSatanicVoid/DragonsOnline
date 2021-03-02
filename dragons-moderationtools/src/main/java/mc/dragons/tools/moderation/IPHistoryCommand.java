package mc.dragons.tools.moderation;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.storage.mongo.pagination.PaginationUtil;
import mc.dragons.core.util.PermissionUtil;

public class IPHistoryCommand implements CommandExecutor {

	private UserLoader userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.MODERATION, true)) return true;
		}	
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/iphistory <player> [page#]");
			return true;
		}
		
		int page = 1;
		if(args.length > 1) page = Integer.valueOf(args[1]);
		
		User target = userLoader.loadObject(args[0]);
		if(target == null) {
			sender.sendMessage(ChatColor.RED + "That user was not found!");
			return true;
		}
		
		List<String> ips = PaginationUtil.paginateList(target.getIPHistory(), page, 10);
		int pages = (int) Math.ceil((double) target.getIPHistory().size() / 10);
		
		sender.sendMessage(ChatColor.DARK_GREEN + "IP History for user " + target.getName() + ChatColor.GREEN + " (Page " + page + " of " + pages + " - " + target.getIPHistory().size() + " results)");
		for(String ip : ips) {
			sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.RESET + ip);
		}
		
		return true;
	}

}
