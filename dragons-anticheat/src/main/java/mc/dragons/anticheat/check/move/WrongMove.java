package mc.dragons.anticheat.check.move;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import mc.dragons.anticheat.check.Check;
import mc.dragons.anticheat.check.CheckType;
import mc.dragons.anticheat.check.ViolationData;
import mc.dragons.anticheat.check.move.MoveData.MoveEntry;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.MathUtil;
import mc.dragons.tools.moderation.report.ReportLoader.Report;

public class WrongMove implements Check {

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
		
	}

	@Override
	public boolean check(User user) {
		Player player = user.getPlayer();
		MoveData moveData = MoveData.of(user);
		ViolationData violationData = ViolationData.of(this, user);
		
		MoveEntry lastMove = moveData.moveHistory.get(moveData.moveHistory.size() - 1);
		Vector delta = lastMove.getDelta();
		
		double dx = delta.getX();
		double dy = delta.getY();
		double dz = delta.getZ();
		
		user.debug("AC: <" + MathUtil.round(dx) + ", " + MathUtil.round(dy) + ", " + MathUtil.round(dz) + ">");
		
		
		
		return true;
	}

}
