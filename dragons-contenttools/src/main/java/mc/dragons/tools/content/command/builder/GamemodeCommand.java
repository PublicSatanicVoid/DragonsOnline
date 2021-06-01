package mc.dragons.tools.content.command.builder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;

public class GamemodeCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		User user = user(sender);
		Player player = player(sender);
		
		boolean isAdminFloor = user.getSystemProfile() != null && user.getSystemProfile().getLocalAdminFloors().contains(FloorLoader.fromLocation(player.getLocation()));
		if(!isAdminFloor && !hasPermission(sender, PermissionLevel.BUILDER)) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to do this!");
			return true;
		}
		
		boolean hasModPermission = hasPermission(sender, PermissionLevel.MODERATOR);
		
		GameMode gameMode = GameMode.ADVENTURE;
		
		switch(label) {
		case "gma":
			gameMode = GameMode.ADVENTURE;
			break;
		case "gmc":
			gameMode = GameMode.CREATIVE;
			break;
		case "gms":
			gameMode = GameMode.SURVIVAL;
			break;
		case "gm":
		case "gamemode":
			if(args.length == 0) {
				sender.sendMessage(ChatColor.YELLOW + "/" + label + " <adventure|creative|survival|spectator>" + (hasModPermission ? " [player]" : ""));
				return true;
			}
			switch(args[0]) {
			case "2":
			case "a":
			case "adventure":
				gameMode = GameMode.ADVENTURE;
				break;
			case "1":
			case "c":
			case "creative":
				gameMode = GameMode.CREATIVE;
				break;
			case "0":
			case "s":
			case "survival":
				gameMode = GameMode.SURVIVAL;
				break;
			case "3":
			case "spec":
			case "spectator":
				gameMode = GameMode.SPECTATOR;
				break;
			default:
				sender.sendMessage(ChatColor.RED + "Invalid gamemode! /" + label + " <adventure|creative|survival|spectator>" + (hasModPermission ? " [player]" : ""));
				return true;
			}
		}
		
		if((gameMode == GameMode.CREATIVE || gameMode == GameMode.SURVIVAL) && !isAdminFloor && !PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.BUILD, false)) {
			sender.sendMessage(ChatColor.RED + "Creative and survival gamemodes require build permissions.");
			return true;
		}
		
		Player target = player;
		if(args.length == 2 && (label.equalsIgnoreCase("gm") || label.equalsIgnoreCase("gamemode"))) {
			if(!hasModPermission) {
				sender.sendMessage(ChatColor.RED + "Changing others' gamemodes requires permission level MOD.");
				return true;
			}
			target = Bukkit.getPlayerExact(args[1]);
		}
		else if(!isPlayer(sender)) {
			sender.sendMessage(ChatColor.RED + "Console must specify a target player /" + label + " <gamemode> <player>");
			return true;
		}
		
		target.setGameMode(gameMode);
		sender.sendMessage(ChatColor.GREEN + "Gamemode updated successfully.");
		
		return true;
	}
	
	
	
}
