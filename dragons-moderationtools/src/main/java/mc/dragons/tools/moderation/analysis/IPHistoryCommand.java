package mc.dragons.tools.moderation.analysis;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.mongo.pagination.PaginationUtil;

public class IPHistoryCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/iphistory <player> [page#]");
			return true;
		}
		
		Integer page = 1;
		if(args.length > 1) page = parseInt(sender, args[1]);
		
		User target = lookupUser(sender, args[0]);
		
		if(page == null || target == null) return true;
		
		List<String> ips = PaginationUtil.paginateList(target.getIPHistory(), page, 10);
		int pages = (int) Math.ceil((double) target.getIPHistory().size() / 10);
		
		sender.sendMessage(ChatColor.DARK_GREEN + "IP History for user " + target.getName() + ChatColor.GREEN + " (Page " + page + " of " + pages + " - " + target.getIPHistory().size() + " results)");
		for(String ip : ips) {
			sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.RESET + ip);
		}
		
		return true;
	}

}
