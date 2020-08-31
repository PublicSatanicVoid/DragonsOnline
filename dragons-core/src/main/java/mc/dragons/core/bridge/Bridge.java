package mc.dragons.core.bridge;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface Bridge {
	String getAPIVersion();
	void sendActionBar(Player paramPlayer, String paramString);
	void sendTitle(Player paramPlayer, ChatColor paramChatColor1, String paramString1, ChatColor paramChatColor2, String paramString2, int paramInt1, int paramInt2, int paramInt3);
	void respawnPlayer(Player paramPlayer);
	void setEntityAI(Entity paramEntity, boolean paramBoolean);
	void setItemStackUnbreakable(ItemStack paramItemStack, boolean paramBoolean);
	double[] getAABB(Entity paramEntity);
	void setEntityInvulnerable(Entity paramEntity, boolean paramBoolean);
	int getPing(Player paramPlayer);
}
