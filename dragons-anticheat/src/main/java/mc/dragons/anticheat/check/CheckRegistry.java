package mc.dragons.anticheat.check;

import java.util.List;

public class CheckRegistry {
	private List<Check> checks;
	
	public Check getCheckByName(String name) {
		for(Check check : checks) {
			if(check.getName().equalsIgnoreCase(name)) {
				return check;
			}
		}
		return null;
	}
	
	public void registerCheck(Check check) {
		checks.add(check);
	}
}
