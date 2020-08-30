package mc.dragons.npcs.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.npcs.ComplexAddon;
import mc.dragons.npcs.DragonsNPCAddons;
import mc.dragons.npcs.util.NPCUtil;

public class SoulStealerAddon extends ComplexAddon {

	private Map<NPC, List<ArmorStand>> capturedSouls = new HashMap<>();
	
	public SoulStealerAddon() {
		super("SoulStealer");
		hideEntity = false;
	}

	private static final int N_SOULS = 5;
	
	private Map<NPC, BukkitRunnable> attackRunnables = new HashMap<>();
	
	@Override
	public void initializeParts(NPC npc) {
		List<ArmorStand> souls = new ArrayList<>();
		for(int i = 0; i < N_SOULS; i++) {
			ArmorStand soul = newPart(npc);
			soul.setHelmet(new ItemStack(Material.MAGMA_CREAM));
			soul.setHeadPose(NPCUtil.randomRotation());
			souls.add(soul);
		}
		capturedSouls.put(npc, souls);
		attackRunnables.put(npc, new BukkitRunnable() {
			@Override public void run() {
				User target = null;
				for(Entity e : npc.getEntity().getNearbyEntities(10.0, 10.0, 10.0)) {
					if(e instanceof Player) {
						target = UserLoader.fromPlayer((Player) e);
						break;
					}
				}
				if(target == null) return;
				final User fTarget = target;
				if(!(target instanceof User)) return;
				ArmorStand soul = (ArmorStand) npc.getEntity().getWorld().spawnEntity(npc.getEntity().getLocation(), EntityType.ARMOR_STAND);
				if(target.getPlayer().getWorld() != soul.getWorld()) {
					this.cancel();
					return;
				}
				soul.setMetadata("allow", new FixedMetadataValue(JavaPlugin.getPlugin(DragonsNPCAddons.class), true));
				soul.setHelmet(new ItemStack(Material.MAGMA_CREAM));
				soul.setHeadPose(NPCUtil.randomRotation());
				soul.setVisible(false);
				Vector move = target.getPlayer().getLocation().subtract(soul.getLocation()).toVector().normalize().multiply(0.7);
				new BukkitRunnable() {
					private int i = 0;
					@Override public void run() {
						if(fTarget.getPlayer().getWorld() != soul.getWorld()) {
							this.cancel();
							soul.remove();
							return;
						}
						Location to = soul.getLocation();
						if(soul.isDead() || !soul.isValid()) {
							this.cancel();
							soul.remove();
							return;
						}
						try {
							to.add(move);
							to.checkFinite();
							soul.teleport(to);
						}
						catch(Exception e) {
							this.cancel();
							soul.remove();
							return;
						}
						if(soul.getLocation().distanceSquared(fTarget.getPlayer().getLocation()) < 1.0) {
							this.cancel();
							soul.remove();
							fTarget.getPlayer().damage(5.0, npc.getEntity());
						}
						i++;
						if(i > 250) {
							this.cancel();
							soul.remove();
						}
					}
				}.runTaskTimer(Dragons.getInstance(), 0L, 2L);
			}
		});
		attackRunnables.get(npc).runTaskTimer(Dragons.getInstance(), 0L, 20L * 5);
	}
	
	@Override
	public void onMove(NPC npc, Location to) {
		//super.onMove(npc, to);
		if(!capturedSouls.containsKey(npc)) return; // This would be strange
		for(ArmorStand soul : capturedSouls.get(npc)) {
			soul.teleport(to.clone().add(NPCUtil.randomVector().add(new Vector(0, 0.5, 0))));
			soul.setHeadPose(NPCUtil.randomRotation());
		}
	}
	
	@Override
	public void onDeath(NPC npc) {
		for(ArmorStand soul : capturedSouls.get(npc)) {
			soul.remove();
		}
		attackRunnables.get(npc).cancel();
		attackRunnables.remove(npc);
	}
	
	@Override
	public void onDealDamage(NPC from, GameObject to, double amount) {

	}

}
