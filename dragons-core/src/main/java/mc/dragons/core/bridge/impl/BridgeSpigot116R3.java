package mc.dragons.core.bridge.impl;

import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import mc.dragons.core.Dragons;
import mc.dragons.core.bridge.Bridge;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.util.StringUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_16_R3.AxisAlignedBB;

public class BridgeSpigot116R3 implements Bridge {
	private DragonsLogger LOGGER = Dragons.getInstance().getLogger();

	@Override
	public String getAPIVersion() {
		return "1_16_R3";
	}

	@Override
	public void sendActionBar(Player player, String message) {
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
	}

	@Override
	@Deprecated
	public void sendTitle(Player player, ChatColor titleColor, String title, ChatColor subtitleColor, String subtitle, int fadeInTime, int showTime, int fadeOutTime) {
		player.sendTitle(titleColor + title, subtitleColor + subtitle, 20 * fadeInTime, 20 * showTime, 20 * fadeOutTime);
	}

	@Override
	public void respawnPlayer(Player player) {
		player.spigot().respawn();
		LOGGER.trace("BRIDGE: Respawning player " + player.getName());
	}

	@Override
	public boolean hasAI(Entity entity) {
		return !NBTEditor.getBoolean(entity, "NoAI");
	}
	
	@Override
	public void setEntityAI(Entity entity, boolean ai) {
		NBTEditor.set(entity, (byte) (!ai ? 1 : 0), "NoAI");
		LOGGER.trace("BRIDGE: Set AI on entity " + StringUtil.entityToString(entity) + " to " + ai);
	}

	@Override
	public void setItemStackUnbreakable(ItemStack itemStack, boolean unbreakable) {
		NBTEditor.set(itemStack, unbreakable ? 1 : 0, "Unbreakable");
		LOGGER.trace("BRIDGE: Set Unbreakability on item stack " + itemStack + " to " + unbreakable);
	}

	@Override
	public double[] getAABB(Entity entity) {
		net.minecraft.server.v1_16_R3.Entity nmsEntity = ((CraftEntity) entity).getHandle();
		AxisAlignedBB aabb = nmsEntity.getBoundingBox();
		return new double[] { aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ };
	}

	@Override
	public void setEntityInvulnerable(Entity entity, boolean immortal) {
		entity.setInvulnerable(immortal);
		LOGGER.trace("BRIDGE: Set Invulnerability on entity " + StringUtil.entityToString(entity) + " to " + immortal);
	}

	@Override
	public int getPing(Player player) {
		return ((CraftPlayer) player).getHandle().ping;
	}
}
