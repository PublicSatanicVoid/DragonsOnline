package mc.dragons.core.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Open-source library to implement entity phasing.
 * 
 * @author comphenix
 *
 */

public class EntityHider implements Listener {
	protected Table<Integer, Integer, Boolean> observerEntityMap = HashBasedTable.create();

	@SuppressWarnings("deprecation")
	private static final PacketType[] ENTITY_PACKETS = new PacketType[] { PacketType.Play.Server.ENTITY_EQUIPMENT, PacketType.Play.Server.BED, PacketType.Play.Server.ANIMATION,
			PacketType.Play.Server.NAMED_ENTITY_SPAWN, PacketType.Play.Server.COLLECT, PacketType.Play.Server.SPAWN_ENTITY, PacketType.Play.Server.SPAWN_ENTITY_LIVING,
			PacketType.Play.Server.SPAWN_ENTITY_PAINTING, PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB, PacketType.Play.Server.ENTITY_VELOCITY, PacketType.Play.Server.REL_ENTITY_MOVE,
			PacketType.Play.Server.ENTITY_LOOK, PacketType.Play.Server.ENTITY_MOVE_LOOK, PacketType.Play.Server.ENTITY_MOVE_LOOK, PacketType.Play.Server.REL_ENTITY_MOVE,
			PacketType.Play.Server.REL_ENTITY_MOVE_LOOK, PacketType.Play.Server.ENTITY_TELEPORT, PacketType.Play.Server.ENTITY_HEAD_ROTATION, PacketType.Play.Server.ENTITY_STATUS,
			PacketType.Play.Server.ATTACH_ENTITY, PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.ENTITY_EFFECT, PacketType.Play.Server.REMOVE_ENTITY_EFFECT,
			PacketType.Play.Server.BLOCK_BREAK_ANIMATION, PacketType.Play.Server.UPDATE_ENTITY_NBT, PacketType.Play.Server.COMBAT_EVENT };

	private ProtocolManager manager;
	private Listener bukkitListener;
	private PacketAdapter protocolListener;
	protected final Policy policy;

	/**
	 * The current entity visibility policy.
	 * @author Kristian
	 *
	 */
	public enum Policy {
		WHITELIST, BLACKLIST;
	}

	public EntityHider(Plugin plugin, Policy policy) {
		Preconditions.checkNotNull(plugin, "plugin cannot be NULL.");
		this.policy = policy;
		this.manager = ProtocolLibrary.getProtocolManager();
		plugin.getServer().getPluginManager().registerEvents(this.bukkitListener = constructBukkit(), plugin);
		this.manager.addPacketListener(this.protocolListener = constructProtocol(plugin));
	}

	protected boolean setVisibility(Player observer, int entityID, boolean visible) {
		switch (this.policy) {
		case BLACKLIST:
			return !setMembership(observer, entityID, !visible);
		case WHITELIST:
			return setMembership(observer, entityID, visible);
		}
		throw new IllegalArgumentException("Unknown policy: " + this.policy);
	}

	protected boolean setMembership(Player observer, int entityID, boolean member) {
		if (member)
			return (this.observerEntityMap.put(Integer.valueOf(observer.getEntityId()), Integer.valueOf(entityID), Boolean.valueOf(true)) != null);
		return (this.observerEntityMap.remove(Integer.valueOf(observer.getEntityId()), Integer.valueOf(entityID)) != null);
	}

	protected boolean getMembership(Player observer, int entityID) {
		return this.observerEntityMap.contains(Integer.valueOf(observer.getEntityId()), Integer.valueOf(entityID));
	}

	protected boolean isVisible(Player observer, int entityID) {
		boolean presence = getMembership(observer, entityID);
		return (this.policy == Policy.WHITELIST) ? presence : (!presence);
	}

	protected void removeEntity(Entity entity, boolean destroyed) {
		int entityID = entity.getEntityId();
		for (Map<Integer, Boolean> maps : (Iterable<Map<Integer, Boolean>>) this.observerEntityMap.rowMap().values())
			maps.remove(Integer.valueOf(entityID));
	}

	protected void removePlayer(Player player) {
		this.observerEntityMap.rowMap().remove(Integer.valueOf(player.getEntityId()));
	}

	private Listener constructBukkit() {
		return new Listener() {
			@EventHandler
			public void onEntityDeath(EntityDeathEvent e) {
				EntityHider.this.removeEntity(e.getEntity(), true);
			}

			@EventHandler
			public void onChunkUnload(ChunkUnloadEvent e) {
				for(Entity entity : e.getChunk().getEntities()) {
					EntityHider.this.removeEntity(entity, false);
				}

			}

			@EventHandler
			public void onPlayerQuit(PlayerQuitEvent e) {
				EntityHider.this.removePlayer(e.getPlayer());
			}
		};
	}

	private PacketAdapter constructProtocol(Plugin plugin) {
		return new PacketAdapter(plugin, ENTITY_PACKETS) {
			@Override
			public void onPacketSending(PacketEvent event) {
				int index = (event.getPacketType() == PacketType.Play.Server.COMBAT_EVENT) ? 1 : 0;
				Integer entityID = event.getPacket().getIntegers().readSafely(index);
				if (entityID != null && !EntityHider.this.isVisible(event.getPlayer(), entityID.intValue()))
					event.setCancelled(true);
			}
		};
	}

	public final boolean toggleEntity(Player observer, Entity entity) {
		if (isVisible(observer, entity.getEntityId()))
			return hideEntity(observer, entity);
		return !showEntity(observer, entity);
	}

	public final boolean showEntity(Player observer, Entity entity) {
		validate(observer, entity);
		boolean hiddenBefore = !setVisibility(observer, entity.getEntityId(), true);
		if (this.manager != null && hiddenBefore)
			this.manager.updateEntity(entity, Arrays.asList(new Player[] { observer }));
		return hiddenBefore;
	}

	public final boolean hideEntity(Player observer, Entity entity) {
		validate(observer, entity);
		boolean visibleBefore = setVisibility(observer, entity.getEntityId(), false);
		if (visibleBefore) {
			PacketContainer destroyEntity = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
			destroyEntity.getIntegerArrays().write(0, new int[] { entity.getEntityId() });
			try {
				this.manager.sendServerPacket(observer, destroyEntity);
			} catch (InvocationTargetException e) {
				throw new RuntimeException("Cannot send server packet.", e);
			}
		}
		return visibleBefore;
	}

	public final boolean canSee(Player observer, Entity entity) {
		validate(observer, entity);
		return isVisible(observer, entity.getEntityId());
	}

	private void validate(Player observer, Entity entity) {
		Preconditions.checkNotNull(observer, "observer cannot be NULL.");
		Preconditions.checkNotNull(entity, "entity cannot be NULL.");
	}

	public Policy getPolicy() {
		return this.policy;
	}

	public void close() {
		if (this.manager != null) {
			HandlerList.unregisterAll(this.bukkitListener);
			this.manager.removePacketListener(this.protocolListener);
			this.manager = null;
		}
	}
}
