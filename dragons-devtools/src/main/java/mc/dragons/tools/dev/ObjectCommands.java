package mc.dragons.tools.dev;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class ObjectCommands implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player) sender;
			User user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true)) return true;
		}
		
		GameObjectRegistry registry = Dragons.getInstance().getGameObjectRegistry();
		StorageManager localStorageManager = Dragons.getInstance().getLocalStorageManager();
		
		if(label.equalsIgnoreCase("invalidate")) {
			if(args.length < 2) {
				sender.sendMessage(ChatColor.RED + "/invalidate <type> <uuid>");
				return true;
			}
			GameObjectType type = StringUtil.parseEnum(sender, GameObjectType.class, args[0].toUpperCase());
			UUID uuid = StringUtil.parseUUID(sender, args[1]);
			if(type == null || uuid == null) return true;
			boolean result = registry.getRegisteredObjects().removeIf(obj -> obj.getType() == type && obj.getUUID().equals(uuid));
			if(result) {
				sender.sendMessage(ChatColor.GREEN + "Invalidated game object " + type + "#" + uuid + ".");
			}
			else {
				sender.sendMessage(ChatColor.RED + "Unable to locate game object (is it registered properly?)");
			}
		}
		
		else if(label.equalsIgnoreCase("invalidateall")) {
			registry.getRegisteredObjects().clear();
			sender.sendMessage(ChatColor.GREEN + "Invalidated all game objects.");
		}
		
		else if(label.equalsIgnoreCase("invalidatetype")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/invalidatetype <type>");
				return true;
			}
			GameObjectType type = StringUtil.parseEnum(sender, GameObjectType.class, args[0].toUpperCase());
			if(type == null) return true;
			registry.getRegisteredObjects().removeIf(obj -> obj.getType() == type);
			sender.sendMessage(ChatColor.GREEN + "Invalidated all game objects of type " + type);
		}
		
		else if(label.equalsIgnoreCase("invalidateuser")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/invalidateuser <player>");
				return true;
			}
			boolean result = UserLoader.allUsers().removeIf(u -> u.getName().equalsIgnoreCase(args[0]));
			result &= registry.getRegisteredObjects(GameObjectType.USER).removeIf(u -> ((User) u).getName().equalsIgnoreCase(args[0]));
			if(result) {
				sender.sendMessage(ChatColor.GREEN + "Invalidated user " + args[0]);
			}
			else {
				sender.sendMessage(ChatColor.RED + "That user was not found locally!");
			}
		}
		
		else if(label.equalsIgnoreCase("localize")) {
			if(args.length < 2) {
				sender.sendMessage(ChatColor.RED + "/localize <type> <uuid>");
				return true;
			}
			GameObjectType type = StringUtil.parseEnum(sender, GameObjectType.class, args[0].toUpperCase());
			UUID uuid = StringUtil.parseUUID(sender, args[1]);
			if(type == null || uuid == null) return true;
			registry.getRegisteredObjects().stream().filter(obj -> obj.getType() == type && obj.getUUID().equals(uuid)).forEach(obj -> 
				obj.replaceStorageAccess(localStorageManager.getNewStorageAccess(type, obj.getData())));
			sender.sendMessage(ChatColor.GREEN + "Localized game object " + type + "#" + uuid + ".");
		}
		
		else if(label.equalsIgnoreCase("localizeall")) {
			registry.getRegisteredObjects().forEach(obj -> obj.replaceStorageAccess(localStorageManager.getNewStorageAccess(obj.getType(), obj.getData())));
			sender.sendMessage(ChatColor.GREEN + "Localized all game objects.");
		}
		
		else if(label.equalsIgnoreCase("localizetype")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/localizetype <type>");
				return true;
			}
			GameObjectType type = StringUtil.parseEnum(sender, GameObjectType.class, args[0].toUpperCase());
			if(type == null) return true;
			registry.getRegisteredObjects().stream().filter(obj -> obj.getType() == type).forEach(
					obj -> obj.replaceStorageAccess(localStorageManager.getNewStorageAccess(type, obj.getData())));
			sender.sendMessage(ChatColor.GREEN + "Localized all game objects of type " + type);
		}
		
		else if(label.equalsIgnoreCase("localizeuser")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/localizeuser <player>");
				return true;
			}
			UserLoader.allUsers().stream().filter(u -> u.getName().equalsIgnoreCase(args[0])).forEach(
					u -> u.replaceStorageAccess(localStorageManager.getNewStorageAccess(GameObjectType.USER, u.getData())));
			sender.sendMessage(ChatColor.GREEN + "Localized user " + args[0]);
		}
		
		
		
		return true;
	}

}
