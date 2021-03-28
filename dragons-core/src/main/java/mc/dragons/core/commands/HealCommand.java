package mc.dragons.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class HealCommand extends DragonsCommandExecutor {

	private void healPlayer(Player player) {
		player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
	}
	
	private void healSelf(CommandSender sender) {
		if(!isPlayer(sender)) {
			sender.sendMessage(ChatColor.RED + "Specify a player to heal. /heal <player>");
			return;
		}
		healPlayer(player(sender));
		sender.sendMessage(ChatColor.GREEN + "Healed yourself successfully.");
	}
	
	private void healOther(CommandSender sender, String[] args) {
		Player player = lookupPlayer(sender, args[0]);
		if(player == null) return;
		if(player == sender) {
			healSelf(sender);
			return;
		}
		healPlayer(player);
		player.sendMessage(ChatColor.GREEN + "You have been healed.");
		sender.sendMessage(ChatColor.GREEN + "Healed " + player.getName() + " successfully.");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.MODERATOR)) return true;
		
		if(args.length == 0) {
			healSelf(sender);
		}
		else {
			healOther(sender, args);
		}

		return true;
	}
	
}
