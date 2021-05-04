package mc.dragons.core.gameobject.npc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bson.Document;
import org.bukkit.Location;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.region.RegionLoader;

/**
 * Stores probabilities of an NPC dropping a certain item on death
 * as a function of the region the NPC was located in.
 * 
 * @author Adam
 *
 */
public class LootTable {
	private static RegionLoader regionLoader = GameObjectType.REGION.getLoader();
	private static ItemClassLoader itemClassLoader = GameObjectType.ITEM_CLASS.getLoader();
	private static ItemLoader itemLoader = GameObjectType.ITEM.getLoader();
	
	private Document lootTable;

	public LootTable(NPCClass npcClass) {
		lootTable = (Document) npcClass.getStorageAccess().get("lootTable");
	}

	/**
	 * 
	 * @param loc
	 * @return The items the NPC dropped at a specified location.
	 * 
	 * @implNote Probabilities are factored in to this, so identical
	 * 	calls may not produce identical results.
	 */
	public Set<Item> getDrops(Location loc) {
		if (lootTable == null) {
			return new HashSet<>();
		}
		double multiplier = Dragons.getInstance().getServerOptions().getDropChanceMultiplier();
		Set<Region> regions = regionLoader.getRegionsByLocation(loc);
		Set<Item> drops = new HashSet<>();
		for (Region region : regions) {
			Document regionLoots = (Document) lootTable.get(region.getName());
			if (regionLoots == null) {
				continue;
			}
			for (Entry<String, Object> loot : (Iterable<Entry<String, Object>>) regionLoots.entrySet()) {
				double chance = (double) loot.getValue();
				if (Math.random() < chance * multiplier / 100.0D) {
					ItemClass itemClass = itemClassLoader.getItemClassByClassName(loot.getKey());
					Item item = itemLoader.registerNew(itemClass);
					drops.add(item);
				}
			}
		}
		return drops;
	}

	/**
	 * 
	 * @return The loot table as a nested map. Region names are mapped to 
	 * a map of item names and drop changes.
	 */
	public Map<String, Map<String, Double>> asMap() {
		if (lootTable == null) {
			return new HashMap<>();
		}
		Map<String, Map<String, Double>> result = new HashMap<>();
		for (Entry<String, Object> regions : (Iterable<Entry<String, Object>>) lootTable.entrySet()) {
			Map<String, Double> regionItemChances = new HashMap<>();
			String regionName = regions.getKey();
			Document chances = (Document) regions.getValue();
			for (Entry<String, Object> itemChance : (Iterable<Entry<String, Object>>) chances.entrySet()) {
				regionItemChances.put(itemChance.getKey(), (double) itemChance.getValue());
			}
			result.put(regionName, regionItemChances);
		}
		return result;
	}
}
