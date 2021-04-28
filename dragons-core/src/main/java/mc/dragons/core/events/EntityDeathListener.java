package mc.dragons.core.events;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.util.HologramUtil;
import mc.dragons.core.util.StringUtil;

public class EntityDeathListener implements Listener {
	private DragonsLogger LOGGER;
	private GameObjectRegistry registry;

	public EntityDeathListener(Dragons instance) {
		LOGGER = instance.getLogger();
		registry = instance.getGameObjectRegistry();
	}

	public static int getXPReward(int levelOfKiller, int levelOfVictim) {
		return Math.max(1, 5 * (1 + levelOfVictim - 2 * levelOfKiller / 3));
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		LOGGER.debug("Death event on " + StringUtil.entityToString(event.getEntity()));
		if(event.getEntity().getPersistentDataContainer().has(Dragons.FIXED_ENTITY_KEY, PersistentDataType.SHORT)) {
			LOGGER.severe("A fixed entity (" + StringUtil.entityToString(event.getEntity()) + ") has died! Location: "
				+ StringUtil.locToString(event.getEntity().getLocation()) + " [" + event.getEntity().getWorld().getName() + "]");
			return;
		}
		
		Player player = event.getEntity().getKiller();
		User user = UserLoader.fromPlayer(player);
		LivingEntity livingEntity = event.getEntity();
		for (Entity passenger : new ArrayList<>(livingEntity.getPassengers())) {
			livingEntity.removePassenger(passenger);
			passenger.remove();
		}
		NPC npc = NPCLoader.fromBukkit(livingEntity);
		if (npc == null) {
			return;
		}
		if (npc.isImmortal() || npc.getNPCType().canRespawnOnDeath()) {
			npc.setEntity(livingEntity.getLocation().getWorld().spawnEntity(livingEntity.getLocation(), npc.getEntity().getType()));
			npc.initializeEntity();
		} else {
			registry.removeFromDatabase(npc);
			npc.getNPCClass().handleDeath(npc);
		}
		npc.updateHealthBar();
		if (player == null) {
			return;
		}
		Location loc = user.getPlayer().getLocation();
		World world = loc.getWorld();
		for (Item item : npc.getNPCClass().getLootTable().getDrops(loc)) {
			world.dropItem(loc, item.getItemStack());
		}
		if (npc.getNPCType() == NPC.NPCType.HOSTILE) {
			int xpReward = getXPReward(user.getLevel(), npc.getLevel());
			user.sendActionBar("+ " + ChatColor.GREEN + xpReward + " XP");
			String tag = "+ " + ChatColor.GREEN + ChatColor.BOLD + xpReward + " XP";
			if (livingEntity.getNearbyEntities(10.0D, 10.0D, 10.0D).stream().filter(e -> (e.getType() == EntityType.PLAYER)).count() > 1L) {
				tag = String.valueOf(tag) + ChatColor.GRAY + " to " + user.getName();
			}
			HologramUtil.temporaryArmorStand(livingEntity, tag, 20, false);
			user.addXP(xpReward);
		}
		user.updateQuests(event);
	}
}
