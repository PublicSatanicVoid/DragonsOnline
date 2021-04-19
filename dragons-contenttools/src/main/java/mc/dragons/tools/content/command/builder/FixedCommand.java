package mc.dragons.tools.content.command.builder;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class FixedCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !hasPermission(sender, PermissionLevel.BUILDER)) return true;
		
		Player player = player(sender);
		
		List<Entity> possibilities = player.getNearbyEntities(10.0, 10.0, 10.0);
		Entity target = null;
		Location eye = player.getEyeLocation();
		for(Entity test : possibilities) {
			if(test.equals(player)) continue;
			Vector to = test.getLocation().toVector().subtract(eye.toVector());
			if(to.normalize().dot(eye.getDirection()) > 0.99D) {
				target = test;
				break;
			}
		}
		if(target == null) {
			sender.sendMessage(ChatColor.RED + "Please look at the base of the entity you want to fix in place!");
			return true;
		}

		if(NPCLoader.fromBukkit(target) != null) {
			sender.sendMessage(ChatColor.RED + "This entity is a registered NPC and cannot be modified through this command!");
			return true;
		}
		
		PersistentDataContainer pdc = target.getPersistentDataContainer();
		
		if(pdc.has(Dragons.FIXED_ENTITY_KEY, PersistentDataType.INTEGER)) {
			pdc.remove(Dragons.FIXED_ENTITY_KEY);
			Dragons.getInstance().getBridge().setEntityAI(target, true);
			Dragons.getInstance().getBridge().setEntityInvulnerable(target, false);
			sender.sendMessage(ChatColor.GREEN + "Target entity (#" + target.getEntityId() + ") is " + ChatColor.ITALIC + "no longer fixed");
		}
		else {
			pdc.set(Dragons.FIXED_ENTITY_KEY, PersistentDataType.INTEGER, 1);
			Dragons.getInstance().getBridge().setEntityAI(target, false);
			Dragons.getInstance().getBridge().setEntityInvulnerable(target, true);
			sender.sendMessage(ChatColor.GREEN + "Target entity (#" + target.getEntityId() + ") is " + ChatColor.ITALIC + "now fixed");
		}
		
		return true;
	}

}
