package mc.dragons.tools.content.command.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.content.DragonsContentTools;
import mc.dragons.tools.content.HologramLoader;
import mc.dragons.tools.content.HologramLoader.Hologram;

public class HologramCommand extends DragonsCommandExecutor {
	private HologramLoader loader;
	
	public HologramCommand(DragonsContentTools plugin) {
		loader = plugin.getDragonsInstance().getLightweightLoaderRegistry().getLoader(HologramLoader.class);
	}
	
	private Hologram getHologramById(CommandSender sender, String input) {
		Integer id = parseInt(sender, input);
		if(id == null) return null;
		Hologram hologram = loader.getHologram(id);
		if(hologram == null) {
			sender.sendMessage(ChatColor.RED + "No hologram with that ID exists!");
			return null;
		}
		return hologram;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.GM)) return true; 
		
		Player player = player(sender);
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GREEN + "Standalone Hologram Manager");
			sender.sendMessage(ChatColor.DARK_GRAY + "Click on a hologram to identify and manage it");
			sender.sendMessage(ChatColor.GRAY + "/holo list [radius]");
			sender.sendMessage(ChatColor.GRAY + "/holo create <text> [-cmd <playerCmd>|-ccmd <consoleCmd>]");
			sender.sendMessage(ChatColor.GRAY + "/holo <#> info");
			sender.sendMessage(ChatColor.GRAY + "/holo <#> movehere");
			sender.sendMessage(ChatColor.GRAY + "/holo <#> addline <text>");
			sender.sendMessage(ChatColor.GRAY + "/holo <#> remline <#>");
			sender.sendMessage(ChatColor.GRAY + "/holo <#> runactions");
			sender.sendMessage(ChatColor.GRAY + "/holo <#> delete");
			sender.sendMessage(ChatColor.GRAY + "/holo reload");
			sender.sendMessage(ChatColor.DARK_GRAY + "Remember that hologram commands should NOT be prefixed with a slash");
			sender.sendMessage(ChatColor.DARK_GRAY + "Use %PLAYER% as a placeholder for the player's username in commands");
			sender.sendMessage(ChatColor.RED + "This does NOT manage system holograms, e.g. residences, external health indicators");
		}
		
		else if(args[0].equalsIgnoreCase("list")) {
			Collection<Hologram> results = loader.getAllHolograms();
			Location loc = player.getLocation();
			if(args.length == 2) {
				Integer radius = parseInt(sender, args[1]);
				if(radius == null) return true;
				int radius2 = radius * radius;
				results = results.stream().filter(h -> h.getLocation().distanceSquared(loc) <= radius2).toList();
			}
			sender.sendMessage(ChatColor.GREEN + "" + results.size() + " Results found");
			for(Hologram hologram : results) {
				sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + " #" + hologram.getId() + 
						": " + StringUtil.locToString(hologram.getLocation()) + " (" + hologram.getText().length + " lines)", 
						"/hologram " + hologram.getId() + " info", hologram.getText()));
			}
		}
		
		else if(args[0].equalsIgnoreCase("create")) {
			int cmdIndex = StringUtil.argIndex(args, "-cmd");
			int ccmdIndex = StringUtil.argIndex(args, "-ccmd");
			if(ccmdIndex != -1 && !hasPermission(sender, SystemProfileFlag.CMD)) {
				sender.sendMessage(ChatColor.RED + "Setting -ccmd (console command) flag requires profile flag CMD");
				return true;
			}
			int endTextIndex = Math.max(cmdIndex, ccmdIndex);
			if(endTextIndex == -1) endTextIndex = args.length;
			String text = StringUtil.concatArgs(args, 1, Math.max(cmdIndex, ccmdIndex));
			Hologram h = loader.new Hologram(player.getLocation().add(0, HologramLoader.Y_OFFSET, 0), new String[] { text });
			sender.sendMessage(ChatColor.GREEN + "Created new hologram at your location");
			if(cmdIndex != -1) {
				String cmd = StringUtil.concatArgs(args, cmdIndex + 1);
				h.setCmdAction(cmd);
				sender.sendMessage(ChatColor.GRAY + "Player Cmd: " + cmd);
			}
			else if(ccmdIndex != -1) {
				String cmd = StringUtil.concatArgs(args, ccmdIndex + 1);
				h.setCcmdAction(cmd);
				sender.sendMessage(ChatColor.GRAY + "Console Cmd: " + cmd);
			}
			h.spawn();
		}
		
		else if(args[0].equalsIgnoreCase("reload")) {
			loader.destroyAll();
			loader.spawnAll();
			sender.sendMessage(ChatColor.GREEN + "Reloaded holograms");
		}
		
		else if(args.length == 1) {
			sender.sendMessage(ChatColor.RED + "Invalid arguments! /holo");
		}
		
		else if(args[1].equalsIgnoreCase("info")) {
			Hologram hologram = getHologramById(sender, args[0]);
			if(hologram == null) return true;
			sender.sendMessage(ChatColor.GREEN + "Info for hologram #" + hologram.getId());
			sender.sendMessage(ChatColor.GRAY + "Location: " + StringUtil.locToString(hologram.getLocation()));
			sender.sendMessage(ChatColor.GRAY + "Click Action: " + hologram._describeActions());
			sender.sendMessage(ChatColor.GRAY + "Text:");
			for(String line : hologram.getText()) {
				sender.sendMessage(ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', line));
			}
		}
		
		else if(args[1].equalsIgnoreCase("movehere")) {
			Hologram hologram = getHologramById(sender, args[0]);
			if(hologram == null) return true;
			hologram.setLocation(player.getLocation().add(0, HologramLoader.Y_OFFSET, 0));
			sender.sendMessage(ChatColor.GREEN + "Moved hologram #" + hologram.getId() + " to your location");
		}
		
		else if(args[1].equalsIgnoreCase("addline")) {
			Hologram hologram = getHologramById(sender, args[0]);
			if(hologram == null) return true;
			List<String> lines = new ArrayList<>(List.of(hologram.getText()));
			lines.add(StringUtil.concatArgs(args, 2));
			hologram.setText(lines.toArray(new String[] {}));
			sender.sendMessage(ChatColor.GREEN + "Added line to hologram #" + hologram.getId());
		}
		
		else if(args[1].equalsIgnoreCase("remline")) {
			Hologram hologram = getHologramById(sender, args[0]);
			if(hologram == null) return true;
			List<String> lines = new ArrayList<>(List.of(hologram.getText()));
			Integer lineno = parseInt(sender, args[2]);
			if(lineno == null || lineno < 0 || lineno >= lines.size()) return true;
			lines.remove((int) lineno);
			hologram.setText(lines.toArray(new String[] {}));
			sender.sendMessage(ChatColor.GREEN + "Removed line " + lineno + " from hologram #" + hologram.getId());
		}
		
		else if(args[1].equalsIgnoreCase("runactions")) {
			Hologram hologram = getHologramById(sender, args[0]);
			if(hologram == null) return true;
			hologram.runActions(user(sender));
		}
		
		else if(args[1].equalsIgnoreCase("delete")) {
			Hologram hologram = getHologramById(sender, args[0]);
			if(hologram == null) return true;
			loader.deleteHologram(hologram);
			sender.sendMessage(ChatColor.GREEN + "Removed hologram #" + hologram.getId());
		}
		
		return true;
	}

}
