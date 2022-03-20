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
import mc.dragons.core.util.CompanionUtil;

public class IWannaCompanionCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.TESTER)) return true;
		Player player = player(sender);
		User user = user(sender);
			
		if(CompanionUtil.hasCompanion(user)) {
			sender.sendMessage("You already have a companion");
			return true;
		}
		
		NPC companion = GameObjectType.getLoader(NPCLoader.class).registerNew(player.getLocation(), "Companion-Kitty");
		CompanionUtil.addCompanion(user, companion);
		
		sender.sendMessage("Spawned a companion (companion uuid: " + companion.getUUID() + ")");
		return true;
	}

}
