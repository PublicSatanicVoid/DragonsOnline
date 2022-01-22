package mc.dragons.tools.content.command.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

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
	
	private void listHolograms(CommandSender sender, String[] args) {
		Collection<Hologram> results = loader.getAllHolograms();
		Location loc = player(sender).getLocation();
		if(args.length == 2) {
			Integer radius = parseInt(sender, args[1]);
			if(radius == null) return;
			int radius2 = radius * radius;
			results = results.stream().filter(h -> h.getLocation().distanceSquared(loc) <= radius2).toList();
		}
		sender.sendMessage(ChatColor.GREEN + "" + results.size() + " Results found");
		for(Hologram hologram : results) {
			sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + " #" + hologram.getId() + 
					": " + StringUtil.locToString(hologram.getLocation()) + " [" + hologram.getLocation().getWorld().getName() + "] " 
					+ StringUtil.colorize(hologram.getText()[0]), 
					"/hologram " + hologram.getId() + " info", 
					Stream.of(hologram.getText()).map(s -> StringUtil.colorize(s))
						.collect(Collectors.toList())
						.toArray(new String[] {})));
		}
	}
	
	private void createHologram(CommandSender sender, String[] args) {
		int cmdIndex = StringUtil.getFlagIndex(args, "-cmd", 0);
		int ccmdIndex = StringUtil.getFlagIndex(args, "-ccmd", 0);
		if(ccmdIndex != -1 && !hasPermission(sender, SystemProfileFlag.CMD)) {
			sender.sendMessage(ChatColor.RED + "Setting -ccmd (console command) flag requires profile flag CMD");
			return;
		}
		int endTextIndex = Math.max(cmdIndex, ccmdIndex);
		if(endTextIndex == -1) endTextIndex = args.length;
		String text = StringUtil.concatArgs(args, 1, Math.max(cmdIndex, ccmdIndex));
		Hologram h = loader.new Hologram(player(sender).getLocation().add(0, HologramLoader.Y_OFFSET, 0), new String[] { text });
		sender.sendMessage(ChatColor.GREEN + "Created new hologram #" + h.getId() + " at your location");
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
	
	private void hologramInfo(CommandSender sender, String[] args) {
		Hologram hologram = getHologramById(sender, args[0]);
		if(hologram == null) return;
		String[] text = hologram.getText();
		sender.sendMessage(ChatColor.GREEN + "Info for hologram #" + hologram.getId());
		sender.sendMessage(ChatColor.GRAY + "Location: " + StringUtil.locToString(hologram.getLocation())
				+ " [" + hologram.getLocation().getWorld().getName() + "]");
		sender.sendMessage(ChatColor.GRAY + "Click Action: " + hologram.describeActions());
		sender.sendMessage(ChatColor.GRAY + "Text:");
		for(int i = 0; i < text.length; i++) {
			sender.sendMessage(ChatColor.DARK_GRAY + "#" + i + ": " + ChatColor.GRAY + StringUtil.colorize(text[i]));
		}
	}
	
	private void moveHologramHere(CommandSender sender, String[] args) {
		Hologram hologram = getHologramById(sender, args[0]);
		if(hologram == null) return;
		hologram.setLocation(player(sender).getLocation().add(0, HologramLoader.Y_OFFSET, 0));
		sender.sendMessage(ChatColor.GREEN + "Moved hologram #" + hologram.getId() + " to your location");
	}
	
	private void addHologramLine(CommandSender sender, String[] args) {
		Hologram hologram = getHologramById(sender, args[0]);
		if(hologram == null) return;
		List<String> lines = new ArrayList<>(List.of(hologram.getText()));
		lines.add(StringUtil.concatArgs(args, 2));
		hologram.setText(lines.toArray(new String[] {}));
		sender.sendMessage(ChatColor.GREEN + "Added line to hologram #" + hologram.getId());
	}
	
	private void insertHologramLine(CommandSender sender, String[] args) {
		Hologram hologram = getHologramById(sender, args[0]);
		if(hologram == null) return;
		Integer index = parseInt(sender, args[2]);
		if(index == null) return;
		List<String> lines = new ArrayList<>(List.of(hologram.getText()));
		lines.add(index, StringUtil.concatArgs(args, 3));
		hologram.setText(lines.toArray(new String[] {}));
		sender.sendMessage(ChatColor.GREEN + "Inserted line to hologram #" + hologram.getId());
	}
	
	private void removeHologramLine(CommandSender sender, String[] args) {
		Hologram hologram = getHologramById(sender, args[0]);
		if(hologram == null) return;
		List<String> lines = new ArrayList<>(List.of(hologram.getText()));
		Integer lineno = parseInt(sender, args[2]);
		if(lineno == null || lineno < 0 || lineno >= lines.size()) return;
		lines.remove((int) lineno);
		hologram.setText(lines.toArray(new String[] {}));
		sender.sendMessage(ChatColor.GREEN + "Removed line " + lineno + " from hologram #" + hologram.getId());
	}
	
	private void runHologramActions(CommandSender sender, String[] args) {
		Hologram hologram = getHologramById(sender, args[0]);
		if(hologram == null) return;
		hologram.runActions(user(sender));
	}
	
	private void deleteHologram(CommandSender sender, String[] args) {
		Hologram hologram = getHologramById(sender, args[0]);
		if(hologram == null) return;
		loader.deleteHologram(hologram);
		sender.sendMessage(ChatColor.GREEN + "Removed hologram #" + hologram.getId());
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.GM)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GREEN + "Standalone Hologram Manager");
			sender.sendMessage(ChatColor.DARK_GRAY + "Click on a hologram to identify and manage it");
			sender.sendMessage(ChatColor.GRAY + "/holo list [radius]");
			sender.sendMessage(ChatColor.GRAY + "/holo create <text> [-cmd <playerCmd>|-ccmd <consoleCmd>]");
			sender.sendMessage(ChatColor.GRAY + "/holo <#> info");
			sender.sendMessage(ChatColor.GRAY + "/holo <#> movehere");
			sender.sendMessage(ChatColor.GRAY + "/holo <#> addline <text>");
			sender.sendMessage(ChatColor.GRAY + "/holo <#> insline <before#> <text>");
			sender.sendMessage(ChatColor.GRAY + "/holo <#> remline <#>");
			sender.sendMessage(ChatColor.GRAY + "/holo <#> runactions");
			sender.sendMessage(ChatColor.GRAY + "/holo <#> delete");
			sender.sendMessage(ChatColor.GRAY + "/holo reload");
			sender.sendMessage(ChatColor.DARK_GRAY + "Remember that hologram commands should NOT be prefixed with a slash");
			sender.sendMessage(ChatColor.DARK_GRAY + "Use %PLAYER% as a placeholder for the player's username in commands");
			sender.sendMessage(ChatColor.RED + "This does NOT manage system holograms, e.g. residences, external health indicators");
		}
		
		else if(args[0].equalsIgnoreCase("list")) {
			listHolograms(sender, args);
		}
		
		else if(args[0].equalsIgnoreCase("create")) {
			createHologram(sender, args);
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
			hologramInfo(sender, args);
		}
		
		else if(args[1].equalsIgnoreCase("movehere")) {
			moveHologramHere(sender, args);
		}
		
		else if(args[1].equalsIgnoreCase("addline")) {
			addHologramLine(sender, args);
		}
		
		else if(args[1].equalsIgnoreCase("insline")) {
			insertHologramLine(sender, args);
		}
		
		else if(args[1].equalsIgnoreCase("remline")) {
			removeHologramLine(sender, args);
		}
		
		else if(args[1].equalsIgnoreCase("runactions")) {
			runHologramActions(sender, args);
		}
		
		else if(args[1].equalsIgnoreCase("delete")) {
			deleteHologram(sender, args);
		}
		
		return true;
	}

}
