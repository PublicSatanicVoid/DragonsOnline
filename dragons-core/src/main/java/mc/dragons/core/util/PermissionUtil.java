package mc.dragons.core.util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;

/**
 * Used primarily in commands to verify a user's access to sensitive functionality or data.
 * 
 * @author Adam
 *
 */
public class PermissionUtil {
	public static boolean verifyActivePermissionLevel(User user, PermissionLevel required, boolean notify) {
		if(user == null) {
			return false;
		}
		if (user.getActivePermissionLevel().ordinal() < required.ordinal()) {
			if (notify) {
				user.getPlayer().sendMessage(ChatColor.RED + "This requires permission level " + ChatColor.ITALIC + required.toString().toLowerCase() + ChatColor.RED + " or higher.");
			}
			return false;
		}
		return true;
	}

	public static boolean verifyActiveProfileFlag(User user, SystemProfileFlag flag, boolean notify) {
		if(user == null) {
			return false;
		}
		boolean hasFlag = false;
		if (user.getSystemProfile() != null) {
			hasFlag = user.getSystemProfile().getFlags().hasFlag(flag);
		}
		if (!hasFlag && notify) {
			user.getPlayer().sendMessage(ChatColor.RED + "This requires profile flag " + ChatColor.ITALIC + flag.toString().toLowerCase() + ChatColor.RED + ".");
		}
		return hasFlag;
	}
	
	public static List<PermissionLevel> getAllowedLevels(PermissionLevel max) {
		List<PermissionLevel> result = new ArrayList<>();
		for(PermissionLevel level : PermissionLevel.values()) {
			if(level.ordinal() <= max.ordinal()) {
				result.add(level);
			}
		}
		return result;
	}
}
