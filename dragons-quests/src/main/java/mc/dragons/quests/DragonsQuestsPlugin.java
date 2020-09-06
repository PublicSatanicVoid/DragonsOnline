package mc.dragons.quests;

import org.bukkit.plugin.java.JavaPlugin;

public class DragonsQuestsPlugin extends JavaPlugin {
	
	public void onEnable() {
		getCommand("loadquest").setExecutor(new LoadQuestCommand());
	}
}
