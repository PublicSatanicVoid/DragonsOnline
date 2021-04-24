package mc.dragons.quests;

import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;

public class DragonsQuests extends JavaPlugin {
	private Dragons dragons;
	
	public void onEnable() {
		dragons = Dragons.getInstance();
		dragons.registerDragonsPlugin(this);
		
		getCommand("loadquest").setExecutor(new LoadQuestCommand());
	}
}
