package mc.dragons.tools.content.command.builder;

import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.loader.WarpLoader;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class WarpCommands implements CommandExecutor {

	private WarpLoader warpLoader;
	
	public WarpCommands(Dragons instance) {
		warpLoader = instance.getLightweightLoaderRegistry().getLoader(WarpLoader.class);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.BUILDER, true)) return true;
		}
		
		if(label.equalsIgnoreCase("warp")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/warp <WarpName> [PlayerToWarp]");
				sender.sendMessage(ChatColor.RED + "To list all warps, do /warps");
				return true;
			}
			if(args.length == 1 && !(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "Console must specify a player to warp! /warp <WarpName> <PlayerToWarp>");
				return true;
			}
			Player target = sender instanceof Player ? (Player) sender : null;
			if(args.length == 2) {
				target = Bukkit.getPlayerExact(args[1]);
				if(target == null) {
					sender.sendMessage(ChatColor.RED + "That player is not online!");
					return true;
				}
			}
			Location to = warpLoader.getWarp(args[0]);
			if(to == null) {
				sender.sendMessage(ChatColor.RED + "No warp by that name exists!");
				return true;
			}
			target.teleport(to);
			sender.sendMessage(ChatColor.GREEN + "Warped" + (target.equals(sender) ? "" : " " + target.getName()) + " to " + args[0] + " successfully.");
			return true;
		}
		
		if(label.equalsIgnoreCase("delwarp")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/delwarp <WarpName>");
				return true;
			}
			Location warp = warpLoader.getWarp(args[0]);
			if(warp == null) {
				sender.sendMessage(ChatColor.RED + "No warp by that name exists!");
				return true;
			}
			warpLoader.delWarp(args[0]);
			sender.sendMessage(ChatColor.GREEN + "Deleted warp " + args[0] + " successfully.");
			return true;
		}
		
		if(label.equalsIgnoreCase("warps")) {
			sender.sendMessage(ChatColor.DARK_GREEN + "Listing all warps: " + ChatColor.GREEN + StringUtil.parseList(
					warpLoader.getWarps().stream().map(e -> e.getWarpName()).collect(Collectors.toList())));
			return true;
		}
		
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED +"This is an ingame-only command!");
			return true;
		}
		
		
		if(label.equalsIgnoreCase("setwarp")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/setwarp <WarpName>");
				return true;
			}
			Location warp = warpLoader.getWarp(args[0]);
			if(warp != null) {
				sender.sendMessage(ChatColor.RED + "A warp by that name already exists!");
				return true;
			}
			warpLoader.addWarp(args[0], player.getLocation());
			sender.sendMessage(ChatColor.GREEN + "Set warp " + args[0] + " to your current location");
			return true;
		}
		
		return true;
	}

}
