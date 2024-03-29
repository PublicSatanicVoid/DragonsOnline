package mc.dragons.tools.content.command.gameobject;

import java.io.File;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.Floor.FloorStatus;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.storage.local.LocalStorageAccess;
import mc.dragons.core.util.FileUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.content.AuditLogLoader;
import mc.dragons.tools.content.DragonsContentTools;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

/**
 * GM command to manage floors.
 * 
 * 
 * @author Adam
 *
 */
public class FloorCommand extends DragonsCommandExecutor {
	private static final String LOCAL_FLOOR_WARNING = ChatColor.RED + " (Warning: Dynamically injected floor!)";
	private AuditLogLoader AUDIT_LOG = dragons.getLightweightLoaderRegistry().getLoader(AuditLogLoader.class);
	private GameObjectRegistry registry = dragons.getGameObjectRegistry();
	
	private static void pushFloor(Floor floor) {
		Bukkit.getLogger().info("Pushing floor " + floor.getDisplayName());
		String pushRoot = DragonsContentTools.PUSH_FOLDER;
		File pushFolder = new File(pushRoot + floor.getWorldName());
		pushFolder.mkdirs();
		World world = floor.getWorld();
		File sourceFolder = world.getWorldFolder();
		FileUtil.copyFolder(sourceFolder, pushFolder);
 	}
	
	private void showHelp(CommandSender sender) {
		boolean gmGeneral = hasPermission(sender, PermissionLevel.GM);
		boolean gm = hasPermission(sender, SystemProfileFlag.GM_FLOOR);
		boolean tester = hasPermission(sender, PermissionLevel.TESTER);
		boolean del = gm && hasPermission(sender, SystemProfileFlag.GM_DELETE);
		boolean tm = hasPermission(sender, SystemProfileFlag.TASK_MANAGER);
		
		ChatColor gmGeneralColor = gmGeneral ? ChatColor.YELLOW : ChatColor.RED;
		ChatColor gmColor = gm ? ChatColor.YELLOW : ChatColor.RED;
		ChatColor testerColor = tester ? ChatColor.YELLOW : ChatColor.RED;
		ChatColor delColor = del ? ChatColor.YELLOW : ChatColor.RED;
		ChatColor tmColor = tm ? ChatColor.YELLOW : ChatColor.RED;
		
		sender.sendMessage(gmColor + "/floor create <FloorName> <WorldName> <LevelMin> [-superflat]" + ChatColor.GRAY + " create a new floor");
		sender.sendMessage(testerColor + "/floor list" + ChatColor.GRAY + " list all floors");
		sender.sendMessage(delColor + "/floor delete <FloorName>" + ChatColor.GRAY + " delete floor");
		sender.sendMessage(testerColor + "/floor goto <FloorName>" + ChatColor.GRAY + " teleport to floor (bypasses floor level min)");
		sender.sendMessage(ChatColor.DARK_GRAY + " Testers can only access floors they're authorized for. All GMs bypass this.");
		sender.sendMessage(gmGeneralColor + "/floor <FloorName>" + ChatColor.GRAY + " view info about floor");
		sender.sendMessage(gmColor + "/floor <FloorName> name <NewFloorName>" + ChatColor.GRAY + " change floor name");
		sender.sendMessage(gmColor + "/floor <FloorName> displayname <NewFloorDisplayName>" + ChatColor.GRAY + " change floor display name");
		sender.sendMessage(gmColor + "/floor <FloorName> lvmin <NewLevelMin>" + ChatColor.GRAY + " change floor level requirement");
		sender.sendMessage(tmColor + "/floor <FloorName> status <Status>" + ChatColor.GRAY + " change floor status");
		sender.sendMessage(ChatColor.DARK_GRAY + " Floor statuses: " + StringUtil.parseList(FloorStatus.values()));
		sender.sendMessage(gmColor + "/floor <FloorName> volatile <Volatile>" + ChatColor.GRAY + " set whether the floor is volatile");
		sender.sendMessage(tmColor + "/floor push <FloorName>" + ChatColor.GRAY + " push the floor to production staging");
		sender.sendMessage(ChatColor.DARK_GRAY + "" +  ChatColor.BOLD + "Note:" + ChatColor.DARK_GRAY + " Floor names must not contain spaces.");
		sender.sendMessage(ChatColor.GRAY + "View the full documentation at " + ChatColor.UNDERLINE + Dragons.STAFF_DOCUMENTATION);
	}
	
