package mc.dragons.core.gameobject.floor;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;

public class Floor extends GameObject {
	public Floor(StorageManager storageManager, StorageAccess storageAccess, boolean superflat) {
		super(storageManager, storageAccess);
		LOGGER.fine("Constructing floor (" + storageManager + ", " + storageAccess + ", superflat=" + superflat + ")");
		LOGGER.info("Loading floor " + getFloorName() + " [" + getWorldName() + "]");
		WorldCreator creator = WorldCreator.name(getWorldName());
		if (superflat)
			creator.type(WorldType.FLAT);
		World world = Bukkit.createWorld(creator);
		world.setGameRuleValue("announceAdvancements", "false");
		world.setGameRuleValue("commandBlockFeedback", "false");
		world.setGameRuleValue("commandBlockOutput", "false");
		world.setGameRuleValue("doMobLoot", "false");
		world.setGameRuleValue("doMobSpawning", "false");
		world.setGameRuleValue("doEntityDrops", "false");
		world.setGameRuleValue("doFireTick", "false");
		world.setGameRuleValue("keepInventory", "true");
		world.setGameRuleValue("mobGriefing", "false");
		world.setGameRuleValue("reducedDebugInfo", "false");
		world.setGameRuleValue("showDeathMessages", "false");
	}

	public String getWorldName() {
		return (String) getData("worldName");
	}

	public World getWorld() {
		return Bukkit.getWorld(getWorldName());
	}

	public String getFloorName() {
		return (String) getData("floorName");
	}

	public void setFloorName(String floorName) {
		FloorLoader.updateFloorName(getFloorName(), floorName);
		setData("floorName", floorName);
	}

	public String getDisplayName() {
		return (String) getData("displayName");
	}

	public void setDisplayName(String displayName) {
		setData("displayName", displayName);
	}

	public int getLevelMin() {
		return (int) getData("levelMin");
	}

	public void setLevelMin(int levelMin) {
		setData("levelMin", Integer.valueOf(levelMin));
	}
}
