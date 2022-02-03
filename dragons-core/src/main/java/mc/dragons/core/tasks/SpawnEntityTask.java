package mc.dragons.core.tasks;

import static mc.dragons.core.util.BukkitUtil.await;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
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
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.BlockUtil;
import mc.dragons.core.util.dataholder.ReusableDataMap;

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
	private final int MAX_SPAWN_TRIES = 5;
	
//	private DragonsLogger LOGGER;

	private Dragons plugin;
	private NPCLoader npcLoader;
	private RegionLoader regionLoader;
	private NPCClassLoader npcClassLoader;
	
	private Map<String, Double> emptySpawnMap = Collections.synchronizedMap(new HashMap<>());

	private Comparator<String> comparingNPCClassForLevel(int level) {
		return (c1, c2) -> Math.abs(npcClassLoader.getNPCClassByClassName(c1).getLevel() - level) - Math.abs(npcClassLoader.getNPCClassByClassName(c2).getLevel() - level);
	}

	private Comparator<Entry<String, Double>> comparingSpawnRateEntryForLevel(int level) {
		return (e1, e2) -> comparingNPCClassForLevel(level).compare(e1.getKey(), e2.getKey());
	}

	public SpawnEntityTask(Dragons instance) {
//		LOGGER = instance.getLogger();
		plugin = instance;
		npcClassLoader = GameObjectType.NPC_CLASS.<NPCClass, NPCClassLoader>getLoader();
		npcLoader = GameObjectType.NPC.<NPC, NPCLoader>getLoader();
		regionLoader = GameObjectType.REGION.<Region, RegionLoader>getLoader();
	}
	
	/**
	 * Return spawn rates at the given location
	 * 
	 * <p>May change over time due to changing
	 * entity distribution
	 * 
	 * @apiNote Can be run asynchronously
	 * 
	 * @param center
	 * @return
	 */
	private Map<String, Double> getSpawnRates(ReusableDataMap data, Location center, int level) {
		int cap = -1;
		int radiusCap = -1;
		Set<Region> regions = regionLoader.getRegionsByLocation(center);
		Map<String, Double> spawnRates = new HashMap<>();
		Vector min = center.toVector();
		Vector max = center.toVector();
		
		data.set("cap", cap);
		data.set("radiusCap", radiusCap);
		data.set("min", min);
		data.set("max", max);
		
		// Ensure we aren't crowding the region or exceeding its given capacities.
		// Rough estimates will do, as it would be impractical performance-wise
		// to check within the exact bounds of each region: this would require
		// M*N region bound checks, for M regions in the floor and N spawned entities 
		// in the floor.
		
		for (Region region : regions) {
			if (Boolean.valueOf(region.getFlags().getString(Region.FLAG_NOSPAWN))) {
				return emptySpawnMap;
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
				.sorted(comparingSpawnRateEntryForLevel(level))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, 
						(oldValue, newValue) -> oldValue, java.util.LinkedHashMap::new));
		data.set("cap", cap);
		data.set("radiusCap", radiusCap);
		data.set("min", min);
		data.set("max", max);
		return optimizedSpawnRates;
	}
	
	@Override
	public void run() {
		if (!plugin.getServerOptions().isCustomSpawningEnabled()) {
			return;
		}
		int margin = plugin.getServerOptions().getCustomSpawnMargin();

		ReusableDataMap data = new ReusableDataMap();
		Map<Location, String> spawns = new HashMap<>();
		Bukkit.getOnlinePlayers().stream().map(p -> UserLoader.fromPlayer(p)).forEach(user -> {
			if (user.getPlayer().getGameMode() == GameMode.CREATIVE || user.getPlayer().getGameMode() == GameMode.SPECTATOR || user.isGodMode() || user.isVanished()) {
				return;
			}
			
			Location center = user.getPlayer().getLocation();
			
			await(() -> getSpawnRates(data, center, user.getLevel()), spawnRates -> {
				
				/* SYNC */
				
				long entityCount = 0L;
				int cap = data.get("cap");
				int radiusCap = data.get("radiusCap");
				Vector min = data.get("min");
				Vector max = data.get("max");
				
				data.set("entityRadiusCount", 0L);
				
				if (cap != -1) {
					double searchRadius = Math.max(min.distance(center.toVector()), max.distance(center.toVector()));
					entityCount = user.getPlayer().getNearbyEntities(searchRadius, searchRadius, searchRadius).stream().map(e -> NPCLoader.fromBukkit(e)).filter(n -> (n != null))
							.filter(n -> (n.getNPCType() == NPC.NPCType.HOSTILE)).count();
				}
				if (entityCount > cap && cap != -1) {
					return;
				}
				long entityRadiusCount = 0L;
				if (radiusCap != -1) {
					entityRadiusCount = user.getPlayer().getNearbyEntities(margin, margin, margin).stream().map(e -> NPCLoader.fromBukkit(e)).filter(n -> (n != null))
							.filter(n -> (n.getNPCType() == NPC.NPCType.HOSTILE)).count();
				}
				
				long fEntityCount = entityCount;
				long fEntityRadiusCount = entityRadiusCount;
				
				await(() -> {
					
					/* ASYNC */
					
					long modifiedEntityCount = fEntityCount;
					long modifiedEntityRadiusCount = fEntityRadiusCount;
					
					if (modifiedEntityRadiusCount > radiusCap && radiusCap != -1) {
						return emptySpawnMap;
					}
					
					int priority = 1;
					for (Entry<String, Double> spawnRate : spawnRates.entrySet()) {
						boolean spawn = Math.random() <= spawnRate.getValue() / Math.sqrt(priority) * 100.0D;
						if (spawn) {
							int tries = 0;
							while(tries < MAX_SPAWN_TRIES) {
								double xOffset = Math.signum(Math.random() - 0.5D) * (5.0D + Math.random() * SPAWN_RADIUS);
								double zOffset = Math.signum(Math.random() - 0.5D) * (5.0D + Math.random() * SPAWN_RADIUS);
								double yOffset = 0.0D;
								Location loc = user.getPlayer().getLocation().add(xOffset, yOffset, zOffset);
								loc = BlockUtil.getClosestAirXZ(loc); //.add(0.0D, 1.0D, 0.0D);
								if(loc.getBlock().getType().isSolid()) {
									tries++;
									continue;
								}
								// Schedule for spawning
								spawns.put(loc, spawnRate.getKey());
								break;
							}
						}
						modifiedEntityCount++;
						modifiedEntityRadiusCount++;
						priority++;
						if (modifiedEntityCount > cap && cap != -1 || modifiedEntityRadiusCount > radiusCap && radiusCap != -1) {
							break;
						}
					}
					return spawns;
				}, finalSpawns -> {
					
					/* SYNC */
					
					finalSpawns.forEach((loc, npcClassName) -> {
						npcLoader.registerNew(((Location) loc).getWorld(), (Location) loc, (String) npcClassName);
					});
				});
				
			});
			
			data.clear();
		});
	}
}
