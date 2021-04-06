package mc.dragons.npcs.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class ToggleCompanionLaunch extends DragonsCommandExecutor {

	public static final String DISABLE_COMPANION_LAUNCH = "disableCompanionLaunch";
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.GM)) return true;
		
		user(sender).getLocalData().append(DISABLE_COMPANION_LAUNCH, !user(sender).getLocalData().getBoolean(DISABLE_COMPANION_LAUNCH, false));
		sender.sendMessage(ChatColor.GREEN + "Toggled companion launch invincibility.");
		
		return true;
	}

}
