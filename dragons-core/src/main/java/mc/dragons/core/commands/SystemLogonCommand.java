package mc.dragons.core.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.gameobject.user.permission.SystemProfileLoader;
import mc.dragons.core.util.StringUtil;

public class SystemLogonCommand extends DragonsCommandExecutor {
	private SystemProfileLoader systemProfileLoader = instance.getLightweightLoaderRegistry().getLoader(SystemProfileLoader.class);
	
	private Map<User, Long> rateLimiting;
	private Map<User, Integer> rateLimitingCounter;

	public SystemLogonCommand() {
		rateLimiting = new HashMap<>();
		rateLimitingCounter = new HashMap<>();
	}
	
	private boolean requireLogin(CommandSender sender) {
		if (!isPlayer(sender) || user(sender).getSystemProfile() == null) {
			sender.sendMessage(ChatColor.RED + "You're not logged in to a system profile.");
			return false;
		}
		return true;
	}
	
	private void showHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.GREEN + "System Logon Service");
		if (isPlayer(sender)) {
			sender.sendMessage(ChatColor.YELLOW + "/syslogon <profile> <password>");
			sender.sendMessage(ChatColor.YELLOW + "/syslogon password <current password> <new password>");
			sender.sendMessage(ChatColor.YELLOW + "/syslogon level <new permission level>");
			sender.sendMessage(ChatColor.YELLOW + "/syslogon alt <alt username> <alt password>");
			sender.sendMessage(ChatColor.YELLOW + "/syslogon rmalt <alt username>");
			sender.sendMessage(ChatColor.YELLOW + "/syslogon logout [-clean]");
		}
		String adminReq = ChatColor.RED + " (Admin+)";
		sender.sendMessage(ChatColor.YELLOW + "/syslogon create <profile> <username> <password> <max. permission level>" + adminReq);
		sender.sendMessage(ChatColor.YELLOW + "/syslogon update <profile> <new max. permission level>" + adminReq);
		sender.sendMessage(ChatColor.YELLOW + "/syslogon flag <profile> <" + StringUtil.parseList(SystemProfileFlag.values(), "|") + "> <true|false>" + adminReq);
		sender.sendMessage(ChatColor.YELLOW + "/syslogon info <profile>" + adminReq);
		sender.sendMessage(ChatColor.YELLOW + "/syslogon list" + adminReq);
		sender.sendMessage(ChatColor.YELLOW + "/syslogon migrate <profile> <owner>" + adminReq);
		sender.sendMessage(ChatColor.YELLOW + "/syslogon resetpassword <profile>" + adminReq);
		sender.sendMessage(ChatColor.YELLOW + "/syslogon [de]activate <profile>" + adminReq);
		sender.sendMessage(ChatColor.YELLOW + "/syslogon [de]opfloor <profile> <floor>" + adminReq);
		sender.sendMessage(ChatColor.DARK_GRAY + "Note: Profiles and passwords cannot contain spaces.");
	}
	
	private void createProfile(CommandSender sender, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return;
		
		if(args.length < 5) {
			sender.sendMessage(ChatColor.RED + "/syslogon create <profile> <username> <password> <max. permission level>");
			return;
		}
		
		PermissionLevel level = StringUtil.parseEnum(sender, PermissionLevel.class, args[4]);
		if(level == null) return;
		
		if(level.compareTo(PermissionLevel.ADMIN) > 0 && !hasPermission(sender, PermissionLevel.SYSOP)) return;
		
		systemProfileLoader.createProfile(args[1], args[2], args[3], level);
		sender.sendMessage(ChatColor.GREEN + "Created system profile successfully.");
	}
	
	private void updateProfilePermissionLevel(CommandSender sender, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return;
		
		if(args.length < 3) {
			sender.sendMessage(ChatColor.RED + "/syslogon update <profile> <new max. permission level>");
		}
		
		PermissionLevel level = StringUtil.parseEnum(sender, PermissionLevel.class, args[2]);
		if(level == null) return;
		
		if(level.compareTo(PermissionLevel.ADMIN) > 0 && !hasPermission(sender, PermissionLevel.SYSOP)) return;
		
		systemProfileLoader.setProfileMaxPermissionLevel(args[1], level);
		sender.sendMessage(ChatColor.GREEN + "Updated system profile successfully.");
	}
	
	private void updateProfilePermissionFlag(CommandSender sender, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return;
		
		Boolean value = parseBoolean(sender, args[3]);
		if(value == null) return;
		
		systemProfileLoader.setProfileFlag(args[1], args[2].toUpperCase(), value);
		sender.sendMessage(ChatColor.GREEN + "Updated system profile successfully.");
	}
	
	private void resetProfilePassword(CommandSender sender, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return;
		
		String pwd = StringUtil.encodeBase64(UUID.randomUUID().toString()).substring(0, 5);
		
		SystemProfile systemProfile = systemProfileLoader.loadProfile(args[1]);
		for(UUID uuid : systemProfile.getAllowedUUIDs()) {
			systemProfileLoader.setProfilePassword(systemProfile.getProfileName(), uuid, pwd);
		}
		
		sender.sendMessage(ChatColor.GREEN + "Reset the password for all accounts on this profile. The temporary password is " + pwd);
	}
	
	private void showProfileInfo(CommandSender sender, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return;
		
		SystemProfile systemProfile = systemProfileLoader.loadProfile(args[1]);
		sender.sendMessage(ChatColor.GOLD + "Viewing system profile " + systemProfile.getProfileName());
		sender.sendMessage(ChatColor.YELLOW + "Status: " + ChatColor.RESET + (systemProfile.isActive() ? "Active" : "Inactive"));
		sender.sendMessage(ChatColor.YELLOW + "Max. Permission Level: " + ChatColor.RESET + systemProfile.getMaxPermissionLevel());
		sender.sendMessage(ChatColor.YELLOW + "Flags: " + ChatColor.RESET + systemProfile.getFlags());
		sender.sendMessage(ChatColor.YELLOW + "Allowed Users:");
		for(UUID uuid : systemProfile.getAllowedUUIDs()) {
			User user = GameObjectType.USER.<User, UserLoader>getLoader().loadObject(uuid);
			if(user == null) {
				sender.sendMessage(ChatColor.RESET + "- " + uuid);
			}
			else {
				sender.sendMessage(ChatColor.RESET + "- " + uuid + " (" + user.getName() + ")");
			}
		}
		sender.sendMessage(ChatColor.YELLOW + "Current User: " + ChatColor.RESET + (systemProfile.getCurrentUser() == null ? "(None)" : systemProfile.getCurrentUser().getName()));
	}
	
	private void listProfiles(CommandSender sender) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return;
		
		List<SystemProfile> profiles = systemProfileLoader.getAllProfiles();
		sender.sendMessage(ChatColor.GOLD + "" + profiles.size() + " profiles:");
		for(SystemProfile profile : profiles) {
			sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + profile.getProfileName()
				+ ChatColor.GRAY + " (" + profile.getMaxPermissionLevel() + ", " + profile.getAllowedUUIDs().size() + " authorized)");
		}
	}
	
	private void migrateProfile(CommandSender sender, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return;
		
		systemProfileLoader.migrateProfile(args[1], args[2]);
		sender.sendMessage(ChatColor.GREEN + "Migrated profile " + args[1] + " and locked to user " + args[2]);
	}
	
	private void activateProfile(CommandSender sender, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return;

		systemProfileLoader.setActive(args[1], true);
		sender.sendMessage(ChatColor.GREEN + "Activated system profile successfully.");
	}
	
	private void deactivateProfile(CommandSender sender, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return;

		systemProfileLoader.setActive(args[1], false);
		sender.sendMessage(ChatColor.GREEN + "Deactivated system profile successfully.");
	}
	
	private void opFloor(CommandSender sender, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return;

		Floor floor = lookupFloor(sender, args[2]);
		if(floor == null) return;
		
		systemProfileLoader.addAdminFloor(args[1], floor);
		sender.sendMessage(ChatColor.GREEN + "Added admin floor successfully.");
	}
	
	private void deopFloor(CommandSender sender, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return;

		Floor floor = lookupFloor(sender, args[2]);
		if(floor == null) return;
		
		systemProfileLoader.removeAdminFloor(args[1], floor);
		sender.sendMessage(ChatColor.GREEN + "Removed admin floor successfully.");
	}
	
	private void changePassword(CommandSender sender, String[] args) {
		if(!requirePlayer(sender)) return;
		User user = user(sender);
		
		if (!requireLogin(sender)) return;
		if (!SystemProfileLoader.passwordHash(args[1]).equals(user.getSystemProfile().getPasswordHash(user.getUUID()))) {
			sender.sendMessage(ChatColor.RED + "Incorrect current password!");
			return;
		}
		systemProfileLoader.setProfilePassword(user.getSystemProfile().getProfileName(), user.getUUID(), args[2]);
		systemProfileLoader.logoutProfile(user.getSystemProfile().getProfileName());
		user.setActivePermissionLevel(PermissionLevel.USER);
		sender.sendMessage(ChatColor.GREEN + "Password updated successfully. Please log back in to your system profile with your updated credentials.");
	}
	
	private void changeActiveLevel(CommandSender sender, String[] args) {
		if(!requirePlayer(sender)) return;
		User user = user(sender);
		
		if(!requireLogin(sender)) return;
		if (user.getSystemProfile() == null) {
			sender.sendMessage(ChatColor.RED + "You're not logged in to a system profile.");
			return;
		}
		PermissionLevel level = StringUtil.parseEnum(sender, PermissionLevel.class, args[1]);
		if(level == null) return;
		
		boolean result = user.setActivePermissionLevel(level);
		if (result) {
			sender.sendMessage(ChatColor.GREEN + "Changed active permission level to " + args[1]);
		} else {
			sender.sendMessage(ChatColor.RED + "Could not change active permission level: requested permission level exceeds maximum level for this profile.");
		}
	}
	
	private void registerAlt(CommandSender sender, String[] args) {
		if(!requirePlayer(sender)) return;
		User user = user(sender);

		if(!requireLogin(sender)) return;
		if (user.getSystemProfile() == null) {
			sender.sendMessage(ChatColor.RED + "You're not logged in to a system profile.");
			return;
		}
		systemProfileLoader.registerAlt(user.getSystemProfile().getProfileName(), args[1], args[2]);
		sender.sendMessage(ChatColor.GREEN + "Registered alt account " + args[1] + " successfully.");
	}
	
	private void unregisterAlt(CommandSender sender, String[] args) {
		if(!requirePlayer(sender)) return;
		User user = user(sender);

		if(!requireLogin(sender)) return;
		if (user.getSystemProfile() == null) {
			sender.sendMessage(ChatColor.RED + "You're not logged in to a system profile.");
			return;
		}
		systemProfileLoader.unregisterAlt(user.getSystemProfile().getProfileName(), args[1]);
		sender.sendMessage(ChatColor.GREEN + "Unregistered alt account " + args[1] + " successfully.");
	}
	
	private void logout(CommandSender sender, String[] args) {
		if(!requirePlayer(sender)) return;
		User user = user(sender);
		Player player = player(sender);
	
		if(!requireLogin(sender)) return;
		systemProfileLoader.logoutProfile(user.getSystemProfile().getProfileName());
		user.setActivePermissionLevel(PermissionLevel.USER);
		user.setSystemProfile(null);
		if(args.length > 1 && args[1].equalsIgnoreCase("-clean")) {
			player.teleport(user.getSavedLocation());
			player.setGameMode(GameMode.ADVENTURE);
			user.setDebuggingErrors(false);
			user.setChatSpy(false);
			user.setGodMode(false);
			user.setVanished(false);
		}
		sender.sendMessage(ChatColor.GREEN + "Successfully logged out of your system profile.");
	}
	
	private void login(CommandSender sender, String[] args) {
		if(!requirePlayer(sender)) return;
		User user = user(sender);
		
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "/syslogon <profile> <password>");
			return;
		}
		long wait = rateLimiting.getOrDefault(user, 0L) + 1000 * rateLimitingCounter.getOrDefault(user, 0);
		if (wait > System.currentTimeMillis()) {
			sender.sendMessage(ChatColor.RED + "Please wait " + rateLimitingCounter.get(user) + "s.");
			return;
		}
		UUID cid = CORRELATION.registerNewCorrelationID();
		if (user.getSystemProfile() != null) {
			CORRELATION.log(cid, Level.FINE, "user is currently signed in. signing out first");
			systemProfileLoader.logoutProfile(user.getSystemProfile().getProfileName());
			user.setActivePermissionLevel(PermissionLevel.USER);
			sender.sendMessage(ChatColor.GREEN + "Signed out of current system profile");
		}
		SystemProfile profile = systemProfileLoader.authenticateProfile(user, args[0], args[1], cid);
		if (profile == null) {
			CORRELATION.log(cid, Level.FINE, "user notified of error");
			sender.sendMessage(ChatColor.RED + "Could not log in! Make sure you are authorized on this account and entered the correct password.");
			sender.sendMessage(ChatColor.RED + "If the issue persists, report the following error message: " + StringUtil.toHdFont("Correlation ID: " + cid));
			rateLimiting.put(user, System.currentTimeMillis());
			rateLimitingCounter.put(user, Integer.valueOf(Math.max(rateLimitingCounter.getOrDefault(user, 0) * 2, 1)));
			return;
		}
		user.setSystemProfile(profile);
		user.setActivePermissionLevel(profile.getMaxPermissionLevel());
		sender.sendMessage(ChatColor.GREEN + "Logged on to system console as " + profile.getProfileName() + ". Permission level: " + profile.getMaxPermissionLevel().toString());
		CORRELATION.discard(cid);
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			showHelp(sender);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-create", "-c", "create", "c")) {
			createProfile(sender, args);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-update", "-c", "update", "u")) {
			updateProfilePermissionLevel(sender, args);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-flag", "-f", "flag", "f")) {
			updateProfilePermissionFlag(sender, args);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-info", "-i", "info", "i")) {
			showProfileInfo(sender, args);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-migrate", "migrate")) {
			migrateProfile(sender, args);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-activate", "activate")) {
			activateProfile(sender, args);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-deactivate", "deactivate")) {
			deactivateProfile(sender, args);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-opfloor", "opfloor")) {
			opFloor(sender, args);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-deopfloor", "deopfloor")) {
			deopFloor(sender, args);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-level", "level")) {
			changeActiveLevel(sender, args);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-password", "password", "-pwd", "pwd", "-p", "p", "-changepassword", "changepassword", "-pass", "pass")) {
			changePassword(sender, args);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-alt", "alt")) {
			registerAlt(sender, args);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-resetpassword", "resetpassword", "-rp", "rp")) {
			resetProfilePassword(sender, args);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-rmalt", "rmalt")) {
			unregisterAlt(sender, args);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-logout", "logout", "-exit", "exit", "-x", "x")) {
			logout(sender, args);
		}
		else if (StringUtil.equalsAnyIgnoreCase(args[0], "-list", "-l", "list", "l")) {
			listProfiles(sender);
		}
		else {
			login(sender, args);
		}
		return true;
	}
}
