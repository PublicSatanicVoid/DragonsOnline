package mc.dragons.spells;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.AddonRegistry;
import mc.dragons.spells.commands.BindSpellCommand;
import mc.dragons.spells.events.SpellListeners;

public class DragonsSpellAddons extends JavaPlugin {
	
	public void onLoad() {
		getLogger().info("Loading item addons...");
		AddonRegistry registry = Dragons.getInstance().getAddonRegistry();
		
		registry.register(new SpellCastAddon());
		registry.register(new SpellScrollAddon());
	}
	
	public void onEnable() {
		getCommand("bindspell").setExecutor(new BindSpellCommand());
	
		Bukkit.getPluginManager().registerEvents(new SpellListeners(), this);
	}
}
