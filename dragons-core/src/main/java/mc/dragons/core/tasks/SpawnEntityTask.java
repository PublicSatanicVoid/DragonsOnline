package mc.dragons.core.tasks;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
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

public class SpawnEntityTask extends BukkitRunnable {
	private Logger LOGGER = Dragons.getInstance().getLogger();

	private final double SPAWN_RADIUS = 15.0D;

	private Dragons plugin;

	private NPCLoader npcLoader;

	private RegionLoader regionLoader;

	private NPCClassLoader npcClassLoader;

	private Comparator<String> comparingNPCClassForLevel(int level) {
		return (c1, c2) -> Math.abs(this.npcClassLoader.getNPCClassByClassName(c1).getLevel() - level) - Math.abs(this.npcClassLoader.getNPCClassByClassName(c2).getLevel() - level);
	}

	private Comparator<Entry<String, Double>> comparingSpawnRateEntryForLevel(int level) {
		return (e1, e2) -> comparingNPCClassForLevel(level).compare(e1.getKey(), e2.getKey());
	}

	public SpawnEntityTask(Dragons instance) {
		this.plugin = instance;
		this.npcClassLoader = GameObjectType.NPC_CLASS.<NPCClass, NPCClassLoader>getLoader();
		this.npcLoader = GameObjectType.NPC.<NPC, NPCLoader>getLoader();
		this.regionLoader = GameObjectType.REGION.<Region, RegionLoader>getLoader();
	}

	@Override
	public void run() {
		if (!this.plugin.getServerOptions().isCustomSpawningEnabled())
			return;
		long start = System.currentTimeMillis();
		for (User user : UserLoader.allUsers()) {
			if (user.getPlayer() == null || user.getPlayer().getGameMode() == GameMode.CREATIVE || user.getPlayer().getGameMode() == GameMode.SPECTATOR)
				continue;
			World world = user.getPlayer().getWorld();
			int cap = -1;
			int radiusCap = -1;
			Location center = user.getPlayer().getLocation();
			Set<Region> regions = this.regionLoader.getRegionsByLocation(center);
			Map<String, Double> spawnRates = new HashMap<>();
			Vector min = center.toVector();
			Vector max = center.toVector();
			boolean nospawn = false;
			for (Region region : regions) {
				if (Boolean.valueOf(region.getFlags().getString("nospawn")).booleanValue()) {
					nospawn = true;
					break;
				}
				for (Entry<String, Double> entry : (Iterable<Entry<String, Double>>) region.getSpawnRates().entrySet()) {
					if (entry.getValue() > spawnRates.getOrDefault(entry.getKey(), 0.0D))
						spawnRates.put(entry.getKey(), entry.getValue());
				}
				int regionCap = Integer.valueOf(region.getFlags().getString("spawncap")).intValue();
				if ((regionCap < cap && regionCap != -1) || cap == -1)
					cap = regionCap;
				int theRadiusCap = Integer.valueOf(region.getFlags().getString("nearbyspawncap")).intValue();
				if ((theRadiusCap < radiusCap && theRadiusCap != -1) || radiusCap == -1)
					radiusCap = theRadiusCap;
				min = Vector.getMinimum(min, region.getMin().toVector());
				max = Vector.getMaximum(max, region.getMax().toVector());
			}
			Map<String, Double> optimizedSpawnRates = spawnRates.entrySet().stream()
					.sorted(comparingSpawnRateEntryForLevel(user.getLevel()))
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue, 
							(oldValue, newValue) -> oldValue, java.util.LinkedHashMap::new));
			if (nospawn)
				continue;
			long entityCount = 0L;
			if (cap != -1) {
				double searchRadius = Math.max(min.distance(center.toVector()), max.distance(center.toVector()));
				entityCount = user.getPlayer().getNearbyEntities(searchRadius, searchRadius, searchRadius).stream().map(e -> NPCLoader.fromBukkit(e)).filter(n -> (n != null))
						.filter(n -> (n.getNPCType() == NPC.NPCType.HOSTILE)).count();
			}
			if (entityCount > cap && cap != -1)
				continue;
			long entityRadiusCount = 0L;
			if (radiusCap != -1) {
				double searchRadius = 20.0D;
				entityRadiusCount = user.getPlayer().getNearbyEntities(searchRadius, searchRadius, searchRadius).stream().map(e -> NPCLoader.fromBukkit(e)).filter(n -> (n != null))
						.filter(n -> (n.getNPCType() == NPC.NPCType.HOSTILE)).count();
			}
			if (entityRadiusCount > radiusCap && radiusCap != -1)
				continue;
			int priority = 1;
			for (Entry<String, Double> spawnRate : optimizedSpawnRates.entrySet()) {
				boolean spawn = (Math.random() <= spawnRate.getValue() / Math.sqrt(priority) * 100.0D);
				if (spawn) {
					double xOffset = Math.signum(Math.random() - 0.5D) * (5.0D + Math.random() * SPAWN_RADIUS);
					double zOffset = Math.signum(Math.random() - 0.5D) * (5.0D + Math.random() * SPAWN_RADIUS);
					double yOffset = 0.0D;
					Location loc = user.getPlayer().getLocation().add(xOffset, yOffset, zOffset);
					loc = BlockUtil.getClosestGroundXZ(loc).add(0.0D, 1.0D, 0.0D);
					this.npcLoader.registerNew(world, loc, spawnRate.getKey());
				}
				entityCount++;
				entityRadiusCount++;
				priority++;
				if ((entityCount > cap && cap != -1) || (entityRadiusCount > radiusCap && radiusCap != -1))
					break;
			}
		}
		long end = System.currentTimeMillis();
		this.LOGGER.fine("Ran entity spawn task in " + (end - start) + "ms");
	}
}
