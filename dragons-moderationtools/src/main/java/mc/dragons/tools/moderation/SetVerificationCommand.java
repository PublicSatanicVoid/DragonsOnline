package mc.dragons.tools.moderation;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;

public class SetVerificationCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "/setverification <user> <true|false>");
			return true;
		}
		
		User target = lookupUser(sender, args[0]);
		if(target == null) return true;
		
		Boolean verified = parseBooleanType(sender, args[1]);
		if(verified == null) return true;
		
		target.setVerified(verified);
		sender.sendMessage(ChatColor.GREEN + "User " + target.getName() + " is " + (verified ? "now" : "no longer") + " verified.");
		
		return true;
	}

}
