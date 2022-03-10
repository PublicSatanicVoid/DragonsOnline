package mc.dragons.core.gameobject.npc;

import static mc.dragons.core.util.BukkitUtil.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

import mc.dragons.core.Dragons;
import mc.dragons.core.bridge.PlayerNPC;

public class PlayerNPCRegistry implements Listener {
	public static final int SPAWN_RADIUS = 30;
	private static final int MIN_SPAWN_INTERVAL_MS = 200;
	private static final double EYE_TOLERANCE = 43 * Math.PI / 180;
	private Map<World, List<PlayerNPC>> registry = new HashMap<>();
	private Map<Integer, PlayerNPC> ids = new HashMap<>();
	private Map<Player, List<PlayerNPC>> spawnedFor = new HashMap<>();
	private Map<Player, List<PlayerNPC>> refreshedFor = new HashMap<>();
	private Map<Player, Long> lastChangedWorlds = new HashMap<>();
	private Map<Player, Long> lastUpdatedSpawns = new HashMap<>();
	
	public PlayerNPCRegistry(Dragons plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.ENTITY_DESTROY) {
			@Override
			public void onPacketSending(PacketEvent event) {
				PacketContainer packet = event.getPacket();
				Player player = event.getPlayer();
				int[] destroy = packet.getIntegerArrays().read(0);
				for(int id : destroy) {
					if(ids.containsKey(id)) {
						spawnedFor.computeIfAbsent(player, p -> new ArrayList<>()).remove(ids.get(id));
						refreshedFor.computeIfAbsent(player, p -> new ArrayList<>()).remove(ids.get(id));
					}
				}
			}
		});
	}
	
	public void register(PlayerNPC npc) {
		registry.computeIfAbsent(npc.getLocation().getWorld(), world -> new ArrayList<>()).add(npc);
		ids.put(npc.getEntityId(), npc);
	}

	public void unregister(PlayerNPC npc) {
		ArrayList<PlayerNPC> dummy = new ArrayList<>();
		registry.getOrDefault(npc.getLocation().getWorld(), dummy).remove(npc);
		if(ids.containsValue(npc)) {
			ids.remove(npc.getEntityId());
		}
		for(Player p : spawnedFor.keySet()) {
			spawnedFor.getOrDefault(p, dummy).remove(npc);
			refreshedFor.getOrDefault(p, dummy).remove(npc);
		}
	}
	
	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		updateSpawns(event.getPlayer(), true);
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		sync(() -> updateSpawns(event.getPlayer(), true), 20);		
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		spawnedFor.remove(event.getPlayer());
		refreshedFor.remove(event.getPlayer());
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		updateSpawns(event.getPlayer(), false);
	}
	
	public void updateSpawns(Player p, boolean force) {
		if(!force && System.currentTimeMillis() - lastUpdatedSpawns.getOrDefault(p, 0L) < MIN_SPAWN_INTERVAL_MS) return;
		if(!force && System.currentTimeMillis() - lastChangedWorlds.getOrDefault(p, 0L) < 1000) return;
		lastUpdatedSpawns.put(p, System.currentTimeMillis());
		Location ploc = p.getLocation();
		int i = 0;
		List<PlayerNPC> spawned = spawnedFor.computeIfAbsent(p, pp -> new ArrayList<>());
		List<PlayerNPC> refreshed = refreshedFor.computeIfAbsent(p, pp -> new ArrayList<>());
		for(PlayerNPC npc : registry.getOrDefault(p.getWorld(), new ArrayList<>())) {
			if(npc.getLocation().distanceSquared(ploc) > SPAWN_RADIUS * SPAWN_RADIUS) {
				refreshed.remove(npc);
			}
			if(isReallyInSight(p, npc.getEntity())) {
				if(spawned.contains(npc) && npc.getLocation().distanceSquared(ploc) < SPAWN_RADIUS * SPAWN_RADIUS
						&& !refreshed.contains(npc)) {
					sync(() -> npc.refreshRotationFor(p), i);
					refreshed.add(npc);
				}
				if(spawned.contains(npc) || npc.getLocation().distanceSquared(ploc) > SPAWN_RADIUS * SPAWN_RADIUS) {
					continue;
				}
				sync(() -> npc.spawnFor(p), i);
				spawned.add(npc);
			}
		}
	}
	
	private boolean isReallyInSight(Player p, Entity e) {
		return p.hasLineOfSight(e) && p.getEyeLocation().getDirection().angle(e.getLocation().clone().subtract(p.getLocation()).toVector()) < EYE_TOLERANCE;
	}
}
