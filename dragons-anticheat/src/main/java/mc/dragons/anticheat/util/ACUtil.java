package mc.dragons.anticheat.util;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public class ACUtil {
	private static double BB_EPSILON = 0.001;
	
	public static Set<Block> getNearbyBlocks(Player player, int apothem) {
		Set<Block> blocks = new HashSet<>();
		for(int dx = -apothem; dx <= apothem; dx++) {
			for(int dy = -apothem; dy <= apothem; dy++) {
				for(int dz = -apothem; dz <= apothem; dz++) {
					blocks.add(player.getLocation().add(dx, dy, dz).getBlock());
				}
			}
		}
		return blocks;
	}
	
	// TODO there are other cases for this.
	public static boolean isSolid(Block block) {
		return block.getType().isSolid();
	}
	
	public static Set<Block> filterSolid(Set<Block> blocks) {
		Set<Block> result = new HashSet<>(blocks);
		result.removeIf(block -> !isSolid(block));
		return result;
	}
	
	public static boolean isOnGround(Player player) {
		BoundingBox translated = player.getBoundingBox().shift(0, -BB_EPSILON, 0);
		Set<BoundingBox> nearby = filterSolid(getNearbyBlocks(player, 1)).stream().map(block -> block.getBoundingBox()).collect(Collectors.toSet());
		for(BoundingBox block : nearby) {
			if(block.overlaps(translated)) return true;
		}
		return false;
	}
}
