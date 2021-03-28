package mc.dragons.core.commands;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.npc.NPC;
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
import mc.dragons.core.logging.correlation.CorrelationLogger;
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
	
	protected Dragons instance = Dragons.getInstance();
	
	protected UserLoader userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	protected FloorLoader floorLoader = GameObjectType.FLOOR.<Floor, FloorLoader>getLoader();
	protected RegionLoader regionLoader = GameObjectType.REGION.<Region, RegionLoader>getLoader();
	protected NPCLoader npcLoader = GameObjectType.NPC.<NPC, NPCLoader>getLoader();
	protected NPCClassLoader npcClassLoader = GameObjectType.NPC_CLASS.<NPCClass, NPCClassLoader>getLoader();
	protected ItemLoader itemLoader = GameObjectType.ITEM.<Item, ItemLoader>getLoader();
	protected ItemClassLoader itemClassLoader = GameObjectType.ITEM_CLASS.<ItemClass, ItemClassLoader>getLoader();
	protected QuestLoader questLoader = GameObjectType.QUEST.<Quest, QuestLoader>getLoader();
	
	protected Logger LOGGER = instance.getLogger();
	protected CorrelationLogger CORRELATION = instance.getLightweightLoaderRegistry().getLoader(CorrelationLogger.class);
	
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
	public static final String ERR_NO_RESULTS = ChatColor.RED + "No results were returned for this query.";
	
	
	/* Helper functions */
	
	protected void unusedParameter(Object param) {}
	
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
	
	protected <T> Optional<T> parse(CommandSender sender, T parsed, String errorMessage) {
		if(parsed == null) {
			sender.sendMessage(errorMessage);
			return Optional.empty();
		}
		return Optional.of(parsed);
	}

	/* eventually this should replace parse, so we can get rid of Optionals. Instead
	 * 	of checking isEmpty we can check if null... so much easier
	 */
	protected <T> T parseType(CommandSender sender, T parsed, String errorMessage) {
		if(parsed == null) {
			sender.sendMessage(errorMessage);
			return null;
		}
		return parsed;
	}
	
	protected Optional<Integer> parseIntOpt(CommandSender sender, String value) {
		return parse(sender, Integer.valueOf(value), ERR_NOT_INTEGER);
	}
	
	protected Integer parseIntType(CommandSender sender, String value) {
		return parseType(sender, Integer.valueOf(value), ERR_NOT_INTEGER);
	}
	
	protected Optional<Double> parseDoubleOpt(CommandSender sender, String value) {
		return parse(sender, Double.valueOf(value), ERR_NOT_NUMBER);
	}
	
	protected Double parseDoubleType(CommandSender sender, String value) {
		return parseType(sender, Double.valueOf(value), ERR_NOT_NUMBER);
	}
	
	protected Optional<Float> parseFloatOpt(CommandSender sender, String value) {
		return parse(sender, Float.valueOf(value), ERR_NOT_NUMBER);
	}
	 
	protected Float parseFloatType(CommandSender sender, String value) {
		return parseType(sender, Float.valueOf(value), ERR_NOT_NUMBER);
	}
	
	protected Optional<Boolean> parseBooleanOpt(CommandSender sender, String value) {
		return parse(sender, Boolean.valueOf(value), ERR_NOT_BOOLEAN);
	}
	
	protected Boolean parseBooleanType(CommandSender sender, String value) {
		return parseType(sender, Boolean.valueOf(value), ERR_NOT_BOOLEAN);
	}
	
	protected UUID reportException(Exception e) {
		UUID cid = CORRELATION.registerNewCorrelationID();
		Arrays.asList(e.getStackTrace()).stream().forEachOrdered(elem -> {
			CORRELATION.log(cid, Level.SEVERE, elem.toString());
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
