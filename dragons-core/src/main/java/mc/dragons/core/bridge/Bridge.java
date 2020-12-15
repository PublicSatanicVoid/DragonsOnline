package mc.dragons.core.bridge;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface Bridge {
	public String getAPIVersion();
	public void sendActionBar(Player paramPlayer, String paramString);
	public void sendTitle(Player paramPlayer, ChatColor paramChatColor1, String paramString1, ChatColor paramChatColor2, String paramString2, int paramInt1, int paramInt2, int paramInt3);
	public void respawnPlayer(Player paramPlayer);
	public boolean hasAI(Entity entity);
	public void setEntityAI(Entity paramEntity, boolean paramBoolean);
	public void setItemStackUnbreakable(ItemStack paramItemStack, boolean paramBoolean);
	public double[] getAABB(Entity paramEntity);
	public void setEntityInvulnerable(Entity paramEntity, boolean paramBoolean);
	public int getPing(Player paramPlayer);
}
