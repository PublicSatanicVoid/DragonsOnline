package mc.dragons.npcs.commands;

import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;

public class TestCompanionCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.GM)) return true;
		Player player = player(sender);
		User user = user(sender);
		
		if(args.length == 0) {
			sender.sendMessage("/testcompanion remove");
			sender.sendMessage("/testcompanion create <class>");
			sender.sendMessage("/testcompanion tphere");
			sender.sendMessage("/testcompanion mine");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("create")) {
			NPC companion = GameObjectType.NPC.<NPC, NPCLoader>getLoader().registerNew(player.getWorld(), player.getLocation(), args[1]);
			companion.getStorageAccess().set("companionOwner", user.getUUID());
			user.getStorageAccess().set("companion", companion.getUUID());
			return true;
		}
		
		UUID companionUUID = user.getStorageAccess().getDocument().get("companion", UUID.class);
		if(companionUUID == null) {
			sender.sendMessage("You don't have a companion");
			return true;
		}
		
		NPC companion = GameObjectType.NPC.<NPC, NPCLoader>getLoader().loadObject(companionUUID);
		
		if(args[0].equalsIgnoreCase("mine")) {
			sender.sendMessage("companionUUID=" + companionUUID);
			sender.sendMessage("companion name=" + companion.getName());
			sender.sendMessage("companion location=" + StringUtil.locToString(companion.getEntity().getLocation()));
		}
		
		else if(args[0].equalsIgnoreCase("tphere")) {
			companion.getEntity().teleport(player);
		}
		
		else if(args[0].equalsIgnoreCase("remove")) {
			user.getStorageAccess().set("companion", null);
			companion.getEntity().remove();
		}
		
		else {
			sender.sendMessage("I dont understandddddd");
		}
		
		return true;
	}

}
