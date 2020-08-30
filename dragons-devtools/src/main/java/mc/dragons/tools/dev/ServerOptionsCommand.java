package mc.dragons.tools.dev;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.ServerOptions;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.PermissionUtil;

public class ServerOptionsCommand implements CommandExecutor {	
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	
		User user = null;
		if(sender instanceof Player) {
			user = UserLoader.fromPlayer((Player) sender);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true)) return true;
		}
		
		ServerOptions options = Dragons.getInstance().getServerOptions();
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions deathcountdown [Seconds]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions autosave [On|Off]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions autosaveperiod [Seconds]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions customspawning [On|Off]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions customspawnrate [Seconds]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions gameverification [On|Off]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions gameverificationsweeprate [Seconds]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions defaultwalkspeed [Value]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions loglevel [LogLevel]");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("deathcountdown")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "Death countdown is currently " + options.getDeathCountdown() + "s");
			}
			else {
				options.setDeathCountdown(Integer.valueOf(args[1]));
				sender.sendMessage(ChatColor.GREEN + "Set death countdown to " + args[1] + "s");
			}
			return true;
		}
		
		if(args[0].equalsIgnoreCase("autosave")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "GameObject auto-saving is currently " + (options.isAutoSaveEnabled() ? "ON" : "OFF"));
			}
			else {
				options.setAutoSaveEnabled(args[1].equalsIgnoreCase("ON"));
				sender.sendMessage(ChatColor.GREEN + "GameObject auto-saving is now " + (options.isAutoSaveEnabled() ? "ON" : "OFF"));
			}
			return true;
		}
		
		if(args[0].equalsIgnoreCase("autosaveperiod")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "GameObject auto-save period is currently " + options.getAutoSavePeriodTicks() / 20 + "s");
			}
			else {
				options.setAutoSavePeriodTicks((int) (double) Double.valueOf(args[1]) * 20);
				sender.sendMessage(ChatColor.GREEN + "GameObject auto-save period is now " + args[1] + "s");
			}
			return true;
		}
		
		if(args[0].equalsIgnoreCase("customspawning")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "GameObject custom spawning is currently " + (options.isCustomSpawningEnabled() ? "ON" : "OFF"));
			}
			else {
				options.setCustomSpawningEnabled(args[1].equalsIgnoreCase("ON"));
				sender.sendMessage(ChatColor.GREEN + "GameObject custom spawning is now " + (options.isCustomSpawningEnabled() ? "ON" : "OFF"));
			}
			return true;
		}
		
		if(args[0].equalsIgnoreCase("customspawnrate")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "GameObject custom spawn rate is currently " + options.getCustomSpawnRate() / 20 + "s");
			}
			else {
				options.setCustomSpawnRate((int) (double) Double.valueOf(args[1]) * 20);
				sender.sendMessage(ChatColor.GREEN + "GameObject custom spawn rate is now " + args[1] + "s");
			}
			return true;
		}
		
		if(args[0].equalsIgnoreCase("gameverification")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "Game environment verification is currently " + (options.isVerifyIntegrityEnabled() ? "ON" : "OFF"));
			}
			else {
				options.setVerifyIntegrityEnabled(args[1].equalsIgnoreCase("ON"));
				sender.sendMessage(ChatColor.GREEN + "Game environment verification is now " + (options.isVerifyIntegrityEnabled() ? "ON" : "OFF"));
			}
			return true;
		}
		
		if(args[0].equalsIgnoreCase("gameverificationsweeprate")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "Game environment verification sweep rate is currently " + options.getVerifyIntegritySweepRate() / 20 + "s");
			}
			else {
				options.setVerifyIntegritySweepRate((int) (double) Double.valueOf(args[1]) * 20);
				sender.sendMessage(ChatColor.GREEN + "Game environment verification sweep rate is now " + args[1] + "s");
			}
			return true;
		}
		
		if(args[0].equalsIgnoreCase("defaultwalkspeed")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "Default walk speed is currently " + options.getDefaultWalkSpeed());
			}
			else {
				options.setDefaultWalkSpeed(Double.valueOf(args[1]));
				sender.sendMessage(ChatColor.GREEN + "Default walk speed is now " + args[1]);
			}
			return true;
		}
		
		if(args[0].equalsIgnoreCase("loglevel")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "Log level is currently " + options.getLogLevel());
			}
			else {
				options.setLogLevel(Level.parse(args[1]));
				sender.sendMessage(ChatColor.GREEN + "Log level is now " + options.getLogLevel());
			}
			return true;
		}

		sender.sendMessage(ChatColor.RED + "Invalid option! /serveroptions");
		return true;
	}

}
