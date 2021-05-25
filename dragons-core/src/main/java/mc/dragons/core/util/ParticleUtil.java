package mc.dragons.core.util;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class ParticleUtil {
	public static final double ANGULAR_RESOLUTION = 0.5;

	public static <T> void drawSphere(World world, Particle particle, double x, double y, double z, int r, int count, T data) {
		for(double theta = 0.0; theta < 2 * Math.PI; theta += ANGULAR_RESOLUTION) {
			for(double phi = 0.0; phi < Math.PI; phi += ANGULAR_RESOLUTION) {
				world.spawnParticle(particle, x + r * cos(theta) * sin(phi), y + r * sin(theta) * sin(phi), z + r * cos(phi), count, 0, 0, 0, 0.0, data);
			}
		}
	}
	
	public static <T> void drawSphere(Player player, Particle particle, double x, double y, double z, double r, int count, T data) {
		for(double theta = 0.0; theta < 2 * Math.PI; theta += ANGULAR_RESOLUTION) {
			for(double phi = 0.0; phi < Math.PI; phi += ANGULAR_RESOLUTION) {
				player.spawnParticle(particle, x + r * cos(theta) * sin(phi), y + r * sin(theta) * sin(phi), z + r * cos(phi), count, 0, 0, 0, 0.0, data);
			}
		}
	}
	
	public static <T> void drawBurst(World world, Particle particle, double x, double y, double z, int count, T data) {
		for(int i = 0; i < count; i++) {
			world.spawnParticle(particle, x + random() - 0.5, y + random() - 0.5, z + random() - 0.5, 1, 0, 0, 0, 0.0, data);
		}
	}
	
	public static <T> void drawBurst(Player player, Particle particle, double x, double y, double z, int count, T data) {
		for(int i = 0; i < count; i++) {
			player.spawnParticle(particle, x + random() * 0.1 - 0.05, y + random() * 0.1 - 0.05, z + random() * 0.1 - 0.05, 1, 0, 0, 0, 0.0, data);
		}
	}
 }
