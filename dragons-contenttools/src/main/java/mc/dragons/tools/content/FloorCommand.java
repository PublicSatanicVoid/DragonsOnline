package mc.dragons.tools.content;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class FloorCommand implements CommandExecutor {
	private FloorLoader floorLoader;
	//private UserLoader userLoader;
	private GameObjectRegistry registry;
	
	public FloorCommand(Dragons instance) {
		floorLoader = GameObjectType.FLOOR.<Floor, FloorLoader>getLoader();
		//userLoader = (UserLoader) GameObjectType.USER.<User>getLoader();
		registry = instance.getGameObjectRegistry();
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			//if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, true)) return true;
			if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.GM_FLOOR, true)) return true;
		}
		else {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command.");
			return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/floor -c <FloorName> <WorldName> <LevelMin> [-superflat]" + ChatColor.GRAY + " create a new floor");
			sender.sendMessage(ChatColor.YELLOW + "/floor -l" + ChatColor.GRAY + " list all floors");
			sender.sendMessage(ChatColor.YELLOW + "/floor -s <FloorName>" + ChatColor.GRAY + " view info about floor");
			sender.sendMessage(ChatColor.YELLOW + "/floor -s <FloorName> name <NewFloorName>" + ChatColor.GRAY + " change floor name");
			sender.sendMessage(ChatColor.YELLOW + "/floor -s <FloorName> displayname <NewFloorDisplayName>" + ChatColor.GRAY + " change floor display name");
			sender.sendMessage(ChatColor.YELLOW + "/floor -s <FloorName> lvmin <NewLevelMin>" + ChatColor.GRAY + " change floor level requirement");
			sender.sendMessage(ChatColor.YELLOW + "/floor -d <FloorName>" + ChatColor.GRAY + " delete floor");
			sender.sendMessage(ChatColor.YELLOW + "/floor -g <FloorName>" + ChatColor.GRAY + " teleport to floor (bypasses level min)");
			sender.sendMessage(ChatColor.DARK_GRAY + "" +  ChatColor.BOLD + "Note:" + ChatColor.DARK_GRAY + " Floor names must not contain spaces.");
			sender.sendMessage(ChatColor.GRAY + "View the full documentation at " + ChatColor.UNDERLINE + Dragons.STAFF_DOCUMENTATION);
			return true;
		}
		
		if(args[0].equalsIgnoreCase("-c")) {
			if(args.length < 4) {
				sender.sendMessage(ChatColor.RED + "Insufficient arguments! /floor -c <FloorName> <WorldName> <LevelMin> [-superflat]");
				return true;
			}
			sender.sendMessage(ChatColor.GREEN + "Creating world " + args[2] + "...");
			boolean superflat = false;
			if(args.length > 4) {
				if(args[4].equalsIgnoreCase("-superflat")) {
					superflat = true;
				}
			}
			floorLoader.registerNew(args[1], args[2], args[2], Integer.valueOf(args[3]), superflat);
			sender.sendMessage(ChatColor.GREEN + "Created new floor successfully!");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("-l")) {
			sender.sendMessage(ChatColor.GREEN + "Listing all floors:");
			for(GameObject gameObject : registry.getRegisteredObjects(GameObjectType.FLOOR)) {
				Floor floor = (Floor) gameObject;
				sender.sendMessage(ChatColor.GRAY + "- " + floor.getFloorName() + " [" + floor.getWorldName() + "] [Lv " + floor.getLevelMin() + "]");
			}
			return true;
		}
		
		Floor floor = FloorLoader.fromFloorName(args[1]);
		if(floor == null) {
			sender.sendMessage(ChatColor.RED + "That floor does not exist! To see all floors, do /floor -l");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("-s")) {
			if(args.length == 2) {
				sender.sendMessage(ChatColor.GREEN + "=== Floor: " + floor.getFloorName() + " ===");
				sender.sendMessage(ChatColor.GRAY + "Database identifier: " + ChatColor.GREEN + floor.getIdentifier().toString());
				sender.sendMessage(ChatColor.GRAY + "Floor Name: " + ChatColor.GREEN + floor.getFloorName());
				sender.sendMessage(ChatColor.GRAY + "Display Name: " + ChatColor.GREEN + floor.getDisplayName());
				sender.sendMessage(ChatColor.GRAY + "World Name: " + ChatColor.GREEN + floor.getWorldName());
				sender.sendMessage(ChatColor.GRAY + "Level Min: " + ChatColor.GREEN + floor.getLevelMin());
				return true;
			}
			if(args.length == 3) {
				sender.sendMessage(ChatColor.RED + "Insufficient arguments! /floor -s <FloorName> <name|displayname|lvmin> <Value>");
				return true;
			}
			if(args[2].equalsIgnoreCase("name")) {
				floor.setFloorName(args[3]);
				sender.sendMessage(ChatColor.GREEN + "Updated floor name successfully.");
				return true;
			}
			if(args[2].equalsIgnoreCase("displayname")) {
				floor.setDisplayName(StringUtil.concatArgs(args, 3));
				sender.sendMessage(ChatColor.GREEN + "Updated floor display name successfully.");
				return true;
			}
			if(args[2].equalsIgnoreCase("lvmin")) {
				floor.setLevelMin(Integer.valueOf(args[3]));
				sender.sendMessage(ChatColor.GREEN + "Updated floor level requirement successfully.");
				return true;
			}
			sender.sendMessage(ChatColor.RED + "Invalid arguments! /floor -s <FloorName> <name|displayname|lvmin> <Value>");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("-d")) {
			if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.GM_DELETE, true)) return true;
			registry.removeFromDatabase(floor);
			sender.sendMessage(ChatColor.GREEN + "Deleted this floor successfully. World files remain intact. Changes may not fully take effect until a server restart.");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("-g")) {
			user.sendToFloor(floor.getFloorName(), true);
			sender.sendMessage(ChatColor.GREEN + "Teleported to floor successfully.");
			return true;
		}
		
		sender.sendMessage(ChatColor.RED + "Invalid arguments! For usage info, do /floor");
		return true;
	}
}
