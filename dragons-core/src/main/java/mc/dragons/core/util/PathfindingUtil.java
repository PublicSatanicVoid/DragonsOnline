package mc.dragons.core.util;

import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import mc.dragons.core.Dragons;
import mc.dragons.core.logging.DragonsLogger;

public class PathfindingUtil {
	private static Dragons dragons = Dragons.getInstance();
	private static DragonsLogger LOGGER = dragons.getLogger();
	
	public static void walkToLocation(final Entity entity, final Location location, final double speed, final Consumer<Entity> callback) {
		LOGGER.trace("PATHFIND BEGIN " + StringUtil.entityToString(entity) + ": " + StringUtil.locToString(entity.getLocation()) + " -> " + StringUtil.locToString(location) + " (speed=" + speed + ")");

		// In most cases, we should be able to rely on Paper's pathfinding API.
		if(entity instanceof Mob) {
			Mob mob = (Mob) entity;
			final boolean wasAware = mob.isAware();
			mob.setAware(false); // Disable other behaviors from overriding this one.
			if(mob.getPathfinder().moveTo(location, speed)) {
				new BukkitRunnable() {
					@Override
					public void run() {
						if(entity.isValid()) {
							if(entity.getLocation().distanceSquared(location) <= 1.0D) {
								callback.accept(entity);
								mob.setAware(wasAware);
								cancel();
							}
						}
						else {
							LOGGER.trace("Entity died while pathfinding: " + StringUtil.entityToString(entity));
							cancel();
						}
					}
				}.runTaskTimer(dragons, 0L, 10L);
				return;
			}
			else {
				LOGGER.trace("Could not successfully start pathfinding through standard API on entity " + StringUtil.entityToString(entity));
			}
			
		}
		else {
			LOGGER.trace("Falling through to naive pathfinding algorithm on entity " + StringUtil.entityToString(entity));
		}
		
		// Otherwise, use this simple algorithm
		final double adjustedSpeed = speed / 2; // Roughly account for discrepancies between configured speeds and block-per-second speed.
		entity.teleport(BlockUtil.getClosestGroundXZ(entity.getLocation()).add(0.0D, 1.0D, 0.0D));
		new BukkitRunnable() {
			@Override
			public void run() {
				Location curr = entity.getLocation();
				if (entity.isValid() && entity.getLocation().distanceSquared(location) > 1.0D) {
					Vector direction = location.clone().subtract(curr).toVector().normalize().multiply(adjustedSpeed).setY(0);
					Location to = curr.clone().add(direction);
					double groundY = BlockUtil.getClosestGroundXZ(to).getY();
					to.setY(groundY + 1.0D);
					entity.setVelocity(direction);
					entity.teleport(to);
					LOGGER.verbose("PATHFIND " + StringUtil.entityToString(entity) + ": currY=" + curr.getY() + ", toY=" + to.getY() + ", toBlock=" + to.getBlock().getType());
				} else {
					cancel();
					entity.setVelocity(new Vector(0, 0, 0));
					if (callback != null) {
						callback.accept(entity);
					}
				}
			}
		}.runTaskTimer(dragons, 0L, 1L);
	}
}
