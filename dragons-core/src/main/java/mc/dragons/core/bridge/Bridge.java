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
	public String getAPIVersion();
	public void sendActionBar(Player player, String message);
	public void sendTitle(Player player, ChatColor titleColor, String titleMessage, ChatColor subtitleColor, String subtitleMessage, int fadeIn, int stay, int fadeOut);
	public void respawnPlayer(Player player);
	public boolean hasAI(Entity entity);
	public void setEntityAI(Entity entity, boolean ai);
	public void setItemStackUnbreakable(ItemStack itemStack, boolean unbreakable);
	public double[] getAABB(Entity entity);
	public void setEntityInvulnerable(Entity entity, boolean invulnerable);
	public int getPing(Player player);
}
