package mc.dragons.tools.dev.gameobject;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class ReloadObjectsCommands extends DragonsCommandExecutor  {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return true;
		
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
		else if(label.equalsIgnoreCase("resyncuserdata")) {
			sender.sendMessage(ChatColor.GREEN + "Resyncing user data...");
			int n = 0;
			for(GameObject obj : dragons.getGameObjectRegistry().getRegisteredObjects(GameObjectType.USER)) {
				obj.replaceStorageAccess(dragons.getPersistentStorageManager().getStorageAccess(GameObjectType.USER, obj.getUUID()));
				n++;
			}
			sender.sendMessage(ChatColor.GREEN + "Resynced " + n + " users");
			return true;
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
