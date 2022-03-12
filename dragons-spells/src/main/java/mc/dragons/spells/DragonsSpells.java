package mc.dragons.spells;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;

import mc.dragons.core.Dragons;
import mc.dragons.core.DragonsJavaPlugin;
import mc.dragons.core.addon.AddonRegistry;
import mc.dragons.spells.commands.BindSpellCommand;
import mc.dragons.spells.events.SpellListeners;
import mc.dragons.spells.spells.SpellRegistry;
import mc.dragons.spells.spells.TestSpell;

public class DragonsSpells extends DragonsJavaPlugin {
	private SpellRegistry spellRegistry;

	private SpellCastAddon spellCastAddon;
	private SpellScrollAddon spellScrollAddon;
	
	public void onLoad() {
		enableDebugLogging();
		
		Dragons dragons = getDragonsInstance();
		
		getLogger().info("Loading item addons...");
		spellRegistry = new SpellRegistry();
		
		AddonRegistry registry = dragons.getAddonRegistry();
		registry.register(spellCastAddon = new SpellCastAddon(this));
		registry.register(spellScrollAddon = new SpellScrollAddon());
		
		dragons.getUserHookRegistry().registerHook(new SpellUserHook(this));
		
		// Instantiate all spells here
		new TestSpell(this);
	}
	
	public void onEnable() {
		CommandExecutor bindSpellCommand = new BindSpellCommand(this);
		getCommand("bindspell").setExecutor(bindSpellCommand);
		getCommand("testspellbinding").setExecutor(bindSpellCommand);
		Bukkit.getPluginManager().registerEvents(new SpellListeners(this), this);
	}
	
	public SpellRegistry getSpellRegistry() {
		return spellRegistry;
	}
	
	public SpellCastAddon getSpellCastAddon() {
		return spellCastAddon;
	}
	
	public SpellScrollAddon getSpellScrollAddon() {
		return spellScrollAddon;
	}
}
