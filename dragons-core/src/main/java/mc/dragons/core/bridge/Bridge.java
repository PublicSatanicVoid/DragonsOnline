package mc.dragons.core.bridge;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Abstraction layer for version-dependent OBC/NMS API calls.
 * 
 * @author Adam
 *
 */
public interface Bridge {
	String getAPIVersion();
	void sendActionBar(Player player, String message);
	void sendTitle(Player player, ChatColor titleColor, String titleMessage, ChatColor subtitleColor, String subtitleMessage, int fadeIn, int stay, int fadeOut);
	void respawnPlayer(Player player);
	boolean hasAI(Entity entity);
	void setEntityAI(Entity entity, boolean ai);
	void setItemStackUnbreakable(ItemStack itemStack, boolean unbreakable);
	double[] getAABB(Entity entity);
	void setEntityInvulnerable(Entity entity, boolean invulnerable);
	int getPing(Player player);
}
