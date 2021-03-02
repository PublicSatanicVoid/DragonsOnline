package mc.dragons.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;

public class SetVerificationCommand implements CommandExecutor {

	private UserLoader userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		
		if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.MODERATION, true)) return true;
		
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "/setverification <user> <true|false>");
			return true;
		}
		
		User target = userLoader.loadObject(args[0]);
		if(target == null) {
			sender.sendMessage(ChatColor.RED + "That user was not found!");
			return true;
		}
		
		boolean verified = Boolean.valueOf(args[1]);
		target.setVerified(verified);
		sender.sendMessage(ChatColor.GREEN + "User " + target.getName() + " is " + (verified ? "now" : "no longer") + " verified.");
		
		return true;
	}

}
