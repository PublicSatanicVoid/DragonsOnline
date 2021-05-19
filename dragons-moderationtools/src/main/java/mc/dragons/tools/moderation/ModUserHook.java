package mc.dragons.tools.moderation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.ChatColor;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.tools.moderation.analysis.IPAnalysisUtil;
import mc.dragons.tools.moderation.hold.HoldLoader;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldEntry;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldStatus;
import mc.dragons.tools.moderation.punishment.PunishmentData;
import mc.dragons.tools.moderation.punishment.PunishmentType;
import mc.dragons.tools.moderation.punishment.StandingLevelType;

public class ModUserHook implements UserHook {
	private HoldLoader holdLoader;
	
	public ModUserHook(Dragons instance) {
		holdLoader = instance.getLightweightLoaderRegistry().getLoader(HoldLoader.class);
	}
	
	@Override
	public void onCreateStorageAccess(Document data) {
		Document blankStandingLevel = new Document();
		for(StandingLevelType type : StandingLevelType.values()) {
			blankStandingLevel.append(type.toString(), new Document("level", 0).append("on", 0L));
		}
		data.append("standingLevel", blankStandingLevel);
	}
	
	@Override
	public void onInitialize(User user) {
		WrappedUser wrapped = WrappedUser.of(user);
		wrapped.updateStandingLevels();
		
		if(user.getPlayer() != null) {
			PunishmentData banData = wrapped.getActivePunishmentData(PunishmentType.BAN);
			if (banData != null) {
				user.getPlayer().kickPlayer(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You are banned.\n\n"
						+ (banData.getReason().equals("") ? "" : ChatColor.GRAY + "Reason: " + ChatColor.WHITE + banData.getReason() + "\n") + ChatColor.GRAY + "Expires: " + ChatColor.WHITE
						+ (banData.isPermanent() ? "Never" : banData.getExpiry().toString()));
				return;
			}
			HoldEntry entry = holdLoader.getHoldByUser(user);
			if(entry != null && entry.getStatus() == HoldStatus.PENDING) {
				user.getPlayer().kickPlayer(ChatColor.RED + "Your account was flagged for suspicious activity and is suspended pending review.");
			}
		}
	}
	
	@Override
	public boolean checkAllowChat(User user, String message) {
		WrappedUser wrapped = WrappedUser.of(user);
		PunishmentData muteData = wrapped.getActivePunishmentData(PunishmentType.MUTE);
		if (muteData != null) {
			user.getPlayer().sendMessage(ChatColor.RED + "You are muted!" + (muteData.getReason().equals("") ? "" : " (" + muteData.getReason() + ")"));
			if(!muteData.isPermanent()) {
				user.getPlayer().sendMessage(ChatColor.RED + "Expires " + muteData.getExpiry().toString());
			}
			return false;
		}
		return true;
	}
	
	@Override
	public void onVerifiedJoin(User user) {
		WrappedUser wrapped = WrappedUser.of(user);
		Set<User> alts = IPAnalysisUtil.scanAlts(Dragons.getInstance().getPersistentStorageManager(), user)
				.stream()
				.filter(u -> WrappedUser.of(u).getActivePunishmentData(PunishmentType.BAN) != null)
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
		
		if(wrapped.reportWasHandled()) {
			user.getPlayer().sendMessage(ChatColor.GOLD + "" + ChatColor.ITALIC + "Your recent report was handled and closed. Thank you!");
			wrapped.setReportHandled(false);
		}
	}
	
}
