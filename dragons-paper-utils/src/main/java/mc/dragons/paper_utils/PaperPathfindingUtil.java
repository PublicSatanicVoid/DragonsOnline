package mc.dragons.paper_utils;

import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PaperPathfindingUtil {
	private static boolean pathfinderWarned = false;
	
	/**
	 * Calls the Paper Pathfinding API to make the specified entity move to the
	 * specified location, if possible.
	 * 
	 * <p>It is not recommended to depend directly on the Paper API as it deprecates
	 * a number of methods that we still need.
	 * 
	 * @param entity
	 * @param location
	 * @param speed
	 * @param callback
	 * @return Whether the Paper API was able to perform the action
	 */
	public static boolean paperPathfinderMoveTo(final Entity entity, final Location location, double speed, final Consumer<Entity> callback, Plugin plugin) {
		if(entity instanceof Mob) {
			final Mob mob = (Mob) entity;
			final boolean wasAware = mob.isAware();
			mob.setAware(false); // Disable other behaviors from overriding this one.
			if(mob.getPathfinder().moveTo(location, speed)) {
				new BukkitRunnable() {
					public void run() {
						if(entity.isValid()) {
							if(entity.getLocation().distanceSquared(location) <= 1.0D) {
								callback.accept(entity);
								mob.setAware(wasAware);
								cancel();
							}
						}
						else {
							cancel();
						}
					}
				}.runTaskTimer(plugin, 0L, 10L);
				return true;
			}
			else if(!pathfinderWarned) {
				plugin.getLogger().config("Could not pathfind using Paper API: Pathfinder#moveTo returned false");
				pathfinderWarned = true;
			}
		}
		if(!pathfinderWarned) {
			plugin.getLogger().config("Could not pathfind using Paper API: Entity type " + entity.getType() + " is not supported");
			pathfinderWarned = true;
		}
		return false;
	}
}
