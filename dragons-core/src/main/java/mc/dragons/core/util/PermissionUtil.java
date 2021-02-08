package mc.dragons.core.util;

import org.bukkit.ChatColor;

import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.SystemProfile;
import mc.dragons.core.gameobject.user.User;

/**
 * Used primarily in commands to verify a user's access to sensitive functionality or data.
 * 
 * @author Adam
 *
 */
public class PermissionUtil {
	public static boolean verifyActivePermissionLevel(User user, PermissionLevel required, boolean notify) {
		if(user == null) return false;
		if (user.getActivePermissionLevel().ordinal() < required.ordinal()) {
			if (notify)
				user.getPlayer().sendMessage(ChatColor.RED + "This requires permission level " + required.toString().toLowerCase() + " or higher.");
			return false;
		}
		return true;
	}

	public static boolean verifyActiveProfileFlag(User user, SystemProfile.SystemProfileFlags.SystemProfileFlag flag, boolean notify) {
		if(user == null) return false;
		boolean hasFlag = false;
		if (user.getSystemProfile() != null)
			hasFlag = user.getSystemProfile().getFlags().hasFlag(flag);
		if (!hasFlag && notify)
			user.getPlayer().sendMessage(ChatColor.RED + "This requires profile flag " + flag.toString().toLowerCase() + ".");
		return hasFlag;
	}
}
