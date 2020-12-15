package mc.dragons.npcs;

import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.AddonRegistry;
import mc.dragons.npcs.commands.CompanionCommand;
import mc.dragons.npcs.commands.IWannaCompanionCommand;
import mc.dragons.npcs.commands.SlayCommand;
import mc.dragons.npcs.commands.TestCompanionCommand;
import mc.dragons.npcs.model.BoneCrusherAddon;
import mc.dragons.npcs.model.PossessedWoodChipsAddon;
import mc.dragons.npcs.model.SoulStealerAddon;
import mc.dragons.npcs.model.UndeadMurdererAddon;
import mc.dragons.npcs.model.WalkingArmorStandAddon;

public class DragonsNPCAddons extends JavaPlugin {
	
	public void onLoad() {
		getLogger().info("Loading NPC addons...");
		AddonRegistry registry = Dragons.getInstance().getAddonRegistry();
		
		registry.register(new GuardAddon());
		registry.register(new EnchanterAddon());
		registry.register(new BoneCrusherAddon());
		registry.register(new UndeadMurdererAddon());
		registry.register(new SoulStealerAddon());
		registry.register(new AuraAddon());
		registry.register(new WalkingArmorStandAddon());
		registry.register(new PossessedWoodChipsAddon());
		registry.register(new CompanionAddon());
		
		Dragons.getInstance().getUserHookRegistry().registerHook(new NPCUserHook());
	}

	public void onEnable() {
		getCommand("companion").setExecutor(new CompanionCommand());
		getCommand("testcompanion").setExecutor(new TestCompanionCommand());
		getCommand("iwannacompanion").setExecutor(new IWannaCompanionCommand());
		getCommand("/slay").setExecutor(new SlayCommand());
	}
}
