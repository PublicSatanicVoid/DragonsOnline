package mc.dragons.tools.content;

import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.region.RegionLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class RegionCommand implements CommandExecutor {
	//private UserLoader userLoader;
	private RegionLoader regionLoader;
	private Dragons plugin;
	
	public RegionCommand(Dragons instance) {
		//userLoader = (UserLoader) GameObjectType.USER.<User>getLoader();
		regionLoader = GameObjectType.REGION.<Region, RegionLoader>getLoader();
		plugin = instance;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.GM_REGION, true)) return true;
			//if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, true)) return true;
		}
		else {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command.");
			return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/region -c <RegionName>" + ChatColor.GRAY + " create a region");
			sender.sendMessage(ChatColor.YELLOW + "/region -l [startingWith]" + ChatColor.GRAY + " list all regions, optionally starting with the specified text");
			sender.sendMessage(ChatColor.YELLOW + "/region -s <RegionName>" + ChatColor.GRAY + " view region info");
			sender.sendMessage(ChatColor.YELLOW + "/region -s <RegionName> corner <Corner#|go>" + ChatColor.GRAY + " set region boundary");
			sender.sendMessage(ChatColor.YELLOW + "/region -s <RegionName> spawnrate [NpcClass] [SpawnRate]" + ChatColor.GRAY + " modify spawn rate");
			sender.sendMessage(ChatColor.YELLOW + "/region -s <RegionName> flag <FlagName> <Value>" + ChatColor.GRAY + " modify region flags");
			sender.sendMessage(ChatColor.DARK_GRAY + "  Flags: " + ChatColor.GRAY + "fullname(string), desc(string), lvmin(int), lvrec(int), showtitle(boolean), "
					+ "allowhostile(boolean), pvp(boolean), pve(boolean), hidden(boolean), spawncap(int), nearbyspawncap(int), nospawn(boolean), 3d(boolean)");
			sender.sendMessage(ChatColor.YELLOW + "/region -d <RegionName>" + ChatColor.GRAY + " delete a region");
			sender.sendMessage(ChatColor.DARK_GRAY + "" +  ChatColor.BOLD + "Note:" + ChatColor.DARK_GRAY + " Region names must not contain spaces.");
			sender.sendMessage(ChatColor.GRAY + "View the full documentation at " + ChatColor.UNDERLINE + Dragons.STAFF_DOCUMENTATION);
			return true;
		}
		
		if(args[0].equalsIgnoreCase("-c")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Specify a name for the region! /region -c <RegionName>");
				return true;
			}
			if(args.length > 2) {
				sender.sendMessage(ChatColor.RED + "The region name cannot contain spaces! /region -c <RegionName>");
				return true;
			}
			Region test = regionLoader.getRegionByName(args[1]);
			if(test != null) {
				sender.sendMessage(ChatColor.RED + "A region by that name already exists! Its database identifier is " + test.toString());
				return true;
			}
			Region region = (Region) regionLoader.registerNew(args[1], player.getLocation(), player.getLocation());
			sender.sendMessage(ChatColor.GREEN + "Created region " + args[1] + ". Its database identifier is " + region.getIdentifier().toString());
			return true;
		}
		
		else if(args[0].equalsIgnoreCase("-l")) {
			String startingWith = "";
			if(args.length > 1) {
				startingWith = args[1];
			}
			sender.sendMessage(ChatColor.GREEN + "Listing all regions" + (startingWith.length() > 0 ? (" starting with \"" + startingWith + "\"") : "") + ":");
			for(GameObject gameObject : plugin.getGameObjectRegistry().getRegisteredObjects(GameObjectType.REGION)) {
				Region region = (Region) gameObject;
				if(!region.getName().startsWith(startingWith)) continue;
				String floorData = "";
				if(region.getFloor() != null) {
					floorData = " (Floor: " + region.getFloor().getDisplayName() + ")";
				}
				sender.sendMessage(ChatColor.GRAY + "- " + region.getName() + floorData);
			}
			return true;
		}
		
		else if(args[0].equalsIgnoreCase("-s")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Specify a region! /region -s <RegionName> [corner <Corner#>|spawnrate [NpcClass] [SpawnRate]|flag <FlagName> <Value>]");
				return true;
			}
			Region region = regionLoader.getRegionByName(args[1]);
			if(region == null) {
				sender.sendMessage(ChatColor.RED + "No region by that name exists!");
				return true;
			}
			if(args.length == 2) {
				sender.sendMessage(ChatColor.GREEN + "=== Region: " + region.getName() + " ===");
				sender.sendMessage(ChatColor.GRAY + "Database identifier: " + ChatColor.GREEN + region.getIdentifier().toString());
				sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.GREEN + region.getWorld().getName());
				sender.sendMessage(ChatColor.GRAY + "Min: " + ChatColor.GREEN + StringUtil.locToString(region.getMin()));
				sender.sendMessage(ChatColor.GRAY + "Max: " + ChatColor.GREEN + StringUtil.locToString(region.getMax()));
				sender.sendMessage(ChatColor.GRAY + "Flags: " + ChatColor.GREEN + StringUtil.docToString(region.getFlags()));
				return true;
			}
			if(args[2].equalsIgnoreCase("corner")) {
				if(args.length == 3) {
					sender.sendMessage(ChatColor.RED + "Specify corner number! (1 or 2). /region -s <RegionName> corner <Corner#>");
					return true;
				}
				switch(args[3]) {
				case "1":
					user.getLocalData().append("regionSelCorner1", player.getLocation());
					sender.sendMessage(ChatColor.GREEN + "Corner 1 selection set to " + StringUtil.locToString(player.getLocation()) + ".");
					sender.sendMessage(ChatColor.GREEN + "Changes will not take effect until you set both corners and run /region -s " + args[1] + " corner go");
					break;
				case "2":
					user.getLocalData().append("regionSelCorner2", player.getLocation());
					sender.sendMessage(ChatColor.GREEN + "Corner 2 selection set to " + StringUtil.locToString(player.getLocation()) + ".");
					sender.sendMessage(ChatColor.GREEN + "Changes will not take effect until you set both corners and run /region -s " + args[1] + " corner go");
					break;
				case "go":
					Vector corner1 = ((Location) user.getLocalData().get("regionSelCorner1")).toVector();
					Vector corner2 = ((Location) user.getLocalData().get("regionSelCorner2")).toVector();
					Vector newMin = Vector.getMinimum(corner1, corner2);
					Vector newMax = Vector.getMaximum(corner1, corner2);
					region.updateCorners(newMin.toLocation(player.getWorld()), newMax.toLocation(player.getWorld()));
					sender.sendMessage(ChatColor.GREEN + "Updated region corners successfully. Min=(" + StringUtil.vecToString(newMin) + "), Max=" + StringUtil.vecToString(newMax) + ")");
					break;
				default:
					sender.sendMessage(ChatColor.RED + "Invalid corner number! Must be 1 or 2. /region -s <RegionName> corner <Corner#|go>");
					return true;
				}
				return true;
			}
			if(args[2].equalsIgnoreCase("spawnrate")) {
				if(args.length == 3) {
					sender.sendMessage(ChatColor.GREEN + "Listing spawn rates for region " + args[1]);
					for(Entry<String, Double> entry : region.getSpawnRates().entrySet()) {
						sender.sendMessage(ChatColor.GRAY + entry.getKey() + ": " + ChatColor.GREEN + entry.getValue());
					}
					sender.sendMessage(ChatColor.GRAY + "All other entities have a spawn rate of 0.");
					return true;
				}
				if(args.length == 4) {
					sender.sendMessage(ChatColor.GREEN + "Entity class " + args[3] + " has a spawn rate of " + region.getSpawnRate(args[3]));
					return true;
				}
				try {
					double spawnRate = Double.parseDouble(args[4]);
					region.setSpawnRate(args[3], spawnRate);
					sender.sendMessage(ChatColor.GREEN + "Set spawn rate of entity class " + args[3] + " to " + spawnRate);
				}
				catch(Exception e) {
					sender.sendMessage(ChatColor.RED + "That's not a valid number! Please specify a decimal number for the spawn rate. /region -s <RegionName> spawnrate [NpcClass] [SpawnRate]");
					return true;
				}
				return true;
			}
			if(args[2].equalsIgnoreCase("flag")) {
				if(args.length == 3) {
					sender.sendMessage(ChatColor.RED + "Specify a flag name! /region -s <RegionName> flag <FlagName> [Value]");
					return true;
				}
				if(args.length == 4) {
					Object value = region.getFlags().get(args[3]);
					if(value == null) {
						sender.sendMessage(ChatColor.RED + "No flag by that name exists!");
						return true;
					}
					sender.sendMessage(ChatColor.GREEN + "Flag " + args[3] + " has value " + value.toString());
					return true;
				}
				String value = StringUtil.concatArgs(args, 4);
				region.setFlag(args[3], value);
				sender.sendMessage(ChatColor.GREEN + "Set flag " + args[3] + " to " + value);
				return true;
			}
			sender.sendMessage(ChatColor.RED + "Invalid arguments! For usage info, do /region");
			return true;
		}
		
		else if(args[0].equalsIgnoreCase("-d")) {
			if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.GM_DELETE, true)) return true;
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Specify a region to delete! /region -d <RegionName>");
				return true;
			}
			Region region = regionLoader.getRegionByName(args[1]);
			if(region == null) {
				sender.sendMessage(ChatColor.RED + "No region by that name exists!");
				return true;
			}
			plugin.getGameObjectRegistry().removeFromDatabase(region);
			sender.sendMessage(ChatColor.GREEN + "Removed region " + args[1] + " successfully. Changes may not fully take effect until a network-wide restart.");
			return true;
		}
		
		sender.sendMessage(ChatColor.RED + "Invalid arguments! For usage info, do /region");
		return true;
	}

}
