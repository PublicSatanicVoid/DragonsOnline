package mc.dragons.tools.moderation;

import org.bukkit.command.CommandExecutor;

import mc.dragons.core.Dragons;
import mc.dragons.core.DragonsJavaPlugin;
import mc.dragons.tools.moderation.abilities.FlyCommand;
import mc.dragons.tools.moderation.abilities.GodModeCommand;
import mc.dragons.tools.moderation.abilities.UnVanishCommand;
import mc.dragons.tools.moderation.abilities.VanishCommand;
import mc.dragons.tools.moderation.analysis.IPHistoryCommand;
import mc.dragons.tools.moderation.analysis.IPScanCommand;
import mc.dragons.tools.moderation.analysis.InfoCommand;
import mc.dragons.tools.moderation.analysis.LocateCommand;
import mc.dragons.tools.moderation.analysis.RecentLoginsCommand;
import mc.dragons.tools.moderation.hold.HoldCommand;
import mc.dragons.tools.moderation.hold.HoldLoader;
import mc.dragons.tools.moderation.hold.HoldMessageHandler;
import mc.dragons.tools.moderation.hold.ReleaseHoldCommand;
import mc.dragons.tools.moderation.hold.ViewHoldsCommand;
import mc.dragons.tools.moderation.punishment.PunishMessageHandler;
import mc.dragons.tools.moderation.punishment.command.AcknowledgeWarningCommand;
import mc.dragons.tools.moderation.punishment.command.NSPunishCommands;
import mc.dragons.tools.moderation.punishment.command.PunishCodesCommand;
import mc.dragons.tools.moderation.punishment.command.PunishCommand;
import mc.dragons.tools.moderation.punishment.command.PurgePunishmentsCommand;
import mc.dragons.tools.moderation.punishment.command.RemovePunishmentCommand;
import mc.dragons.tools.moderation.punishment.command.StandingLevelCommand;
import mc.dragons.tools.moderation.punishment.command.UnPunishCommands;
import mc.dragons.tools.moderation.punishment.command.ViewPunishmentsCommand;
import mc.dragons.tools.moderation.report.ChatReportCommand;
import mc.dragons.tools.moderation.report.EscalateCommand;
import mc.dragons.tools.moderation.report.ModQueueCommand;
import mc.dragons.tools.moderation.report.ReportAdminCommands;
import mc.dragons.tools.moderation.report.ReportCommand;
import mc.dragons.tools.moderation.report.ReportLoader;
import mc.dragons.tools.moderation.report.ReportsCommand;
import mc.dragons.tools.moderation.report.ViewReportCommand;
import mc.dragons.tools.moderation.report.WatchlistCommand;

public class DragonsModerationTools extends DragonsJavaPlugin {
	private Dragons dragons;
	private PunishMessageHandler punishMessageHandler;
	private HoldMessageHandler holdMessageHandler;
	private PunishCommand punishCommand;
	
	public void onEnable() {
		enableDebugLogging();
		
		dragons = getDragonsInstance();
		
		holdMessageHandler = new HoldMessageHandler(dragons);
		dragons.getLightweightLoaderRegistry().register(new ReportLoader(dragons.getMongoConfig()));
		dragons.getLightweightLoaderRegistry().register(new HoldLoader(this));
		dragons.getUserHookRegistry().registerHook(new ModUserHook(dragons));
		punishMessageHandler = new PunishMessageHandler();
		
		new PeriodicNotificationTask(dragons).runTaskTimer(this, 20L, 20L * 60 * 10);
		
		getCommand("info").setExecutor(new InfoCommand());
		getCommand("iphistory").setExecutor(new IPHistoryCommand());
		getCommand("ipscan").setExecutor(new IPScanCommand());
		getCommand("godmode").setExecutor(new GodModeCommand());
		getCommand("fly").setExecutor(new FlyCommand());
		getCommand("setverification").setExecutor(new SetVerificationCommand());
		getCommand("locate").setExecutor(new LocateCommand());
		getCommand("modnotifications").setExecutor(new ModNotificationsCommand());
		getCommand("staffalert").setExecutor(new StaffAlertCommand());
		getCommand("recentlogins").setExecutor(new RecentLoginsCommand());
		
		getCommand("report").setExecutor(new ReportCommand());
		getCommand("escalate").setExecutor(new EscalateCommand());
		getCommand("chatreport").setExecutor(new ChatReportCommand());
		getCommand("reports").setExecutor(new ReportsCommand());
		getCommand("watchlist").setExecutor(new WatchlistCommand());
		
		getCommand("hold").setExecutor(new HoldCommand());
		getCommand("viewholds").setExecutor(new ViewHoldsCommand());
		getCommand("releasehold").setExecutor(new ReleaseHoldCommand());
		
		getCommand("viewreport").setExecutor(new ViewReportCommand());
		getCommand("modqueue").setExecutor(new ModQueueCommand());
		
		CommandExecutor reportAdminCommands = new ReportAdminCommands();
		getCommand("toggleselfreport").setExecutor(reportAdminCommands);
		getCommand("deletereport").setExecutor(reportAdminCommands);
		getCommand("bulkdeletereports").setExecutor(reportAdminCommands);
		
		punishCommand = new PunishCommand(this);
		getCommand("punish").setExecutor(punishCommand);
		getCommand("punishcodes").setExecutor(new PunishCodesCommand());
		getCommand("setstandinglevel").setExecutor(new StandingLevelCommand());
		getCommand("purgepunishments").setExecutor(new PurgePunishmentsCommand());
		
		CommandExecutor nsPunishCommands = new NSPunishCommands(this);
		getCommand("safekick").setExecutor(nsPunishCommands);
		getCommand("kick").setExecutor(nsPunishCommands);
		getCommand("warn").setExecutor(nsPunishCommands);
		getCommand("mute").setExecutor(nsPunishCommands);
		getCommand("ban").setExecutor(nsPunishCommands);
		
		CommandExecutor unPunishCommands = new UnPunishCommands();
		getCommand("unban").setExecutor(unPunishCommands);
		getCommand("unmute").setExecutor(unPunishCommands);
		
		getCommand("viewpunishments").setExecutor(new ViewPunishmentsCommand());
		getCommand("removepunishment").setExecutor(new RemovePunishmentCommand());
		getCommand("acknowledgewarning").setExecutor(new AcknowledgeWarningCommand());
		
		getCommand("vanish").setExecutor(new VanishCommand());
		getCommand("unvanish").setExecutor(new UnVanishCommand());	
	}
	
	public PunishMessageHandler getPunishMessageHandler() {
		return punishMessageHandler;
	}

	public HoldMessageHandler getHoldMessageHandler() {
		return holdMessageHandler;
	}
	
	public PunishCommand getPunishCommand() {
		return punishCommand;
	}
}
