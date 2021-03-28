package mc.dragons.npcs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class IWannaCompanionCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.TESTER)) return true;
		Player player = player(sender);
		User user = user(sender);
			
		if(user.getStorageAccess().get("companion") != null) {
			sender.sendMessage("You already have a companion");
			return true;
		}
		
		NPC companion = GameObjectType.NPC.<NPC, NPCLoader>getLoader().registerNew(player.getWorld(), player.getLocation(), "Companion-Kitty");
		companion.getStorageAccess().set("companionOwner", user.getUUID());
		user.getStorageAccess().set("companion", companion.getUUID());
		
		sender.sendMessage("Spawned a companion (companion uuid: " + companion.getUUID() + ")");
		return true;
	}

}
