package mc.dragons.core.events;

import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;

public class EntityTargetEventListener implements Listener {
	Logger LOGGER;

	public EntityTargetEventListener(Dragons instance) {
		LOGGER = instance.getLogger();
	}

	@EventHandler
	public void onEntityTarget(EntityTargetEvent e) {
		NPC npc = NPCLoader.fromBukkit(e.getEntity());
		if (npc == null) {
			return;
		}
		LivingEntity currTarget = npc.getDeclaredTarget();
		if (currTarget != null && (e.getTarget() == null || e.getTarget() instanceof LivingEntity) && currTarget != e.getTarget()) {
			LOGGER.finest("- Cancelled due to having a declared target");
			e.setCancelled(true);
			e.setTarget(currTarget);
			return;
		}
		if (e.getTarget() instanceof Player) {
			Player p = (Player) e.getTarget();
			LOGGER.finest("Entity target event on " + p.getName());
			User user = UserLoader.fromPlayer(p);
			if (npc != null && npc.getNPCType() != NPC.NPCType.HOSTILE) {
				LOGGER.finest(" - Cancelled due to mob non-hostility");
				e.setCancelled(true);
				return;
			}
			if (user.isGodMode() || user.isVanished() || user.hasActiveDialogue()) {
				LOGGER.finest(" - Cancelled due to state");
				e.setCancelled(true);
				return;
			}
			Set<Region> regions = user.getRegions();
			for (Region r : regions) {
				if (!Boolean.valueOf(r.getFlags().getString("pve")).booleanValue()) {
					LOGGER.finest("- Cancelled due to region");
					e.setCancelled(true);
					return;
				}
			}
		}
	}
}
