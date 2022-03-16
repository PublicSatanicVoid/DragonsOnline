package mc.dragons.anticheat.event;

import static mc.dragons.core.util.BukkitUtil.syncPeriodic;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

import mc.dragons.anticheat.DragonsAntiCheat;
import mc.dragons.anticheat.util.ACUtil;
import mc.dragons.core.tasks.LagMeter;
import mc.dragons.core.util.MathUtil;
import mc.dragons.core.util.StringUtil;
import net.md_5.bungee.api.ChatMessageType;

/**
 * Thwarts ping spoofing and similar attacks by verifying that players are
 * interacting plausibly based on what they could see / what was in range at
 * the time of the interaction.
 * 
 * Interactions include attacking as well as right-clicking, etc.
 * 
 * @author Adam
 *
 */
public class CombatRewinder {
	public Map<Player, Long> debug_pingSpoof;
	public Map<Player, Boolean> debug_hitStats;
	public Map<Player, Integer> debug_hitCount;
	public Map<Player, Integer> debug_rejectedCount;
	
	public double rayTraceTolerance = 0.2;
	
	private DragonsAntiCheat plugin;
	private ProtocolManager protocolManager;
	private Map<Player, Map<Long, Long>> lastSent;
	private Map<Player, Long> lastPing;
	private Map<Entity, Map<Long, Location>> entityHistory;
	private Map<Entity, Map<Long, Location>> lookHistory;
	private long lastCleanup = 0L;
	
	private static int TPS_DISABLEBELOW = 17;
	private static int MAX_PACKET_ID = 10000; // Should be lower than what Bukkit uses for its packet IDs, i.e. time in ms
	private static int PING_POLLING_PERIOD_TICKS = 10;
	private static int MAX_RETENTION_MS = 2000; // How long we retain movement records. NOTE: These are kept for ALL entities that move!
	private static int CLEANUP_INTERVAL_MS = 1000; // How often we remove old (> MAX_RETENTION_MS) movement records
	private static double MAX_DISTANCE = 4.5; // Max reach distance. Increase to be more lenient
	private static boolean ENFORCE_LOOKING_DIR = true; // Whether we should check eye location in addition to distance. More error-prone.
	
	private static double MAX_DISTANCE_SQ = MAX_DISTANCE * MAX_DISTANCE;
	
	private static double RAYTRACE_STEP = 0.01;
	
	public CombatRewinder(DragonsAntiCheat plugin) {
		this.plugin = plugin;
		lastSent = new HashMap<>();
		lastPing = new HashMap<>();
		debug_hitStats = new HashMap<>();
		debug_pingSpoof = new HashMap<>();
		debug_hitCount = new HashMap<>();
		debug_rejectedCount = new HashMap<>();
		entityHistory = new HashMap<>();
		lookHistory = new HashMap<>();
		protocolManager = ProtocolLibrary.getProtocolManager();
		
		initKeepAliveTask();
		initDebugStatsTask();
		
		// Packet level interaction handling
		protocolManager.addPacketListener(new PacketAdapter(plugin, new PacketType[] { 
				PacketType.Play.Client.USE_ENTITY, PacketType.Play.Client.KEEP_ALIVE, PacketType.Play.Server.KEEP_ALIVE,
				PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK, PacketType.Play.Client.BOAT_MOVE, PacketType.Play.Client.VEHICLE_MOVE,
				PacketType.Play.Server.REL_ENTITY_MOVE, PacketType.Play.Server.REL_ENTITY_MOVE_LOOK  }) {
			
			@Override
			public void onPacketReceiving(PacketEvent event) {
				handlePacketReceive(event);
			}
			
			// Build record of player interacts
			@Override
			public void onPacketSending(PacketEvent event) {
				handlePacketSend(event);
			}
		});
	}
	
