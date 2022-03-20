package mc.dragons.core.commands;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.npc.NPCClass;
import mc.dragons.core.gameobject.npc.NPCClassLoader;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestLoader;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.region.RegionLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.storage.loader.GlobalVarLoader;
import mc.dragons.core.util.PermissionUtil;

/**
 * Provides utility functions that make command coding easier.
 * Subclasses must still implement onCommand as required by
 * CommandExecutor from Bukkit.
 * 
 * @author Adam
 *
 */
public abstract class DragonsCommandExecutor implements CommandExecutor {
	/* Common instance variables */
	
	protected Dragons dragons = Dragons.getInstance();
	
	protected UserLoader userLoader = GameObjectType.USER.getLoader();
	protected FloorLoader floorLoader = GameObjectType.FLOOR.getLoader();
	protected RegionLoader regionLoader = GameObjectType.REGION.getLoader();
	protected NPCLoader npcLoader = GameObjectType.NPC.getLoader();
	protected NPCClassLoader npcClassLoader = GameObjectType.NPC_CLASS.getLoader();
	protected ItemLoader itemLoader = GameObjectType.ITEM.getLoader();
	protected ItemClassLoader itemClassLoader = GameObjectType.ITEM_CLASS.getLoader();
	protected QuestLoader questLoader = GameObjectType.QUEST.getLoader();
	
	protected final DragonsLogger LOGGER = dragons.getLogger();
	protected final GlobalVarLoader VAR = dragons.getLightweightLoaderRegistry().getLoader(GlobalVarLoader.class);
	
	/* Standard error messages */
	public static final String ERR_USER_NOT_FOUND = ChatColor.RED + "That user was not found in our records!";
	public static final String ERR_LOCAL_PLAYER_NOT_FOUND = ChatColor.RED + "That player is not online on this server!";
	public static final String ERR_FLOOR_NOT_FOUND = ChatColor.RED + "No floor by that name exists!";
	public static final String ERR_REGION_NOT_FOUND = ChatColor.RED + "No region by that name exists!";
	public static final String ERR_NPC_CLASS_NOT_FOUND = ChatColor.RED + "No NPC class by that name exists!";
	public static final String ERR_ITEM_CLASS_NOT_FOUND = ChatColor.RED + "No item class by that name exists!";
	public static final String ERR_QUEST_NOT_FOUND = ChatColor.RED + "No quest by that name exists!";
	public static final String ERR_EXCEPTION_OCCURRED = ChatColor.RED + "An error occurred!";
	public static final String ERR_EXCEPTION_OCCURRED_W_CORREL = ERR_EXCEPTION_OCCURRED + " Correlation ID: ";
	public static final String ERR_INGAME_ONLY = ChatColor.RED + "This is an ingame-only command!";
	public static final String ERR_NOT_INTEGER = ChatColor.RED + "Invalid input! Please specify a valid integer!";
	public static final String ERR_NOT_NUMBER = ChatColor.RED + "Invalid input! Please specify a valid number!";
	public static final String ERR_NOT_BOOLEAN = ChatColor.RED + "Invalid input! Please specify 'true' or 'false'!";
	public static final String ERR_NOT_UUID = ChatColor.RED + "Invalid input! Please specify a valid hyphenated UUID, e.g. " + UUID.randomUUID();
	public static final String ERR_NO_RESULTS = ChatColor.RED + "No results were returned for this query.";
	public static final String ERR_INTERNAL_ONLY = ChatColor.RED + "This command is for internal use only! Do not run it directly.";
	
	
	/* Helper functions */
	
	/**
	 * Avoid code style warnings for unused parameters by
	 * calling this function for all unused parameters.
	 * 
	 * <p>Only use this if there is a compelling need to not
	 * change the parameter list.
	 * 
	 * @param param
	 */
	protected void unusedParameters(Object... params) { /* unused */ }
	protected void unusedParameter(Object param) { /* unused */ }
	
	protected Player player(CommandSender sender) {
		if(sender instanceof Player) return (Player) sender;
		return null;
	}
	
	protected User user(CommandSender sender) {
		return UserLoader.fromPlayer(player(sender));
	}
	
	protected boolean hasPermission(CommandSender sender, PermissionLevel level) {
		User user = user(sender);
		if(user == null) return true;
		return hasPermission(user, level);
	}

	protected boolean hasPermission(User user, PermissionLevel level) {
		return PermissionUtil.verifyActivePermissionLevel(user, level, false);
	}
	
	protected boolean requirePermission(CommandSender sender, PermissionLevel level) {
		User user = user(sender);
		if(user == null) return true;
		return PermissionUtil.verifyActivePermissionLevel(user, level, true);
	}
	
