package mc.dragons.tools.dev.gameobject;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.StringUtil;

public class ObjectCommands extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.DEVELOPMENT)) return true;
		
		GameObjectRegistry registry = instance.getGameObjectRegistry();
		StorageManager localStorageManager = instance.getLocalStorageManager();
		
		if(label.equalsIgnoreCase("autosave")) {
			Dragons.getInstance().getGameObjectRegistry().executeAutoSave(true);
			sender.sendMessage(ChatColor.GREEN + "Auto-save executed successfully.");
		}
		
		else if(label.equalsIgnoreCase("invalidate")) {
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
