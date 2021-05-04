package mc.dragons.core.gameobject.floor;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;

/**
 * Represents a world with a unique name and level requirement.
 * 
 * There is typically a specific build style, set of regions and
 * NPCs, and dungeon/boss associated with each floor as well.
 * 
 * In production, all player-accessible floors should be in one-to-one
 * relation with a level requirement, so that they can be identified
 * by this number as well as their name. However, this will not be
 * programmatically enforced.
 * 
 * @author Adam
 *
 */
public class Floor extends GameObject {
	
	/**
	 * The current state of a floor.
	 * 
	 * <p>Editability and accessibility are affected
	 * by these settings, which also serve as benchmarks
	 * for progress on development.
	 * 
	 * @author Adam
	 *
	 */
	public enum FloorStatus {
		FROZEN(true, true),
		DISABLED(false, true),
		DEVELOPMENT(false, true),
		CONFIGURING(false, false),
		TERRAFORMING(false, false),
		TERRAIN(false, false),
		BUILDING(false, false),
		CONTENT(false, false),
		TESTING(false, false),
		REVIEWING(true, false),
		LIVE(false, false);
		
		private boolean gmLock;
		private boolean playerLock;
		
		FloorStatus(boolean gmLock, boolean playerLock) {
			this.gmLock = gmLock;
			this.playerLock = playerLock;
		}
		
		public boolean isGMLocked() {
			return gmLock;
		}
		
		public boolean isPlayerLocked() {
			return playerLock;
		}
	}
	
	public static FloorStatus DEFAULT_STATUS = FloorStatus.CONFIGURING;
	
	public Floor(StorageManager storageManager, StorageAccess storageAccess, boolean superflat) {
		super(storageManager, storageAccess);
		LOGGER.verbose("Constructing floor (" + storageManager + ", " + storageAccess + ", superflat=" + superflat + ")");
		LOGGER.debug("Loading floor " + getFloorName() + " [" + getWorldName() + "]");
		WorldCreator creator = WorldCreator.name(getWorldName());
		if (superflat) {
			creator.type(WorldType.FLAT);
		}
		World world = Bukkit.createWorld(creator);
		world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
		world.setGameRule(GameRule.COMMAND_BLOCK_OUTPUT, false);
		world.setGameRule(GameRule.DO_MOB_LOOT, false);
		world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
		world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
		world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
		world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
		world.setGameRule(GameRule.DO_ENTITY_DROPS, false);
		world.setGameRule(GameRule.DO_TILE_DROPS, false);
		world.setGameRule(GameRule.DO_FIRE_TICK, false);
		world.setGameRule(GameRule.KEEP_INVENTORY, true);
		world.setGameRule(GameRule.MOB_GRIEFING, false);
		world.setGameRule(GameRule.REDUCED_DEBUG_INFO, !Dragons.getInstance().isDebug());
		world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
	}

	/**
	 * 
	 * @return The name of the world file associated with this floor
	 */
	public String getWorldName() {
		return (String) getData("worldName");
	}

	/**
	 * 
	 * @return The Bukkit world associated with this floor
	 */
	public World getWorld() {
		return Bukkit.getWorld(getWorldName());
	}

	/**
	 * 
	 * @return The internal (GM) name associated with this floor
	 */
	public String getFloorName() {
		return (String) getData("floorName");
	}

	/**
	 * Change the internal (GM) name associated with this floor
	 * 
	 * @implSpec Does not update references to this name.
	 * 
	 * @param floorName
	 */
	public void setFloorName(String floorName) {
		FloorLoader.updateFloorName(getFloorName(), floorName);
		setData("floorName", floorName);
	}
	
	/**
	 * 
	 * @return The current state of this floor in its lifecycle.
	 */
	public FloorStatus getFloorStatus() {
		return FloorStatus.valueOf((String) getData("status"));
	}
	
	/**
	 * Change the current state of this floor in its lifecycle.
	 * 
	 * @param status
	 */
	public void setFloorStatus(FloorStatus status) {
		setData("status", status.toString());
	}
	
	/**
	 * 
	 * @return Whether this floor is locked from editing
	 * by builders or GMs.
	 */
	public boolean isGMLocked() {
		// Coupled to floor status, for now.
		return getFloorStatus().isGMLocked();
	}
	
	/**
	 * 
	 * @return Whether this floor is open to players.
	 */
	public boolean isPlayerLocked() {
		return getFloorStatus().isPlayerLocked();
	}

	/**
	 * 
	 * @return The publicly shown display name of this floor.
	 */
	public String getDisplayName() {
		return (String) getData("displayName");
	}

	/**
	 * Change the publicly shown display name of this floor.
	 * 
	 * @param displayName
	 */
	public void setDisplayName(String displayName) {
		setData("displayName", displayName);
	}

	/**
	 * 
	 * @return The minimum level for players to go to this floor.
	 */
	public int getLevelMin() {
		return (int) getData("levelMin");
	}

	/**
	 * Change the minimum level for players to go to this floor.
	 * 
	 * @param levelMin
	 */
	public void setLevelMin(int levelMin) {
		setData("levelMin", levelMin);
	}
	
	/**
	 * While a player is on a volatile floor, their location is not saved. 
	 * Thus, if they rejoin, they will go to their last saved location 
	 * <i>not</i> on that floor.
	 * 
	 * <p>This is useful for floors like the Duel floor where rejoining on 
	 * that floor would cause phasing issues with other players.
	 * 
	 * @return whether the floor is volatile.
	 */
	public boolean isVolatile() {
		return (boolean) getData("volatile");
	}
	
	/**
	 * Change the volatility status of this floor.
	 * 
	 * @param isVolatile
	 */
	public void setVolatile(boolean isVolatile) {
		setData("volatile", isVolatile);
	}
}
