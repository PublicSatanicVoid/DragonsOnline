package mc.dragons.quests;

import mc.dragons.core.DragonsJavaPlugin;

public class DragonsQuests extends DragonsJavaPlugin {
	
	public void onEnable() {
		enableDebugLogging();
		
		getCommand("loadquest").setExecutor(new LoadQuestCommand());
	}
}
