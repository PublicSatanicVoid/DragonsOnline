package mc.dragons.core.events;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.npc.PlayerNPCRegistry;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.region.RegionLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;

public class EntityMoveListener extends PacketAdapter {
	private static int SPAWN_RADIUS = PlayerNPCRegistry.SPAWN_RADIUS;
	private RegionLoader regionLoader;

	public EntityMoveListener(Dragons instance) {
		super(instance, new PacketType[] { PacketType.Play.Server.REL_ENTITY_MOVE, PacketType.Play.Server.REL_ENTITY_MOVE_LOOK });
		regionLoader = GameObjectType.REGION.getLoader();
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		Entity entity = event.getPacket().getEntityModifier(event.getPlayer().getWorld()).read(0);
		if(entity == null) return;
		NPC npc = NPCLoader.fromBukkit(entity);
		if(entity.hasMetadata("shadow")) {
			for(MetadataValue s : entity.getMetadata("shadow")) {
				Entity shadow = (Entity) s.value();
				if(shadow.hasMetadata("followDY")) {
					shadow.teleport(entity.getLocation().add(0, shadow.getMetadata("followDY").get(0).asDouble(), 0));
				}
				else {
					shadow.teleport(entity.getLocation());
				}
				NPC npcShadow = NPCLoader.fromBukkit(shadow);
				if(npcShadow != null && npcShadow.getEntityType() == EntityType.PLAYER) {
					Set<Entity> nearby = new HashSet<>(entity.getNearbyEntities(30, 30, 30));
					nearby.addAll(shadow.getNearbyEntities(SPAWN_RADIUS, SPAWN_RADIUS, SPAWN_RADIUS));
					for(Entity e : nearby) {
						if(e instanceof Player && UserLoader.fromPlayer((Player) e) != null) {
							npcShadow.getPlayerNPC().updateLocationFor((Player) e, entity.getLocation().getPitch(), entity.getLocation().getYaw());
						}
					}
				}
			}
		}
		if (npc == null) {
			return;
		}
		npc.getNPCClass().handleMove(npc, entity.getLocation());
		if (npc.getNPCType() == NPC.NPCType.HOSTILE) {
			Set<Region> regions = regionLoader.getRegionsByLocation(entity.getLocation());
			for (Region region : regions) {
				if (!Boolean.valueOf(region.getFlags().getString("allowhostile"))) {
					npc.remove();
					// TODO Push back or turn away rather than destroy
				}
			}
		}
		for (Entity e : entity.getNearbyEntities(2.0D, 2.0D, 2.0D)) {
			if (e instanceof Player) {
				User user = UserLoader.fromPlayer((Player) e);
				if(user == null) return;
				if (user.hasDeathCountdown()) {
					entity.setVelocity(entity.getLocation().toVector().subtract(e.getLocation().toVector()).normalize().multiply(5.0D));
				}
			}
		}
	}
}
