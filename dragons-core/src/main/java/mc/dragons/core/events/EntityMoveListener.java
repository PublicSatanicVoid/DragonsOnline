package mc.dragons.core.events;

import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.region.RegionLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;

public class EntityMoveListener extends PacketAdapter {
	private RegionLoader regionLoader;

	public EntityMoveListener(Dragons instance) {
		super(instance, new PacketType[] { PacketType.Play.Server.REL_ENTITY_MOVE, PacketType.Play.Server.REL_ENTITY_MOVE_LOOK });
		regionLoader = GameObjectType.REGION.getLoader();
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		Entity entity = event.getPacket().getEntityModifier(event.getPlayer().getWorld()).read(0);
		NPC npc = NPCLoader.fromBukkit(entity);
		if (npc == null) {
			return;
		}
		npc.getNPCClass().handleMove(npc, entity.getLocation());
		if (npc.getNPCType() == NPC.NPCType.HOSTILE) {
			Set<Region> regions = regionLoader.getRegionsByLocation(entity.getLocation());
			for (Region region : regions) {
				if (!Boolean.valueOf(region.getFlags().getString("allowhostile"))) {
					npc.remove();
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
