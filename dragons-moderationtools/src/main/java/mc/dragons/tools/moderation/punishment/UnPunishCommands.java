package mc.dragons.tools.moderation.punishment;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.gameobject.user.punishment.PunishmentType;
import mc.dragons.tools.moderation.DragonsModerationTools;

public class UnPunishCommands extends DragonsCommandExecutor {
	private PunishMessageHandler handler = JavaPlugin.getPlugin(DragonsModerationTools.class).getPunishMessageHandler();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a player! /" + label + " <player>");
			return true;
		}
		
		User targetUser = lookupUser(sender, args[0]);
		if(targetUser == null) return true;
		
		PunishmentType type = label.equalsIgnoreCase("unban") ? PunishmentType.BAN : PunishmentType.MUTE;
		targetUser.unpunish(type);
		
		// Check if we need to tell a different server to immediately apply the punishment
		if(targetUser.getServer() != null && !targetUser.getServer().equals(dragons.getServerName())) {
			handler.forwardUnpunishment(targetUser, type);
		}
		
		sender.sendMessage(ChatColor.GREEN + "Punishment revoked successfully.");
		
		return true;
	}

}
