package mc.dragons.tools.moderation;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.tools.moderation.abilities.FlyCommand;
import mc.dragons.tools.moderation.abilities.GodModeCommand;
import mc.dragons.tools.moderation.abilities.VanishCommands;
import mc.dragons.tools.moderation.analysis.IPHistoryCommand;
import mc.dragons.tools.moderation.analysis.IPScanCommand;
import mc.dragons.tools.moderation.analysis.InfoCommand;
import mc.dragons.tools.moderation.analysis.LocateCommand;
import mc.dragons.tools.moderation.punishment.PunishCommands;
import mc.dragons.tools.moderation.punishment.PunishMessageHandler;
import mc.dragons.tools.moderation.punishment.RemovePunishmentCommand;
import mc.dragons.tools.moderation.punishment.UnPunishCommands;
import mc.dragons.tools.moderation.punishment.ViewPunishmentsCommand;
import mc.dragons.tools.moderation.report.ChatReportCommand;
import mc.dragons.tools.moderation.report.EscalateCommand;
import mc.dragons.tools.moderation.report.ReportAdminCommands;
import mc.dragons.tools.moderation.report.ReportCommand;
import mc.dragons.tools.moderation.report.ReportLoader;
import mc.dragons.tools.moderation.report.ReportsCommand;
import mc.dragons.tools.moderation.report.ViewReportCommand;

public class DragonsModerationToolsPlugin extends JavaPlugin implements CommandExecutor {
	
	private PunishMessageHandler punishMessageHandler;
	
	public void onEnable() {
		Dragons.getInstance().getLightweightLoaderRegistry().register(new ReportLoader(Dragons.getInstance().getMongoConfig()));
		Dragons.getInstance().getUserHookRegistry().registerHook(new ModUserHook());
		
		punishMessageHandler = new PunishMessageHandler();
		
		getCommand("info").setExecutor(new InfoCommand());
		getCommand("iphistory").setExecutor(new IPHistoryCommand());
		getCommand("ipscan").setExecutor(new IPScanCommand());
		getCommand("godmode").setExecutor(new GodModeCommand());
		getCommand("fly").setExecutor(new FlyCommand());
		getCommand("setverification").setExecutor(new SetVerificationCommand());
		getCommand("locate").setExecutor(new LocateCommand());
		
		getCommand("report").setExecutor(new ReportCommand());
		getCommand("escalate").setExecutor(new EscalateCommand());
		getCommand("chatreport").setExecutor(new ChatReportCommand());
		getCommand("reports").setExecutor(new ReportsCommand());
		getCommand("viewreport").setExecutor(new ViewReportCommand());
		
		CommandExecutor reportAdminCommands = new ReportAdminCommands();
		getCommand("toggleselfreport").setExecutor(reportAdminCommands);
		getCommand("deletereport").setExecutor(reportAdminCommands);
		
		CommandExecutor punishCommands = new PunishCommands();
		getCommand("ban").setExecutor(punishCommands);
		getCommand("mute").setExecutor(punishCommands);
		getCommand("kick").setExecutor(punishCommands);
		getCommand("warn").setExecutor(punishCommands);
		
		CommandExecutor unPunishCommands = new UnPunishCommands();
		getCommand("unban").setExecutor(unPunishCommands);
		getCommand("unmute").setExecutor(unPunishCommands);
		
		getCommand("viewpunishments").setExecutor(new ViewPunishmentsCommand());
		getCommand("removepunishment").setExecutor(new RemovePunishmentCommand());
		
		CommandExecutor vanishCommands = new VanishCommands();
		getCommand("vanish").setExecutor(vanishCommands);
		getCommand("unvanish").setExecutor(vanishCommands);	
	}
	
	public PunishMessageHandler getPunishMessageHandler() {
		return punishMessageHandler;
	}
}
