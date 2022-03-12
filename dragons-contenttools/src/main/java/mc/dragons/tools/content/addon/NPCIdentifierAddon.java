package mc.dragons.tools.content.addon;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import mc.dragons.core.addon.ItemAddon;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.EntityUtil;
import mc.dragons.core.util.StringUtil;

public class NPCIdentifierAddon extends ItemAddon {

	@Override
	public String getName() {
		return "X.NPCIdentifier";
	}
	
	@Override
	public void onRightClick(User user) {
		Player player = user.getPlayer();
		Entity target = EntityUtil.getTarget(player);
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
	public void onPrepareCombo(User user, String combo) { /* do nothing */ }

	@Override
	public void onCombo(User user, String combo) { /* do nothing */ }

	@Override
	public void initialize(User user, Item item) { /* do nothing */ }

}
