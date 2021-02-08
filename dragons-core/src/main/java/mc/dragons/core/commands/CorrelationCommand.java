package mc.dragons.core.commands;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.logging.correlation.CorrelationLogLoader;
import mc.dragons.core.logging.correlation.CorrelationLogLoader.CorrelationLogEntry;
import mc.dragons.core.util.PermissionUtil;
import net.md_5.bungee.api.ChatColor;

public class CorrelationCommand implements CommandExecutor {

	private CorrelationLogLoader loader;
	
	public CorrelationCommand(Dragons instance) {
		loader = instance.getLightweightLoaderRegistry().getLoader(CorrelationLogLoader.class);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true)) return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "View log entries for a given correlation ID.");
			sender.sendMessage(ChatColor.GRAY + "/correlation <beginning of ID> [min. log level to display]");
			return true;
		}
		
		String beginning = args[0];
		if(beginning.length() < 5) {
			sender.sendMessage(ChatColor.RED + "Please specify at least five characters of the correlation ID!");
			return true;
		}
		Level minLevel = Level.FINEST;
		if(args.length > 1) {
			minLevel = Level.parse(args[1].toUpperCase());
		}
		List<CorrelationLogEntry> result = loader.getAllByCorrelationID(beginning);
		Set<UUID> ids = new HashSet<>();
		for(CorrelationLogEntry log : result) {
			if(!log.getCorrelationID().toString().startsWith(beginning)) continue; // Hotfix for when an unwanted correlation ID is returned
			ids.add(log.getCorrelationID());
		}
		if(ids.size() == 0) {
			sender.sendMessage(ChatColor.RED + "No results found with a correlation ID beginning with " + beginning);
			return true;
		}
		if(ids.size() > 1) {
			sender.sendMessage(ChatColor.RED + "Multiple correlation IDs exist beginning with " + beginning);
			for(UUID id : ids) {
				sender.sendMessage(ChatColor.RED + "- " + id);
			}
			sender.sendMessage(ChatColor.RED + "Please specify more characters to uniquely identify the desired correlation ID.");
			return true;
		}
		sender.sendMessage(ChatColor.YELLOW + "Log entries for correlation ID " + ids.iterator().next() + ":");
		for(CorrelationLogEntry log : result) {
			if(log.getLevel().intValue() < minLevel.intValue()) continue;
			sender.sendMessage(ChatColor.GRAY + "[" + log.getTimestamp() + "/" + log.getInstance() + "/" + log.getLevel().getName() + "] " + ChatColor.WHITE + log.getMessage());
		}
		
		return true;
	}

}
