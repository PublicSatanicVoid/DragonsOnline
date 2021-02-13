package mc.dragons.core.bridge.impl;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import mc.dragons.core.Dragons;
import mc.dragons.core.bridge.Bridge;
import mc.dragons.core.util.StringUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_12_R1.AxisAlignedBB;

public class BridgeSpigot112R1 implements Bridge {
	private Logger LOGGER = Dragons.getInstance().getLogger();

	@Override
	public String getAPIVersion() {
		return "1_12_R1";
	}

	@Override
	public void sendActionBar(Player player, String message) {
		if (player == null) {
			LOGGER.warning("Attempted to send action bar to null player");
			return;
		}
		if (message == null) {
			LOGGER.warning("Attempted to send null message to player " + player.getName());
		}
		if (player.spigot() == null) {
			LOGGER.warning("Player#spigot() returned null for player " + player.getName() + " in sendActionBar()");
		}
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
	}

	@Override
	public void sendTitle(Player player, ChatColor titleColor, String title, ChatColor subtitleColor, String subtitle, int fadeInTime, int showTime, int fadeOutTime) {
		player.sendTitle(titleColor + title, subtitleColor + subtitle, 20 * fadeInTime, 20 * showTime, 20 * fadeOutTime);
	}

	@Override
	public void respawnPlayer(Player player) {
		player.spigot().respawn();
		LOGGER.finest("Respawning player " + player.getName());
	}

	@Override
	public boolean hasAI(Entity entity) {
		return !NBTEditor.getBoolean(entity, "NoAI");
	}
	
	@Override
	public void setEntityAI(Entity entity, boolean ai) {
		NBTEditor.set(entity, (byte) (!ai ? 1 : 0), "NoAI");
		LOGGER.finest("Set AI on entity " + StringUtil.entityToString(entity) + " to " + ai);
	}

	@Override
	public void setItemStackUnbreakable(ItemStack itemStack, boolean unbreakable) {
		NBTEditor.set(itemStack, unbreakable ? 1 : 0, "Unbreakable");
		LOGGER.finest("Set Unbreakability on item stack " + itemStack + " to " + unbreakable);
	}

	@Override
	public double[] getAABB(Entity entity) {
		net.minecraft.server.v1_12_R1.Entity nmsEntity = ((CraftEntity) entity).getHandle();
		AxisAlignedBB aabb = nmsEntity.getBoundingBox();
		return new double[] { aabb.a, aabb.b, aabb.c, aabb.d, aabb.e, aabb.f };
	}

	@Override
	public void setEntityInvulnerable(Entity entity, boolean immortal) {
		entity.setInvulnerable(immortal);
		LOGGER.finest("Set Invulnerability on entity " + StringUtil.entityToString(entity) + " to " + immortal);
	}

	@Override
	public int getPing(Player player) {
		return ((CraftPlayer) player).getHandle().ping;
	}
}
