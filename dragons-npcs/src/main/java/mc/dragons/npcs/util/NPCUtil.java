package mc.dragons.npcs.util;

import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class NPCUtil {
	public static EulerAngle randomRotation() {
		return new EulerAngle(Math.random() * Math.PI / 2, Math.random() * Math.PI / 2, Math.random() * Math.PI / 2);
	}
	
	public static double random(double maxUnsigned) {
		return 2 * maxUnsigned * (Math.random() - 0.5);
	}
	
	public static Vector randomVector() {
		return randomVector(1.0);
	}
	
	public static Vector randomVector(double maxUnsignedComponent) {
		return new Vector(random(maxUnsignedComponent), random(maxUnsignedComponent), random(maxUnsignedComponent));
	}
}
