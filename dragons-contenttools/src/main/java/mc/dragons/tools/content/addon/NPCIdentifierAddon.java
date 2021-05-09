package mc.dragons.tools.content.addon;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import mc.dragons.core.addon.ItemAddon;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.StringUtil;

public class NPCIdentifierAddon extends ItemAddon {

	@Override
	public String getName() {
		return "X.NPCIdentifier";
	}
	
	@Override
	public void onRightClick(User user) {
		Player player = user.getPlayer();
		Entity target = null;
		Location eye = player.getEyeLocation();
		Vector lookDir = eye.getDirection();
		double minDist = Double.MAX_VALUE;
		for(Entity test : player.getNearbyEntities(10.0, 10.0, 10.0)) {
			if(test.equals(player)) continue;
			if(!(test instanceof LivingEntity)) continue;
			LivingEntity le = (LivingEntity) test;
			Vector to = le.getEyeLocation().subtract(eye).toVector().normalize();
			double dist = test.getLocation().distance(eye);
			if(to.dot(lookDir) > 0.99 && dist < minDist) {
				target = test;
				minDist = dist;
			}
		}
		if(target == null) {
			player.sendMessage(ChatColor.RED + "Right-click while looking at an entity to view information about it!");
			return;
		}
		NPC npc = NPCLoader.fromBukkit(target);
		if(npc == null) {
			player.sendMessage(ChatColor.RED + "This entity (" + StringUtil.entityToString(target) + ") is not a game object!");
			return;
		}
		player.sendMessage(ChatColor.GRAY + StringUtil.entityToString(target) + " - " + npc.getUUID());
		player.sendMessage(ChatColor.GRAY + "Class: " + ChatColor.RESET + npc.getNPCClass().getClassName());
		player.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.YELLOW + "[Manage Class] ", "/npc " + npc.getNPCClass().getClassName(), true, "Click to manage this NPC class"),
				StringUtil.clickableHoverableText(ChatColor.RED + "[Delete]", "/deletenpc " + target.getEntityId() + " " + npc.getUUID(), "Click to delete this instance of the NPC permanently"));
	}
	
	@Override
	public void onPrepareCombo(User user, String combo) {}

	@Override
	public void onCombo(User user, String combo) {}

}
