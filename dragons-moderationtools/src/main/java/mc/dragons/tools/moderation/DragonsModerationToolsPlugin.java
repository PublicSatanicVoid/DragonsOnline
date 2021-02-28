package mc.dragons.tools.moderation;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.tools.moderation.report.ChatReportCommand;
import mc.dragons.tools.moderation.report.EscalateCommand;
import mc.dragons.tools.moderation.report.ReportCommand;
import mc.dragons.tools.moderation.report.ReportLoader;
import mc.dragons.tools.moderation.report.ReportsCommand;
import mc.dragons.tools.moderation.report.ViewReportCommand;

public class DragonsModerationToolsPlugin extends JavaPlugin implements CommandExecutor {
	
	public void onEnable() {
		Dragons.getInstance().getLightweightLoaderRegistry().register(new ReportLoader(Dragons.getInstance().getMongoConfig()));
		
		getCommand("info").setExecutor(new InfoCommand());
		getCommand("godmode").setExecutor(new GodModeCommand());
		getCommand("fly").setExecutor(new FlyCommand());
		
		getCommand("report").setExecutor(new ReportCommand(Dragons.getInstance()));
		getCommand("escalate").setExecutor(new EscalateCommand(Dragons.getInstance()));
		getCommand("chatreport").setExecutor(new ChatReportCommand(Dragons.getInstance()));
		getCommand("reports").setExecutor(new ReportsCommand(Dragons.getInstance()));
		getCommand("viewreport").setExecutor(new ViewReportCommand(Dragons.getInstance()));
		
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
