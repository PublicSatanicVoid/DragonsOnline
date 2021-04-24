package mc.dragons.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.anticheat.command.AntiCheatCommand;
import mc.dragons.anticheat.event.MoveListener;
import mc.dragons.core.Dragons;

public class DragonsAntiCheat extends JavaPlugin {
	private boolean debug;
	
	private Dragons dragons;
	private MoveListener moveListener;
	
	@Override
	public void onEnable() {
		dragons = Dragons.getInstance();
		dragons.registerDragonsPlugin(this);
		
		CommandExecutor ac = new AntiCheatCommand(this);
		getCommand("ac").setExecutor(ac);
		getCommand("acdebug").setExecutor(ac);
		getCommand("acflushlog").setExecutor(ac);
		getCommand("acstartlog").setExecutor(ac);
		getCommand("acblockdata").setExecutor(ac);
		
		moveListener = new MoveListener(this);
		
		Bukkit.getServer().getPluginManager().registerEvents(moveListener, this);
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
	
	public MoveListener getMoveListener() {
		return moveListener;
	}
}