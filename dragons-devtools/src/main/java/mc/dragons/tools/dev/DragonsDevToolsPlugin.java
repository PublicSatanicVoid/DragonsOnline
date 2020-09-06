package mc.dragons.tools.dev;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;

public class DragonsDevToolsPlugin extends JavaPlugin implements CommandExecutor {
	
	public void onEnable() {

		Dragons dragons = Dragons.getInstance();
		
		getCommand("verifygameintegrity").setExecutor(new VerifyGameIntegrityCommand(dragons));
		getCommand("loglevel").setExecutor(new LogLevelCommand());
		getCommand("debug").setExecutor(new DebugCommand());
		getCommand("lag").setExecutor(new LagCommand());
		getCommand("reloadquests").setExecutor(new ReloadObjectsCommands());
		getCommand("serveroptions").setExecutor(new ServerOptionsCommand());
		getCommand("placeholder").setExecutor(new PlaceholderCommand());
		getCommand("addon").setExecutor(new AddonCommand(dragons));
		getCommand("ping").setExecutor(new PingCommand());
		getCommand("worldperformance").setExecutor(new WorldPerformanceCommand());
	
		CommandExecutor experimentalCommands = new ExperimentalCommands();
		getCommand("helditemdata").setExecutor(experimentalCommands);
		getCommand("rawtext").setExecutor(experimentalCommands);
		getCommand("whoami").setExecutor(experimentalCommands);
		getCommand("stresstest").setExecutor(experimentalCommands);
		getCommand("killmobs").setExecutor(experimentalCommands);
		getCommand("testmineregen").setExecutor(experimentalCommands);
		getCommand("testpermission").setExecutor(experimentalCommands);
		getCommand("testlocaluserstorage").setExecutor(experimentalCommands);
		getCommand("testgui").setExecutor(experimentalCommands);
		getCommand("testhdfont").setExecutor(experimentalCommands);
		getCommand("testtabname").setExecutor(experimentalCommands);
		getCommand("testpathfinding").setExecutor(experimentalCommands);
		getCommand("testphasing").setExecutor(experimentalCommands);
		getCommand("testarmorstandpose").setExecutor(experimentalCommands);
		getCommand("testleveling").setExecutor(experimentalCommands);
		getCommand("testlogging").setExecutor(experimentalCommands);
	}
}
