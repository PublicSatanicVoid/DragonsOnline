package mc.dragons.tools.moderation.punishment.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.tools.moderation.punishment.PunishmentCode;

public class PunishCodesCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.HELPER)) return true;
		
		sender.sendMessage(ChatColor.DARK_GREEN + "Listing all punishment codes:");
		for(PunishmentCode code : PunishmentCode.values()) {
			if(code.isHidden()) continue;
			sender.sendMessage(" " + code.getCode() + ChatColor.GRAY + " - " + code.getName());
		}
		
		if(hasPermission(sender, SystemProfileFlag.HELPER)) {
			sender.sendMessage(ChatColor.ITALIC + "Do /punish <player> <code> to apply a punishment");
		}
		
		return true;
	}

}
