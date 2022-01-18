package mc.dragons.core.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class LogoutCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Bukkit.dispatchCommand(sender, "syslogon logout -clean");
		return true;
	}

}
