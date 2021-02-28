package mc.dragons.tools.content;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.tools.content.command.builder.ClearInventoryCommand;
import mc.dragons.tools.content.command.builder.GamemodeCommand;
import mc.dragons.tools.content.command.builder.SpeedCommand;
import mc.dragons.tools.content.command.builder.WarpCommands;
import mc.dragons.tools.content.command.gameobject.FloorCommand;
import mc.dragons.tools.content.command.gameobject.ItemCommand;
import mc.dragons.tools.content.command.gameobject.NPCCommand;
import mc.dragons.tools.content.command.gameobject.QuestCommand;
import mc.dragons.tools.content.command.gameobject.RegionCommand;
import mc.dragons.tools.content.command.statistics.ReloreCommand;
import mc.dragons.tools.content.command.statistics.RenameCommand;
import mc.dragons.tools.content.command.statistics.ResetProfileCommand;
import mc.dragons.tools.content.command.statistics.RestatCommand;
import mc.dragons.tools.content.command.statistics.UpdateStatsCommand;
import mc.dragons.tools.content.command.testing.GoToFloorCommand;
import mc.dragons.tools.content.command.testing.TestQuestCommand;
import mc.dragons.tools.content.event.PlayerChangedWorldListener;

public class DragonsContentToolsPlugin extends JavaPlugin implements CommandExecutor {
	
	public void onEnable() {
		Dragons dragons = Dragons.getInstance();
		
		getCommand("region").setExecutor(new RegionCommand(dragons));
		getCommand("npc").setExecutor(new NPCCommand(dragons));
		getCommand("item").setExecutor(new ItemCommand(dragons));
		getCommand("floor").setExecutor(new FloorCommand(dragons));
		getCommand("clear").setExecutor(new ClearInventoryCommand());
		getCommand("testquest").setExecutor(new TestQuestCommand(dragons));
		getCommand("quest").setExecutor(new QuestCommand(dragons));
		getCommand("rename").setExecutor(new RenameCommand());
		getCommand("relore").setExecutor(new ReloreCommand());
		getCommand("restat").setExecutor(new RestatCommand());
		getCommand("resetprofile").setExecutor(new ResetProfileCommand());
		
		CommandExecutor gamemodeCommandExecutor = new GamemodeCommand();
		getCommand("gamemode").setExecutor(gamemodeCommandExecutor);
		getCommand("gma").setExecutor(gamemodeCommandExecutor);
		getCommand("gmc").setExecutor(gamemodeCommandExecutor);
		getCommand("gms").setExecutor(gamemodeCommandExecutor);
		
		getCommand("gotofloor").setExecutor(new GoToFloorCommand(dragons));
		getCommand("updatestats").setExecutor(new UpdateStatsCommand());
		getCommand("speed").setExecutor(new SpeedCommand());
		
		WarpCommands warpCommandsExecutor = new WarpCommands(dragons);
		getCommand("delwarp").setExecutor(warpCommandsExecutor);
		getCommand("setwarp").setExecutor(warpCommandsExecutor);
		getCommand("warp").setExecutor(warpCommandsExecutor);
		getCommand("warps").setExecutor(warpCommandsExecutor);
		
		Bukkit.getPluginManager().registerEvents(new PlayerChangedWorldListener(), this);
	}
}
