package mc.dragons.npcs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;

public class IWannaCompanionCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		NPC companion = GameObjectType.NPC.<NPC, NPCLoader>getLoader().registerNew(player.getWorld(), player.getLocation(), "Companion-Kitty");
		companion.getStorageAccess().set("companionOwner", user.getUUID());
		user.getStorageAccess().set("companion", companion.getUUID());
		sender.sendMessage("There you go");
		return true;
	}

}