	private void pushFloor(CommandSender sender, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return;
		Floor floor = lookupFloor(sender, args[1]);
		if(floor == null) return;
		if(args.length <= 2 || !args[2].equalsIgnoreCase("--confirm")) {
			sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.UNDERLINE + "/!\\" + ChatColor.RED + " Pushing this floor will copy it into the PRODUCTION STAGING folder. "
					+ "This is an indication that it is ready to be moved to the live production environment. It may be picked up by Apollo Sync at any time. " + ChatColor.BOLD + "Confirm?");
			TextComponent confirm = new TextComponent(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[CONFIRM]");
			confirm.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
				new Text(ChatColor.GREEN + "Click to confirm push of floor #" + floor.getLevelMin() + " " + floor.getDisplayName())));
			confirm.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/floor push " + args[1] + " --confirm"));
			sender.spigot().sendMessage(confirm);
			return;
		}
		sender.sendMessage(ChatColor.GREEN + "Pushing floor " + floor.getDisplayName() + "...");
		pushFloor(floor);
		AUDIT_LOG.savePush(floor, user(sender));
		floor.setFloorStatus(FloorStatus.LIVE);
		sender.sendMessage(ChatColor.GREEN + "...Complete! Floor status was changed to LIVE");
		Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[FLOOR PUSH] " + ChatColor.GREEN + "Floor " + floor.getDisplayName() + " was pushed to production staging! Great work everyone");
	}
	
	private void createFloor(CommandSender sender, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.GM_FLOOR)) return;
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
		Integer lvMinOpt = parseInt(sender, args[3]);
		if(lvMinOpt == null) return;
		Floor floor = floorLoader.registerNew(args[1], args[2], args[2], lvMinOpt, superflat);
		AUDIT_LOG.saveEntry(floor, user(sender), "Created");
		sender.sendMessage(ChatColor.GREEN + "Created new floor successfully!");
	}
	
	private void listFloors(CommandSender sender) {
		sender.sendMessage(ChatColor.GREEN + "Listing all floors:");
		for(GameObject gameObject : registry.getRegisteredObjects(GameObjectType.FLOOR)) {
			Floor floor = (Floor) gameObject;
			sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + "- " + floor.getFloorName() + " [" + floor.getFloorStatus() + "] [Lv " + floor.getLevelMin() + "]" 
				+ (floor.getStorageAccess() instanceof LocalStorageAccess ? LOCAL_FLOOR_WARNING : ""),
				"/floor goto " + floor.getFloorName(), "Click to go to floor " + floor.getFloorName()));
		}
	}
	
	private void selectFloor(CommandSender sender, String[] args) {
		User user = user(sender);
		Floor floor = lookupFloor(sender, args[0]);
		if(floor == null) return;
		
		if(args.length == 1) {
			sender.sendMessage(ChatColor.GREEN + "=== Floor: " + floor.getFloorName() + " ===");
			String usable = floor.isPlayerLocked() ? ChatColor.RED + "Closed to players" : ChatColor.GREEN + "Open to players";
			String editable = floor.isGMLocked() ? ChatColor.RED + "Closed to editing" : ChatColor.GREEN + "Open to editing";
			sender.sendMessage(usable + ChatColor.GRAY + " - " + editable);
			sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + "[Go To Floor]", "/floor goto " + floor.getFloorName(), "Click to go to floor " + floor.getFloorName()));
			sender.sendMessage(ChatColor.GRAY + "DBID: " + ChatColor.GREEN + floor.getIdentifier().toString());
			sender.sendMessage(ChatColor.GRAY + "Floor Name: " + ChatColor.GREEN + floor.getFloorName());
			sender.sendMessage(ChatColor.GRAY + "Display Name: " + ChatColor.GREEN + floor.getDisplayName());
			sender.sendMessage(ChatColor.GRAY + "Status: " + ChatColor.GREEN + floor.getFloorStatus());
			sender.sendMessage(ChatColor.GRAY + "World Name: " + ChatColor.GREEN + floor.getWorldName());
			sender.sendMessage(ChatColor.GRAY + "Level Min: " + ChatColor.GREEN + floor.getLevelMin());
			sender.sendMessage(ChatColor.GRAY + "Volatile: " + ChatColor.GREEN + floor.isVolatile());
			sender.spigot().sendMessage(ObjectMetadataCommand.getClickableMetadataLink(GameObjectType.FLOOR, floor.getUUID()));
			return;
		}
		Document base = Document.parse(floor.getData().toJson());
		if(floor.isGMLocked() && !hasPermission(sender, PermissionLevel.ADMIN)) {
			sender.sendMessage(ChatColor.RED + "This floor is not currently editable. (Status: " + floor.getFloorStatus() + ")");
		}
		else if(args.length <= 2) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /floor <FloorName> <name|displayname|lvmin|volatile|status> <Value>");
		}
		else if(args[1].equalsIgnoreCase("name")) {
			floor.setFloorName(args[2]);
			sender.sendMessage(ChatColor.GREEN + "Updated floor name successfully.");
			AUDIT_LOG.saveEntry(floor, user, base, "Updated floor internal name to " + args[2]);
		}
		else if(args[1].equalsIgnoreCase("displayname")) {
			floor.setDisplayName(StringUtil.concatArgs(args, 2));
			sender.sendMessage(ChatColor.GREEN + "Updated floor display name successfully.");
			AUDIT_LOG.saveEntry(floor, user, base, "Updated floor display name to " + StringUtil.concatArgs(args, 2));
		}
		else if(args[1].equalsIgnoreCase("status")) {
			if(!requirePermission(sender, SystemProfileFlag.TASK_MANAGER)) return;
			FloorStatus status = StringUtil.parseEnum(sender, FloorStatus.class, args[2]);
			if(status == null) return;
			floor.setFloorStatus(status);
			sender.sendMessage(ChatColor.GREEN + "Updated floor status successfully.");
			AUDIT_LOG.saveEntry(floor, user, base, "Updated floor status to " + status);
		}
		else if(args[1].equalsIgnoreCase("lvmin")) {
			Integer lvMin = parseInt(sender, args[2]);
			if(lvMin == null) return;
			floor.setLevelMin(lvMin);
			sender.sendMessage(ChatColor.GREEN + "Updated floor level requirement successfully.");
			AUDIT_LOG.saveEntry(floor, user, base, "Updated floor level min to " + lvMin);
		}
		else if(args[1].equalsIgnoreCase("volatile")) {
			Boolean isVolatile = parseBoolean(sender, args[2]);
			if(isVolatile == null) return;
			floor.setVolatile(isVolatile);
			sender.sendMessage(ChatColor.GREEN + "Updated floor volatility status sucessfully.");
			AUDIT_LOG.saveEntry(floor, user, base, "Updated floor volatility to " + isVolatile);
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid arguments! /floor <FloorName> <name|displayname|lvmin|volatile> <Value>");
		}
	}
	
	private void deleteFloor(CommandSender sender, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return;
		Floor floor = lookupFloor(sender, args[1]);
		if(floor == null) return;
		if((floor.isGMLocked() || floor.getFloorStatus() == FloorStatus.LIVE)) {
			sender.sendMessage(ChatColor.RED + "This floor cannot currently be deleted (status: " + floor.getFloorStatus() + ")");
			return;
		}
		registry.removeFromDatabase(floor);
		AUDIT_LOG.saveEntry(floor, user(sender), "Deleted");
		sender.sendMessage(ChatColor.GREEN + "Deleted this floor successfully. World files remain intact. Changes may not fully take effect until a server restart.");
	}
	
	private void goToFloor(CommandSender sender, String[] args) {
		Floor floor = lookupFloor(sender, args[1]);
		if(floor == null) return;
		FloorStatus status = floor.getFloorStatus();
		boolean access = status == FloorStatus.TESTING || hasPermission(sender, PermissionLevel.BUILDER);
		if(!access) {
			sender.sendMessage(ChatColor.RED + "Testers can only teleport to floors that are in testing!");
			return;
		}
		user(sender).sendToFloor(floor.getFloorName(), true);
		sender.sendMessage(ChatColor.GREEN + "Teleported to floor successfully.");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.TESTER)) return true;

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
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "-p", "p", "-push", "push")) {
			pushFloor(sender, args);
		}
		else {
			selectFloor(sender, args);
		}
		
		return true;
	}
}
