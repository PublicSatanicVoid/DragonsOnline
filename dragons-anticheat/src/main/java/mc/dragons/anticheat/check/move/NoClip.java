package mc.dragons.anticheat.check.move;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import mc.dragons.anticheat.DragonsAntiCheat;
import mc.dragons.anticheat.check.Check;
import mc.dragons.anticheat.check.CheckType;
import mc.dragons.anticheat.check.ViolationData;
import mc.dragons.anticheat.util.ACUtil;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.util.MathUtil;
import mc.dragons.core.util.StringUtil;

public class NoClip extends Check {
	private final double VL_THRESHOLD = 10;
	private final double VL_FACTOR = 0.99;
	private final double VL_RUBBERBAND = 5;
	private final double VOLUME_THRESHOLD = 0.05;
	private final Set<Material> EXCLUDED_TYPES = Arrays.stream(Material.values())
			.filter(material -> StringUtil.containsAnyIgnoreCase(material.toString(), "DOOR", "STAIRS"))
			.collect(Collectors.toSet());
	
	public NoClip(DragonsAntiCheat plugin) {
		super(plugin);
	}
	
	
	@Override
	public String getName() {
		return "NoClip";
	}
	
	@Override
	public CheckType getType() {
		return CheckType.MOVING;
	}

	@Override
	public void setup() {
		// Nothing to do yet
	}

	@Override
	public boolean check(User user) {
		Player player = user.getPlayer();
		ViolationData violationData = ViolationData.of(this, user);
		if(player.getGameMode() == GameMode.SPECTATOR && !plugin.isDebug()) return true; // Spectators can NoClip legitimately
		if(!enabled) {
			violationData.vl *= VL_FACTOR;
			return true;
		}
		MoveData moveData = MoveData.of(user);
		BoundingBox playerBB = player.getBoundingBox();
		Set<Block> nearby = ACUtil.filterSolid(ACUtil.getNearbyBlocks(player, 1));
		for(Block block : nearby) {
			if(!EXCLUDED_TYPES.contains(block.getType()) && block.getBoundingBox().overlaps(playerBB)) {
				double volume = block.getBoundingBox().intersection(playerBB).getVolume();
				if(volume < VOLUME_THRESHOLD) {
					return true;
				}
				if(violationData.vl >= VL_RUBBERBAND) {
					if(plugin.isDebug()) {
						plugin.debug(player, "NoClip | Lagback (" + MathUtil.round(volume) + "volume, " + violationData.vl + "vl)");
					}
					
					moveData.rubberband();
				}
				violationData.raiseVl(VL_THRESHOLD, () -> new Document("type", block.getType().toString())
					.append("intersectionVolume", MathUtil.round(volume))
					.append("playerLoc", StorageUtil.locToDoc(player.getLocation()))
					.append("blockLoc", StorageUtil.locToDoc(block.getLocation())));
				return false;
			}
		}
		violationData.vl *= VL_FACTOR;
		return true;
	}

}
