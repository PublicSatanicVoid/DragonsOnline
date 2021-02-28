package mc.dragons.core.gameobject.npc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.NPCAddon;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.npc.NPC.NPCType;
import mc.dragons.core.gameobject.npc.NPCConditionalActions.NPCTrigger;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;

public class NPCClass extends GameObject {
	private LootTable lootTable;
	private NPCConditionalActions[] conditionals = new NPCConditionalActions[NPCTrigger.values().length];
	private List<NPCAddon> addons;

	@SuppressWarnings("unchecked")
	public NPCClass(StorageManager storageManager, StorageAccess storageAccess) {
		super(storageManager, storageAccess);
		LOGGER.fine("Constructing NPC Class (" + storageManager + ", " + storageAccess + ")");
		lootTable = new LootTable(this);
		int i = 0;
		for(NPCTrigger trigger : NPCTrigger.values()) {
			conditionals[i] = new NPCConditionalActions(trigger, this);
			i++;
		}
		addons = ((List<String>) getData("addons")).stream()
				.map(addonName -> (NPCAddon) Dragons.getInstance().getAddonRegistry().getAddonByName(addonName))
				.collect(Collectors.toList());
	}

	private void saveAddons() {
		setData("addons", addons.stream().map(a -> a.getName()).collect(Collectors.toList()));
	}

	public void addAddon(NPCAddon addon) {
		addons.add(addon);
		saveAddons();
	}

	public void removeAddon(NPCAddon addon) {
		addons.remove(addon);
		saveAddons();
	}

	public List<NPCAddon> getAddons() {
		return addons;
	}

	public void handleMove(NPC npc, Location loc) {
		addons.forEach(addon -> addon.onMove(npc, loc));
	}

	public void handleTakeDamage(NPC on, GameObject from, double amt) {
		addons.forEach(addon -> addon.onTakeDamage(on, from, amt));
	}

	public void handleDealDamage(NPC from, GameObject to, double amt) {
		addons.forEach(addon -> addon.onDealDamage(from, to, amt));
	}

	public void handleDeath(NPC npc) {
		addons.forEach(addon -> addon.onDeath(npc));
	}

	public void executeConditionals(NPCTrigger trigger, User user, NPC npc) {
		user.debug("Executing conditionals");
		for(NPCConditionalActions conditionalAction : conditionals) {
			if (conditionalAction.getTrigger() == trigger) {
				conditionalAction.executeConditionals(user, npc);
			}
		}
	}

	public NPCConditionalActions getConditionalActions(NPCTrigger trigger) {
		for(NPCConditionalActions conditionalAction : conditionals) {
			if (conditionalAction.getTrigger() == trigger) {
				return conditionalAction;
			}
		}
		return null;
	}

	public LootTable getLootTable() {
		return lootTable;
	}

	public void updateLootTable(String regionName, String itemName, double lootChancePercent) {
		Document lootTableData = (Document) storageAccess.get("lootTable");
		Document regionLoot = (Document) lootTableData.get(regionName);
		if (regionLoot == null) {
			lootTableData.append(regionName, new Document(itemName, lootChancePercent));
			return;
		}
		regionLoot.append(itemName, Double.valueOf(lootChancePercent));
		update(new Document("lootTable", lootTableData));
	}

	public void deleteFromLootTable(String regionName, String itemName) {
		Document lootTableData = (Document) storageAccess.get("lootTable");
		Document regionLoot = (Document) lootTableData.get(regionName);
		if (regionLoot == null) {
			return;
		}
		regionLoot.remove(itemName);
		update(new Document("lootTable", lootTableData));
	}

	public String getClassName() {
		return (String) getData("className");
	}

	public String getName() {
		return (String) getData("name");
	}

	public void setName(String displayName) {
		setData("name", displayName);
	}

	public EntityType getEntityType() {
		return EntityType.valueOf((String) getData("entityType"));
	}

	public void setEntityType(EntityType type) {
		setData("entityType", type.toString());
	}

	public Material getHeldItemType() {
		Object holding = getData("holding");
		if (holding == null) {
			return null;
		}
		return Material.valueOf((String) holding);
	}

	public void setHeldItemType(Material type) {
		setData("holding", type.toString());
	}

	public boolean isImmortal() {
		return (boolean) getData("immortal");
	}

	public void setImmortal(boolean immortal) {
		setData("immortal", immortal);
	}

	public boolean hasAI() {
		return (boolean) getData("ai");
	}

	public void setAI(boolean hasAI) {
		setData("ai", hasAI);
	}

	public double getMaxHealth() {
		return (double) getData("maxHealth");
	}

	public void setMaxHealth(double maxHealth) {
		setData("maxHealth", maxHealth);
	}

	public int getLevel() {
		return (int) getData("level");
	}

	public void setLevel(int level) {
		setData("level", level);
	}

	public NPCType getNPCType() {
		return NPCType.valueOf((String) getData("npcType"));
	}

	public void setNPCType(NPCType npcType) {
		setData("npcType", npcType.toString());
	}

	public Map<Attribute, Double> getCustomAttributes() {
		Map<Attribute, Double> result = new HashMap<>();
		for (Entry<String, Object> attribute : (Iterable<Entry<String, Object>>) ((Document) getData("attributes")).entrySet()) {
			result.put(Attribute.valueOf(attribute.getKey()), (double) attribute.getValue());
		}
		return result;
	}

	public void setCustomAttribute(Attribute attribute, double base) {
		Document attributes = (Document) getData("attributes");
		attributes.append(attribute.toString(), base);
		storageAccess.update(new Document("attributes", attributes));
	}

	public void removeCustomAttribute(Attribute attribute) {
		Document attributes = (Document) getData("attributes");
		attributes.remove(attribute.toString());
		storageAccess.update(new Document("attributes", attributes));
	}
}
