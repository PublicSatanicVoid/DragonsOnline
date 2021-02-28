package mc.dragons.npcs.commands;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class TestCompanionCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command!");
		}
		
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		
		if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, true)) return true;
		
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
			return true;
		}
		
		if(args[0].equalsIgnoreCase("tphere")) {
			companion.getEntity().teleport(player);
			return true;
		}
		
		if(args[0].equalsIgnoreCase("remove")) {
			user.getStorageAccess().set("companion", null);
			companion.getEntity().remove();
			return true;
		}
		
		sender.sendMessage("I dont understandddddd");
		
		return true;
	}

}
