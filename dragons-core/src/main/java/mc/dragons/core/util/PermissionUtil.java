package mc.dragons.core.util;

import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.storage.impl.SystemProfile;
import org.bukkit.ChatColor;

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
