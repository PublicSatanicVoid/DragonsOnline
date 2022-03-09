package mc.dragons.core.commands;

import java.util.UUID;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.user.StateLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;

public class StateCommands extends DragonsCommandExecutor {
	private StateLoader stateLoader = dragons.getLightweightLoaderRegistry().getLoader(StateLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.MODERATOR)) return true;
		
		Player player = player(sender);
		User user = user(sender);
		
		if(label.equalsIgnoreCase("getstate")) {
			User targetUser = user;
			if(args.length > 0) {
				targetUser = lookupUser(sender, args[0]);
				if(targetUser == null) return true;
			}
			
			UUID stateToken = targetUser.getState();
			player.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GREEN + "Your current state token is " + ChatColor.GRAY + stateToken, 
				"/setstate " + stateToken, true, "Click for copy-able state token"));
		}
		
		else if(label.equalsIgnoreCase("setstate")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/setstate <StateToken> [TargetPlayer]");
				return true;
			}
			
			Player target = player;
			User targetUser = user;
			if(args.length > 1) {
				target = lookupPlayer(sender, args[1]);
				if(target == null) return true;
				targetUser = UserLoader.fromPlayer(target);
			}
			
			UUID token = parseUUID(sender, args[0]);
			Document state = stateLoader.getState(token);
			if(state == null) {
				sender.sendMessage(ChatColor.RED + "Invalid state token!");
				return true;
			}
			UUID backup = targetUser.setState(token);
			User fromUser = userLoader.loadObject(UUID.fromString(state.getString("originalUser")));
			Integer ping = state.getInteger("ping");
			Double tps = state.getDouble("tps");
			sender.sendMessage(ChatColor.GREEN + "Snapshot: " + fromUser.getName() + ", " + state.getString("originalTime") 
			+ " (" + (ping == null ? "??" : ping) + "ms, " + (tps == null ? "??" : tps) + "tps)");
			sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + " [Restore]", "/setstate " + backup + " " + targetUser.getName(), true, "Restore prior state"));
		}
		
		return true;
	}

}
