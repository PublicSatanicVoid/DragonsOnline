package mc.dragons.core.tasks;

import static mc.dragons.core.util.BukkitUtil.async;
import static mc.dragons.core.util.BukkitUtil.sync;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCClass;
import mc.dragons.core.gameobject.npc.NPCClassLoader;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.region.RegionLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.BlockUtil;

/**
 * Periodically spawns entities around players, based on regional
 * spawn rates.
 * 
 * One of the most performance-intensive parts of DragonsOnline,
 * according to recent profiling results.
 * 
 * @author Adam
 *
 */
public class SpawnEntityTask extends BukkitRunnable {
	private final double SPAWN_RADIUS = 15.0D;
	
	private Logger LOGGER;

	private Dragons plugin;
	private NPCLoader npcLoader;
	private RegionLoader regionLoader;
	private NPCClassLoader npcClassLoader;

	private Comparator<String> comparingNPCClassForLevel(int level) {
		return (c1, c2) -> Math.abs(npcClassLoader.getNPCClassByClassName(c1).getLevel() - level) - Math.abs(npcClassLoader.getNPCClassByClassName(c2).getLevel() - level);
	}

	private Comparator<Entry<String, Double>> comparingSpawnRateEntryForLevel(int level) {
		return (e1, e2) -> comparingNPCClassForLevel(level).compare(e1.getKey(), e2.getKey());
	}

	public SpawnEntityTask(Dragons instance) {
		LOGGER = instance.getLogger();
		plugin = instance;
		npcClassLoader = GameObjectType.NPC_CLASS.<NPCClass, NPCClassLoader>getLoader();
		npcLoader = GameObjectType.NPC.<NPC, NPCLoader>getLoader();
		regionLoader = GameObjectType.REGION.<Region, RegionLoader>getLoader();
	}

	@Override
	public void run() {
		if (!plugin.getServerOptions().isCustomSpawningEnabled()) {
			return;
		}
		int margin = plugin.getServerOptions().getCustomSpawnMargin();
		long start = System.currentTimeMillis();
		
		async(() -> {
			Map<Location, String> spawns = new HashMap<>();
			for (User user : UserLoader.allUsers()) {
				if (user.getPlayer() == null || user.getPlayer().getGameMode() == GameMode.CREATIVE || user.getPlayer().getGameMode() == GameMode.SPECTATOR) {
					continue;
				}
				int cap = -1;
				int radiusCap = -1;
				Location center = user.getPlayer().getLocation();
				Set<Region> regions = regionLoader.getRegionsByLocation(center);
				Map<String, Double> spawnRates = new HashMap<>();
				Vector min = center.toVector();
				Vector max = center.toVector();
				
				// Ensure we aren't crowding the region or exceeding its given capacities.
				// Rough estimates will do, as it would be impractical performance-wise
				// to check within the exact bounds of each region: this would require
				// M*N region bound checks, for M regions in the floor and N spawned entities 
				// in the floor.
				
				boolean nospawn = false;
				for (Region region : regions) {
					if (Boolean.valueOf(region.getFlags().getString(Region.FLAG_NOSPAWN))) {
						nospawn = true;
						break;
					}
					for (Entry<String, Double> entry : (Iterable<Entry<String, Double>>) region.getSpawnRates().entrySet()) {
						if (entry.getValue() > spawnRates.getOrDefault(entry.getKey(), 0.0D)) {
							spawnRates.put(entry.getKey(), entry.getValue());
						}
					}
					int regionCap = Integer.valueOf(region.getFlags().getString(Region.FLAG_SPAWNCAP));
					if (regionCap < cap && regionCap != -1 || cap == -1) {
						cap = regionCap;
					}
					int theRadiusCap = Integer.valueOf(region.getFlags().getString(Region.FLAG_NEARBYSPAWNCAP)).intValue();
					if (theRadiusCap < radiusCap && theRadiusCap != -1 || radiusCap == -1) {
						radiusCap = theRadiusCap;
					}
					min = Vector.getMinimum(min, region.getMin().toVector());
					max = Vector.getMaximum(max, region.getMax().toVector());
				}
				Map<String, Double> optimizedSpawnRates = spawnRates.entrySet().stream()
						.sorted(comparingSpawnRateEntryForLevel(user.getLevel()))
						.collect(Collectors.toMap(Entry::getKey, Entry::getValue, 
								(oldValue, newValue) -> oldValue, java.util.LinkedHashMap::new));
				if (nospawn) {
					continue;
				}
				long entityCount = 0L;
				if (cap != -1) {
					double searchRadius = Math.max(min.distance(center.toVector()), max.distance(center.toVector()));
					entityCount = user.getPlayer().getNearbyEntities(searchRadius, searchRadius, searchRadius).stream().map(e -> NPCLoader.fromBukkit(e)).filter(n -> (n != null))
							.filter(n -> (n.getNPCType() == NPC.NPCType.HOSTILE)).count();
				}
				if (entityCount > cap && cap != -1) {
					continue;
				}
				long entityRadiusCount = 0L;
				if (radiusCap != -1) {
					entityRadiusCount = user.getPlayer().getNearbyEntities(margin, margin, margin).stream().map(e -> NPCLoader.fromBukkit(e)).filter(n -> (n != null))
							.filter(n -> (n.getNPCType() == NPC.NPCType.HOSTILE)).count();
				}
				if (entityRadiusCount > radiusCap && radiusCap != -1) {
					continue;
				}
				
				int priority = 1;
				for (Entry<String, Double> spawnRate : optimizedSpawnRates.entrySet()) {
					boolean spawn = Math.random() <= spawnRate.getValue() / Math.sqrt(priority) * 100.0D;
					if (spawn) {
						double xOffset = Math.signum(Math.random() - 0.5D) * (5.0D + Math.random() * SPAWN_RADIUS);
						double zOffset = Math.signum(Math.random() - 0.5D) * (5.0D + Math.random() * SPAWN_RADIUS);
						double yOffset = 0.0D;
						Location loc = user.getPlayer().getLocation().add(xOffset, yOffset, zOffset);
						loc = BlockUtil.getClosestAirXZ(loc).add(0.0D, 1.0D, 0.0D);
						
						// Schedule for spawning
						spawns.put(loc, spawnRate.getKey());
					}
					entityCount++;
					entityRadiusCount++;
					priority++;
					if (entityCount > cap && cap != -1 || entityRadiusCount > radiusCap && radiusCap != -1) {
						break;
					}
				}
			}
			
			// We can do everything asynchronously except for the actual spawning
			sync(() -> {
				spawns.forEach((loc, npcClassName) -> {
					npcLoader.registerNew(loc.getWorld(), loc, npcClassName);
				});
				long end = System.currentTimeMillis();
				LOGGER.fine("Ran entity spawn task in " + (end - start) + "ms");
			});
			
		});
	}
}
