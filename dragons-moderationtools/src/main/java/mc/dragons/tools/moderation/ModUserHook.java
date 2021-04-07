package mc.dragons.tools.moderation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.gameobject.user.punishment.PunishmentType;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.tools.moderation.analysis.IPAnalysisUtil;

public class ModUserHook implements UserHook {
	
	@Override
	public void onVerifiedJoin(User user) {
		Set<User> alts = IPAnalysisUtil.scanAlts(Dragons.getInstance().getPersistentStorageManager(), user)
				.stream()
				.filter(u -> u.getActivePunishmentData(PunishmentType.BAN) != null)
				.collect(Collectors.toSet());
		if(alts.size() > 0) {
			UserLoader.allUsers()
				.stream()
				.filter(u -> PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.MODERATION, false))
				.filter(u -> u.getData().getEmbedded(Arrays.asList("modnotifs", "susjoin"), true))
				.forEach(u -> {
					u.getPlayer().sendMessage(ChatColor.GRAY + "[" + ChatColor.RED + "Join Alert" + ChatColor.GRAY + "] User " 
						+ user.getName() + " shares an IP address with " + alts.size() + " currently banned user" + (alts.size() > 1 ? "s" : ""));
				});
		}
	}
	
}
