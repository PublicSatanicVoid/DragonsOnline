package mc.dragons.dev.notifier;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;

public class TipsCommand extends DragonsCommandExecutor {

	public static final String DISABLE_TIPS = "disableTips";
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		
		User user = user(sender);
		user.getStorageAccess().set(DISABLE_TIPS, !user.getData().getBoolean(DISABLE_TIPS, false));
		sender.sendMessage(ChatColor.GREEN + "Toggled staff tip notifications " + (user.getData().getBoolean(DISABLE_TIPS, false) ? "off" : "on"));
		
		return true;
	}

}
