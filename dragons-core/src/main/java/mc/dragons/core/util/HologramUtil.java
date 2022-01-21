package mc.dragons.core.util;

import static mc.dragons.core.util.BukkitUtil.sync;

import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Slime;
import org.bukkit.metadata.FixedMetadataValue;

import mc.dragons.core.Dragons;
import mc.dragons.core.bridge.Bridge;
import mc.dragons.core.events.PlayerEventListeners;
import mc.dragons.core.gameobject.user.User;

/**
 * Generates holograms, implemented using invisible armor stands.
 * 
 * @author Adam
 *
 */
public class HologramUtil {
	public static final String KEY_CLICKABLE_SLIME = "IsClickableSlime";
	private static final Bridge BRIDGE = Dragons.getInstance().getBridge();
	
	public static ArmorStand makeHologram(String text, Location loc) {
		ArmorStand hologram = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
		hologram.setCustomName(text);
		hologram.setCustomNameVisible(true);
		hologram.setGravity(false);
		hologram.setVisible(false);
		hologram.setAI(false);
		hologram.setRemoveWhenFarAway(false);
		hologram.setInvulnerable(true);
		hologram.setSmall(true);
		hologram.setMetadata("allow", new FixedMetadataValue(Dragons.getInstance(), true));
		sync(() -> hologram.setCollidable(false), 1);
		return hologram;
	}

	// Uses Slimes for clickable region - idea from filoghost's HolographicDisplays plugin
	public static ArmorStand clickableHologram(String text, Location loc, Consumer<User> onClick) {
		ArmorStand hologram = makeHologram(text, loc);
		Slime click = (Slime) loc.getWorld().spawnEntity(loc.clone().add(0, 1, 0), EntityType.SLIME);
		click.setInvisible(true);
		click.setSize(2);
		click.setGravity(false);
		click.setAI(false);
		BRIDGE.setEntityNoClip(click, true);
		click.setInvulnerable(true);
		click.setRemoveWhenFarAway(false);
		click.teleport(loc);
		click.setMetadata(KEY_CLICKABLE_SLIME, new FixedMetadataValue(Dragons.getInstance(), true));
		click.setMetadata("allow", new FixedMetadataValue(Dragons.getInstance(), true));
		PlayerEventListeners.addRightClickHandler(click, onClick);
		sync(() -> click.setCollidable(false), 1);
		return hologram;
	}
	
	public static Slime clickableSlime(String text, Location loc, Consumer<User> onClick) {
		Slime click = (Slime) loc.getWorld().spawnEntity(loc, EntityType.SLIME);
		click.setInvisible(true);
		click.setSize(2);
		click.setGravity(false);
		click.setAI(false);
		BRIDGE.setEntityNoClip(click, true);
		click.setInvulnerable(true);
//		click.setCollidable(false);
		click.setRemoveWhenFarAway(false);
		click.setMetadata(KEY_CLICKABLE_SLIME, new FixedMetadataValue(Dragons.getInstance(), true));
		click.setMetadata("allow", new FixedMetadataValue(Dragons.getInstance(), true));
		PlayerEventListeners.addRightClickHandler(click, onClick);
		sync(() -> click.setCollidable(false), 1);
		return click;
	}
	
	public static void unclickableifySlime(Slime slime) {
		slime.removeMetadata(KEY_CLICKABLE_SLIME, Dragons.getInstance());
		slime.removeMetadata("allow", Dragons.getInstance());
		PlayerEventListeners.removeRightClickHandlers(slime);
	}
	
	public static ArmorStand makeArmorStandNameTag(final Entity entity, String nameTag, double xOffset, double yOffset, double zOffset, final boolean bind) {
		final Entity nameTagFix = entity.getWorld().spawnEntity(entity.getWorld().getSpawnLocation().add(0.0D, -5.0D, 0.0D), EntityType.ARMOR_STAND);
		nameTagFix.setGravity(false);
		ArmorStand armorStand = (ArmorStand) nameTagFix;
		armorStand.setVisible(false);
		//armorStand.setAI(false); // This will cause the name tag to stay in one place, even if it is riding an entity. Client can disable AI downstream if desired.
		armorStand.setCollidable(false);
		armorStand.setInvulnerable(true);
		armorStand.setSmall(true);
		armorStand.setRemoveWhenFarAway(false);
		armorStand.setMetadata("allow", new FixedMetadataValue(Dragons.getInstance(), true));
		sync(() -> {
			nameTagFix.teleport(entity.getLocation().add(xOffset, yOffset, zOffset));		
			nameTagFix.setCustomName(nameTag);
			nameTagFix.setCustomNameVisible(true);
			if (bind) {
				entity.addPassenger(nameTagFix);
			}
		}, 1);
		return armorStand;
	}

	public static ArmorStand makeArmorStandNameTag(Entity entity, String nameTag, double xOffset, double yOffset, double zOffset) {
		return makeArmorStandNameTag(entity, nameTag, xOffset, yOffset, zOffset, false);
	}

	public static ArmorStand makeArmorStandNameTag(Entity entity, String nameTag) {
		return makeArmorStandNameTag(entity, nameTag, 0.0D, 0.0D, 0.0D);
	}

	/**
	 * A short-lived hologram.
	 * @param entity
	 * @param label
	 * @param durationTicks
	 * @param bind Whether the hologram should follow the provided entity, or stay stationary.
	 * @return
	 */
	public static ArmorStand temporaryHologram(final Entity entity, String label, int durationTicks, final boolean bind) {
		double xOffset = bind ? 0.0D : Math.random() * 0.5D;
		double yOffset = bind ? 0.5D : Math.random() * 1.2D;
		double zOffset = bind ? 0.0D : Math.random() * 0.5D;
		final ArmorStand tag = makeArmorStandNameTag(entity, label, xOffset, yOffset, zOffset, bind);
		sync(() -> {
			if (bind) {
				entity.removePassenger(tag);
			}
			tag.remove();
		}, durationTicks);
		return tag;
	}
}
