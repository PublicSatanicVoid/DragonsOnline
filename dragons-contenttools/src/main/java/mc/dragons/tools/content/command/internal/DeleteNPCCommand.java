package mc.dragons.tools.content.command.internal;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;

public class DeleteNPCCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.GM_DELETE) || !requirePermission(sender, SystemProfileFlag.GM_NPC)) return true;
		
		if(args.length < 2) {
			sender.sendMessage(ERR_INTERNAL_ONLY);
			return true;
		}
		
		Integer id = parseInt(sender, args[0]);
		UUID uuid = parseUUID(sender, args[1]);
		if(id == null || uuid == null) {
			sender.sendMessage(ERR_INTERNAL_ONLY);
			return true;
		}
		
		for(Entity entity : dragons.getEntities()) {
			if(entity.getEntityId() == id) {
				NPC npc = NPCLoader.fromBukkit(entity);
				if(npc == null) {
					sender.sendMessage(ChatColor.RED + "Could not delete this entity: It does not correspond to a valid NPC!");
					return true;
				}
				npc.remove();
				sender.sendMessage(ChatColor.GREEN + "Removed NPC successfully.");
				return true;
			}
		}
		
		sender.sendMessage(ChatColor.RED + "Could not delete this entity: Invalid parameters!");
		
		
		return true;
	}

}
