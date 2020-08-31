package mc.dragons.tools.moderation;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.User.PunishmentType;
import mc.dragons.core.storage.impl.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;

public class UnPunishCommands implements CommandExecutor {

	private UserLoader userLoader;
	
	public UnPunishCommands() {
		userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	
		Player player = null; 
		User user = null;
		
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			//if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.MOD, true)) return true;
			if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.MODERATION, true)) return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a player! /" + label + " <player> [reason] [-d #y#w#d#h#m#s]");
			return true;
		}
		
		User targetUser = userLoader.loadObject(args[0]);
		
		if(targetUser == null) {
			sender.sendMessage(ChatColor.RED + "That player was not found!");
			return true;
		}
		
		targetUser.unpunish(label.equalsIgnoreCase("unban") ? PunishmentType.BAN : PunishmentType.MUTE);
		sender.sendMessage(ChatColor.GREEN + "Punishment removed successfully.");
		
		return true;
	}

}
