package mc.dragons.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.anticheat.command.AntiCheatCommand;
import mc.dragons.anticheat.event.MoveListener;

public class DragonsAntiCheatPlugin extends JavaPlugin {
	private boolean debug;
	
	@Override
	public void onEnable() {
		CommandExecutor ac = new AntiCheatCommand(this);
		getCommand("ac").setExecutor(ac);
		getCommand("acdebug").setExecutor(ac);
		
		Bukkit.getServer().getPluginManager().registerEvents(new MoveListener(this), this);
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
	
}