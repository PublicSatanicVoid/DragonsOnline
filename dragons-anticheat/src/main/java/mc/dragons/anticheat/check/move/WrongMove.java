package mc.dragons.anticheat.check.move;

import mc.dragons.anticheat.DragonsAntiCheat;
import mc.dragons.anticheat.check.Check;
import mc.dragons.anticheat.check.CheckType;
import mc.dragons.anticheat.check.ViolationData;
import mc.dragons.core.gameobject.user.User;

public class WrongMove extends Check {
//	private final double VL_THRESHOLD = 10;
	private final double VL_FACTOR = 0.99;
//	private final double VL_RUBBERBAND = 5;
	
	public WrongMove(DragonsAntiCheat plugin) {
		super(plugin);
	}

	@Override
	public String getName() {
		return "WrongMove";
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
//		Player player = user.getPlayer();
//		MoveData moveData = MoveData.of(user);
		ViolationData violationData = ViolationData.of(this, user);
		
		if(!enabled) {
			violationData.vl *= VL_FACTOR;
			return true;
		}
		
//		MoveEntry lastMove = moveData.moveHistory.get(moveData.moveHistory.size() - 1);
//		Vector delta = lastMove.getDelta();
		
//		double dx = delta.getX();
//		double dy = delta.getY();
//		double dz = delta.getZ();
		
//		 user.debug("AC: <" + MathUtil.round(dx) + ", " + MathUtil.round(dy) + ", " + MathUtil.round(dz) + ">");
		
		
		// If we reached here, the move is legitimate
		violationData.vl *= VL_FACTOR;
		
		return true;
	}

}
