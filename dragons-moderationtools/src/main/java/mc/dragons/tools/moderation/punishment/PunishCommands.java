package mc.dragons.tools.moderation.punishment;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.punishment.PunishmentType;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.DragonsModerationTools;

public class PunishCommands extends DragonsCommandExecutor {
	private PunishMessageHandler handler = JavaPlugin.getPlugin(DragonsModerationTools.class).getPunishMessageHandler();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		PunishmentType type = PunishmentType.fromDataHeader(label);
		
		if(!requirePermission(sender, type.getRequiredFlagToApply())) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a player! /" + label + " <player> [reason] "
				+ (label.equalsIgnoreCase("ban") || label.equalsIgnoreCase("mute") ? "[-d #y#w#d#h#m#s]" : ""));
			return true;
		}
		
		User targetUser = lookupUser(sender, args[0]);
		if(targetUser == null) return true;
		
		int durationIndex = StringUtil.getFlagIndex(args, "-d", 1);
		String duration = durationIndex == -1 ? "" : StringUtil.concatArgs(args, durationIndex + 1);
		String reason = StringUtil.concatArgs(args, 1, durationIndex == -1 ? args.length : durationIndex);
		long durationSeconds = StringUtil.parseTimespanToSeconds(duration);
		

		targetUser.punish(type, reason, durationSeconds);
		
		// Check if we need to tell a different server to immediately apply the punishment
		if(targetUser.getServer() != null && !targetUser.getServer().equals(dragons.getServerName())) {
			LOGGER.finer("forwarding punishment to " + targetUser.getServer());
			handler.forwardPunishment(targetUser, type, reason, durationSeconds);
		}
		
		sender.sendMessage(ChatColor.GREEN + "Punishment applied successfully.");
		
		return true;
	}

}