	protected boolean hasPermission(CommandSender sender, SystemProfileFlag flag) {
		User user = user(sender);
		if(user == null) return true;
		return hasPermission(user, flag);
	}
	
	protected boolean hasPermission(User user, SystemProfileFlag flag) {
		return PermissionUtil.verifyActiveProfileFlag(user, flag, false);
	}
	
	
	protected boolean requirePermission(CommandSender sender, SystemProfileFlag flag) {
		User user = user(sender);
		if(user == null) return true;
		return PermissionUtil.verifyActiveProfileFlag(user, flag, true);
	}

	protected boolean isPlayer(CommandSender sender) {
		return player(sender) != null;
	}
	
	protected boolean requirePlayer(CommandSender sender) {
		if(!isPlayer(sender)) {
			sender.sendMessage(ERR_INGAME_ONLY);
			return false;
		}
		return true;
	}

	protected <T> T parseType(CommandSender sender, T parsed, String errorMessage) {
		if(parsed == null) {
			sender.sendMessage(errorMessage);
			return null;
		}
		return parsed;
	}
	
	protected <T> T parseType(CommandSender sender, Supplier<T> parser, String errorMessage) {
		T value = null;
		try {
			value = parser.get();
		}
		catch(Exception e) { /* ignored */ }
		if(value == null) {
			sender.sendMessage(errorMessage);
		}
		return value;
	}
	
	protected Integer parseInt(CommandSender sender, String value) {
		return parseType(sender, () -> Integer.valueOf(value), ERR_NOT_INTEGER);
	}
	
	protected Double parseDouble(CommandSender sender, String value) {
		return parseType(sender, () -> Double.valueOf(value), ERR_NOT_NUMBER);
	}
	
	protected Float parseFloat(CommandSender sender, String value) {
		return parseType(sender, () -> Float.valueOf(value), ERR_NOT_NUMBER);
	}
	
	protected Boolean parseBoolean(CommandSender sender, String value) {
		return parseType(sender, () -> Boolean.valueOf(value), ERR_NOT_BOOLEAN);
	}
	
	protected UUID parseUUID(CommandSender sender, String value) {
		return parseType(sender, () -> UUID.fromString(value), ERR_NOT_UUID);
	}
	
	protected UUID reportException(Exception e) {
		UUID cid = LOGGER.newCID();
		LOGGER.severe(cid, "An exception occurred: " + e.getCause() + " (" + e.getMessage() + ")");
		Arrays.asList(e.getStackTrace()).stream().forEachOrdered(elem -> {
			LOGGER.severe(cid, elem.toString());
		});
		return cid;
	}
	
	protected <T> T lookup(CommandSender sender, Supplier<T> lookuper, String errorMessage) {
		T result = null;
		try {
			result = lookuper.get();
		} catch (Exception e) {
			UUID cid = reportException(e);
			sender.sendMessage(ERR_EXCEPTION_OCCURRED_W_CORREL + cid);
		}
		
		if(result == null) {
			sender.sendMessage(errorMessage);
		}
		
		return result;
	}
	
	protected void wrappedTry(CommandSender sender, Runnable runnable) {
		try {
			runnable.run();
		} catch (Exception e) {
			UUID cid = reportException(e);
			sender.sendMessage(ERR_EXCEPTION_OCCURRED_W_CORREL + cid);
		}
	}
	
	protected User lookupUser(CommandSender sender, String lookup) {
		return lookup(sender, () -> userLoader.loadObject(lookup), ERR_USER_NOT_FOUND);
	}
	
	protected Player lookupPlayer(CommandSender sender, String lookup) {
		return lookup(sender, () -> Bukkit.getPlayer(lookup), ERR_LOCAL_PLAYER_NOT_FOUND);
	}
	
	protected Floor lookupFloor(CommandSender sender, String lookup) {
		return lookup(sender, () -> FloorLoader.fromFloorName(lookup), ERR_FLOOR_NOT_FOUND);
	}
	
	protected Region lookupRegion(CommandSender sender, String lookup) {
		return lookup(sender, () -> regionLoader.getRegionByName(lookup), ERR_REGION_NOT_FOUND);
	}
	
	protected NPCClass lookupNPCClass(CommandSender sender, String lookup) {
		return lookup(sender, () -> npcClassLoader.getNPCClassByClassName(lookup), ERR_NPC_CLASS_NOT_FOUND);
	}
	
	protected ItemClass lookupItemClass(CommandSender sender, String lookup) {
		return lookup(sender, () -> itemClassLoader.getItemClassByClassName(lookup), ERR_ITEM_CLASS_NOT_FOUND);
	}
	
	protected Quest lookupQuest(CommandSender sender, String lookup) {
		return lookup(sender, () -> questLoader.getQuestByName(lookup), ERR_QUEST_NOT_FOUND);
	}
}
