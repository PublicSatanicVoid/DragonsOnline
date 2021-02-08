package mc.dragons.core.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.SystemProfile;
import mc.dragons.core.gameobject.user.SystemProfileLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.logging.correlation.CorrelationLogLoader;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class SystemLogonCommand implements CommandExecutor {
	private SystemProfileLoader systemProfileLoader;
	private CorrelationLogLoader CORRELATION;
	
	private Map<User, Long> rateLimiting;
	private Map<User, Integer> rateLimitingCounter;

	public SystemLogonCommand(Dragons instance) {
		this.systemProfileLoader = instance.getLightweightLoaderRegistry().getLoader(SystemProfileLoader.class);
		this.CORRELATION = instance.getLightweightLoaderRegistry().getLoader(CorrelationLogLoader.class);
		this.rateLimiting = new HashMap<>();
		this.rateLimitingCounter = new HashMap<>();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(ChatColor.GREEN + "System Logon Service");
			if (sender instanceof Player) {
				sender.sendMessage(ChatColor.YELLOW + "/syslogon <profile> <password>");
				sender.sendMessage(ChatColor.YELLOW + "/syslogon -password <current password> <new password>");
				sender.sendMessage(ChatColor.YELLOW + "/syslogon -level <new permission level>");
				sender.sendMessage(ChatColor.YELLOW + "/syslogon -alt <alt username> <alt password>");
				sender.sendMessage(ChatColor.YELLOW + "/syslogon -rmalt <alt username>");
				sender.sendMessage(ChatColor.YELLOW + "/syslogon -logout");
			}
			sender.sendMessage(ChatColor.YELLOW + "/syslogon -create <profile> <username> <password> <max. permission level>");
			sender.sendMessage(ChatColor.YELLOW + "/syslogon -update <profile> <new max. permission level>");
			sender.sendMessage(ChatColor.YELLOW + "/syslogon -flag <profile> <" + StringUtil.parseList((Object[]) SystemProfile.SystemProfileFlags.SystemProfileFlag.values(), "|") + "> <true|false>");
			sender.sendMessage(ChatColor.YELLOW + "/syslogon -info <profile>");
			sender.sendMessage(ChatColor.YELLOW + "/syslogon -migrate <profile> <owner>");
			sender.sendMessage(ChatColor.YELLOW + "/syslogon -[de]activate <profile>");
			sender.sendMessage(ChatColor.DARK_GRAY + "Note: Profiles and passwords cannot contain spaces.");
			return true;
		}
		if (args[0].equalsIgnoreCase("-create")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				User user = UserLoader.fromPlayer(player);
				if (!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.SYSOP, true))
					return true;
			}
			PermissionLevel level = StringUtil.parseEnum(sender, PermissionLevel.class, args[4]);
			if(level == null) return true;
			this.systemProfileLoader.createProfile(args[1], args[2], args[3], level);
			sender.sendMessage(ChatColor.GREEN + "Created system profile successfully.");
			return true;
		}
		if (args[0].equalsIgnoreCase("-update")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				User user = UserLoader.fromPlayer(player);
				if (!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.SYSOP, true))
					return true;
			}
			PermissionLevel level = StringUtil.parseEnum(sender, PermissionLevel.class, args[2]);
			if(level == null) return true;
			this.systemProfileLoader.setProfileMaxPermissionLevel(args[1], level);
			sender.sendMessage(ChatColor.GREEN + "Updated system profile successfully.");
			return true;
		}
		if (args[0].equalsIgnoreCase("-flag")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				User user = UserLoader.fromPlayer(player);
				if (!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.SYSOP, true))
					return true;
			}
			this.systemProfileLoader.setProfileFlag(args[1], args[2].toUpperCase(), Boolean.valueOf(args[3]).booleanValue());
			sender.sendMessage(ChatColor.GREEN + "Updated system profile successfully.");
			return true;
		}
		if (args[0].equalsIgnoreCase("-info")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				User user = UserLoader.fromPlayer(player);
				if (!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.SYSOP, true))
					return true;
			}
			SystemProfile systemProfile = this.systemProfileLoader.loadProfile(args[1]);
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
			sender.sendMessage(ChatColor.YELLOW + "Current User: " + ChatColor.RESET + ((systemProfile.getCurrentUser() == null) ? "(None)" : systemProfile.getCurrentUser().getName()));
			return true;
		}
		if(args[0].equalsIgnoreCase("-migrate")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				User user = UserLoader.fromPlayer(player);
				if (!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.SYSOP, true))
					return true;
			}
			this.systemProfileLoader.migrateProfile(args[1], args[2]);
			sender.sendMessage(ChatColor.GREEN + "Migrated profile " + args[1] + " and locked to user " + args[2]);
			return true;
		}
		
		if (args[0].equalsIgnoreCase("-activate")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				User user = UserLoader.fromPlayer(player);
				if (!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.SYSOP, true))
					return true;
			}
			this.systemProfileLoader.setActive(args[1], true);
			sender.sendMessage(ChatColor.GREEN + "Activated system profile successfully.");
			return true;
		}
		if (args[0].equalsIgnoreCase("-deactivate")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				User user = UserLoader.fromPlayer(player);
				if (!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.SYSOP, true))
					return true;
			}
			this.systemProfileLoader.setActive(args[1], false);
			sender.sendMessage(ChatColor.GREEN + "Deactivated system profile successfully.");
			return true;
		}
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command!");
			return true;
		}
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		if (args[0].equalsIgnoreCase("-level")) {
			if (user.getSystemProfile() == null) {
				sender.sendMessage(ChatColor.RED + "You're not logged in to a system profile.");
				return true;
			}
			PermissionLevel level = StringUtil.parseEnum(sender, PermissionLevel.class, args[1]);
			if(level == null) return true;
			boolean result = user.setActivePermissionLevel(level);
			if (result) {
				sender.sendMessage(ChatColor.GREEN + "Changed active permission level to " + args[1]);
			} else {
				sender.sendMessage(ChatColor.RED + "Could not change active permission level: requested permission level exceeds maximum level for this profile.");
			}
			return true;
		}
		if (args[0].equalsIgnoreCase("-password")) {
			if (user.getSystemProfile() == null) {
				sender.sendMessage(ChatColor.RED + "You're not logged in to a system profile.");
				return true;
			}
			if (!SystemProfileLoader.passwordHash(args[1]).equals(user.getSystemProfile().getPasswordHash(user.getUUID()))) {
				sender.sendMessage(ChatColor.RED + "Incorrect current password!");
				return true;
			}
			this.systemProfileLoader.setProfilePassword(user.getSystemProfile().getProfileName(), user.getUUID(), args[2]);
			this.systemProfileLoader.logoutProfile(user.getSystemProfile().getProfileName());
			user.setActivePermissionLevel(PermissionLevel.USER);
			sender.sendMessage(ChatColor.GREEN + "Password updated successfully. Please log back in to your system profile with your updated credentials.");
			return true;
		}
		if(args[0].equalsIgnoreCase("-alt")) {
			if (user.getSystemProfile() == null) {
				sender.sendMessage(ChatColor.RED + "You're not logged in to a system profile.");
				return true;
			}
			this.systemProfileLoader.registerAlt(user.getSystemProfile().getProfileName(), args[1], args[2]);
			sender.sendMessage(ChatColor.GREEN + "Registered alt account " + args[1] + " successfully.");
			return true;
		}
		if(args[0].equalsIgnoreCase("-rmalt")) {
			if (user.getSystemProfile() == null) {
				sender.sendMessage(ChatColor.RED + "You're not logged in to a system profile.");
				return true;
			}
			this.systemProfileLoader.unregisterAlt(user.getSystemProfile().getProfileName(), args[1]);
			sender.sendMessage(ChatColor.GREEN + "Unregistered alt account " + args[1] + " successfully.");
			return true;
		}
		if (args[0].equalsIgnoreCase("-logout")) {
			if (user.getSystemProfile() == null) {
				sender.sendMessage(ChatColor.RED + "You're not logged in to a system profile.");
				return true;
			}
			this.systemProfileLoader.logoutProfile(user.getSystemProfile().getProfileName());
			user.setActivePermissionLevel(PermissionLevel.USER);
			user.setSystemProfile(null);
			sender.sendMessage(ChatColor.GREEN + "Successfully logged out of your system profile.");
			return true;
		}
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "/syslogon <profile> <password>");
			return true;
		}
		long wait = this.rateLimiting.getOrDefault(user, Long.valueOf(0L)).longValue() + (1000 * this.rateLimitingCounter.getOrDefault(user, Integer.valueOf(0)));
		if (wait > System.currentTimeMillis()) {
			sender.sendMessage(ChatColor.RED + "Please wait " + this.rateLimitingCounter.get(user) + "s.");
			return true;
		}
		UUID cid = CORRELATION.registerNewCorrelationID();
		if (user.getSystemProfile() != null) {
			CORRELATION.log(cid, Level.INFO, "user is currently signed in. signing out first");
			this.systemProfileLoader.logoutProfile(user.getSystemProfile().getProfileName());
			user.setActivePermissionLevel(PermissionLevel.USER);
			sender.sendMessage(ChatColor.GREEN + "Signed out of current system profile");
		}
		SystemProfile profile = this.systemProfileLoader.authenticateProfile(user, args[0], args[1], cid);
		if (profile == null) {
			CORRELATION.log(cid, Level.INFO, "user notified of error");
			sender.sendMessage(ChatColor.RED + "Could not log in! Make sure you are authorized on this account and entered the correct password.");
			sender.sendMessage(ChatColor.RED + "If the issue persists, report the following error message: " + StringUtil.toHdFont("Correlation ID: " + cid));
			this.rateLimiting.put(user, Long.valueOf(System.currentTimeMillis()));
			this.rateLimitingCounter.put(user, Integer.valueOf(Math.max(this.rateLimitingCounter.getOrDefault(user, Integer.valueOf(0)) * 2, 1)));
			return true;
		}
		CORRELATION.log(cid, Level.INFO, "completing sign-on");
		user.setSystemProfile(profile);
		user.setActivePermissionLevel(profile.getMaxPermissionLevel());
		sender.sendMessage(ChatColor.GREEN + "Logged on to system console as " + profile.getProfileName() + ". Permission level: " + profile.getMaxPermissionLevel().toString());
		CORRELATION.log(cid, Level.INFO, "sign-on complete");
		return true;
	}
}
