package mc.dragons.tools.dev;

import org.bukkit.command.CommandExecutor;

import mc.dragons.core.Dragons;
import mc.dragons.core.DragonsJavaPlugin;
import mc.dragons.tools.dev.debug.DebugCommands;
import mc.dragons.tools.dev.debug.StateCommands;
import mc.dragons.tools.dev.debug.StateLoader;
import mc.dragons.tools.dev.gameobject.AddonCommand;
import mc.dragons.tools.dev.gameobject.ObjectCommands;
import mc.dragons.tools.dev.gameobject.ReloadObjectsCommands;
import mc.dragons.tools.dev.management.PluginManagementCommands;
import mc.dragons.tools.dev.management.ServerOptionsCommands;
import mc.dragons.tools.dev.management.TerminateCommands;
import mc.dragons.tools.dev.management.VariableCommands;
import mc.dragons.tools.dev.monitor.CorrelationCommand;
import mc.dragons.tools.dev.monitor.LagCommand;
import mc.dragons.tools.dev.monitor.LogLevelCommand;
import mc.dragons.tools.dev.monitor.MongoCommand;
import mc.dragons.tools.dev.monitor.PerformanceCommands;
import mc.dragons.tools.dev.monitor.PingCommand;
import mc.dragons.tools.dev.monitor.VerifyGameIntegrityCommand;

public class DragonsDevTools extends DragonsJavaPlugin {
	private Dragons dragons;
	
	public void onEnable() {
		enableDebugLogging();
	
		dragons = getDragonsInstance();
		dragons.getLightweightLoaderRegistry().register(new StateLoader(dragons.getMongoConfig()));
		
		getCommand("verifygameintegrity").setExecutor(new VerifyGameIntegrityCommand());
		getCommand("loglevel").setExecutor(new LogLevelCommand());
		
		getCommand("lag").setExecutor(new LagCommand());
		getCommand("addon").setExecutor(new AddonCommand());
		getCommand("ping").setExecutor(new PingCommand());
		getCommand("mongo").setExecutor(new MongoCommand());
		getCommand("correlation").setExecutor(new CorrelationCommand());
		
		CommandExecutor reloadObjectsCommands = new ReloadObjectsCommands();
		getCommand("reloadquests").setExecutor(reloadObjectsCommands);
		getCommand("resyncuserdata").setExecutor(reloadObjectsCommands);
		
		CommandExecutor pluginManagementCommands = new PluginManagementCommands();
		getCommand("enableplugin").setExecutor(pluginManagementCommands);
		getCommand("disableplugin").setExecutor(pluginManagementCommands);
		getCommand("ilikevanilla").setExecutor(pluginManagementCommands);
		
		CommandExecutor serverOptionsCommands = new ServerOptionsCommands();
		getCommand("serveroptions").setExecutor(serverOptionsCommands);
		getCommand("getservername").setExecutor(serverOptionsCommands);
		getCommand("spoofserver").setExecutor(serverOptionsCommands);
		getCommand("ignoreremoterestarts").setExecutor(serverOptionsCommands);
		getCommand("getlogtoken").setExecutor(serverOptionsCommands);
		getCommand("vgir").setExecutor(serverOptionsCommands);
		getCommand("getnetworkstate").setExecutor(serverOptionsCommands);
		
		CommandExecutor objectCommands = new ObjectCommands();
		getCommand("autosave").setExecutor(objectCommands);
		getCommand("invalidate").setExecutor(objectCommands);
		getCommand("invalidateall").setExecutor(objectCommands);
		getCommand("invalidatetype").setExecutor(objectCommands);
		getCommand("invalidateuser").setExecutor(objectCommands);
		getCommand("localize").setExecutor(objectCommands);
		getCommand("localizeall").setExecutor(objectCommands);
		getCommand("localizetype").setExecutor(objectCommands);
		getCommand("localizeuser").setExecutor(objectCommands);
		
		CommandExecutor variableCommands = new VariableCommands();
		getCommand("getglobalaccessiontoken").setExecutor(variableCommands);
		getCommand("setglobalaccessiontoken").setExecutor(variableCommands);
		getCommand("getglobalvariables").setExecutor(variableCommands);
		
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
		getCommand("tickperformance").setExecutor(performanceCommands);
		getCommand("getstacktrace").setExecutor(performanceCommands);
		getCommand("getactivethreads").setExecutor(performanceCommands);
		getCommand("clearnetworkmessagecache").setExecutor(performanceCommands);
		getCommand("printnetworkmessages").setExecutor(performanceCommands);
		getCommand("manifest").setExecutor(performanceCommands);
		getCommand("getsystemproperties").setExecutor(performanceCommands);
		
		CommandExecutor stateCommands = new StateCommands();
		getCommand("getstate").setExecutor(stateCommands);
		getCommand("setstate").setExecutor(stateCommands);
		
		CommandExecutor debugCommands = new DebugCommands();
		getCommand("debug").setExecutor(debugCommands);
		getCommand("streamconsole").setExecutor(debugCommands);
		
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
		getCommand("testtpsrecord").setExecutor(experimentalCommands);
		getCommand("testpathfinding").setExecutor(experimentalCommands);
		getCommand("testphasing").setExecutor(experimentalCommands);
		getCommand("testarmorstandpose").setExecutor(experimentalCommands);
		getCommand("testleveling").setExecutor(experimentalCommands);
		getCommand("testlogging").setExecutor(experimentalCommands);
		getCommand("testuuidlookup").setExecutor(experimentalCommands);
		getCommand("testcorrelationlogging").setExecutor(experimentalCommands);
		getCommand("testbase64encoding").setExecutor(experimentalCommands);
		getCommand("testnetworkmessage").setExecutor(experimentalCommands);
		getCommand("testdocumentdelta").setExecutor(experimentalCommands);
		getCommand("testnewfonts").setExecutor(experimentalCommands);
		getCommand("testuserlookup").setExecutor(experimentalCommands);
		getCommand("writelog").setExecutor(experimentalCommands);
		getCommand("testheader").setExecutor(experimentalCommands);
		getCommand("testfooter").setExecutor(experimentalCommands);
	}
}
