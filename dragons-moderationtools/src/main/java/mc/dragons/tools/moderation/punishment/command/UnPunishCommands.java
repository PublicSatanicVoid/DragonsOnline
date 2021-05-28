package mc.dragons.tools.moderation.punishment.command;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.DragonsModerationTools;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.punishment.PunishMessageHandler;
import mc.dragons.tools.moderation.punishment.PunishmentData;
import mc.dragons.tools.moderation.punishment.PunishmentType;
import mc.dragons.tools.moderation.punishment.RevocationCode;

public class UnPunishCommands extends DragonsCommandExecutor {
	private PunishMessageHandler handler = JavaPlugin.getPlugin(DragonsModerationTools.class).getPunishMessageHandler();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.HELPER)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a player and code! /" + label + " <player> <revocation code>");
//			sender.sendMessage(ChatColor.GRAY + "Warning: It is recommended to use /removepunishment to remove a specific punishment. This command revokes only the first applicable punishment.");
			return true;
		}
		
		if(args.length == 1) {
			sender.sendMessage(ChatColor.RED + "Select a revocation code below:");
			for(RevocationCode code : RevocationCode.values()) {
				sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + "- " + ChatColor.RESET + code.getCode() + ChatColor.GRAY + " - " + code.getDescription(),
					"/" + label + " " + args[0] + " " + code.getCode(), code.getDescription()));
			}
			return true;
		}
		
		User targetUser = lookupUser(sender, args[0]);
		RevocationCode code = RevocationCode.parseCode(sender, args[1]);
		if(targetUser == null || code == null) return true;
		
		WrappedUser wrapped = WrappedUser.of(targetUser);
		
		PunishmentType type = label.equalsIgnoreCase("unban") ? PunishmentType.BAN : PunishmentType.MUTE;
		List<PunishmentData> history = wrapped.getPunishmentHistory();
		PunishmentData record = null;
		int id = -1;
		for(int i = 0; i < history.size(); i++) {
			if(history.get(i).getType() == type && !history.get(i).hasExpired() && !history.get(i).isRevoked()) {
				id = i;
				record = history.get(i);
				break;
			}
		}
		if(record == null) {
			sender.sendMessage(ChatColor.RED + "This player does not have any active punishments matching those criteria!");
			return true;
		}
		if(!record.getIssuedBy().equals(user(sender)) && !hasPermission(sender, SystemProfileFlag.APPEALS_TEAM) || !hasPermission(sender, code.getPermissionToApply())) {
			sender.sendMessage(ChatColor.RED + "You do not have permission to revoke this punishment!");
			return true;
		}
		
		wrapped.unpunish(id, code, user(sender));
		
		// Check if we need to tell a different server to immediately revoke the punishment
		if(targetUser.getServerName() != null && !targetUser.getServerName().equals(dragons.getServerName())) {
			handler.forwardUnpunishment(targetUser, id);
		}
		
		sender.sendMessage(ChatColor.GREEN + "Punishment revoked successfully.");
		
		return true;
	}
}
