package mc.dragons.tools.content.command.gameobject;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.storage.local.LocalStorageAccess;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.content.util.MetadataConstants;

/**
 * GM command to manage floors.
 * 
 * 
 * @author Adam
 *
 */
public class FloorCommand extends DragonsCommandExecutor {
	private static final String LOCAL_FLOOR_WARNING = ChatColor.RED + " (Warning: Dynamically injected floor!)";
	
	private GameObjectRegistry registry = instance.getGameObjectRegistry();
	
	private void showHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.YELLOW + "/floor create <FloorName> <WorldName> <LevelMin> [-superflat]" + ChatColor.GRAY + " create a new floor");
		sender.sendMessage(ChatColor.YELLOW + "/floor list" + ChatColor.GRAY + " list all floors");
		sender.sendMessage(ChatColor.YELLOW + "/floor delete <FloorName>" + ChatColor.GRAY + " delete floor");
		sender.sendMessage(ChatColor.YELLOW + "/floor goto <FloorName>" + ChatColor.GRAY + " teleport to floor (bypasses floor level min)");
		sender.sendMessage(ChatColor.YELLOW + "/floor <FloorName>" + ChatColor.GRAY + " view info about floor");
		sender.sendMessage(ChatColor.YELLOW + "/floor <FloorName> name <NewFloorName>" + ChatColor.GRAY + " change floor name");
		sender.sendMessage(ChatColor.YELLOW + "/floor <FloorName> displayname <NewFloorDisplayName>" + ChatColor.GRAY + " change floor display name");
		sender.sendMessage(ChatColor.YELLOW + "/floor <FloorName> lvmin <NewLevelMin>" + ChatColor.GRAY + " change floor level requirement");
		sender.sendMessage(ChatColor.YELLOW + "/floor <FloorName> volatile <Volatile>" + ChatColor.GRAY + " set whether the floor is volatile");
		sender.sendMessage(ChatColor.DARK_GRAY + "" +  ChatColor.BOLD + "Note:" + ChatColor.DARK_GRAY + " Floor names must not contain spaces.");
		sender.sendMessage(ChatColor.GRAY + "View the full documentation at " + ChatColor.UNDERLINE + Dragons.STAFF_DOCUMENTATION);
	}
	
	private void createFloor(CommandSender sender, String[] args) {
		if(args.length < 4) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /floor create <FloorName> <WorldName> <LevelMin> [-superflat]");
			return;
		}
		sender.sendMessage(ChatColor.GREEN + "Creating world " + args[2] + "...");
		boolean superflat = false;
		if(args.length > 4) {
			if(args[4].equalsIgnoreCase("-superflat")) {
				superflat = true;
			}
		}
		Integer lvMinOpt = parseIntType(sender, args[3]);
		if(lvMinOpt == null) return;
		Floor floor = floorLoader.registerNew(args[1], args[2], args[2], lvMinOpt, superflat);
		MetadataConstants.addBlankMetadata(floor, user(sender));
		sender.sendMessage(ChatColor.GREEN + "Created new floor successfully!");
	}
	
	private void listFloors(CommandSender sender) {
		sender.sendMessage(ChatColor.GREEN + "Listing all floors:");
		for(GameObject gameObject : registry.getRegisteredObjects(GameObjectType.FLOOR)) {
			Floor floor = (Floor) gameObject;
			sender.sendMessage(ChatColor.GRAY + "- " + floor.getFloorName() + " [" + floor.getWorldName() + "] [Lv " + floor.getLevelMin() + "]" + (floor.getStorageAccess() instanceof LocalStorageAccess ? LOCAL_FLOOR_WARNING : ""));
		}
	}
	
	private void selectFloor(CommandSender sender, String[] args) {
		User user = user(sender);
		Floor floor = lookupFloor(sender, args[0]);
		if(floor == null) return;
		
		if(args.length == 1) {
			sender.sendMessage(ChatColor.GREEN + "=== Floor: " + floor.getFloorName() + " ===");
			sender.sendMessage(ChatColor.GRAY + "Database identifier: " + ChatColor.GREEN + floor.getIdentifier().toString());
			sender.sendMessage(ChatColor.GRAY + "Floor Name: " + ChatColor.GREEN + floor.getFloorName());
			sender.sendMessage(ChatColor.GRAY + "Display Name: " + ChatColor.GREEN + floor.getDisplayName());
			sender.sendMessage(ChatColor.GRAY + "World Name: " + ChatColor.GREEN + floor.getWorldName());
			sender.sendMessage(ChatColor.GRAY + "Level Min: " + ChatColor.GREEN + floor.getLevelMin());
			sender.sendMessage(ChatColor.GRAY + "Volatile: " + ChatColor.GREEN + floor.isVolatile());
			MetadataConstants.displayMetadata(sender, floor);
		}
		else if(args.length == 2) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /floor -s <FloorName> <name|displayname|lvmin|volatile> <Value>");
			return;
		}
		else if(args[1].equalsIgnoreCase("name")) {
			floor.setFloorName(args[2]);
			sender.sendMessage(ChatColor.GREEN + "Updated floor name successfully.");
			MetadataConstants.incrementRevisionCount(floor, user);
		}
		else if(args[1].equalsIgnoreCase("displayname")) {
			floor.setDisplayName(StringUtil.concatArgs(args, 2));
			sender.sendMessage(ChatColor.GREEN + "Updated floor display name successfully.");
			MetadataConstants.incrementRevisionCount(floor, user);
		}
		else if(args[1].equalsIgnoreCase("lvmin")) {
			Integer lvMinOpt = parseIntType(sender, args[2]);
			if(lvMinOpt == null) return;
			floor.setLevelMin(lvMinOpt);
			sender.sendMessage(ChatColor.GREEN + "Updated floor level requirement successfully.");
			MetadataConstants.incrementRevisionCount(floor, user);
		}
		else if(args[1].equalsIgnoreCase("volatile")) {
			Boolean volatileOpt = parseBooleanType(sender, args[2]);
			if(volatileOpt == null) return;
			floor.setVolatile(volatileOpt);
			sender.sendMessage(ChatColor.GREEN + "Updated floor volatility status sucessfully.");
			MetadataConstants.incrementRevisionCount(floor, user);
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid arguments! /floor <FloorName> <name|displayname|lvmin|volatile> <Value>");
		}
	}
	
	private void deleteFloor(CommandSender sender, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.GM_DELETE)) return;
		Floor floor = lookupFloor(sender, args[1]);
		if(floor == null) return;
		registry.removeFromDatabase(floor);
		sender.sendMessage(ChatColor.GREEN + "Deleted this floor successfully. World files remain intact. Changes may not fully take effect until a server restart.");
	}
	
	private void goToFloor(CommandSender sender, String[] args) {
		Floor floor = lookupFloor(sender, args[1]);
		if(floor == null) return;
		user(sender).sendToFloor(floor.getFloorName(), true);
		sender.sendMessage(ChatColor.GREEN + "Teleported to floor successfully.");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, SystemProfileFlag.GM_FLOOR)) return true;

		if(args.length == 0) {
			showHelp(sender);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "-c", "c", "create")) {
			createFloor(sender, args);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "-l", "l", "list")) {
			listFloors(sender);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "-d", "d", "del", "delete")) {
			deleteFloor(sender, args);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "-g", "g", "go", "goto")) {
			goToFloor(sender, args);
		}
		else {
			selectFloor(sender, args);
		}
		
		return true;
	}
}
