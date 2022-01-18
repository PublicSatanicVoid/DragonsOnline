package mc.dragons.anticheat.check;

import java.util.ArrayList;
import java.util.List;

import mc.dragons.core.gameobject.user.User;

public class CheckRegistry {
	private List<Check> checks = new ArrayList<>();

	
	public Check getCheckByName(String name) {
		for(Check check : checks) {
			if(check.getName().equalsIgnoreCase(name)) {
				return check;
			}
		}
		return null;
	}
	
	public List<Check> getChecks() {
		return checks;
	}
	
	public void registerCheck(Check check) {
		checks.add(check);
	}
	
	public boolean runChecks(CheckType checkType, User user) {
		boolean okay = true;
		for(Check check : checks) {
			if(check.getType() != checkType) continue;
			if(!check.check(user)) {
				okay = false;
			}
		}
		return okay;
	}
}
