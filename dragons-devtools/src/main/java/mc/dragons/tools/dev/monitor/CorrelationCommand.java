package mc.dragons.tools.dev.monitor;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.logging.correlation.CorrelationLogger.CorrelationLogEntry;
import net.md_5.bungee.api.ChatColor;

public class CorrelationCommand extends DragonsCommandExecutor {
	
	private void showHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.YELLOW + "View log entries for a given correlation ID.");
		sender.sendMessage(ChatColor.GRAY + "/correlation <beginning of ID> [min. log level to display|--purge]");
	}
	
	private void displayEntry(CommandSender sender, CorrelationLogEntry log) {
		sender.sendMessage(ChatColor.GRAY + "[" + log.getTimestamp() + "/" + log.getInstance() + "/" + log.getLevel().getName() + "] " + ChatColor.WHITE + log.getMessage());
	}
	
	private UUID lookupCID(CommandSender sender, String startsWith) {
		List<CorrelationLogEntry> result = CORRELATION.getAllByCorrelationID(startsWith);
		Set<UUID> ids = result.stream()
				.filter(log -> log.getCorrelationID().toString().startsWith(startsWith))
				.map(log -> log.getCorrelationID())
				.collect(Collectors.toSet());
		if(ids.size() == 0) {
			sender.sendMessage(ChatColor.RED + "No results found with a correlation ID beginning with " + startsWith);
			return null;
		}
		if(ids.size() > 1) {
			sender.sendMessage(ChatColor.RED + "Multiple correlation IDs exist beginning with " + startsWith);
			for(UUID id : ids) {
				sender.sendMessage(ChatColor.RED + "- " + id);
			}
			sender.sendMessage(ChatColor.RED + "Please specify more characters to uniquely identify the desired correlation ID.");
			return null;
		}
		return ids.iterator().next();
	}
	
	private void viewEntries(CommandSender sender, Level minLevel, String startsWith) {
		UUID cid = lookupCID(sender, startsWith);
		if(cid == null) return;
		sender.sendMessage(ChatColor.YELLOW + "Log entries for correlation ID " + cid + ":");
		CORRELATION.getAllByCorrelationID(cid).stream()
			.filter(log -> log.getLevel().intValue() >= minLevel.intValue())
			.forEach(log -> displayEntry(sender, log));
	}
	
	private void purgeEntries(CommandSender sender, String startsWith) {
		UUID cid = lookupCID(sender, startsWith);
		if(cid == null) return;
		CORRELATION.discard(cid);
		sender.sendMessage(ChatColor.GREEN + "Purged all logs with correlation ID " + cid);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return true;
		
		if(args.length == 0) {
			showHelp(sender);
			return true;
		}
		
		String startsWith = args[0];
		if(startsWith.length() < 5) {
			sender.sendMessage(ChatColor.RED + "Please specify at least five characters of the correlation ID!");
			return true;
		}
		
		if(args.length > 1 && args[1].equalsIgnoreCase("--purge")) {
			purgeEntries(sender, startsWith);
		}
		else {
			Level minLevel = Level.FINEST;
			if(args.length > 1) {
				minLevel = lookup(sender, () -> Level.parse(args[1].toUpperCase()), ChatColor.RED + "Invalid log level!");
			}
			viewEntries(sender, minLevel, startsWith);
		}
		
		return true;
	}

}
