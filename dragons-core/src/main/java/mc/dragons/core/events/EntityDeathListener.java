package mc.dragons.core.events;

import static mc.dragons.core.util.BukkitUtil.sync;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
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
import mc.dragons.core.bridge.PlayerNPC.NPCStatus;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.util.HologramUtil;
import mc.dragons.core.util.StringUtil;

public class EntityDeathListener implements Listener {
	private static final boolean CRASH_ON_FIXED_DEATH = true;
	
	private DragonsLogger LOGGER;

	public EntityDeathListener(Dragons instance) {
		LOGGER = instance.getLogger();
	}

	public static int getXPReward(int levelOfKiller, int levelOfVictim) {
		return Math.max(1, 5 * (1 + levelOfVictim - 2 * levelOfKiller / 3));
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		LOGGER.trace("Death event on " + StringUtil.entityToString(event.getEntity()));
		if(event.getEntity().getPersistentDataContainer().has(Dragons.FIXED_ENTITY_KEY, PersistentDataType.SHORT)) {
			UUID cid = LOGGER.newCID();
			LOGGER.severe(cid, "A fixed entity (" + StringUtil.entityToString(event.getEntity()) + ") has died! Location: "
				+ StringUtil.locToString(event.getEntity().getLocation()) + " [" + event.getEntity().getWorld().getName() + "]");
			Dragons.getInstance().getStaffAlertHandler().sendGenericMessage(PermissionLevel.BUILDER, "A fixed entity died! Correlation ID: " + cid);
			if(CRASH_ON_FIXED_DEATH) {
				LOGGER.severe(cid, "Crashing server to prevent saving");
				for(Player p : Bukkit.getOnlinePlayers()) {
					p.kickPlayer(ChatColor.RED + "The server entered an unexpected error state and was terminated to prevent data loss.\n\n"
							+ "Server: " + Dragons.getInstance().getServerName() + "\n"
							+ "Log Token: " + Dragons.getInstance().getCustomLoggingProvider().getCustomLogFilter().getLogEntryUUID());
				}
				System.exit(-1);
			}
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
		if(npc.getEntityType() == EntityType.PLAYER) {
			npc.getPlayerNPC().setStatus(NPCStatus.DIE);
		}
		if (npc.isImmortal() || npc.getNPCType().canRespawnOnDeath()) {
			npc.regenerate(livingEntity.getLocation());
		} else {
			npc.getNPCClass().handleDeath(npc);
			sync(() -> npc.remove(), 5);
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
			HologramUtil.temporaryHologram(livingEntity, tag, 20, false);
			user.addXP(xpReward);
		}
		user.updateQuests(event);
	}
}
