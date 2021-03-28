package mc.dragons.tools.moderation.punishment;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.gameobject.user.punishment.PunishmentType;

public class UnPunishCommands extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a player! /" + label + " <player>");
			return true;
		}
		
		User targetUser = lookupUser(sender, args[0]);
		if(targetUser == null) return true;
		
		targetUser.unpunish(label.equalsIgnoreCase("unban") ? PunishmentType.BAN : PunishmentType.MUTE);
		sender.sendMessage(ChatColor.GREEN + "Punishment revoked successfully.");
		
		return true;
	}

}
