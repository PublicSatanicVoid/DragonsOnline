package mc.dragons.npcs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CompanionCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		sender.sendMessage("This command is not ready yet!!!!! Go bother DragonRider747 or something");
		return true;
	}

}
