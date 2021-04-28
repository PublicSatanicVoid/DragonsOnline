package mc.dragons.spells.spells;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.spells.DragonsSpells;

public class TestSpell extends Spell {
	private DragonsSpells plugin;
	
	public TestSpell(DragonsSpells instance) {
		super(instance, "TestSpell", "Lightning Strike", "RLL");
		plugin = instance;
	}

	@Override
	public void execute(User user) {
		user.debug("Test spell activated!");
		Player p = user.getPlayer();
		Location buf = p.getEyeLocation();
		Vector dir = p.getEyeLocation().getDirection().normalize().multiply(0.5);
		buf.add(dir.clone().multiply(2.0));
		new BukkitRunnable() {
			private int n = 2;
			@Override public void run() {
				Location lbuf = buf;
				for(int i = 0; i < n; i++) {
					lbuf = lbuf.add(dir);
					if(lbuf.getBlock().getType().isSolid()) {
						lbuf = lbuf.subtract(dir.clone().multiply(2.0));
						n = 100;
						break;
					}
					p.spawnParticle(Particle.SPELL_INSTANT, lbuf, 5);
					user.debug("d=" + lbuf.distance(p.getEyeLocation()));
				}
				n++;
				if(n > 10) {
					this.cancel();
					p.teleport(lbuf);
					p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 5, 1, false, false));
					p.getWorld().spigot().strikeLightningEffect(lbuf, false);
					for(Entity e : p.getNearbyEntities(5, 5, 5)) {
						NPC npc = NPCLoader.fromBukkit(e);
						if(npc == null) continue;
						npc.damage(3.0);
					}
				}
			}
		}.runTaskTimer(plugin, 0L, 1L);
	}
}
