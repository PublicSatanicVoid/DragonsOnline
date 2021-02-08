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
		getCommand("mongo").setExecutor(new MongoCommand());
		
		CommandExecutor pluginManagementCommands = new PluginManagementCommands();
		getCommand("enableplugin").setExecutor(pluginManagementCommands);
		getCommand("disableplugin").setExecutor(pluginManagementCommands);
		getCommand("ilikevanilla").setExecutor(pluginManagementCommands);
		
		CommandExecutor objectCommands = new ObjectCommands();
		getCommand("invalidate").setExecutor(objectCommands);
		getCommand("invalidateall").setExecutor(objectCommands);
		getCommand("invalidatetype").setExecutor(objectCommands);
		getCommand("invalidateuser").setExecutor(objectCommands);
		getCommand("localize").setExecutor(objectCommands);
		getCommand("localizeall").setExecutor(objectCommands);
		getCommand("localizetype").setExecutor(objectCommands);
		getCommand("localizeuser").setExecutor(objectCommands);
		
		CommandExecutor terminateCommands = new TerminateCommands();
		getCommand("killtask").setExecutor(terminateCommands);
		getCommand("killtasks").setExecutor(terminateCommands);
		getCommand("killtasksfor").setExecutor(terminateCommands);
		getCommand("crashserver").setExecutor(terminateCommands);
		getCommand("panic").setExecutor(terminateCommands);
		
		CommandExecutor performanceCommands = new PerformanceCommands();
		getCommand("worldperformance").setExecutor(performanceCommands);
		getCommand("serverperformance").setExecutor(performanceCommands);
		getCommand("getprocessid").setExecutor(performanceCommands);
		getCommand("requestgc").setExecutor(performanceCommands);
		getCommand("generatedump").setExecutor(performanceCommands);
		
		CommandExecutor experimentalCommands = new ExperimentalCommands();
		getCommand("helditemdata").setExecutor(experimentalCommands);
		getCommand("rawtext").setExecutor(experimentalCommands);
		getCommand("whoami").setExecutor(experimentalCommands);
		getCommand("stresstest").setExecutor(experimentalCommands);
		getCommand("killmobs").setExecutor(experimentalCommands);
		getCommand("testexceptions").setExecutor(experimentalCommands);
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
		getCommand("testuuidlookup").setExecutor(experimentalCommands);
	}
}
