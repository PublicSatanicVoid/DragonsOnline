package mc.dragons.tools.moderation.punishment.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.tools.moderation.WrappedUser;

public class AcknowledgeWarningCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) ||args.length == 0) return true;
		
		WrappedUser wrappedUser = WrappedUser.of(user(sender));
		Integer id = parseInt(sender, args[0]);
		if(id == null) return true;
		wrappedUser.acknowledgeWarning(id);
		sender.sendMessage(ChatColor.GRAY + "Warning acknowledged.");
		
		return true;
	}

}
