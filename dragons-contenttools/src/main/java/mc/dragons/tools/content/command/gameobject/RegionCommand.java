package mc.dragons.tools.content.command.gameobject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.BlockUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.content.util.MetadataConstants;

public class RegionCommand extends DragonsCommandExecutor {
	private void displayHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.YELLOW + "/region create <RegionName>" + ChatColor.GRAY + " create a region");
		sender.sendMessage(ChatColor.YELLOW + "/region list [startingWith]" + ChatColor.GRAY + " list all regions, optionally starting with the specified text");
		sender.sendMessage(ChatColor.YELLOW + "/region delete <RegionName>" + ChatColor.GRAY + " delete a region");
		sender.sendMessage(ChatColor.YELLOW + "/region <RegionName>" + ChatColor.GRAY + " view region info");
		sender.sendMessage(ChatColor.YELLOW + "/region <RegionName> corner <Corner#|go>" + ChatColor.GRAY + " set region boundary");
		sender.sendMessage(ChatColor.YELLOW + "/region <RegionName> border [BorderMaterial=WOOL]" + ChatColor.GRAY + " generate border around region");
		sender.sendMessage(ChatColor.YELLOW + "/region <RegionName> spawnrate [NpcClass] [SpawnRate]" + ChatColor.GRAY + " modify spawn rate");
		sender.sendMessage(ChatColor.YELLOW + "/region <RegionName> flag <FlagName> <Value>" + ChatColor.GRAY + " modify region flags");
		sender.sendMessage(ChatColor.DARK_GRAY + "  Flags: " + ChatColor.GRAY + "fullname(string), desc(string), lvmin(int), lvrec(int), showtitle(boolean), "
				+ "allowhostile(boolean), pvp(boolean), pve(boolean), hidden(boolean), spawncap(int), nearbyspawncap(int), nospawn(boolean), 3d(boolean)");
		sender.sendMessage(ChatColor.DARK_GRAY + "" +  ChatColor.BOLD + "Note:" + ChatColor.DARK_GRAY + " Region names must not contain spaces.");
		sender.sendMessage(ChatColor.GRAY + "View the full documentation at " + ChatColor.UNDERLINE + Dragons.STAFF_DOCUMENTATION);
	}
	
	private void createRegion(CommandSender sender, String[] args) {
		if(!requirePlayer(sender)) return;
		if(args.length == 1) {
			sender.sendMessage(ChatColor.RED + "Specify a name for the region! /region create <RegionName>");
		}
		else if(args.length > 2) {
			sender.sendMessage(ChatColor.RED + "The region name cannot contain spaces! /region create <RegionName>");
		}
		Region test = regionLoader.getRegionByName(args[1]);
		if(test != null) {
			sender.sendMessage(ChatColor.RED + "A region by that name already exists! Its database identifier is " + test.toString());
		}
		else {
			Player player = player(sender);
			Region region = (Region) regionLoader.registerNew(args[1], player.getLocation(), player.getLocation());
			MetadataConstants.addBlankMetadata(region, user(sender));
			sender.sendMessage(ChatColor.GREEN + "Created region " + args[1] + ". Its database identifier is " + region.getIdentifier().toString());
		}
	}
	
	private void listRegions(CommandSender sender, String[] args) {
		String startingWith = "";
		if(args.length > 1) {
			startingWith = args[1];
		}
		sender.sendMessage(ChatColor.GREEN + "Listing all regions" + (startingWith.length() > 0 ? (" starting with \"" + startingWith + "\"") : "") + ":");
		for(GameObject gameObject : dragons.getGameObjectRegistry().getRegisteredObjects(GameObjectType.REGION)) {
			Region region = (Region) gameObject;
			if(!region.getName().startsWith(startingWith)) continue;
			String floorData = "";
			if(region.getFloor() != null) {
				floorData = " (Floor: " + region.getFloor().getDisplayName() + ")";
			}
			sender.sendMessage(ChatColor.GRAY + "- " + region.getName() + floorData);
		}
	}
	
	private void deleteRegion(CommandSender sender, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.GM_DELETE)) return;
		if(args.length == 1) {
			sender.sendMessage(ChatColor.RED + "Specify a region to delete! /region -d <RegionName>");
			return;
		}
		Region region = regionLoader.getRegionByName(args[1]);
		if(region == null) {
			sender.sendMessage(ChatColor.RED + "No region by that name exists!");
		}
		else {
			dragons.getGameObjectRegistry().removeFromDatabase(region);
			sender.sendMessage(ChatColor.GREEN + "Removed region " + args[1] + " successfully. Changes may not fully take effect until a network-wide restart.");
		}
	}
	
	private void displayRegion(CommandSender sender, String[] args) {
		Region region = lookupRegion(sender, args[0]);
		if(region == null) return;
		
		sender.sendMessage(ChatColor.GREEN + "=== Region: " + region.getName() + " ===");
		sender.sendMessage(ChatColor.GRAY + "Database identifier: " + ChatColor.GREEN + region.getIdentifier().toString());
		sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.GREEN + region.getWorld().getName());
		sender.sendMessage(ChatColor.GRAY + "Min: " + ChatColor.GREEN + StringUtil.locToString(region.getMin()));
		sender.sendMessage(ChatColor.GRAY + "Max: " + ChatColor.GREEN + StringUtil.locToString(region.getMax()));
		sender.sendMessage(ChatColor.GRAY + "Flags: " + ChatColor.GREEN + StringUtil.docToString(region.getFlags()));
		sender.spigot().sendMessage(ObjectMetadataCommand.getClickableMetadataLink(GameObjectType.REGION, region.getUUID()));
	}
	
	private void borderRegion(CommandSender sender, String[] args) {
		Region region = lookupRegion(sender, args[0]);
		if(region == null) return;
		
		if(args.length == 1) {
			sender.sendMessage(ChatColor.RED + "/region <RegionName> border [MaterialType=LIME_WOOL]");
		}
		
		Material material = Material.LIME_WOOL;
		if(args.length > 2) {
			material = StringUtil.parseEnum(sender, Material.class, args[2]);
			if(material == null) return;
		}
		
		Location a = region.getMin();
		Location b = region.getMax();
		
		List<Block> changes = new ArrayList<>();
		
		//
		// STAGE 1: Define border in xz-plane
		//
		
		// minX constant, Z from minZ to maxZ
		Block buf = a.getBlock();
		for(int z = a.getBlockZ(); z <= b.getBlockZ(); z++) {
			changes.add(buf);
			buf = buf.getRelative(0, 0, 1);
		}
		
		// minZ constant, X from minX to maxX
		buf = a.getBlock();
		for(int x = a.getBlockX(); x <= b.getBlockX(); x++) {
			changes.add(buf);
			buf = buf.getRelative(1, 0, 0);
		}
		
		// maxX constant, Z from minZ to maxZ
		buf = a.getBlock().getRelative(b.getBlockX() - a.getBlockX(), 0, 0);
		for(int z = a.getBlockZ(); z <= b.getBlockZ(); z++) {
			changes.add(buf);
			buf = buf.getRelative(0, 0, 1);
		}
		
		// maxZ constant, X from minX to maxX
		buf = a.getBlock().getRelative(0, 0, b.getBlockZ() - a.getBlockZ());
		for(int x = a.getBlockX(); x <= b.getBlockX(); x++) {
			changes.add(buf);
			buf = buf.getRelative(1, 0, 0);
		}
		
		//
		// STAGE 2: Deform border around blocks
		//
		List<Block> changesDeformed = changes.stream().map(block -> BlockUtil.getAirAboveXZ(block.getLocation()).getBlock()).collect(Collectors.toList());
		
		//
		// STAGE 3: Update in batches
		//
		final Material fMaterial = material;
		int batchSize = 50;
		for(int i = 0; i < changesDeformed.size(); i += batchSize) {
			int start = i;
			new BukkitRunnable() {
				public void run() {
					for(int j = start; j < Math.min(changesDeformed.size(), start + batchSize); j++) {
						changesDeformed.get(j).setType(fMaterial);
					}
				}
			}.runTaskLater(dragons, 2 * i / 50);
		}
		
		sender.sendMessage(ChatColor.GREEN + "Generated border successfully.");
	}
	
	private void updateCorners(CommandSender sender, String[] args) {
		if(!requirePlayer(sender)) return;
		
		Region region = lookupRegion(sender, args[0]);
		if(region == null) return;
		
		Player player = player(sender);
		User user = user(sender);
		
		if(args.length == 2) {
			sender.sendMessage(ChatColor.RED + "Specify corner number! (1 or 2). /region <RegionName> corner <Corner#>");
			return;
		}
		switch(args[2].toLowerCase()) {
		case "1":
		case "a":
			user.getLocalData().append("regionSelCorner1", player.getLocation());
			sender.sendMessage(ChatColor.GREEN + "Corner 1 selection set to " + StringUtil.locToString(player.getLocation()) + ".");
			sender.sendMessage(ChatColor.GREEN + "Changes will not take effect until you set both corners and run /region " + args[1] + " corner go");
			break;
		case "2":
		case "b":
			user.getLocalData().append("regionSelCorner2", player.getLocation());
			sender.sendMessage(ChatColor.GREEN + "Corner 2 selection set to " + StringUtil.locToString(player.getLocation()) + ".");
			sender.sendMessage(ChatColor.GREEN + "Changes will not take effect until you set both corners and run /region " + args[0] + " corner go");
			break;
		case "go":
			Document base = Document.parse(region.getData().toJson());
			Vector corner1 = user.getLocalData().get("regionSelCorner1", Location.class).toVector();
			Vector corner2 = user.getLocalData().get("regionSelCorner2", Location.class).toVector();
			Vector newMin = Vector.getMinimum(corner1, corner2);
			Vector newMax = Vector.getMaximum(corner1, corner2);
			region.updateCorners(newMin.toLocation(player.getWorld()), newMax.toLocation(player.getWorld()));
			sender.sendMessage(ChatColor.GREEN + "Updated region corners successfully. Min=(" + StringUtil.vecToString(newMin) + "), Max=" + StringUtil.vecToString(newMax) + ")");
			MetadataConstants.logRevision(region, user, base, "Updated region corners");
			break;
		default:
			sender.sendMessage(ChatColor.RED + "Invalid usage! /region <RegionName> corner <1|2|go>");
			break;
		}
	}
	
	private void updateSpawnRate(CommandSender sender, String[] args) {
		Region region = lookupRegion(sender, args[0]);
		if(region == null) return;
		
		if(args.length == 2) {
			sender.sendMessage(ChatColor.GREEN + "Listing spawn rates for region " + args[1]);
			for(Entry<String, Double> entry : region.getSpawnRates().entrySet()) {
				sender.sendMessage(ChatColor.GRAY + entry.getKey() + ": " + ChatColor.GREEN + entry.getValue());
			}
			sender.sendMessage(ChatColor.GRAY + "All other entities have a spawn rate of 0.");
		}
		else if(args.length == 3) {
			sender.sendMessage(ChatColor.GREEN + "Entity class " + args[3] + " has a spawn rate of " + region.getSpawnRate(args[3]));
		}
		else {
			Document base = Document.parse(region.getData().toJson());
			double spawnRate = parseDouble(sender, args[3]);
			region.setSpawnRate(args[2], spawnRate);
			sender.sendMessage(ChatColor.GREEN + "Set spawn rate of entity class " + args[2] + " to " + spawnRate);
			MetadataConstants.logRevision(region, user(sender), base, "Set spawn rate of class " + args[2] + " to " + spawnRate);
		}
	}
	
	private void updateFlag(CommandSender sender, String[] args) {
		Region region = lookupRegion(sender, args[0]);
		if(region == null) return;
		
		if(args.length == 2) {
			sender.sendMessage(ChatColor.RED + "Specify a flag name! /region <RegionName> flag <FlagName> [Value]");
		}
		else if(args.length == 3) {
			Object value = region.getFlags().get(args[2]);
			if(value == null) {
				sender.sendMessage(ChatColor.RED + "No flag by that name exists!");
			}
			else {
				sender.sendMessage(ChatColor.GREEN + "Flag " + args[2] + " has value " + value.toString());
			}
		}
		else {
			Document base = Document.parse(region.getData().toJson());
			String value = StringUtil.concatArgs(args, 3);
			region.setFlag(args[2], value);
			sender.sendMessage(ChatColor.GREEN + "Set flag " + args[2] + " to " + value);
			MetadataConstants.logRevision(region, user(sender), base, "Set flag " + args[2] + " to " + value);
		}
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.GM_REGION)) return true;
		
		if(args.length == 0) {
			displayHelp(sender);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "create", "-c", "-create", "--create")) {
			createRegion(sender, args);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "list", "-l", "-list", "--list")) {
			listRegions(sender, args);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "delete", "del", "-d", "-delete", "--delete")) {
			deleteRegion(sender, args);
		}		
		else if(args.length == 1) {
			displayRegion(sender, args);
		}
		else if(args[1].equalsIgnoreCase("corner")) {
			updateCorners(sender, args);
		}
		else if(args[1].equalsIgnoreCase("border")) {
			borderRegion(sender, args);
		}
		else if(args[1].equalsIgnoreCase("spawnrate")) {
			updateSpawnRate(sender, args);
		}
		else if(args[1].equalsIgnoreCase("flag")) {
			updateFlag(sender, args);
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid arguments! For usage info, do /region");
		}
		
		return true;
	}

}
