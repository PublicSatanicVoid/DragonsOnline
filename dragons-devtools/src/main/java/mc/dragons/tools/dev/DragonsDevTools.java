package mc.dragons.tools.dev;

import org.bukkit.command.CommandExecutor;

import mc.dragons.core.DragonsJavaPlugin;
import mc.dragons.tools.dev.debug.DebugCommands;
import mc.dragons.tools.dev.gameobject.AddonCommand;
import mc.dragons.tools.dev.gameobject.ObjectCommands;
import mc.dragons.tools.dev.gameobject.ReloadObjectsCommands;
import mc.dragons.tools.dev.lab.LabCommand;
import mc.dragons.tools.dev.management.PluginManagementCommands;
import mc.dragons.tools.dev.management.ServerOptionsCommands;
import mc.dragons.tools.dev.management.SudoCommand;
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
	public void onEnable() {
		enableDebugLogging();
		
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
		getCommand("worldmanager").setExecutor(performanceCommands);
		getCommand("unloadchunks").setExecutor(performanceCommands);
		getCommand("reloadchunks").setExecutor(performanceCommands);
		getCommand("cleardrops").setExecutor(performanceCommands);
		getCommand("clearmobs").setExecutor(performanceCommands);
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
		
		CommandExecutor debugCommands = new DebugCommands();
		getCommand("debug").setExecutor(debugCommands);
		getCommand("streamconsole").setExecutor(debugCommands);
		
		getCommand("sudo").setExecutor(new SudoCommand());
		getCommand("lab").setExecutor(new LabCommand());
		
		CommandExecutor experimentalCommands = new ExperimentalCommands();
		String[] experimentalCommandsList = {
				"helditemdata", "rawtext", "whoami", "stresstest", "killmobs",
				"testexceptions", "testmineregen", "testpermission", "testlocaluserstorage",
				"testgui", "testhdfont", "testtabname", "testtpsrecord", "testpathfinding",
				"testphasing", "testarmorstandpose", "testleveling", "testlogging", "testuuidlookup",
				"testcorrelationlogging", "testbase64encoding", "testnetworkmessage",
				"testdocumentdelta", "testnewfonts", "testuserlookup", "writelog",
				"testheader", "testfooter", "testinvisibleslimes", "testrevealslimes",
				"testhideslimes", "testdestroyslimes", "testbadslimes", "mockuser", "mocksudo",
				"mockinject", "mockserver", "mockdelete", "mocklist", "mockuninject",
				"testitemstash", "testitemunstash", "testmobai", "testtakeitem", 
				"testupdateinventory", "testnametag", "testnametag2", "testupdatenametag",
				"testrollingasync", "testinternalnetworkedmsg", "testplayernpc",
				"testrevealallinvisible", "testinventoryreload", "getitemuuid", "tptoentity",
				"dumpteams", "getprotocolversion", "testtabsorting"
		};
		for(String cmd : experimentalCommandsList) {
			getCommand(cmd).setExecutor(experimentalCommands);
		}
	}
}
