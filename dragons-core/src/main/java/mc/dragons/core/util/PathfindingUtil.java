package mc.dragons.core.util;

import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import mc.dragons.core.Dragons;

public class PathfindingUtil {
	public static void walkToLocation(final Entity entity, final Location location, final double speed, final Consumer<Entity> callback) {
		entity.teleport(BlockUtil.getClosestGroundXZ(entity.getLocation()).add(0.0D, 1.0D, 0.0D));
		(new BukkitRunnable() {
			public void run() {
				Location curr = entity.getLocation();
				if (entity.isValid() && entity.getLocation().distanceSquared(location) > 1.0D) {
					Vector direction = location.clone().subtract(curr).toVector().normalize().multiply(speed).setY(0);
					Location to = curr.clone().add(direction);
					double groundY = BlockUtil.getClosestGroundXZ(to).getY();
					to.setY(groundY + 1.0D);
					entity.setVelocity(direction);
					entity.teleport(to);
					Dragons.getInstance().getLogger().finest("PATHFIND " + StringUtil.entityToString(entity) + ": currY=" + curr.getY() + ", toY=" + to.getY() + ",toBlock=" + to.getBlock().getType());
				} else {
					cancel();
					entity.setVelocity(new Vector(0, 0, 0));
					if (callback != null)
						callback.accept(entity);
				}
			}
		}).runTaskTimer((Plugin) Dragons.getInstance(), 0L, 1L);
	}
}
