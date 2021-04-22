package mc.dragons.core.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/**
 * Miscellaneous utilities for block handling.
 * 
 * @author Adam
 *
 */
public class BlockUtil {
	
	/**
	 * Calculates the closest block representing the ground in the XZ-plane.
	 * 
	 * @param start
	 * @return
	 */
	public static Location getClosestGroundXZ(Location start) {
		Block nBelow = start.getBlock();
		Block nAbove = start.getBlock();
		int n = 0;
		while (!nBelow.getType().isSolid() && !nAbove.getType().isSolid()) {
			n++;
			nBelow = nBelow.getRelative(BlockFace.DOWN);
			nAbove = nAbove.getRelative(BlockFace.UP);
			if (n > 10) {
				break;
			}
		}
		if (nBelow.getType().isSolid()) {
			return nBelow.getLocation();
		}
		if (nAbove.getType().isSolid()) {
			return nAbove.getLocation();
		}
		return start.getBlock().getLocation();
	}
	
	/**
	 * Calculates the closest air block in the XZ-plane.
	 * 
	 * @param start
	 * @return
	 */
	public static Location getClosestAirXZ(Location start) {
		Block nBelow = start.getBlock();
		Block nAbove = start.getBlock();
		int n = 0;
		while(n < 10 && nBelow.getType() != Material.AIR && nAbove.getType() != Material.AIR) {
			n++;
			nBelow = nBelow.getRelative(BlockFace.DOWN);
			nAbove = nAbove.getRelative(BlockFace.UP);
		}
		
		if(nAbove.getType() == Material.AIR) {
			return nAbove.getLocation();
		}
		if(nBelow.getType() == Material.AIR) {
			return nBelow.getLocation();
		}
		return start.getBlock().getLocation();
	}
	
	/**
	 * Calculates the closest air block above the given block
	 * in the XZ-plane.
	 * 
	 * @param start
	 * @return
	 */
	public static Location getAirAboveXZ(Location start) {
		Block buf = start.getBlock();
		int n = 0;
		while(n < 10 && buf.getType() != Material.AIR) {
			n++;
			buf = buf.getRelative(BlockFace.UP);
		}
		if(buf.getType() == Material.AIR) {
			return buf.getLocation();
		}
		return start.getBlock().getLocation();
	}
}
