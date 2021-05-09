package mc.dragons.core.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class EntityUtil {
	
	/**
	 * 
	 * @param entity
	 * @return The nearest entity that the specified entity is looking at.
	 */
	public static Entity getTarget(LivingEntity entity) {
		Entity target = null;
		Location eye = entity.getEyeLocation();
		Vector lookDir = eye.getDirection();
		double minDist = Double.MAX_VALUE;
		for(Entity test : entity.getNearbyEntities(10.0, 10.0, 10.0)) {
			if(test.equals(entity)) continue;
			if(!(test instanceof LivingEntity)) continue;
			LivingEntity le = (LivingEntity) test;
			Vector to = le.getEyeLocation().subtract(eye).toVector().normalize();
			double dist = test.getLocation().distance(eye);
			if(to.dot(lookDir) > 0.99 && dist < minDist) {
				target = test;
				minDist = dist;
			}
		}
		return target;
	}
}
