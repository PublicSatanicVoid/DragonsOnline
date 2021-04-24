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
	
	/**
	 * 
	 * @return The version string 
	 */
	String getAPIVersion();
	
	/**
	 * Sends an action bar to the specified player.
	 * 
	 * @param player
	 * @param message
	 */
	@Deprecated
	void sendActionBar(Player player, String message);
	
	/**
	 * Sends a title and subtitle to the specified player.
	 * 
	 * @param player
	 * @param titleColor
	 * @param titleMessage
	 * @param subtitleColor
	 * @param subtitleMessage
	 * @param fadeIn
	 * @param stay
	 * @param fadeOut
	 */
	@Deprecated
	void sendTitle(Player player, ChatColor titleColor, String titleMessage, ChatColor subtitleColor, String subtitleMessage, int fadeIn, int stay, int fadeOut);
	
	/**
	 * Instantly respawns the specified player, skipping the death screen.
	 * 
	 * @param player
	 */
	@Deprecated
	void respawnPlayer(Player player);
	
	/**
	 * 
	 * @param entity
	 * @return Whether the specified entity has AI or not.
	 */
	boolean hasAI(Entity entity);
	
	/**
	 * Set the specified entity's AI status.
	 * 
	 * <p>Setting AI to false freezes the entity in place,
	 * but does not make it invulnerable.
	 * 
	 * <p>It can still be moved through teleportation.
	 * 
	 * @param entity
	 * @param ai
	 */
	void setEntityAI(Entity entity, boolean ai);
	
	/**
	 * Set the specified item's unbreakability status. 
	 * 
	 * @param itemStack
	 * @param unbreakable
	 */
	@Deprecated
	void setItemStackUnbreakable(ItemStack itemStack, boolean unbreakable);
	
	/**
	 * 
	 * @param entity
	 * @return The entity's bounding box coordinates, 
	 * in the order minX, minY, minZ, maxX, maxY, maxZ
	 */
	@Deprecated
	double[] getAABB(Entity entity);
	
	/**
	 * Sets the specified entity's invulnerability status.
	 * @param entity
	 * @param invulnerable
	 */
	void setEntityInvulnerable(Entity entity, boolean invulnerable);
	
	/**
	 * 
	 * @param player
	 * @return The estimated ping of the specified player, in ms
	 */
	int getPing(Player player);
}
