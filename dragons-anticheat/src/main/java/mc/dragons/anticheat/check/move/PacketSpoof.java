package mc.dragons.anticheat.check.move;

import org.bson.Document;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import mc.dragons.anticheat.DragonsAntiCheat;
import mc.dragons.anticheat.check.Check;
import mc.dragons.anticheat.check.CheckType;
import mc.dragons.anticheat.check.ViolationData;
import mc.dragons.anticheat.check.move.MoveData.MoveEntry;
import mc.dragons.anticheat.util.ACUtil;
import mc.dragons.core.gameobject.user.User;

public class PacketSpoof extends Check {
	public PacketSpoof(DragonsAntiCheat plugin) {
		super(plugin);
	}

	private final double VL_THRESHOLD = 10;
	private final double VL_FACTOR = 0.5;
	
	@Override
	public String getName() {
		return "PacketSpoof";
	}
	
	@Override
	public CheckType getType() {
		return CheckType.MOVING;
	}

	@Override
	public void setup() {
		// Nothing to do yet
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean check(User user) {
		Player player = user.getPlayer();
		MoveData moveData = MoveData.of(user);
		ViolationData violationData = ViolationData.of(this, user);
		if(!enabled) {
			violationData.vl *= VL_FACTOR;
			return true;
		}
		
		MoveEntry lastMove = moveData.moveHistory.get(moveData.moveHistory.size() - 1);
		Vector delta = lastMove.getDelta();
		boolean isActuallyOnGround = ACUtil.isOnGround(player);
		
		double dy = delta.getY();
		
		if(player.isOnGround() && isActuallyOnGround && dy < 0.0) {
			violationData.raiseVl(VL_THRESHOLD, () -> new Document("dy", dy));
			return false;
		}
		else {
			violationData.vl *= VL_FACTOR;
			return true;
		}
	}
	
}
