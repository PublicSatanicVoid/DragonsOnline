package mc.dragons.npcs;

import static mc.dragons.core.util.BukkitUtil.sync;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.NPCAddon;
import mc.dragons.core.bridge.Bridge;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.util.BlockUtil;
import mc.dragons.core.util.CompanionUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.npcs.commands.ToggleCompanionLaunch;

public class CompanionAddon extends NPCAddon {
	private static final double FOLLOW_SPEED = 0.2;
	
	private Dragons dragons = Dragons.getInstance();
	private DragonsLogger LOGGER = dragons.getLogger();
	private Bridge BRIDGE = dragons.getBridge();
	
	private Set<NPC> companions = new HashSet<>();	
	private Map<NPC, BukkitTask> followTasks = new HashMap<>();
	public Set<NPC> getCompanions() { return companions; }
	
	private void validateCompanion(NPC companion, Player owner) {
		User user = UserLoader.fromPlayer(owner);
		if(!CompanionUtil.isCompanionOf(companion, user)) {
			user.debug("Companion was removed from userdata, removing ingame");
			CompanionUtil.removeCompanion(user, companion);
			companion.remove(); 
			companions.remove(companion);
			followTasks.remove(companion);
		}
		else {
			if(companion.getEntity() == null || companion.getEntity().isDead() || !companion.getEntity().isValid()) companion.regenerate(owner.getLocation());
			if(companion.getEntity().getWorld() != owner.getWorld()) companion.getEntity().teleport(owner);
		}
	}

	@Override
	public String getName() {
		return "Companion";
	}

	@Override
	public void initialize(GameObject gameObject) {
		NPC companion = (NPC) gameObject;
		LOGGER.trace("Initializing Companion addon on " + companion.getUUID() + " (" + companion.getName() + ")");
		companions.add(companion);
	}
	
	@Override
	public void onEnable() {
		new BukkitRunnable() {
			@Override public void run() {
				for(NPC companion : companions) {
					User owner = CompanionUtil.getCompanionOwner(companion);
					if(owner == null) continue;
					if(owner.getPlayer() == null) continue;
					if(!owner.getPlayer().isOnline()) continue;
					if(companion.getEntity().isDead()) continue;
					followTasks.computeIfAbsent(companion, c -> BRIDGE.followEntity(companion.getEntity(), owner.getPlayer(), FOLLOW_SPEED));
					LOGGER.verbose("Checking position of companion " + companion.getIdentifier() + " [entity " + StringUtil.entityToString(companion.getEntity()) + "]");
					validateCompanion(companion, owner.getPlayer());
					if(owner.getPlayer().getLocation().distanceSquared(companion.getEntity().getLocation()) > 5.0 * 5.0) {
						Vector move = owner.getPlayer().getLocation().subtract(companion.getEntity().getLocation()).toVector();
						move.multiply(Math.random() * 0.9);
						companion.getEntity().teleport(BlockUtil.getClosestGroundXZ(companion.getEntity().getLocation().add(move)).add(0, 1, 0));
						LOGGER.verbose("- Moved companion closer to owner");
					}
					
				}
			}
		}.runTaskTimer(dragons, 0L, 20L * 3);
		
		new BukkitRunnable() {
			@Override public void run() {
				for(NPC companion : companions) {
					User owner = CompanionUtil.getCompanionOwner(companion);
					if(owner == null) continue;
					if(owner.getPlayer() == null) continue;
					if(!owner.getPlayer().isOnline()) continue;
					LOGGER.verbose("Running health boost from companion " + companion.getIdentifier() + " [entity " + StringUtil.entityToString(companion.getEntity()) + "]");
					sync(() -> {
						validateCompanion(companion, owner.getPlayer());
						owner.getPlayer().setHealth(Math.min(owner.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), 
								owner.getPlayer().getHealth() + 0.25 * owner.getPlayer().getLocation().distance(companion.getEntity().getLocation())));
					}, 1);
				}
			}
		}.runTaskTimer(dragons, 20L, 20L * 10);
	}

	@Override
	public void onTakeDamage(NPC on, GameObject from, double amount) {
		if(from instanceof User && CompanionUtil.getCompanionOwner(on) == from && !((User) from).getLocalData().getBoolean(ToggleCompanionLaunch.DISABLE_COMPANION_LAUNCH, false)) {
			User user = (User) from;
			Player player = user.getPlayer();
			player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Your companion is more powerful than you thought. " + on.getName() + " takes revenge on you.");
			Entity companion = on.getEntity();
			boolean hasAI = on.getNPCClass().hasAI();
			BRIDGE.setEntityAI(companion, false);
			companion.getWorld().spawnParticle(Particle.CRIT_MAGIC, companion.getLocation().add(0, 1, 0), 10);
			new BukkitRunnable() {
				private int iterations = 0;
				@Override public void run() {
					iterations++;
					if(iterations > 4 || companion.getLocation().distanceSquared(player.getLocation()) < 0.2 * 0.2) {
						player.damage(10.0, companion);
						player.addPotionEffect(new PotionEffect(PotionEffectType.HARM, 20 * 5, 1));
						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 5, 1));
						this.cancel();
						if(hasAI) {
							BRIDGE.setEntityAI(companion, true);
						}
						return;
					}
					validateCompanion(on, player);
					Vector move = player.getLocation().subtract(companion.getLocation()).toVector().normalize().multiply(0.1).add(new Vector(0, 0.5, 0));
					companion.teleport(companion.getLocation().add(move));
					companion.getWorld().spawnParticle(Particle.CRIT_MAGIC, companion.getLocation().add(0, 1, 0), 10);
				}
			}.runTaskTimer(dragons, 20L, 10L);
		}
	}

}
