package mc.dragons.npcs;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.NPCAddon;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.BlockUtil;
import mc.dragons.core.util.StringUtil;

public class CompanionAddon extends NPCAddon {

	private Set<NPC> companions = new HashSet<>();
	
	public User getCompanionOwner(NPC npc) {
		UUID owner = npc.getStorageAccess().getDocument().get("companionOwner", UUID.class);
		if(owner == null) return null;
		return GameObjectType.USER.<User, UserLoader>getLoader().loadObject(owner);
	}

	private void fixCompanion(NPC companion, Player owner) {
		if(companion.getEntity().getWorld() != owner.getWorld()) companion.getEntity().teleport(owner);
		if(companion.getEntity().isDead()) companion.regenerate(owner.getLocation());
	}

	@Override
	public String getName() {
		return "Companion";
	}

	@Override
	public void initialize(GameObject gameObject) {
		NPC companion = (NPC) gameObject;
		LOGGER.fine("Initializing Companion addon on " + companion.getUUID() + " (" + companion.getName() + ")");
		companions.add(companion);
	}
	
	@Override
	public void onEnable() {
		new BukkitRunnable() {
			@Override public void run() {
				for(NPC companion : companions) {
					User owner = getCompanionOwner(companion);
					if(owner == null) continue;
					if(owner.getPlayer() == null) continue;
					if(!owner.getPlayer().isOnline()) continue;
					if(companion.getEntity().isDead()) continue;
					LOGGER.fine("Checking position of companion " + companion.getIdentifier() + " [entity " + StringUtil.entityToString(companion.getEntity()) + "]");
					fixCompanion(companion, owner.getPlayer());
					if(owner.getPlayer().getLocation().distanceSquared(companion.getEntity().getLocation()) > 5.0 * 5.0) {
						Vector move = owner.getPlayer().getLocation().subtract(companion.getEntity().getLocation()).toVector();
						move.multiply(Math.random() * 0.9);
						companion.getEntity().teleport(BlockUtil.getClosestGroundXZ(companion.getEntity().getLocation().add(move)).add(0, 1, 0));
						owner.debug("Moved companion " + companion.getIdentifier() + " closer");
					}
					
				}
			}
		}.runTaskTimer(Dragons.getInstance(), 0L, 20L * 3);
		
		new BukkitRunnable() {
			@Override public void run() {
				for(NPC companion : companions) {
					User owner = getCompanionOwner(companion);
					if(owner == null) continue;
					if(owner.getPlayer() == null) continue;
					if(!owner.getPlayer().isOnline()) continue;
					LOGGER.fine("Running health boost from companion " + companion.getIdentifier() + " [entity " + StringUtil.entityToString(companion.getEntity()) + "]");
					fixCompanion(companion, owner.getPlayer());
					owner.getPlayer().setHealth(Math.min(owner.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), 
							owner.getPlayer().getHealth() + 0.25 * owner.getPlayer().getLocation().distance(companion.getEntity().getLocation())));
				}
			}
		}.runTaskTimer(Dragons.getInstance(), 20L, 20L * 10);
	}

	@Override
	public void onCreateStorageAccess(Document data) {
		
	}

	@Override
	public void onMove(NPC npc, Location loc) {
		
	}

	@Override
	public void onTakeDamage(NPC on, GameObject from, double amount) {
		if(from instanceof User) {
			if(getCompanionOwner(on) == from) {
				User user = (User) from;
				Player player = user.getPlayer();
				player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Your companion is more powerful than you thought. " + on.getName() + " takes revenge on you.");
				Entity companion = on.getEntity();
				boolean hasAI = on.getNPCClass().hasAI();
				Dragons.getInstance().getBridge().setEntityAI(companion, false);
				companion.getWorld().spawnParticle(Particle.CRIT_MAGIC, companion.getLocation().add(0, 1, 0), 10);
				new BukkitRunnable() {
					private int iterations = 0;
					@Override public void run() {
						iterations++;
						if(iterations > 50 || companion.getLocation().distanceSquared(player.getLocation()) < 0.2 * 0.2) {
							player.damage(10.0, companion);
							player.setVelocity(new Vector(0, 5, 0));
							player.addPotionEffect(new PotionEffect(PotionEffectType.HARM, 20 * 5, 1));
							player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 5, 1));
							this.cancel();
							if(hasAI) {
								Dragons.getInstance().getBridge().setEntityAI(companion, true);
							}
							return;
						}
						fixCompanion(on, player);
						Vector move = player.getLocation().subtract(companion.getLocation()).toVector().normalize().multiply(0.1).add(new Vector(0, 0.5, 0));
						companion.teleport(companion.getLocation().add(move));
						companion.getWorld().spawnParticle(Particle.CRIT_MAGIC, companion.getLocation().add(0, 1, 0), 10);
					}
				}.runTaskTimer(Dragons.getInstance(), 20L, 1L);
			}
		}
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
		// TODO Auto-generated method stub
		
	}

}