	private void initKeepAliveTask() {
		// Check ping more frequently to increase accuracy
		syncPeriodic(() -> {
			for(Player p : Bukkit.getOnlinePlayers()) {
				PacketContainer packet = new PacketContainer(PacketType.Play.Server.KEEP_ALIVE);
				packet.getModifier().writeDefaults();
				long id = (long) Math.random() * MAX_PACKET_ID;
				packet.getLongs().write(0, id);
				lastSent.computeIfAbsent(p, pp -> new HashMap<>()).put(id, System.currentTimeMillis());
				try {
					protocolManager.sendServerPacket(p, packet);
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}, PING_POLLING_PERIOD_TICKS);
	}
	
	private void initDebugStatsTask() {
		syncPeriodic(() -> {
			for(Player p : Bukkit.getOnlinePlayers()) {
				if(!debug_hitStats.getOrDefault(p, false)) continue;
				double rej = debug_rejectedCount.getOrDefault(p, 0);
				double tot = debug_hitCount.getOrDefault(p, 0);
				int ping = getInstantaneousPing(p);
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, StringUtil.plainText("Hit rejection ratio: " + (tot == 0 ? "N/A" : MathUtil.round(100 * rej / tot) + "%") + " (ping: " + ping + "ms)"));
			}
		}, 20);
	}
	
	/**
	 * 
	 * @param player
	 * @param packet
	 * @return Whether to cancel the packet
	 */
	private boolean handleKeepAliveReceive(Player player, PacketContainer packet) {
		long id = packet.getLongs().read(0);
		if(packet.getLongs().read(0) <= MAX_PACKET_ID) {
			lastPing.put(player, System.currentTimeMillis() - lastSent.get(player).get(id));
			return true; // If it was sent by us, don't pass it on to Bukkit
		}
		return false;
	}
	
	private void handleMove(Player player) {
		long time = System.currentTimeMillis();
		Map<Long, Location> history = entityHistory.computeIfAbsent(player, p -> new TreeMap<>());
		history.putIfAbsent(time, player.getLocation());
		Map<Long, Location> lookHistory = null;
		lookHistory = CombatRewinder.this.lookHistory.computeIfAbsent(player, p -> new TreeMap<>());
		lookHistory.putIfAbsent(time, player.getEyeLocation());
		if(time - lastCleanup > CLEANUP_INTERVAL_MS) {
			entityHistory.forEach((k,h) -> h.keySet().removeIf(t -> t < time - MAX_RETENTION_MS));
			CombatRewinder.this.lookHistory.forEach((k,h) -> h.keySet().removeIf(t -> t < time - MAX_RETENTION_MS));
			lastCleanup = time;
		}
	}
	
	/**
	 * 
	 * @param player
	 * @param packet
	 * @return Whether to cancel the packet
	 */
	private boolean handleInteract(Player player, PacketContainer packet) {
		if(LagMeter.getEstimatedTPS() < TPS_DISABLEBELOW) return false;
		
		debug_hitCount.put(player, debug_hitCount.getOrDefault(player, 0) + 1);
		
		// Verify an interaction
		World world = player.getWorld();
		Entity target = packet.getEntityModifier(world).read(0);
		if(target == null) {
			return false;
		}
		int playerPing = getInstantaneousPing(player);
		
		Location[] playerRewound = interpolateLocation(player, playerPing, false);
		Location[] playerLookRewound = interpolateLocation(player, playerPing, true);
		Location[] targetRewound = interpolateLocation(target, playerPing, false);
	
		boolean cancel = true;
		double dist2 = -1.0;
		
		// Loop through possibilities
		for(int i = 0; i < playerRewound.length; i++) {
			Location playerLook = playerLookRewound[i];
			for(Location targetPos : targetRewound) {
				BoundingBox targetRewoundBB = target.getBoundingBox().shift(targetPos.clone().subtract(target.getLocation()));
				targetRewoundBB.expand(rayTraceTolerance);
				dist2 = playerLook.distanceSquared(targetPos);
				if(dist2 < MAX_DISTANCE_SQ && !ENFORCE_LOOKING_DIR) {
					cancel = false;
					break;
				}
				if(dist2 > MAX_DISTANCE_SQ) {
					continue;
				}
				if(ENFORCE_LOOKING_DIR) {
					boolean intersects = rayTrace(playerLook, playerLook.getDirection(), targetRewoundBB);
					if(intersects) {
						cancel = false;
						break;
					}
				}
			}
		}
		
		if(cancel) {
			debug_rejectedCount.put(player, debug_rejectedCount.getOrDefault(player, 0) + 1);
			CombatRewinder.this.plugin.debug(player, "CombatRewind | Cancelled interaction (ping=" + playerPing + ", dist2=" + MathUtil.round(dist2) + ")");
			return true;
//			placeMarker(player, playerRewound, "YOU (-" + playerPing + "ms)");
//			placeMarker(player, targetRewound, "TARGET (-" + (playerPing) + "ms)");
		}
		return false;
	}
	
	private void handlePacketReceive(PacketEvent event) {
		Player player = event.getPlayer();
		PacketContainer packet = event.getPacket();
		
		// Update player ping
		if(event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
			event.setCancelled(handleKeepAliveReceive(player, packet));
		}
		
		else if(event.getPacketType() == PacketType.Play.Client.POSITION
				|| event.getPacketType() == PacketType.Play.Client.POSITION_LOOK
				|| event.getPacketType() == PacketType.Play.Client.BOAT_MOVE
				|| event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {
			handleMove(player);
		}
		
		else {
			event.setCancelled(handleInteract(player, packet));
		}
	}
	
	private void handlePacketSend(PacketEvent event) {
		PacketType type = event.getPacketType();
		PacketContainer packet = event.getPacket();
		if(type != PacketType.Play.Server.KEEP_ALIVE) {
			Entity entity = packet.getEntityModifier(event.getPlayer().getWorld()).read(0);
			long time = System.currentTimeMillis();
			Map<Long, Location> history = entityHistory.computeIfAbsent(entity, e -> new TreeMap<>());
			history.putIfAbsent(time, entity.getLocation());
			if(time - lastCleanup > CLEANUP_INTERVAL_MS) {
				entityHistory.forEach((k,h) -> h.keySet().removeIf(t -> t < time - MAX_RETENTION_MS));
				CombatRewinder.this.lookHistory.forEach((k,h) -> h.keySet().removeIf(t -> t < time - MAX_RETENTION_MS));
				lastCleanup = time;
			}
		}
	}
	
//	private void placeMarker(Player phaseFor, Location loc, String title) {
//		if(plugin.isDebug()) {
//			sync(() -> {
//				LivingEntity dummy = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
//				dummy.setCustomName(title);
//				dummy.setCustomNameVisible(true);
//				dummy.setAI(false);
//				dummy.setGravity(false);
//				for(Player p : Bukkit.getOnlinePlayers()) {
//					if(p.equals(phaseFor)) continue;
//					plugin.getDragonsInstance().getEntityHider().hideEntity(p, dummy);
//				}
//				sync(() -> dummy.remove(), 20 * 5);
//			});
//		}
//	}
	
	public int getInstantaneousPing(Player player) {
		if(debug_pingSpoof.containsKey(player)) 
			return (int) (long) debug_pingSpoof.get(player);
		if(!lastPing.containsKey(player)) return -1;
		return (int) (long) lastPing.get(player);
	}
	
	// TODO: Maybe quantify uncertainty based on how far apart adjacent measurements are, and/or entity velocity
	// For now, we return the two last saved locations and an interpolated one. For 99% of cases this should be enough
	public Location[] interpolateLocation(Entity entity, int ms, boolean look) {
		long last = 0;
		long now = System.currentTimeMillis();
		long search = now - ms;
		Location buf = null;
		if(plugin.isDebug()) {
			plugin.getLogger().info("-- Interpolate " + StringUtil.entityToString(entity) + " -" + ms + " (look=" + look + ") (search=" + search + ")");
		}
		for(Entry<Long, Location> entry : (look ? lookHistory : entityHistory).getOrDefault(entity, new HashMap<>()).entrySet()) {
			if(plugin.isDebug()) {
				plugin.getLogger().info(entry.getKey() + " -> " + StringUtil.locToString(entry.getValue()));
			}
			long test = entry.getKey();
			Location loc = entry.getValue();
			if(test >= search) {
				long lerpa = search - last;
				long lerpb = test - search;
				double weighta = (double) lerpa / (lerpa + lerpb);
				double weightb = (double) lerpb / (lerpa + lerpb);
				double lerpx = weighta * buf.getX() + weightb * loc.getX();
				double lerpy = weighta * buf.getY() + weightb * loc.getY();
				double lerpz = weighta * buf.getZ() + weightb * loc.getZ();
				return new Location[] {buf, loc, buf.clone().zero().add(lerpx, lerpy, lerpz)};
			}
			last = test;
			buf = loc;
		}
		if(plugin.isDebug()) {
			plugin.getLogger().warning("Couldn't interpolate " + StringUtil.entityToString(entity) + " -" + ms + "ms (look=" + look + ")");
		}
		return new Location[] { look ? ((LivingEntity) entity).getEyeLocation() : entity.getLocation() };
	}
	
	public boolean rayTrace(Location from, Vector dir, BoundingBox to) {
		Vector buf = from.toVector();
		Vector fromVec = buf.clone();
		Vector step = dir.clone().normalize().multiply(RAYTRACE_STEP);
		to.expand(rayTraceTolerance);
		while(true) {
			if(to.contains(buf)) return true;
			if(buf.distanceSquared(fromVec) > MAX_DISTANCE_SQ) return false;
			buf.add(step);
			if(ACUtil.isSolid(buf.toLocation(from.getWorld()).getBlock())) return false;
//			if(plugin.isDebug()) {
//				Vector bufclone = buf.clone();
//				BukkitTask task = syncPeriodic(() -> from.getWorld().playEffect(bufclone.toLocation(from.getWorld()), Effect.MOBSPAWNER_FLAMES, 0), 1000);
//				sync(() -> task.cancel(), 20 * 5);
//			}
		}
	}
}
