package mc.dragons.tools.dev.management;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.ServerOptions;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.logging.LogLevel;
import mc.dragons.core.networking.RemoteAdminMessageHandler;
import mc.dragons.core.storage.mongo.MongoConfig;
import mc.dragons.core.util.StringUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class ServerOptionsCommands extends DragonsCommandExecutor {	
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return true;
		
		if(label.equalsIgnoreCase("getservername")) {
			sender.sendMessage(ChatColor.GREEN + "Server Name: " + ChatColor.GRAY + dragons.getServerName());
		}
		
		else if(label.equalsIgnoreCase("spoofserver")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/spoofserver <servername>");
				return true;
			}
			
			user(sender).getStorageAccess().set("currentServer", args[0]);
			sender.sendMessage(ChatColor.GREEN + "Spoofed your connected server.");
		}
		
		else if(label.equalsIgnoreCase("ignoreremoterestarts")) {
			RemoteAdminMessageHandler handler = dragons.getRemoteAdminHandler();
			handler.setIgnoresRemoteRestarts(!handler.ignoresRemoteRestarts());
			sender.sendMessage(ChatColor.GREEN + "Server will " + (handler.ignoresRemoteRestarts() ? "now" : "no longer") 
					+ " ignore remote restart commands.");
		}
		
		else if(label.equalsIgnoreCase("getlogtoken")) {
			UUID logToken = dragons.getCustomLoggingProvider().getCustomLogFilter().getLogEntryUUID();
			TextComponent token = new TextComponent(ChatColor.GREEN + "Log Token: " + ChatColor.GRAY + logToken);
			token.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click for copy-able text")));
			token.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, logToken.toString()));
			sender.spigot().sendMessage(token);
		}
		
		else if(label.equalsIgnoreCase("vgir") || label.equalsIgnoreCase("vrgit")) {
			Bukkit.dispatchCommand(sender, "verifygameintegrity -resolve");
		}
		
		else if(label.equalsIgnoreCase("getnetworkstate")) {
			MongoConfig mongoConfig = dragons.getMongoConfig();
			sender.sendMessage(ChatColor.GREEN + "Server: " + ChatColor.GRAY + dragons.getServerName() + " - " + (Bukkit.getIp().equals("") ? "localhost" : Bukkit.getIp()) + ":" + Bukkit.getPort());
			sender.sendMessage(ChatColor.GREEN + "Database: " + ChatColor.GRAY + mongoConfig.getHost() + ":" + mongoConfig.getPort() + " (" + mongoConfig.getUser() + ")");
			if(!Bukkit.getOnlineMode()) {
				sender.sendMessage(ChatColor.RED + "This server is in offline mode! This should not happen in production.");
			}
		}
		
		else if(label.equalsIgnoreCase("serveroptions")) {
			serverOptionsCommand(sender, args);
		}
		
		return true;
	}
	
	private void serverOptionsCommand(CommandSender sender, String[] args) {
		ServerOptions options = dragons.getServerOptions();
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions deathcountdown [Seconds]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions autosave [On|Off]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions autosaveperiod [Seconds]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions customspawning [On|Off]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions customspawnrate [Seconds]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions customspawnmargin [Margin]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions dropchancemultiplier [Multiplier]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions gameverification [On|Off]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions gameverificationsweeprate [Seconds]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions defaultwalkspeed [Value]");
			sender.sendMessage(ChatColor.YELLOW + "/serveroptions loglevel [LogLevel]");
		}
		
		else if(args[0].equalsIgnoreCase("deathcountdown")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "Death countdown is currently " + options.getDeathCountdown() + "s");
			}
			else {
				options.setDeathCountdown(Integer.valueOf(args[1]));
				sender.sendMessage(ChatColor.GREEN + "Set death countdown to " + args[1] + "s");
			}
		}
		
		else if(args[0].equalsIgnoreCase("autosave")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "GameObject auto-saving is currently " + (options.isAutoSaveEnabled() ? "ON" : "OFF"));
			}
			else {
				options.setAutoSaveEnabled(args[1].equalsIgnoreCase("ON"));
				sender.sendMessage(ChatColor.GREEN + "GameObject auto-saving is now " + (options.isAutoSaveEnabled() ? "ON" : "OFF"));
			}
		}
		
		else if(args[0].equalsIgnoreCase("autosaveperiod")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "GameObject auto-save period is currently " + options.getAutoSavePeriodTicks() / 20 + "s");
			}
			else {
				options.setAutoSavePeriodTicks((int) (double) Double.valueOf(args[1]) * 20);
				sender.sendMessage(ChatColor.GREEN + "GameObject auto-save period is now " + args[1] + "s");
			}
		}
		
		else if(args[0].equalsIgnoreCase("customspawning")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "GameObject custom spawning is currently " + (options.isCustomSpawningEnabled() ? "ON" : "OFF"));
			}
			else {
				options.setCustomSpawningEnabled(args[1].equalsIgnoreCase("ON"));
				sender.sendMessage(ChatColor.GREEN + "GameObject custom spawning is now " + (options.isCustomSpawningEnabled() ? "ON" : "OFF"));
			}
		}
		
		else if(args[0].equalsIgnoreCase("customspawnrate")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "GameObject custom spawn rate is currently " + options.getCustomSpawnRate() / 20 + "s");
			}
			else {
				options.setCustomSpawnRate((int) (double) Double.valueOf(args[1]) * 20);
				sender.sendMessage(ChatColor.GREEN + "GameObject custom spawn rate is now " + args[1] + "s");
			}
		}
		
		else if(args[0].equalsIgnoreCase("customspawnmargin")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "GameObject custom spawn margin is currently " + options.getCustomSpawnMargin() + "m");
			}
			else {
				options.setCustomSpawnMargin(Integer.valueOf(args[1]));
				sender.sendMessage(ChatColor.GREEN + "GameObject custom spawn margin is now " + args[1] + "m");
			}
		}
		
		else if(args[0].equalsIgnoreCase("dropchancemultiplier")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "Drop chance multiplier is currently " + options.getDropChanceMultiplier());
			}
			else {
				options.setDropChanceMultiplier(Double.valueOf(args[1]));
				sender.sendMessage(ChatColor.GREEN + "Drop chance multiplier is now " + args[1] + "x");
			}
		}
		
		else if(args[0].equalsIgnoreCase("gameverification")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "Game environment verification is currently " + (options.isVerifyIntegrityEnabled() ? "ON" : "OFF"));
			}
			else {
				options.setVerifyIntegrityEnabled(args[1].equalsIgnoreCase("ON"));
				sender.sendMessage(ChatColor.GREEN + "Game environment verification is now " + (options.isVerifyIntegrityEnabled() ? "ON" : "OFF"));
			}
		}
		
		else if(args[0].equalsIgnoreCase("gameverificationsweeprate")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "Game environment verification sweep rate is currently " + options.getVerifyIntegritySweepRate() / 20 + "s");
			}
			else {
				options.setVerifyIntegritySweepRate((int) (double) Double.valueOf(args[1]) * 20);
				sender.sendMessage(ChatColor.GREEN + "Game environment verification sweep rate is now " + args[1] + "s");
			}
		}
		
		else if(args[0].equalsIgnoreCase("defaultwalkspeed")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "Default walk speed is currently " + options.getDefaultWalkSpeed());
			}
			else {
				options.setDefaultWalkSpeed(Double.valueOf(args[1]));
				sender.sendMessage(ChatColor.GREEN + "Default walk speed is now " + args[1]);
			}
		}
		
		else if(args[0].equalsIgnoreCase("loglevel")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "Log level is currently " + options.getLogLevel());
			}
			else {
				Level level = this.lookup(sender, () -> Level.parse(args[0].toUpperCase()), ChatColor.RED + "Invalid log level! /loglevel <" + StringUtil.parseList(LogLevel.getApprovedLevels(), "|") + ">");
				if(level == null) return;
				options.setLogLevel(level);
				sender.sendMessage(ChatColor.GREEN + "Log level is now " + level);
			}
		}
		
		else {
			sender.sendMessage(ChatColor.RED + "Invalid option! /serveroptions");
		}
		
	}

}
