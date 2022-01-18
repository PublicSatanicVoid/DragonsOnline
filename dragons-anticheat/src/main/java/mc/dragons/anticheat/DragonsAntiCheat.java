package mc.dragons.anticheat;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;

import mc.dragons.anticheat.check.CheckRegistry;
import mc.dragons.anticheat.check.move.NoClip;
import mc.dragons.anticheat.check.move.PacketSpoof;
import mc.dragons.anticheat.check.move.WrongMove;
import mc.dragons.anticheat.command.AntiCheatCommand;
import mc.dragons.anticheat.event.CheckListeners;
import mc.dragons.anticheat.event.TestingMoveListener;
import mc.dragons.core.DragonsJavaPlugin;

public class DragonsAntiCheat extends DragonsJavaPlugin {
	private boolean debug;
	private TestingMoveListener testingMoveListener;
	private CheckRegistry checkRegistry;
	
	@Override
	public void onEnable() {
		enableDebugLogging();
		
		CommandExecutor ac = new AntiCheatCommand(this);
		getCommand("ac").setExecutor(ac);
		getCommand("acdebug").setExecutor(ac);
		getCommand("acflushlog").setExecutor(ac);
		getCommand("acstartlog").setExecutor(ac);
		getCommand("acblockdata").setExecutor(ac);
		getCommand("acban").setExecutor(ac);
		getCommand("ackick").setExecutor(ac);
		getCommand("acdump").setExecutor(ac);
		getCommand("acresetplayer").setExecutor(ac);
		
		checkRegistry = new CheckRegistry();
		checkRegistry.registerCheck(new PacketSpoof());
		checkRegistry.registerCheck(new NoClip());
		checkRegistry.registerCheck(new WrongMove());
		
		PluginManager pluginManager = getServer().getPluginManager();
		pluginManager.registerEvents(testingMoveListener = new TestingMoveListener(this), this);
		pluginManager.registerEvents(new CheckListeners(this), this);
	}
	
	
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	public boolean isDebug() {
		return debug;
	}
	
	public void debug(CommandSender sender, String message) {
		if(debug) {
			sender.sendMessage(ChatColor.DARK_RED + "* " + ChatColor.RESET + message);
		}
	}
	
	public TestingMoveListener getTestingMoveListener() {
		return testingMoveListener;
	}
	
	public CheckRegistry getCheckRegistry() {
		return checkRegistry;
	}
}