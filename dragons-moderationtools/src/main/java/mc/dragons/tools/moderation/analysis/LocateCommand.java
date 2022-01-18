package mc.dragons.tools.moderation.analysis;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.networking.InternalMessageHandler;

public class LocateCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.HELPER)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/locate <player>");
			return true;
		}
		
		InternalMessageHandler internalHandler = dragons.getInternalMessageHandler();
		
		long start = System.currentTimeMillis();
		User target = lookupUser(sender, args[0]);
		if(target == null) return true;
		String server = target.getServerName();
		
		if(server == null) {
			sender.sendMessage(ChatColor.RED + "That player is not currently connected to any server!");
			return true;
		}
		
		internalHandler.sendCheckUserPing(server, target.getUUID(), online -> {
			long end = System.currentTimeMillis();			
			if(online.isEmpty()) {
				if(hasPermission(sender, PermissionLevel.DEVELOPER)) { 
					sender.sendMessage(ChatColor.RED + "That player is not currently connected to any server! (Last server " + server + " did not respond)");
				}
				else {
					sender.sendMessage(ChatColor.RED + "That player is not currently connected to any server!");
				}
			}
			else if(online.get()) {
				sender.sendMessage(ChatColor.GREEN + target.getName() + " is connected to " + target.getServerName() + " (took " + (end - start) + "ms)");
			}
			else {
				if(hasPermission(sender, PermissionLevel.DEVELOPER)) { 
					sender.sendMessage(ChatColor.RED + "That player is not currently connected to any server! (Stale user data indicated " + server + ")");
				}
				else {
					sender.sendMessage(ChatColor.RED + "That player is not currently connected to any server!");
				}
			}
		});
		
		
		
		return true;
	}
}
