package mc.dragons.core.bridge.impl;

import static mc.dragons.core.util.BukkitUtil.syncPeriodic;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import mc.dragons.core.bridge.Bridge;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_16_R3.AxisAlignedBB;
import net.minecraft.server.v1_16_R3.EntityInsentient;

public class BridgeSpigot116R3 implements Bridge {
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
	}

	@Override
	public boolean hasAI(Entity entity) {
		return !NBTEditor.getBoolean(entity, "NoAI");
	}
	
	@Override
	public void setEntityAI(Entity entity, boolean ai) {
		NBTEditor.set(entity, (byte) (!ai ? 1 : 0), "NoAI");
	}
	
	@Override
	public void setEntityNoClip(Entity entity, boolean noclip) {
		((CraftEntity) entity).getHandle().noclip = noclip;
	}

	@Override
	public void setItemStackUnbreakable(ItemStack itemStack, boolean unbreakable) {
		NBTEditor.set(itemStack, unbreakable ? 1 : 0, "Unbreakable");
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
	}

	@Override
	public int getPing(Player player) {
		return ((CraftPlayer) player).getHandle().ping;
	}
	
	@Override
	public BukkitTask followEntity(Entity entity, LivingEntity target, double speed) {
        return syncPeriodic(() ->  {
        	try {
        		Location loc = target.getLocation();
        		
        		// https://bukkit.org/threads/make-a-mob-follow-a-player.351739/
                ((EntityInsentient) ((CraftEntity) entity).getHandle()).getNavigation().a(loc.getX(), loc.getY(), loc.getZ(), speed);
        	} catch(Exception ex) {
        		
        	}
        }, 0, 2 * 20);
	}
}
