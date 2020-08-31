package mc.dragons.core.gameobject.npc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bukkit.Location;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.loader.ItemClassLoader;
import mc.dragons.core.gameobject.loader.ItemLoader;
import mc.dragons.core.gameobject.loader.RegionLoader;
import mc.dragons.core.gameobject.region.Region;

public class LootTable {
	private static RegionLoader regionLoader;
	private static ItemClassLoader itemClassLoader;
	private static ItemLoader itemLoader;
	
	private Document lootTable;

	public LootTable(NPCClass npcClass) {
		this.lootTable = (Document) npcClass.getStorageAccess().get("lootTable");
		if (regionLoader == null) {
			regionLoader = GameObjectType.REGION.<Region, RegionLoader>getLoader();
			itemClassLoader = GameObjectType.ITEM_CLASS.<ItemClass, ItemClassLoader>getLoader();
			itemLoader = GameObjectType.ITEM.<Item, ItemLoader>getLoader();
		}
	}

	public Set<Item> getDrops(Location loc) {
		if (this.lootTable == null)
			return new HashSet<>();
		Set<Region> regions = regionLoader.getRegionsByLocation(loc);
		Set<Item> drops = new HashSet<>();
		for (Region region : regions) {
			Document regionLoots = (Document) this.lootTable.get(region.getName());
			if (regionLoots == null)
				continue;
			for (Map.Entry<String, Object> loot : (Iterable<Map.Entry<String, Object>>) regionLoots.entrySet()) {
				double chance = ((Double) loot.getValue()).doubleValue();
				if (Math.random() < chance / 100.0D) {
					ItemClass itemClass = itemClassLoader.getItemClassByClassName(loot.getKey());
					Item item = itemLoader.registerNew(itemClass);
					drops.add(item);
				}
			}
		}
		return drops;
	}

	public Map<String, Map<String, Double>> asMap() {
		if (this.lootTable == null)
			return new HashMap<>();
		Map<String, Map<String, Double>> result = new HashMap<>();
		for (Map.Entry<String, Object> regions : (Iterable<Map.Entry<String, Object>>) this.lootTable.entrySet()) {
			Map<String, Double> regionItemChances = new HashMap<>();
			String regionName = regions.getKey();
			Document chances = (Document) regions.getValue();
			for (Map.Entry<String, Object> itemChance : (Iterable<Map.Entry<String, Object>>) chances.entrySet())
				regionItemChances.put(itemChance.getKey(), Double.valueOf(((Double) itemChance.getValue()).doubleValue()));
			result.put(regionName, regionItemChances);
		}
		return result;
	}
}
