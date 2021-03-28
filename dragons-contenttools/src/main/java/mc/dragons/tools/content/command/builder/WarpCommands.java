package mc.dragons.tools.content.command.builder;

import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.loader.WarpLoader;
import mc.dragons.core.util.StringUtil;

public class WarpCommands extends DragonsCommandExecutor {

	private WarpLoader warpLoader = instance.getLightweightLoaderRegistry().getLoader(WarpLoader.class);
	
	private Location getWarp(CommandSender sender, String warpName) {
		Location warp = warpLoader.getWarp(warpName);
		if(warp == null) {
			sender.sendMessage(ChatColor.RED + "No warp by that name exists!");
			return null;
		}
		return warp;
	}
	
	private void warp(CommandSender sender, String[] args) {
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/warp <WarpName> [PlayerToWarp]");
			sender.sendMessage(ChatColor.RED + "To list all warps, do /warps");
			return;
		}
		if(args.length == 1 && !(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Console must specify a player to warp! /warp <WarpName> <PlayerToWarp>");
			return;
		}
		Player target = sender instanceof Player ? (Player) sender : null;
		if(args.length == 2) {
			target = Bukkit.getPlayerExact(args[1]);
			if(target == null) {
				sender.sendMessage(ChatColor.RED + "That player is not online!");
				return;
			}
		}
		Location to = getWarp(sender, args[0]);
		if(to == null) return;
		target.teleport(to);
		sender.sendMessage(ChatColor.GREEN + "Warped" + (target.equals(sender) ? "" : " " + target.getName()) + " to " + args[0] + " successfully.");
	}
	
	private void deleteWarp(CommandSender sender, String[] args) {
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/delwarp <WarpName>");
			return;
		}
		Location warp = getWarp(sender, args[0]);
		if(warp == null) return;
		warpLoader.delWarp(args[0]);
		sender.sendMessage(ChatColor.GREEN + "Deleted warp " + args[0] + " successfully.");
	}
	
	private void setWarp(CommandSender sender, String[] args) {
		if(!requirePlayer(sender)) return;
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/setwarp <WarpName>");
			return;
		}
		Location warp = warpLoader.getWarp(args[0]);
		if(warp != null) {
			sender.sendMessage(ChatColor.RED + "A warp by that name already exists!");
			return;
		}
		warpLoader.addWarp(args[0], player(sender).getLocation());
		sender.sendMessage(ChatColor.GREEN + "Set warp " + args[0] + " to your current location");
	}
	
	private void listWarps(CommandSender sender) {
		sender.sendMessage(ChatColor.DARK_GREEN + "Listing all warps: " + ChatColor.GREEN + StringUtil.parseList(
				warpLoader.getWarps().stream().map(e -> e.getWarpName()).collect(Collectors.toList())));
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.BUILDER)) return true;
		
		if(label.equalsIgnoreCase("warp")) {
			warp(sender, args);
		}
		else if(label.equalsIgnoreCase("delwarp")) {
			deleteWarp(sender, args);
		}
		else if(label.equalsIgnoreCase("warps")) {
			listWarps(sender);
		}
		else if(label.equalsIgnoreCase("setwarp")) {
			setWarp(sender, args);
		}
		
		return true;
	}

}
