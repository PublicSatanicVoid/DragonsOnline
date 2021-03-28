package mc.dragons.tools.moderation.abilities;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class FlyCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.HELPER)) return true;
		
		Player player = player(sender);
		player.setAllowFlight(!player.getAllowFlight());
		sender.sendMessage(ChatColor.GREEN + (player.getAllowFlight() ? "Enabled" : "Disabled") + " fly mode.");
		
		return true;
	}

}
