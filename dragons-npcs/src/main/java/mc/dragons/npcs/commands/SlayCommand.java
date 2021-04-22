package mc.dragons.npcs.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class SlayCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.GM)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Usage: //slay <radius>");
			return true;
		}
		
		Integer radius = parseInt(sender, args[0]);
		if(radius == null) return true;
		
		int slain = 0;
		for(Entity e : player(sender).getNearbyEntities(radius, radius, radius)) {
			NPC npc = NPCLoader.fromBukkit(e);
			if(npc == null) continue;
			if(npc.getNPCType().isPersistent() || npc.isImmortal()) continue;
			npc.getEntity().remove();
			slain++;
		}
		
		sender.sendMessage(ChatColor.GREEN + "Slain " + slain + " entities in radius " + radius);
		return true;
	}

}
