package mc.dragons.npcs;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.NPCAddon;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.user.User;

public class AuraAddon extends NPCAddon {

	@Override
	public String getName() {
		return "Aura";
	}

	private Map<NPC, BukkitRunnable> auraRunnables = new HashMap<>();
	
	@Override
	public void initialize(GameObject gameObject) {
		NPC npc = (NPC) gameObject;
		Entity entity = npc.getEntity();
		BukkitRunnable auraRunnable = new BukkitRunnable() {
			private Vector add = new Vector(1.5, 0.0, 0.0);
			private double theta = 10 * Math.PI / 180;
			private double y_step = 0.07;
			private double y_max = 2.1;
			
			@Override public void run() {
				Location at = entity.getLocation().add(add);
				entity.getWorld().spawnParticle(Particle.REDSTONE, at.getX(), at.getY(), at.getZ(), 2);
				add.setX(add.getX() * Math.cos(theta) - add.getZ() * Math.sin(theta));
				add.setZ(add.getX() * Math.sin(theta) + add.getZ() * Math.cos(theta));
				add.setY((add.getY() + y_step) % y_max);
			}
		};
		auraRunnable.runTaskTimer(Dragons.getInstance(), 0L, 2L);
		auraRunnables.put(npc, auraRunnable);
	}

	@Override
	public void onEnable() {
		
	}

	@Override
	public void onCreateStorageAccess(Document data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMove(NPC npc, Location loc) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTakeDamage(NPC on, GameObject from, double amount) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDealDamage(NPC from, GameObject to, double amount) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInteract(NPC with, User from) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeath(NPC gameObject) {
		auraRunnables.get(gameObject).cancel();
		auraRunnables.remove(gameObject);
	}

}
