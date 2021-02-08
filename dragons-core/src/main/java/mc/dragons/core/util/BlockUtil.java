package mc.dragons.core.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

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
			if (n > 10)
				break;
		}
		if (nBelow.getType().isSolid())
			return nBelow.getLocation();
		if (nAbove.getType().isSolid())
			return nAbove.getLocation();
		return start.getBlock().getLocation();
	}
}
