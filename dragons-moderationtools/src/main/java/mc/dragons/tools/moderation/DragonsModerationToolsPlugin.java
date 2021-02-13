package mc.dragons.tools.moderation;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;

public class DragonsModerationToolsPlugin extends JavaPlugin implements CommandExecutor {
	
	public void onEnable() {
		Dragons dragons = Dragons.getInstance();
		
		getCommand("info").setExecutor(new InfoCommand(dragons));
		getCommand("godmode").setExecutor(new GodModeCommand());
		getCommand("escalate").setExecutor(new EscalateCommand());
		getCommand("fly").setExecutor(new FlyCommand());
		
		CommandExecutor punishCommandsExecutor = new PunishCommands();
		getCommand("ban").setExecutor(punishCommandsExecutor);
		getCommand("mute").setExecutor(punishCommandsExecutor);
		getCommand("kick").setExecutor(punishCommandsExecutor);
		getCommand("warn").setExecutor(punishCommandsExecutor);
		
		CommandExecutor unPunishCommandsExecutor = new UnPunishCommands();
		getCommand("unban").setExecutor(unPunishCommandsExecutor);
		getCommand("unmute").setExecutor(unPunishCommandsExecutor);
		
		getCommand("viewpunishments").setExecutor(new ViewPunishmentsCommand());
		getCommand("removepunishment").setExecutor(new RemovePunishmentCommand());
		
		CommandExecutor vanishCommandsExecutor = new VanishCommands();
		getCommand("vanish").setExecutor(vanishCommandsExecutor);
		getCommand("unvanish").setExecutor(vanishCommandsExecutor);
		
	}
}
