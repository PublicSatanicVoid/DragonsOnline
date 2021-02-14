package mc.dragons.tools.content;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;

public class GamemodeCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command!");
			return true;
		}
		
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.BUILDER, true)) return true;
		
		boolean hasModPermission = PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.MODERATOR, false);
		
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
		
		if((gameMode == GameMode.CREATIVE || gameMode == GameMode.SURVIVAL) && !user.getSystemProfile().getFlags().hasFlag(SystemProfileFlag.BUILD)) {
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
		
		target.setGameMode(gameMode);
		
		
		
		
		
		sender.sendMessage(ChatColor.GREEN + "Gamemode updated successfully.");
		
		return true;
	}
	
	
	
}
