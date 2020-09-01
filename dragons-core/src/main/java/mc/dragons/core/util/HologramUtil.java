package mc.dragons.core.util;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;

public class HologramUtil {
	public static ArmorStand makeHologram(String text, Location loc) {
		ArmorStand hologram = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
		hologram.setCustomName(text);
		hologram.setCustomNameVisible(true);
		hologram.setGravity(false);
		hologram.setVisible(false);
		hologram.setAI(false);
		hologram.setCollidable(false);
		hologram.setInvulnerable(true);
		hologram.setSmall(true);
		hologram.setMetadata("allow", new FixedMetadataValue(Dragons.getInstance(), Boolean.valueOf(true)));
		return hologram;
	}

	public static ArmorStand makeArmorStandNameTag(final Entity entity, String nameTag, final double xOffset, final double yOffset, final double zOffset, final boolean bind) {
		final Entity nameTagFix = entity.getWorld().spawnEntity(entity.getWorld().getSpawnLocation().add(0.0D, -5.0D, 0.0D), EntityType.ARMOR_STAND);
		nameTagFix.setGravity(false);
		ArmorStand armorStand = (ArmorStand) nameTagFix;
		armorStand.setVisible(false);
		armorStand.setAI(false);
		armorStand.setCollidable(false);
		armorStand.setInvulnerable(true);
		armorStand.setSmall(true);
		armorStand.setMetadata("allow", new FixedMetadataValue(Dragons.getInstance(), Boolean.valueOf(true)));
		(new BukkitRunnable() {
			@Override
			public void run() {
				nameTagFix.teleport(entity.getLocation().add(xOffset, yOffset, zOffset));		
				nameTagFix.setCustomName(nameTag);
				nameTagFix.setCustomNameVisible(true);
				if (bind)
					entity.addPassenger(nameTagFix);
			}
		}).runTaskLater(Dragons.getInstance(), 1L);
		return armorStand;
	}

	public static ArmorStand makeArmorStandNameTag(Entity entity, String nameTag, double xOffset, double yOffset, double zOffset) {
		return makeArmorStandNameTag(entity, nameTag, xOffset, yOffset, zOffset, false);
	}

	public static ArmorStand makeArmorStandNameTag(Entity entity, String nameTag) {
		return makeArmorStandNameTag(entity, nameTag, 0.0D, 0.0D, 0.0D);
	}

	public static ArmorStand temporaryArmorStand(final Entity entity, String label, int durationTicks, final boolean bind) {
		double xOffset = bind ? 0.0D : (Math.random() * 0.5D);
		double yOffset = bind ? 0.5D : (Math.random() * 1.2D);
		double zOffset = bind ? 0.0D : (Math.random() * 0.5D);
		final ArmorStand tag = makeArmorStandNameTag(entity, label, xOffset, yOffset, zOffset, bind);
		(new BukkitRunnable() {
			@Override
			public void run() {
				if (bind)
					entity.removePassenger(tag);
				tag.remove();
			}
		}).runTaskLater(Dragons.getInstance(), durationTicks);
		return tag;
	}
}
