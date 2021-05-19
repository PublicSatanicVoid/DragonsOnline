package mc.dragons.tools.moderation.punishment.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.DragonsModerationTools;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.punishment.PunishMessageHandler;
import mc.dragons.tools.moderation.punishment.RevocationCode;

public class RemovePunishmentCommand extends DragonsCommandExecutor {
	private PunishMessageHandler handler = JavaPlugin.getPlugin(DragonsModerationTools.class).getPunishMessageHandler();

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN) || !requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
		if(args.length < 3) {
			sender.sendMessage(ChatColor.RED + "/removepunishment <player> <#> <revocation code> [-delete]");
			sender.sendMessage(ChatColor.RED + "View a player's punishments with /viewpunishments <player>");
			return true;
		}
		
		User targetUser = lookupUser(sender, args[0]);
		Integer punishmentNo = parseInt(sender, args[1]);
		RevocationCode code = RevocationCode.parseCode(sender, args[2]);
		if(targetUser == null || punishmentNo == null || code == null) return true;
		punishmentNo--;
		
		WrappedUser wrapped = WrappedUser.of(targetUser);
		
		boolean deletePermanently = StringUtil.getFlagIndex(args, "-delete", 3) != -1;
		
		if(punishmentNo < 0 || punishmentNo >= wrapped.getPunishmentHistory().size()) {
			sender.sendMessage(ChatColor.RED + "Invalid punishment number! Use /viewpunishments " + targetUser.getName() + " to see their punishment records.");
			return true;
		}
		
		if(deletePermanently) {
			wrapped.deletePunishment(punishmentNo);
		}
		else {
			wrapped.unpunish(punishmentNo, code, user(sender));
		}
		
		sender.sendMessage(ChatColor.GREEN + (deletePermanently ? "Deleted" : "Revoked") + " punishment #" + args[1] + " from " + targetUser.getName());
		if(targetUser.getServer() != null && !targetUser.getServer().equals(dragons.getServerName())) {
			handler.forwardUnpunishment(targetUser, punishmentNo);
		}
		
		if(targetUser.getPlayer() == null) {
			// User was only constructed for querying purposes. Since they're not really online, remove them from local registry
			userLoader.unregister(targetUser);
		}
		
		return true;
	}
}
