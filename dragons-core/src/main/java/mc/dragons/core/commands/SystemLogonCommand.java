package mc.dragons.core.commands;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.storage.impl.SystemProfile;
import mc.dragons.core.storage.impl.loader.SystemProfileLoader;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class SystemLogonCommand implements CommandExecutor {
	private SystemProfileLoader systemProfileLoader;

	private Map<User, Long> rateLimiting;

	private Map<User, Integer> rateLimitingCounter;

	public SystemLogonCommand(Dragons instance) {
		this.systemProfileLoader = instance.getLightweightLoaderRegistry().getLoader(SystemProfileLoader.class);
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
				sender.sendMessage(ChatColor.YELLOW + "/syslogon -logout");
			}
			sender.sendMessage(ChatColor.YELLOW + "/syslogon -create <new profile> <password> <max. permission level>");
			sender.sendMessage(ChatColor.YELLOW + "/syslogon -update <profile> <new max. permission level>");
			sender.sendMessage(ChatColor.YELLOW + "/syslogon -flag <profile> <" + StringUtil.parseList((Object[]) SystemProfile.SystemProfileFlags.SystemProfileFlag.values(), "|") + "> <true|false>");
			sender.sendMessage(ChatColor.YELLOW + "/syslogon -info <profile>");
			sender.sendMessage(ChatColor.YELLOW + "/syslogon -[de]activate <profile>");
			sender.sendMessage(ChatColor.DARK_GRAY + "Note: Profiles and passwords cannot contain spaces.");
			return true;
		}
		if (args[0].equalsIgnoreCase("-create")) {
			if (sender instanceof Player) {
				Player player1 = (Player) sender;
				User user1 = UserLoader.fromPlayer(player1);
				if (!PermissionUtil.verifyActivePermissionLevel(user1, PermissionLevel.SYSOP, true))
					return true;
			}
			this.systemProfileLoader.createProfile(args[1], args[2], PermissionLevel.valueOf(args[3]));
			sender.sendMessage(ChatColor.GREEN + "Created system profile successfully.");
			return true;
		}
		if (args[0].equalsIgnoreCase("-update")) {
			if (sender instanceof Player) {
				Player player1 = (Player) sender;
				User user1 = UserLoader.fromPlayer(player1);
				if (!PermissionUtil.verifyActivePermissionLevel(user1, PermissionLevel.SYSOP, true))
					return true;
			}
			this.systemProfileLoader.setProfileMaxPermissionLevel(args[1], PermissionLevel.valueOf(args[2]));
			sender.sendMessage(ChatColor.GREEN + "Updated system profile successfully.");
			return true;
		}
		if (args[0].equalsIgnoreCase("-flag")) {
			if (sender instanceof Player) {
				Player player1 = (Player) sender;
				User user1 = UserLoader.fromPlayer(player1);
				if (!PermissionUtil.verifyActivePermissionLevel(user1, PermissionLevel.SYSOP, true))
					return true;
			}
			this.systemProfileLoader.setProfileFlag(args[1], args[2].toUpperCase(), Boolean.valueOf(args[3]).booleanValue());
			sender.sendMessage(ChatColor.GREEN + "Updated system profile successfully.");
			return true;
		}
		if (args[0].equalsIgnoreCase("-info")) {
			if (sender instanceof Player) {
				Player player1 = (Player) sender;
				User user1 = UserLoader.fromPlayer(player1);
				if (!PermissionUtil.verifyActivePermissionLevel(user1, PermissionLevel.SYSOP, true))
					return true;
			}
			SystemProfile systemProfile = this.systemProfileLoader.loadProfile(args[1]);
			sender.sendMessage(ChatColor.GOLD + "Viewing system profile " + systemProfile.getProfileName());
			sender.sendMessage(ChatColor.YELLOW + "Status: " + ChatColor.RESET + (systemProfile.isActive() ? "Active" : "Inactive"));
			sender.sendMessage(ChatColor.YELLOW + "Max. Permission Level: " + ChatColor.RESET + systemProfile.getMaxPermissionLevel());
			sender.sendMessage(ChatColor.YELLOW + "Flags: " + ChatColor.RESET + systemProfile.getFlags());
			sender.sendMessage(ChatColor.YELLOW + "Current User: " + ChatColor.RESET + ((systemProfile.getCurrentUser() == null) ? "(None)" : systemProfile.getCurrentUser().getName()));
			return true;
		}
		if (args[0].equalsIgnoreCase("-activate")) {
			if (sender instanceof Player) {
				Player player1 = (Player) sender;
				User user1 = UserLoader.fromPlayer(player1);
				if (!PermissionUtil.verifyActivePermissionLevel(user1, PermissionLevel.SYSOP, true))
					return true;
			}
			this.systemProfileLoader.setActive(args[1], true);
			sender.sendMessage(ChatColor.GREEN + "Activated system profile successfully.");
			return true;
		}
		if (args[0].equalsIgnoreCase("-deactivate")) {
			if (sender instanceof Player) {
				Player player1 = (Player) sender;
				User user1 = UserLoader.fromPlayer(player1);
				if (!PermissionUtil.verifyActivePermissionLevel(user1, PermissionLevel.SYSOP, true))
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
			boolean result = user.setActivePermissionLevel(PermissionLevel.valueOf(args[1]));
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
			if (!SystemProfileLoader.passwordHash(args[1]).equals(user.getSystemProfile().getPasswordHash())) {
				sender.sendMessage(ChatColor.RED + "Incorrect current password!");
				return true;
			}
			this.systemProfileLoader.setProfilePassword(user.getSystemProfile().getProfileName(), args[2]);
			this.systemProfileLoader.logoutProfile(user.getSystemProfile().getProfileName());
			user.setActivePermissionLevel(PermissionLevel.USER);
			sender.sendMessage(ChatColor.GREEN + "Password updated successfully. Please log back in to your system profile with your updated credentials.");
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
		long wait = this.rateLimiting.getOrDefault(user, Long.valueOf(0L)).longValue() + (1000 * this.rateLimitingCounter.getOrDefault(user, Integer.valueOf(0)));
		if (wait > System.currentTimeMillis()) {
			sender.sendMessage(ChatColor.RED + "Please wait " + this.rateLimitingCounter.get(user) + "s.");
			return true;
		}
		if (user.getSystemProfile() != null) {
			this.systemProfileLoader.logoutProfile(user.getSystemProfile().getProfileName());
			user.setActivePermissionLevel(PermissionLevel.USER);
			sender.sendMessage(ChatColor.GREEN + "Signed out of current system profile");
		}
		SystemProfile profile = this.systemProfileLoader.authenticateProfile(user, args[0], args[1]);
		if (profile == null) {
			sender.sendMessage(ChatColor.RED + "Invalid credentials provided!");
			this.rateLimiting.put(user, Long.valueOf(System.currentTimeMillis()));
			this.rateLimitingCounter.put(user, Integer.valueOf(Math.max(this.rateLimitingCounter.getOrDefault(user, Integer.valueOf(0)) * 2, 1)));
			return true;
		}
		user.setSystemProfile(profile);
		user.setActivePermissionLevel(profile.getMaxPermissionLevel());
		sender.sendMessage(ChatColor.GREEN + "Logged on to system console as " + profile.getProfileName() + ". Permission level: " + profile.getMaxPermissionLevel().toString());
		return true;
	}
}
