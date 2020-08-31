package mc.dragons.tools.dev;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestLoader;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.region.RegionLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;

public class ReloadObjectsCommands implements CommandExecutor  {
	
	private QuestLoader questLoader;
	private NPCLoader npcLoader;
	private RegionLoader regionLoader;
	
	public ReloadObjectsCommands() {
		questLoader = GameObjectType.QUEST.<Quest, QuestLoader>getLoader();
		npcLoader = GameObjectType.NPC.<NPC, NPCLoader>getLoader();
		regionLoader = GameObjectType.REGION.<Region, RegionLoader>getLoader();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		Player player = null;
		User user = null;
		
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true)) return true;
		}
		
		if(label.equalsIgnoreCase("reloadquests")) {
			sender.sendMessage(ChatColor.GREEN + "Reloading quests...");
		}
		// NOTE: reloadnpcs and reloadregions are currently disabled because they DON'T WORK
		else if(label.equalsIgnoreCase("reloadnpcs")) {
			sender.sendMessage(ChatColor.GREEN + "Reloading NPCs...");
		}
		else if(label.equalsIgnoreCase("reloadregions")) {
			sender.sendMessage(ChatColor.GREEN + "Reloading regions...");
		}
		
		new BukkitRunnable() {
			@Override
			public void run() {
				if(label.equalsIgnoreCase("reloadquests")) {
					questLoader.loadAll(true);
					sender.sendMessage(ChatColor.GREEN + "All quests have been reloaded!");
				}
				// NOTE: reloadnpcs and reloadregions are currently disabled because they DON'T WORK
				else if(label.equalsIgnoreCase("reloadnpcs")) {
					npcLoader.loadAllPermanent(true);
					sender.sendMessage(ChatColor.GREEN + "All NPCs have been reloaded!");
				}
				else if(label.equalsIgnoreCase("reloadregions")) {
					regionLoader.loadAll(true);
					sender.sendMessage(ChatColor.GREEN + "All regions have been reloaded!");
				}
			}
		}.runTaskLater(Dragons.getInstance(), 1L);
		
		return true;
	}
}
