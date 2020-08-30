package mc.dragons.npcs.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.loader.NPCLoader;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.PermissionUtil;

public class SlayCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command!");
			return true;
		}
		
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		
		if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, true)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Usage: //slay <radius>");
			return true;
		}
		
		int radius = Integer.valueOf(args[0]);
		
		int slain = 0;
		for(Entity e : player.getNearbyEntities(radius, radius, radius)) {
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
